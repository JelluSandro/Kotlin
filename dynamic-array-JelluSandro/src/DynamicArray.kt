package mpp.dynamicarray

import kotlinx.atomicfu.*

interface DynamicArray<E> {
    /**
     * остановить все кроме одного потока один поток завершит свою раброту
     * кто то долженн всегда заканчивать свою работу
     *
     *
     */
    /**
     * Returns the element located in the cell [index],
     * or throws [IllegalArgumentException] if [index]
     * exceeds the [size] of this array.
     */
    fun get(index: Int): E

    /**
     * Puts the specified [element] into the cell [index],
     * or throws [IllegalArgumentException] if [index]
     * exceeds the [size] of this array.
     */
    fun put(index: Int, element: E)

    /**
     * Adds the specified [element] to this array
     * increasing its [size].
     */
    fun pushBack(element: E)

    /**
     * Returns the current size of this array,
     * it increases with [pushBack] invocations.
     */
    val size: Int
}

private class pairEBool<E>(el_: E, t_: Boolean) {
    private val el = el_
    private val t = t_
    public fun getE() : E {
        return el
    }
    public fun getT() : Boolean {
        return t
    }
}

class DynamicArrayImpl<E> : DynamicArray<E> {

    private val core = atomic(Core<E>(INITIAL_CAPACITY))

    override fun get(index: Int): E = core.value.get(index)

    override fun put(index: Int, element: E) = core.value.put(index, element)

    override fun pushBack(element: E) {
        val curCore = core.value
        val newCore = pp(curCore, element)
        core.compareAndSet(curCore, newCore)
    }


    private fun pp(oldCore: Core<E>, element: E) : Core<E> {
        while (true) {
            val sz = oldCore.sz()
            if (sz >= oldCore.cap()) {
                return pp(resize(oldCore), element)
            } else if (oldCore.cas(sz, element)) {
                oldCore.casSZ(sz)
                return oldCore
            }
            oldCore.casSZ(sz)
        }
    }

    override val size: Int get() = core.value.size

    private fun resize(oldCore: Core<E>) : Core<E> {
        val sz = oldCore.sz()
        val y = Core<E>(sz * 2);
        y.addSZ(sz)
        oldCore.next.compareAndSet(null, y);
        val newCore = oldCore.next.value!!
        for (it in 0 until sz) {
            while (true) {
                val x = oldCore.gt(it)
                if (x is pairEBool<*> && x.getT()) {
                    newCore.fullCAS(it, null, x.getE()!!)
                    break
                } else {
                    if (!oldCore.fullCAS(it, x as E?, pairEBool<E>(x!!, true))) {
                        continue
                    }
                    newCore.fullCAS(it, null, x)
                    break
                }
            }
        }
        return oldCore.next.value!!//TODO
    }
}

private class Core<E>(
    capacity: Int
) {
    private val array = atomicArrayOfNulls<Any>(capacity) // Any
    val next: AtomicRef<Core<E>?> = atomic(null)
    private val _size = atomic(0)

    fun sz() : Int {
        return _size.value
    }


    fun cap() : Int {
        return array.size
    }

    fun cas(sz:Int, el:E) : Boolean {
        return array[sz].compareAndSet(null, el)
    }

    fun casSZ(sz: Int) {
        _size.compareAndSet(sz, sz + 1)
    }

    fun addSZ(sz: Int) {
        _size.getAndAdd(sz)
    }


    fun gt(index: Int): Any? {
        return array[index].value
    }

    fun fullCAS(sz: Int, el1: E?, el2 : Any): Boolean {
        return array[sz].compareAndSet(el1, el2)
    }



    val size: Int
        //get() = _size.value
        get() = next.value?.size ?: _size.value


    fun get(i: Int): E {
        if (i >= array.size) {
            return next.value?.get(i)?: throw IllegalArgumentException("out")
        }
        require(i >= 0 && i < _size.value)
        val cur = array[i].value
        if (cur != null && cur is pairEBool<*> && cur.getT()) {
            return next.value?.get(i) ?: cur.getE() as E
        } else {
            return cur as E
        }
    }

    //good

    fun put(i: Int, el: E) {
        if (i >= array.size) {
            return next.value?.put(i, el)?: throw IllegalArgumentException("out")
        }
        require(i >= 0 && i < _size.value)
        while (true) {
            val cur = array[i].value
            if (cur is pairEBool<*> && cur.getT()) {
                next.value!!.put(i, el)
                return
            } else if (array[i].compareAndSet(cur, el)) {
                return
            }
        }
    }

}

private const val INITIAL_CAPACITY = 1 // DO NOT CHANGE ME