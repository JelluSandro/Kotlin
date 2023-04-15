import kotlinx.atomicfu.*

class Descriptor<E>(
    private val el1: Ref<E>, private val expectedEl1: E, private val updateEl1: E,
    private val el2: Ref<E>, private val expectedEl2: E, private val updateEl2: E
) {
    private val outcome = Ref(false)
    private var flag = true
    fun complete() {
        if (el2.ref.value != this) {
            flag = el2.cas(expectedEl2, this)
        }
        outcome.ref.compareAndSet(false, flag)
        if (flag) {
            el1.ref.compareAndSet(this, updateEl1)
            el2.ref.compareAndSet(this, updateEl2)
        } else {
            el1.ref.compareAndSet(this, expectedEl1)
            el2.ref.compareAndSet(this, expectedEl2)
        }
        //  return flag
    }
    fun get(): Boolean {
        return outcome.value
    }
}

class Ref<E>(initialValue: E) {
    val ref = atomic<Any?>(initialValue)
    var value: E
        get() {
            while (true) {
                val cur = ref.value
                if (cur is Descriptor<*>) {
                    cur.complete()
                } else {
                    return cur as E
                }
            }
        }
        set(value) {
            while (true) {
                val cur = ref.value
                if (cur is Descriptor<*>) {
                    cur.complete()
                } else {
                    if (ref.compareAndSet(cur, value)) {
                        return
                    }
                }
            }
        }

    fun cas(expected: Any?, update: Any?): Boolean {
        while (true) {
            val cur = ref.value
            if (cur is Descriptor<*>) {
                cur.complete()
            } else if (cur == expected) {
                if (ref.compareAndSet(cur, update)) {
                    return true
                }
            } else {
                return false
            }
        }
    }
}

class AtomicArrayNoAba<E>(size: Int, initialValue: E) {
    private val a = arrayOfNulls<Ref<E>>(size)

    init {
        for (i in 0 until size) a[i] = Ref(initialValue)
    }

    fun get(index: Int) =
        a[index]!!.value

    fun cas(index: Int, expected: E, update: E): Boolean = a[index]?.cas(expected, update) ?: false

    fun cas2(index1: Int, expected1: E, update1: E,
             index2: Int, expected2: E, update2: E): Boolean {
        // TODO this implementation is not linearizable,
        // TODO a multi-word CAS algorithm should be used here.
        if (index1 == index2) {
            if (expected1 == expected2) {
                return cas(index1, expected1, (expected1.toString().toInt() + 2) as E)
            }
            return false
        }
        val flag: Boolean = index1 < index2
        val a: Ref<E> = if (flag) a[index1]!! else a[index2]!!
        val b: Ref<E> = if (flag) this.a[index2]!! else this.a[index1]!!
        val a1: E = if (flag) expected1 else expected2
        val a2: E = if (flag) expected2 else expected1
        val b1: E = if (flag) update1 else update2
        val b2: E = if (flag) update2 else update1
        val descriptor = Descriptor(a, a1, b1, b, a2, b2)
        if (a.cas(a1, descriptor)) {
            descriptor.complete()
            return descriptor.get()
        }
        return false
    }
}
