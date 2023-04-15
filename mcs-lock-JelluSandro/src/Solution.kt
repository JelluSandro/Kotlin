import java.util.concurrent.atomic.AtomicReference

class Solution(val env: Environment) : Lock<Solution.Node> {
    class Node {
        val curThread = Thread.currentThread();
        val lock = AtomicReference(false);
        val next : AtomicReference<Node> = AtomicReference(null);
    }

    private val lst : AtomicReference<Node> = AtomicReference(null)

    override fun lock(): Node {
        val newNode = Node();
        newNode.lock.getAndSet(true);
        val pr = lst.getAndSet(newNode)
        if (pr != null) {
            pr.next.getAndSet(newNode)
            while (newNode.lock.get()) {
                env.park()
            }
        }
        return newNode;
    }

    override fun unlock(node: Node) {
        if (node.next.value === null) {
            if (lst.compareAndSet(node, null)) {
                return
            }
        }
        while (node.next.value === null) {
            //pass
        }
        node.next.value.lock.getAndSet(false)
        env.unpark(node.next.value.curThread)
    }
}