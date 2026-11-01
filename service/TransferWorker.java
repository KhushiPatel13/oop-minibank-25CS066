package service;

import exception.BankException;
import model.Account;

public class TransferWorker implements Runnable {
    private final Account source;
    private final Account destination;
    private final int transferCount;
    private final long amount;

    public TransferWorker(Account source, Account destination, int transferCount, long amount) {
        this.source = source;
        this.destination = destination;
        this.transferCount = transferCount;
        this.amount = amount;
    }

    @Override
    public void run() {
        for (int i = 0; i < transferCount; i++) {
            try {
                source.transfer(destination, amount);
            } catch (BankException exception) {
                System.err.println("Transfer failed: " + exception.getMessage());
                return;
            }
        }
        System.out.println(Thread.currentThread().getName() + " completed transfers.");
    }
}
