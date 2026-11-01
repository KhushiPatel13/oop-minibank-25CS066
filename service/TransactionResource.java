package service;

public class TransactionResource implements AutoCloseable {
    private boolean open = true;

    public TransactionResource() {
        System.out.println("Transaction resource opened.");
    }

    public boolean isOpen() {
        return open;
    }

    @Override
    public void close() {
        open = false;
        System.out.println("Transaction resource closed.");
    }
}
