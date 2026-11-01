package model;

import java.io.Serial;

public class CurrentAccount extends Account {
    @Serial
    private static final long serialVersionUID = 1L;

    private final long overdraftLimit;

    public CurrentAccount(String ownerName, long openingBalance, long overdraftLimit) {
        super(ownerName, openingBalance);
        if (openingBalance < 0 || overdraftLimit < 0) {
            throw new IllegalArgumentException(
                    "Opening balance and overdraft limit cannot be negative.");
        }
        this.overdraftLimit = overdraftLimit;
    }

    public long getOverdraftLimit() {
        return overdraftLimit;
    }

    @Override
    public double interestRate() {
        return 0.0;
    }

    @Override
    public boolean canWithdraw(long amount) {
        return amount > 0 && amount <= availableForWithdrawal();
    }

    @Override
    protected long availableForWithdrawal() {
        long balance = getBalance();
        if (balance >= 0) {
            return balance > Long.MAX_VALUE - overdraftLimit
                    ? Long.MAX_VALUE
                    : balance + overdraftLimit;
        }
        return balance <= -overdraftLimit ? 0 : overdraftLimit + balance;
    }
}
