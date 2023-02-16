// Profeanu Ioana, 333CA
import java.io.*;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class which contains the main method
 */
public class Tema2 {
    // variables for keeping the input file names
    static String ordersInputFile;
    static String productsInputFile;
    // the maximum number of threads per level
    static int nrMaxThreads;
    // barrier for checking if all worker threads have finished
    static CyclicBarrier orderWorkersBarrier;

    public static void main(String[] args) throws IOException {
        // retrieve the input folder name and compute the input files names
        ordersInputFile = args[0] + "/orders.txt";
        productsInputFile = args[0] + "/order_products.txt";
        // retrieve the maximum number of threads per level
        nrMaxThreads = Integer.parseInt(args[1]);

        // create the output file for orders and products;
        // create one even if it already exists
        File fileOrders = new File("orders_out.txt");
        fileOrders.createNewFile();
        FileWriter f1 = new FileWriter("orders_out.txt", false);
        f1.write("");
        f1.close();

        File fileProducts = new File("order_products_out.txt");
        fileProducts.createNewFile();
        FileWriter f2 = new FileWriter("order_products_out.txt", false);
        f2.write("");
        f2.close();

        // initialize the executor services for orders and products;
        // each of them has a thread pool equal to the maximum
        // number of threads allowed per level
        AtomicInteger inQueueOrders = new AtomicInteger(0);
        ExecutorService tpeOrders = Executors.newFixedThreadPool(Tema2.nrMaxThreads);

        AtomicInteger inQueueProducts = new AtomicInteger(0);
        ExecutorService tpeProducts = Executors.newFixedThreadPool(Tema2.nrMaxThreads);

        // initialize barrier for the order worker threads
        orderWorkersBarrier = new CyclicBarrier(nrMaxThreads);

        // get the size of the order file in bytes
        File orderInputFile = new File(ordersInputFile);
        long orderThreadsBytesToRead = orderInputFile.length();

        // for each level 1 thread (the order workers)
        for (int i = 0; i < nrMaxThreads; i++) {
            // calculate the range in between which the thread must
            // read the data
            int start = (int) (i * (double)orderThreadsBytesToRead / nrMaxThreads);
            int end = (int) Math.min((i + 1) *
                    (double)orderThreadsBytesToRead / nrMaxThreads,
                    orderThreadsBytesToRead);
            // submit the task
            inQueueOrders.incrementAndGet();
            tpeOrders.submit(new OrderWorker(tpeOrders, inQueueOrders,
                    inQueueProducts, tpeProducts, start, end));
        }
    }
}
