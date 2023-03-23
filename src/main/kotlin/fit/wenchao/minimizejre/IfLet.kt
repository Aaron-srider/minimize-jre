package fit.wenchao.minimizejre

class IfLet {
}

inline fun <T> T.iflet(notNullBlock: (T) -> Unit): Else {
    var exeElse: Boolean
    if (this != null) {
        notNullBlock(this);
        exeElse = false
    } else {
        exeElse = true
    }
    return Else(exeElse)
}


class Else(var exeElse: Boolean) {

    inline fun elselet(closure: () -> Unit) {
        if (exeElse) {
            closure()
        }
    }
}

inline fun <T> T.letNull(processNullBlock: () -> Unit): T {
    if (this == null) {
        processNullBlock()
    }

    return this
}
