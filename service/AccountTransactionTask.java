package service;

import exception.BankException;
import model.Account;

public class AccountTransactionTask implements Runnable {
    private final Account account;
    private final long amount;
    private final boolean deposit;

    public AccountTransactionTask(Account account, long amount, boolean deposit) {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null.");
        }
        this.account = account;
        this.amount = amount;
        this.deposit = deposit;
    }

    @Override
    public void run() {
        try {
            if (deposit) {
                account.deposit(amount);
            } else {
                account.withdraw(amount);
            }
        } catch (BankException exception) {
            System.err.println("Transaction failed: " + exception.getMessage());
        }
    }
}
