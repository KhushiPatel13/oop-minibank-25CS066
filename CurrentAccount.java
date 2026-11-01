public class CurrentAccount extends Account {
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
        if (amount <= 0) {
            return false;
        }
        long balance = getBalance();
        if (balance < 0) {
            return amount <= overdraftLimit + balance;
        }
        return amount <= balance || amount - balance <= overdraftLimit;
    }
}
