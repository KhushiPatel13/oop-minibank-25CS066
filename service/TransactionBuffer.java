package service;

public class TransactionBuffer {
    private final Runnable[] buffer;
    private int head;
    private int tail;
    private int size;

    public TransactionBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Buffer capacity must be positive.");
        }
        buffer = new Runnable[capacity];
    }

    public synchronized void put(Runnable transaction) throws InterruptedException {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null.");
        }
        while (size == buffer.length) {
            wait();
        }
        buffer[tail] = transaction;
        tail = (tail + 1) % buffer.length;
        size++;
        notifyAll();
    }

    public synchronized Runnable take() throws InterruptedException {
        while (size == 0) {
            wait();
        }
        Runnable transaction = buffer[head];
        buffer[head] = null;
        head = (head + 1) % buffer.length;
        size--;
        notifyAll();
        return transaction;
    }
}
