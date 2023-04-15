import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.atomicArrayOfNulls
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList
import java.util.Random
//1.1 + 021
//Nikita time 11 mine for compile
class FCPriorityQueue<E : Comparable<E>> {
    private val q = PriorityQueue<E>()
    private val size = 42 //42
    private val fcArray = VectorFC<E>(size)
    private val random = Random()
    /**
     * Retrieves the element with the highest priority
     * and returns it as the result of this function;
     * returns `null` if the queue is empty.
     */
    fun poll(): E? {
        return f(RequestFC<E>(1, null));
        //val x = Flag()
    //return q.poll()
    }

    /**
     * Returns the element with the highest priority
     * or `null` if the queue is empty.
     */
    fun peek(): E? {
        //return f(RequestFC(2, null));
        return f(RequestFC<E>(3, null));
    }

    /**
     * Adds the specified element to the queue.
     */
    fun add(element: E) {
        //q.add(element)
        f(RequestFC(2, element))
    }

    private fun f(requestFC: RequestFC<E>): E? {
        var rd = random.nextInt(size)
        while (!fcArray.fcArray[rd].compareAndSet(null, requestFC)) {
            rd = (rd + 1) % size
        }
        //var cal = 0;
        while (true) {
           // cal++
            //if(cal > 1000) {
            //    println(cal)
             //   throw error("NEYSE PROPALO");
            //}
            if (!fcArray.lock.value) {
                if (fcArray.lock.compareAndSet(false, true)) {
                    for (i in 0 until size) {
                        val requestCurent = fcArray.fcArray[i].value ?: continue
                        val tCur = requestCurent.t
                        if (tCur == 1 || tCur == 2 || tCur == 3) {
                            if (fcArray.fcArray[i].compareAndSet(requestCurent, RequestFC(4, requestCurent.e))) {
                                var res = requestCurent.e
                                when (tCur) {
                                    1 -> {
                                        res = q.poll()
                                    }
                                    2 -> {
                                        q.add(requestCurent.e)
                                    }
                                    else -> {
                                        res = q.peek()
                                    }
                                }
                                fcArray.fcArray[i].getAndSet(RequestFC(5, res))
                            }
                        }
                    }
                    fcArray.lock.value = false
                }
            }
            val ff = fcArray.fcArray[rd].value
            if (ff!!.t == 5) {
                fcArray.fcArray[rd].getAndSet(null)
                return ff.e
            }
        }
    }
    //operation, element
    private class Type<E>(val x1: E?, val y1: Int) {
        val x = x1
        val y = y1
    }

    private class VectorFC<E>(size:Int) {
        val fcArray = atomicArrayOfNulls<RequestFC<E>>(size)
        val lock = atomic(false)
    }

    private class Flag {
        var flag: Boolean = false
        var ans: Any? = null
    }

    private class RequestFC<E>(type:Int, el:E?) {
        val t = type
        var e = el
    }
}