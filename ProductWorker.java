// Profeanu Ioana, 333CA
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class which contains the implementation of a level 2 thread
 * that parses the products data
 */
public class ProductWorker extends Worker implements Runnable {
    // order name
    String orderName;
    // the index of the product (its position in the current
    // order)
    Integer productIndex;
    // semaphore for the level 1 worker to know if all level 2
    // workers it launched have finished
    Semaphore semCheckFinishedOrder;
    // whether the tpe for product workers should shut down
    boolean shutdown;

    public ProductWorker(ExecutorService tpe, AtomicInteger inQueue, String orderName,
                         Integer productIndex, Semaphore semCheckFinishedOrder,
                         boolean shutdown) {
        super(tpe, inQueue);
        this.orderName = orderName;
        this.productIndex = productIndex;
        this.semCheckFinishedOrder = semCheckFinishedOrder;
        this.shutdown = shutdown;
    }

    // constructor for initializing the worker which shuts down the tpe
    public ProductWorker(ExecutorService tpe, AtomicInteger inQueue, boolean shutdown) {
        super(tpe, inQueue);
        this.shutdown = shutdown;
    }

    /**
     * Run method
     */
    @Override
    public void run() {
        // initialize the variables used to look up the
        // wanted product in the file
        int currentOrderProductIndex = -1;
        boolean foundProduct = false;
        String matchedProductLine = null;

        // if the thread is a shutdown thread, it shuts down
        // the tpe
        if (shutdown && getInQueue().decrementAndGet() == 0) {
            getTpe().shutdown();
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(Tema2.productsInputFile))) {
            // while the product isn't found
            while (!foundProduct) {
                // read a line
                String line;
                line = reader.readLine();
                // if the line contains the order name
                if (line.contains(orderName)) {
                    // increase the current product index within the order
                    currentOrderProductIndex++;
                    // if the product index matches the one we are looking for
                    if (currentOrderProductIndex == productIndex) {
                        foundProduct = true;
                        matchedProductLine = new String(line);
                    }
                }
            }
            // write in the output file for products
            writeProductShipping(matchedProductLine + ",shipped");
            // release the semaphore to let the parent order worker thread
            // that this current thread has finished
            semCheckFinishedOrder.release();
        } catch (IOException e) {
            e.printStackTrace();
        }
        getInQueue().decrementAndGet();
    }

    /**
     * Synchronized method to write in the output file; only
     * one thread can write at a time
     * @param toWriteLine the line to read
     * @throws IOException
     */
    public synchronized void writeProductShipping (String toWriteLine) throws IOException {
        // open the file in append mode
        FileWriter fileWriter = new FileWriter("order_products_out.txt", true);
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        PrintWriter printWriter = new PrintWriter(bufferedWriter);

        // write the string at the end
        printWriter.println(toWriteLine);

        // close writers
        printWriter.close();
        bufferedWriter.close();
        fileWriter.close();
    }
}
