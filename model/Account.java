package model;

import java.io.Serializable;
import java.util.Objects;

import exception.AccountNotFoundException;
import exception.BankException;
import exception.InsufficientFundsException;
import exception.InvalidAmountException;
import model.annotation.Id;
import model.annotation.MaxLength;
import model.annotation.Positive;

public abstract class Account implements Transactable, InterestBearing, Serializable,
        Comparable<Account> {
    private static final long serialVersionUID = 1L;

    @Id
    private final String accountNumber;
    @MaxLength(50)
    private String ownerName;
    @Positive
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

    @Override
    public synchronized void deposit(long amount) throws InvalidAmountException {
        if (amount <= 0) {
            throw new InvalidAmountException("Deposit amount must be positive.");
        }
        balance = Math.addExact(balance, amount);
    }

    @Override
    public synchronized boolean withdraw(long amount)
            throws InsufficientFundsException, InvalidAmountException {
        if (amount <= 0) {
            throw new InvalidAmountException("Withdrawal amount must be positive.");
        }
        long available = availableForWithdrawal();
        if (amount > available) {
            throw new InsufficientFundsException(
                    "Insufficient funds for this withdrawal.", amount - available);
        }
        balance = Math.subtractExact(balance, amount);
        return true;
    }

    public void transfer(Account to, long amount) throws BankException {
        if (amount <= 0) {
            throw new InvalidAmountException("Transfer amount must be positive.");
        }
        if (to == null) {
            throw new AccountNotFoundException("Destination account was not found.");
        }
        if (to == this) {
            return;
        }

        Account firstLock = accountNumber.compareTo(to.accountNumber) < 0 ? this : to;
        Account secondLock = firstLock == this ? to : this;
        synchronized (firstLock) {
            synchronized (secondLock) {
                boolean debited = false;
                boolean completed = false;
                try {
                    withdraw(amount);
                    debited = true;
                    to.deposit(amount);
                    completed = true;
                } catch (BankException | RuntimeException exception) {
                    throw exception;
                } finally {
                    if (debited && !completed) {
                        balance = Math.addExact(balance, amount);
                    }
                }
            }
        }
    }

    public abstract double interestRate();

    public abstract boolean canWithdraw(long amount);

    protected abstract long availableForWithdrawal();

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public synchronized long getBalance() {
        return balance;
    }

    @Override
    public int compareTo(Account other) {
        return accountNumber.compareTo(Objects.requireNonNull(other, "other account").accountNumber);
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
