package service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import model.Account;

public class AccountStore {
    private final Map<String, Account> accounts = new HashMap<>();
    private final ArrayList<String> transactionHistory = new ArrayList<>();

    public void add(Account account) {
        Objects.requireNonNull(account, "account");
        Account previous = accounts.putIfAbsent(account.getAccountNumber(), account);
        if (previous != null) {
            throw new IllegalArgumentException(
                    "Account already exists: " + account.getAccountNumber());
        }
    }

    public Account get(String accountNumber) {
        return accounts.get(Objects.requireNonNull(accountNumber, "accountNumber"));
    }

    public ArrayList<Account> getAll() {
        return new ArrayList<>(accounts.values());
    }

    public void addTransaction(String transaction) {
        transactionHistory.add(Objects.requireNonNull(transaction, "transaction"));
    }

    public ArrayList<String> getTransactionHistory() {
        return new ArrayList<>(transactionHistory);
    }

    public ArrayList<Account> sortedByBalance() {
        ArrayList<Account> sorted = getAll();
        sorted.sort(Comparator.comparingLong(Account::getBalance).thenComparing(Account::compareTo));
        return sorted;
    }
}
