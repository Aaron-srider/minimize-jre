package fit.wenchao.minimizejre

import jnr.posix.POSIXFactory
import org.springframework.stereotype.Component
import java.io.*
import javax.annotation.PostConstruct


@Component
class Main {

    @PostConstruct
    fun process() {

        // class列表文件
        val classesFilePath: String = "classes.txt"
        // rt文件夹
        var rtPath: String = "rt"
        // 工作目录
        var newRtPath = "newrt"
        // 检查目录
        rtPath = addSuffixSplash(rtPath)
        newRtPath = addSuffixSplash(newRtPath)
        println("当前工作目录：$newRtPath")
        try {
            println("开始复制class")
            val count: Int = copyClasses(classesFilePath, rtPath, newRtPath)
            println("已复制：$count")

            val baseCreateJarCommand = arrayOf("jar", "cvf", "newrt.jar")
            val packagesToAddToJarFile =
                arrayOf("com", "java", "javax", "jdk", "META-INF", "org", "sun")
            val createJarCommand =
                concatArray(baseCreateJarCommand, packagesToAddToJarFile)
            intoDir("newrt")
            Runtime.getRuntime().exec(createJarCommand)
            println("正在后台打包rt.jar")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun intoDir(targetDir: String) {
        POSIXFactory.getPOSIX().chdir(targetDir);
    }

    private fun copyClasses(
        classesFilePath: String,
        rtPath: String,
        newRtPath: String
    ): Int {
        if (!File(classesFilePath).exists()) {
            return -1
        }

        var count = 0

        // scan each "[Loaded" line, extract .class file path, copy the class file to
        // target path
        BufferedReader(FileReader(classesFilePath)).use {
            var line: String? = ""

            while (it.readLine()?.also { line = it } != null) {
                extractRelativeClassFilePathFromLine(line!!)?.let {
                    val oldPath = rtPath + it
                    val newPath = newRtPath + it
                    println("复制：$newPath")
                    // 复制class
                    copyFile(oldPath, newPath)
                    count++
                }
            }
        }

        if (count > 0) {
            // 复制META-INF
            copyFolder(rtPath + "META-INF", newRtPath + "META-INF")
        }

        return count


    }

    private fun copyFolder(oldPath: String, newPath: String) {
        try {
            // 如果文件夹不存在 则建立新文件夹
            File(newPath).mkdirs()
            var temp: File
            File(oldPath).list()?.letNull { return }?.let { files ->
                for (filename: String in files) {
                    temp = if (oldPath.endsWith(File.separator)) {
                        File(oldPath + filename)
                    } else {
                        File(oldPath + File.separator + filename)
                    }
                    if (temp.isFile) {
                        val input = FileInputStream(temp)
                        val output =
                            FileOutputStream(newPath + File.separator + temp.name)
                        val b = ByteArray(1024 * 5)
                        var len: Int
                        while (input.read(b).also { len = it } != -1) {
                            output.write(b, 0, len)
                        }
                        output.flush()
                        output.close()
                        input.close()
                    }
                    if (temp.isDirectory) {
                        // 如果是子文件夹
                        copyFolder("$oldPath/$filename", "$newPath/$filename")
                    }
                }
            }

        } catch (e: Exception) {
            println("复制文件夹出错")
            e.printStackTrace()
        }
    }

    private fun copyFile(oldPath: String, newPath: String) {
        try {
            val oldFile = File(oldPath)
            if (!oldFile.exists()) {
                return
            }


            val newFolderPath =
                newPath.substring(0, newPath.lastIndexOf(File.separator))
            // 目标路径不存在时自动创建文件夹
            File(newFolderPath).mkdirs()
            // 文件存在时读入原文件
            val inStream: InputStream = FileInputStream(oldPath)
            val fs = FileOutputStream(newPath)
            val buffer = ByteArray(1024)
            var byteSum = 0
            var byteRead: Int
            while (inStream.read(buffer).also { byteRead = it } != -1) {
                // 字节数(文件大小)
                byteSum += byteRead
                fs.write(buffer, 0, byteRead)
            }
            inStream.close()
        } catch (e: Exception) {
            println("复制单个文件出错")
            e.printStackTrace()
        }
    }


    private fun extractRelativeClassFilePathFromLine(s: String): String? {
        if (!s.contains("[Loaded ") || !s.contains("rt.jar")) {
            return null
        }

        var fullClassName: String? = substringBetween(s, "[Loaded ", " ")

        fullClassName?.let { return convertFullClassNameToRelativeClassFilePath(it) }

        return null
    }

    fun convertFullClassNameToRelativeClassFilePath(str: String): String {
        return str.replace(".", File.separator) + ".class"
    }

    /**
     * str = this is a sentence
     *
     * substringBetween(str, "this ", " ") returns "is"
     *
     */
    private fun substringBetween(str: String, open: String, close: String): String? {
        return run {
            // no start point, target does not exist
            val start = str.indexOf(open)
            if (start == -1) {
                return null
            }


            val end = str.indexOf(close, start + open.length)
            // no end point, target does not exist
            if (end == -1) {
                return null
            }

            // return between substring
            return str.substring(start + open.length, end)
        }
    }

    private fun addSuffixSplash(path: String): String {
        if (!path.endsWith(File.separator)) {
            return path + File.separator
        }
        return path
    }

    private fun concatArray(arr1: Array<String>, arr2: Array<String>): Array<String> {
        val arrayOfNulls: Array<String?> = arrayOfNulls<String>(arr1.size + arr2.size)
        System.arraycopy(arr1, 0, arrayOfNulls, 0, arr1.size)
        System.arraycopy(arr2, 0, arrayOfNulls, arr1.size, arr2.size)
        return arrayOfNulls.filterNotNull().toTypedArray()
    }
}