package mpp.counter

import kotlinx.atomicfu.AtomicIntArray
import java.lang.Math.abs
import kotlin.random.Random

class ShardedCounter {
    private val counters = AtomicIntArray(ARRAY_SIZE)

    /**
     * Atomically increments by one the current value of the counter.
     */
    fun inc() {
        val rd = kotlin.math.abs(Random.nextInt()) % ARRAY_SIZE;
        counters[rd].incrementAndGet();
        //TODO("implement me!")
        // TODO: use Random.nextInt() % ARRAY_SIZE to choose the cell
    }

    /**
     * Returns the current counter value.
     */
    fun get(): Int {
        //TODO("implement me!")
        var acc = 0
        for (i in 0 until ARRAY_SIZE) {
            acc += counters[i].value;
        }
        return acc;
    }
}

private const val ARRAY_SIZE = 2 // DO NOT CHANGE ME