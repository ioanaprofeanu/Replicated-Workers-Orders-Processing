// Profeanu Ioana, 333CA
import java.io.*;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class OrderWorker extends Worker implements Runnable {
    // the replicated models variables for the product workers
    AtomicInteger inQueueProducts;
    ExecutorService tpeProducts;
    // the start and end of the chunk of bytes the current
    // worker thread has to read from
    int start;
    int end;

    public OrderWorker(ExecutorService tpe, AtomicInteger inQueue,
                       AtomicInteger inQueueProducts,
                       ExecutorService tpeProducts, int start, int end) {
        super(tpe, inQueue);
        this.inQueueProducts = inQueueProducts;
        this.tpeProducts = tpeProducts;
        this.start = start;
        this.end = end;
    }

    /**
     * Run method
     */
    @Override
    public void run() {
        // create read buffer and open the file
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(Tema2.ordersInputFile));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        // skip bytes until reaching the wanted portion of the file
        try {
            reader.skip(start);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // track the current position in file and the number of bytes read
        int positionInFile = start;
        // while the position in file hasn't reached the end of the chunk
        while (positionInFile < end) {
            // string builder to keep the order in
            StringBuilder orderLine = new StringBuilder();
            // if it is the first order that is read, check if it is a full
            // order; read it char by char (equal to byte by byte)
            if (positionInFile == start) {
                while (true) {
                    char[] readByte = new char[1];
                    try {
                        // read the byte and increase the position in file;
                        int check = reader.read(readByte);
                        positionInFile++;
                        // check if it reaches eof
                        if (check == -1) {
                            return;
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    // if the byte is a newline,
                    // then the whole order has been read
                    if (readByte[0] == '\n') {
                        break;
                    }

                    orderLine.append(readByte[0]);
                }
                // if the file position is not at the start
            } else {
                String line;
                try {
                    // read the line and increase the position in file
                    line = reader.readLine();
                    positionInFile += line.length() + 1;
                    // if the line is null, it means the end of file was reached
                    if (line == null) {
                        return;
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                orderLine.append(line);
            }

            // since each order starts with "o_", if the read order doesn't contain it,
            // it means the order is not full; drop it and continue
            if (!orderLine.toString().contains("o_")) {
                continue;
            }

            // split the line by the comma and retrieve the order name and
            // the number of products in the order
            String[] parseLine = orderLine.toString().split(",");
            String orderName = parseLine[0];
            int numberOfProducts = Integer.parseInt(parseLine[1]);
            // if the order contains products
            if (numberOfProducts > 0) {
                // initialize semaphore so that the current order worker
                // can enter the semaphore only after all the product worker
                // tasks it has initialized have been completed
                Semaphore semCheckFinishedOrder = new Semaphore(-numberOfProducts + 1);
                // add task in the working pool for product workers,
                // to look for the product at a certain index in the order
                for (int j = 0; j < numberOfProducts; j++) {
                    inQueueProducts.getAndIncrement();
                    tpeProducts.submit(new ProductWorker(tpeProducts, inQueueProducts,
                            orderName, j, semCheckFinishedOrder, false));
                }

                // try to pass the semaphore
                try {
                    semCheckFinishedOrder.acquire();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                // write in the output file for orders
                try {
                    writeOrderShipping(orderLine.toString() + ",shipped");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }
        }

        // wait for the other threads to finnish
        try {
            Tema2.orderWorkersBarrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            throw new RuntimeException(e);
        }

        // dequeue from the tpe queue
        int left = getInQueue().decrementAndGet();
        // the last thread has to shut down the tpes
        if (left == 0) {
            // create a product worker task to shut down the product workers tpe
            inQueueProducts.getAndIncrement();
            tpeProducts.submit(new ProductWorker(tpeProducts, inQueueProducts,true));
            getTpe().shutdown();
        }

        try {
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Synchronized method to write in the output file; only
     * one thread can write at a time
     * @param toWriteLine the line to read
     */
    public synchronized void writeOrderShipping (String toWriteLine) throws IOException {
        // open the file in append mode
        FileWriter fileWriter = new FileWriter("orders_out.txt", true);
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
