import kotlinx.atomicfu.AtomicInt
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * An element is transferred from sender to receiver only when [send] and [receive]
 * invocations meet in time (rendezvous), so [send] suspends until another coroutine
 * invokes [receive] and [receive] suspends until another coroutine invokes [send].
 */
class SynchronousQueue<E> {

    private class Node<E>(_type: Int, x: E?) {
        val next: AtomicRef<Node<E>?> = atomic(null)
        val flag: AtomicRef<Continuation<Int>?> = atomic(null);
        val type: AtomicInt = atomic(_type)
        val el: AtomicRef<E?> = atomic(x)
    }

    private val head: AtomicRef<Node<E>>
    private val tail: AtomicRef<Node<E>>

    init {
        val newNode = Node<E>(-1, null)
        head = atomic(newNode)
        tail = atomic(newNode)
    }

    /**
     * Sends the specified [element] to this channel, suspending if there is no waiting
     * [receive] invocation on this channel.
     */

    private suspend fun sus(newNode: Node<E>, curTail: Node<E>): Int {
        return suspendCoroutine suspendCorutin@{ core ->
            newNode.flag.value = core
            if (curTail.next.compareAndSet(null, newNode)) {
                tail.compareAndSet(curTail, newNode)
            } else {
                tail.compareAndSet(curTail, curTail.next.value!!)
                core.resume(1)
                return@suspendCorutin
            }
        }
    }

    private suspend fun amogus(x: Int, element: E?) : E? {
        while (true) {
            val curHead = head.value
            val curTail = tail.value
            if (curTail == curHead || curTail.type.value == x) {
                val newNode = Node(x, element)
                if (sus(newNode, curTail) != 1) {
                    if (x == 1) {
                        break
                    } else {
                        return newNode.el.value!!
                    }
                }
            } else if (curHead.next.value != null) {
                val curHeadnext = curHead.next.value
                if (curHeadnext!!.flag.value != null && head.compareAndSet(curHead, curHeadnext)) {
                    if (x == 1) {
                        curHeadnext.el.compareAndSet(curHeadnext.el.value, element)
                    }
                    curHeadnext.flag.value?.resume(0)
                    if (x == 1) {
                        break
                    } else {
                        return curHeadnext.el.value!!
                    }
                }
            }
        }
        return null
    }

    suspend fun send(element: E) {
        val xx = amogus(1, element)
    }
    suspend fun receive(): E {
        return amogus(0, null)!!
    }

    /**
     * Retrieves and removes an element from this channel if there is a waiting [send] invocation on it,
     * suspends the caller if this channel is empty.
     */
}