package model;

public class SavingsAccount extends Account {
    private final long minBalance;

    public SavingsAccount(String ownerName, long openingBalance, long minBalance) {
        super(ownerName, openingBalance);
        if (minBalance < 0 || openingBalance < minBalance) {
            throw new IllegalArgumentException(
                    "Opening balance must be at least the non-negative minimum balance.");
        }
        this.minBalance = minBalance;
    }

    public long getMinBalance() {
        return minBalance;
    }

    @Override
    public double interestRate() {
        return 4.0;
    }

    @Override
    public boolean canWithdraw(long amount) {
        return amount > 0 && amount <= availableForWithdrawal();
    }

    @Override
    protected long availableForWithdrawal() {
        return Math.max(0, getBalance() - minBalance);
    }
}
