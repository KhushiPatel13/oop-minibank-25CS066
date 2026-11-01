public class Account {
    private final String accountNumber;
    private String ownerName;
    private long balance;
    private boolean active;

    private static long accountCounter;

    public Account(String ownerName, long openingBalance) {
        if (openingBalance < 0) {
            throw new IllegalArgumentException("Opening balance cannot be negative.");
        }

        this.accountNumber = generateAccountNumber();
        this.ownerName = ownerName;
        this.balance = openingBalance;
        this.active = true;
    }

    public Account(String ownerName) {
        this(ownerName, 0);
    }

    private static String generateAccountNumber() {
        return String.format("AC%04d", ++accountCounter);
    }

    public void deposit(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive.");
        }
        balance = Math.addExact(balance, amount);
    }

    public boolean withdraw(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive.");
        }
        if (balance < amount) {
            return false;
        }
        balance -= amount;
        return true;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public long getBalance() {
        return balance;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public String toString() {
        return "Account{accountNumber='" + accountNumber + "', ownerName='" + ownerName
                + "', balance=" + balance + "}";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Account account)) {
            return false;
        }
        return accountNumber.equals(account.accountNumber);
    }

    @Override
    public int hashCode() {
        return accountNumber.hashCode();
    }
}
