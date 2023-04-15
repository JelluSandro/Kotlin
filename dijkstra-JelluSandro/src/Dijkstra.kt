package dijkstra

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.ReentrantLock
import java.util.*
import java.util.concurrent.Phaser
import java.util.concurrent.atomic.AtomicInteger
import kotlin.Comparator
import kotlin.concurrent.thread

private val NODE_DISTANCE_COMPARATOR = Comparator<Node> { o1, o2 -> o1!!.distance.compareTo(o2!!.distance) }
private val random = Random()

// Returns `Integer.MAX_VALUE` if a path has not been found.
fun shortestPathParallel(start: Node) {
    val workers = Runtime.getRuntime().availableProcessors()
    // The distance to the start node is `0`
    start.distance = 0
    // Create a priority (by distance) queue and add the start node into it
    val q = MultiQueue(workers, NODE_DISTANCE_COMPARATOR) // TODO replace me with a multi-queue based PQ!
    q.add(start)
    // Run worker threads and wait until the total work is done
    val onFinish = Phaser(workers + 1) // `arrive()` should be invoked at the end by each worker
    val activeNodes = AtomicInteger(1)
    repeat(workers) {
        thread {
            while (activeNodes.get() > 0) {
                // TODO Write the required algorithm here,
                // TODO break from this loop when there is no more node to process.
                // TODO Be careful, "empty queue" != "all nodes are processed".
                val u: Node = q.poll() ?: continue
                for (v in u.outgoingEdges) {
                    while (true) {
                        val dv = v.to.distance
                        val d = u.distance + v.weight
                        if (dv > d) {
                            if (v.to.casDistance(dv, d)) {
                                q.add(v.to)
                                activeNodes.incrementAndGet()
                                break
                            } else {
                                continue
                            }
                        }
                        break
                    }
                }
                activeNodes.decrementAndGet()
//                val cur: Node? = synchronized(q) { q.poll() }
//                if (cur == null) {
//                    if (workIsDone) break else continue
//                }
//                for (e in cur.outgoingEdges) {
//                    if (e.to.distance > cur.distance + e.weight) {
//                        e.to.distance = cur.distance + e.weight
//                        q.addOrDecreaseKey(e.to)
//                    }
//                }
            }
            onFinish.arrive()
        }
    }
    onFinish.arriveAndAwaitAdvance()
}

private class Pair<Node : Any>(private val lock_: ReentrantLock, private val queue_: PriorityQueue<Node>) {
    private val lock = lock_
    private val queue = queue_

    fun getLock(): ReentrantLock {
        return lock;
    }

    fun getQueue(): PriorityQueue<Node> {
        return queue;
    }
}


private class MultiQueue<Node : Any>(private val workers: Int, private val comparator: java.util.Comparator<Node>) {

    private val queues: MutableList<dijkstra.Pair<Node>> =
        (Collections.nCopies(workers, dijkstra.Pair<Node>(ReentrantLock(), PriorityQueue(comparator))))

    fun poll(): Node? {
        var i = 0
        var j = 0
        while (i == j) {
            i = random.nextInt(workers)
            j = random.nextInt(workers)
        }
        if (j < i) {
            val x = j
            j = i
            i = x
        }
        val qi = queues[i]
        val qj = queues[j]
        val lockI = qi.getLock()
        val lockJ = qj.getLock()
        val b1 = queues[i].getQueue().peek()
        val b2 = queues[j].getQueue().peek()
        try {
            lockI.lock()
            lockJ.lock()
            return if (b1 == null && b2 == null) {
                null
            } else if (b1 == null) {
                queues[j].getQueue().poll()
            } else if (b2 == null) {
                queues[i].getQueue().poll()
            } else {
                if (comparator.compare(b1, b2) >= 0) {
                    queues[j].getQueue().poll()
                } else {
                    queues[i].getQueue().poll()
                }
            }
        } finally {
            lockI.unlock()
            lockJ.unlock()
        }
    }


    fun add(x: Node) {
        val it = random.nextInt(workers)
        val lock = queues[it].getLock()
        try {
            lock.lock()
            queues[it].getQueue().add(x);
        } finally {
            lock.unlock()
        }
    }

}