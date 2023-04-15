package mpp.faaqueue

import kotlinx.atomicfu.*


@Suppress("UNCHECKED_CAST")
class FAAQueue<E> {
    val enqIdx = atomic(0L)
    val deqIdx = atomic(0L)
    private val head: AtomicRef<Segment> // Head pointer, similarly to the Michael-Scott queue (but the first node is _not_ sentinel)
    private val tail: AtomicRef<Segment> // Tail pointer, similarly to the Michael-Scott queue

    init {
        val firstNode = Segment()
        head = atomic(firstNode)
        tail = atomic(firstNode)
    }

    //eneue - true
    //deque - false
    fun funQueue(element1: E?, flag: Boolean): E? {
        while(true) {
            if (!flag && isEmpty) {
                return null
            }
            val tailOrHead = if (flag) {
                tail.value
            } else {
                head.value
            }
            val i = (if (flag) enqIdx.getAndIncrement() else deqIdx.getAndIncrement())
            var x = tailOrHead
            while (x.count.value < i / SEGMENT_SIZE) {
                val next = x.getNext()
                if (next == null) {
                    val newSegment = Segment()
                        newSegment.count.value = x.count.value + 1
                    x.updateNext(newSegment)
                }
                x = x.getNext()!!
            }
            while (true) {
                val tOH = if (flag) {
                    tail.value
                } else {
                    head.value
                }
                if (tOH.count.value < x.count.value) {
                    if (flag) {
                        tail.compareAndSet(tail.value, x)
                    } else {
                        head.compareAndSet(head.value, x)
                    }
                } else {
                    break
                }
            }
            val md = (i % SEGMENT_SIZE).toInt()
            if (flag) {
                if (x.cas(md, null, element1)) {
                    if (flag) return null else continue
                }
                if (!flag) return x.gas(md, null) as E?
            } else {
                if (x.cas(md, null, false)) {
                    continue
                }
                return x.gas(md, null) as E?
            }
        }
    }

    /**
     * Adds the specified element [x] to the queue.
     */
    fun enqueue(element: E) {
        funQueue(element, true)
    }

    /**
     * Retrieves the first element from the queue and returns it;
     * returns `null` if the queue is empty.
     */

    fun dequeue(): E? {
        return funQueue(null, false)
    }


    /**
     * Returns `true` if this queue is empty, or `false` otherwise.
     */
    val isEmpty: Boolean
        get() {
            return deqIdx.value >= enqIdx.value
        }
}

private class Segment {
    private val next: AtomicRef<Segment?> = atomic(null)
    private val elements = atomicArrayOfNulls<Any>(SEGMENT_SIZE)
    val count = atomic(0)

    fun get(i: Int) = elements[i].value
    fun cas(i: Int, expect: Any?, update: Any?) = elements[i].compareAndSet(expect, update)
    fun put(i: Int, value: Any?) {
        elements[i].value = value
    }
    fun gas(i: Int, update: Any?) = elements[i].getAndSet(update)
    fun getNext(): Segment? {
        return next.value
    }
    fun updateNext(segment: Segment) {
        next.compareAndSet(null, segment)
    }
}

const val SEGMENT_SIZE = 2 // DO NOT CHANGE, IMPORTANT FOR TESTS

