// Profeanu Ioana, 333CA
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Common class which is inherited by the two types
 * of workers
 */
public class Worker {
    // replicated workers variables
    private final ExecutorService tpe;
    private final AtomicInteger inQueue;

    public Worker(ExecutorService tpe, AtomicInteger inQueue) {
        this.tpe = tpe;
        this.inQueue = inQueue;
    }

    public ExecutorService getTpe() {
        return tpe;
    }

    public AtomicInteger getInQueue() {
        return inQueue;
    }
}
