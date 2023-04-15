package mpp.linkedlistset

import kotlinx.atomicfu.atomic
import java.util.concurrent.atomic.AtomicMarkableReference


class LinkedListSet<E : Comparable<E>> {

    private class Window<E : Comparable<E>>(_cur: Node<E>?, _next: Node<E>?) {
        var cur: Node<E>? = _cur
        var next: Node<E>? = _next
    }

    //-1 - min value
    //0  - casual value
    //+1 - max value

    private val first = Node<E>(element = null, next = null, false, -1)
    private val last = Node<E>(element = null, next = null, false, 1)
    init {
        first.setNext(last, false)
    }

    private val head = atomic(first)


    private fun findWindow(x: E): Window<E> {
        val removed = BooleanArray(1)
        while (true) {
            var cur: Node<E>? = head.value
            var next = cur!!.next
            var flag = false
            while (next!!.type == -1 || (next.type == 0 && next.element < x)) {
                val node = next.removed(removed)
                if (removed[0]) {
                    if (cur!!.casNext(next, node, false, false)) {
                        next = node
                    } else {
                        flag = true
                        break
                    }
                } else {
                    cur = next
                    next = cur.next
                }
            }
            if (flag) {
                continue
            }
            val nextNext = next.removed(removed)
            if (removed[0]) {
                cur!!.casNext(next, nextNext, false, false)
            } else {
                return Window<E>(cur, next)
            }
        }
    }

    /**
     * Adds the specified element to this set
     * if it is not already present.
     *
     * Returns `true` if this set did not
     * already contain the specified element.
     */
    fun add(element: E): Boolean {
        while (true) {
            val w = findWindow(element)
            val wNext = w.next!!
            if (wNext.type == 0 && wNext.element == element) {
                return false
            }
            val node = Node(element, wNext, false, 0)
            if (w.cur!!.casNext(wNext, node, false, false)) {
                return true
            }
        }
    }

    /**
     * Removes the specified element from this set
     * if it is present.
     *
     * Returns `true` if this set contained
     * the specified element.
     */
    fun remove(element: E): Boolean {
        while (true) {
            val w = findWindow(element)
            val wNext = w.next!!
            if (wNext.type == -1 || wNext.type == 1 || wNext.element != element) {
                return false
            } else {
                val nextNext = wNext.next
                if (wNext.casNext(nextNext, nextNext, false, true)) {
                    wNext.casNext(wNext, nextNext, false, false)
                    return true
                }
            }
        }
    }

    /**
     * Returns `true` if this set contains
     * the specified element.
     */
    fun contains(element: E): Boolean {
        val w = findWindow(element)
        val wNext = w.next!!
        return wNext.type == 0 &&
            wNext.element == element
    }
}
private class Node<E : Comparable<E>>(element: E?, next: Node<E>?, toMark: Boolean, val type: Int) {
    private val _element = element // `null` for the first and the last nodes
    val element get() = _element!!

    private val _next = AtomicMarkableReference(next, toMark)
    val next get() = _next.reference
    fun setNext(value: Node<E>?, b1: Boolean) {
        _next.set(value, b1)
    }
    fun casNext(expected: Node<E>?, update: Node<E>?, b1:Boolean, b2:Boolean) : Boolean {
        if (_next.compareAndSet(expected, update, b1, b2)) {
            return true
        } else {
            return false
        }
    }
    fun removed(rem: BooleanArray): Node<E>? {
        return _next[rem]
    }
}