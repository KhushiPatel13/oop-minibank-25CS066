package service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

import exception.AccountNotFoundException;
import exception.BankException;
import exception.InsufficientFundsException;
import exception.InvalidAmountException;
import exception.InvalidCustomerDataException;
import exception.InvalidTransactionException;
import model.Account;
import model.Command;
import model.CurrentAccount;
import model.SavingsAccount;
import model.TransactionType;
import util.ReportGenerator;
import util.StatePersister;
import util.StatementFormatter;
import util.TransactionLog;
import util.Validator;

public class BankService implements AutoCloseable {
    private final Repository<Account> accounts;
    private final TransactionProcessor transactionProcessor;
    private final Path logFile;
    private final Path reportFile;

    public BankService(Repository<Account> accounts, Path logFile, Path reportFile) {
        this.accounts = Objects.requireNonNull(accounts, "accounts");
        this.logFile = Objects.requireNonNull(logFile, "logFile");
        this.reportFile = Objects.requireNonNull(reportFile, "reportFile");
        this.transactionProcessor = new TransactionProcessor();
        for (Account account : accounts.findAll()) {
            Account.reserveAccountNumber(account.getAccountNumber());
            transactionProcessor.addAccount(account);
        }
    }

    public Account openAccount(String ownerName, String email, String mobile, long openingBalance)
            throws BankException {
        if (ownerName == null || ownerName.isBlank() || ownerName.length() > 50) {
            throw new InvalidCustomerDataException(
                    "Name must contain 1 to 50 characters.");
        }
        if (!Validator.isValidEmail(email)) {
            throw new InvalidCustomerDataException("Please enter a valid email address.");
        }
        if (!Validator.isValidMobile(mobile)) {
            throw new InvalidCustomerDataException("Please enter a valid 10-digit mobile number.");
        }
        if (openingBalance < 0) {
            throw new InvalidAmountException("Opening balance cannot be negative.");
        }

        Account account = new SavingsAccount(ownerName.trim(), openingBalance, 0);
        accounts.save(account);
        transactionProcessor.addAccount(account);
        return account;
    }

    public void deposit(String accountNumber, long amount) throws BankException, IOException {
        Account account = findAccount(accountNumber);
        account.deposit(amount);
        TransactionLog.append(logFile, "DEPOSIT " + amount);
    }

    public void withdraw(String accountNumber, long amount) throws BankException, IOException {
        Account account = findAccount(accountNumber);
        account.withdraw(amount);
        TransactionLog.append(logFile, "WITHDRAW " + amount);
    }

    public void transfer(String fromAccountNumber, String toAccountNumber, long amount)
            throws BankException, IOException {
        Account from = findAccount(fromAccountNumber);
        Account to = findAccount(toAccountNumber);
        from.transfer(to, amount);
        TransactionLog.append(logFile, "WITHDRAW " + amount);
        TransactionLog.append(logFile, "DEPOSIT " + amount);
    }

    public String statement(String accountNumber) throws AccountNotFoundException {
        return StatementFormatter.buildStatement(findAccount(accountNumber));
    }

    public ArrayList<Account> listAccounts() {
        return accounts.findAll();
    }

    public void processBatch(List<Command> commands)
            throws BankException, IOException, InterruptedException {
        Objects.requireNonNull(commands, "commands");
        ConcurrentLinkedQueue<Exception> failures = new ConcurrentLinkedQueue<>();
        ArrayList<Future<?>> tasks = new ArrayList<>();

        for (Command command : commands) {
            Objects.requireNonNull(command, "command");
            if (command.type() == TransactionType.TRANSFER) {
                throw new InvalidTransactionException(
                        "Batch transfers require a source and destination and are not supported.");
            }
        }

        try {
            for (Command command : commands) {
                tasks.add(transactionProcessor.submit(() -> {
                    try {
                        processTransaction(command);
                    } catch (Exception exception) {
                        failures.add(exception);
                    }
                }));
            }
            for (Future<?> task : tasks) {
                task.get();
            }
        } catch (InterruptedException exception) {
            for (Future<?> task : tasks) {
                task.cancel(true);
            }
            Thread.currentThread().interrupt();
            throw exception;
        } catch (java.util.concurrent.ExecutionException exception) {
            throw new IllegalStateException("A batch transaction task failed unexpectedly.",
                    exception.getCause());
        }

        Exception failure = failures.peek();
        if (failure instanceof BankException bankException) {
            throw bankException;
        }
        if (failure instanceof IOException ioException) {
            throw ioException;
        }
        if (failure != null) {
            throw new IllegalStateException("A batch transaction failed.", failure);
        }
    }

    public String generateReport() throws IOException {
        Path logsDirectory = logFile.toAbsolutePath().getParent();
        Files.createDirectories(logsDirectory);
        return ReportGenerator.generate(logsDirectory, reportFile);
    }

    public void saveState(Path stateFile) throws IOException {
        StatePersister.save(accounts.findAll().toArray(Account[]::new), stateFile);
    }

    private void processTransaction(Command command) throws BankException, IOException {
        Account account = transactionProcessor.getAccount(command.accountNumber());
        if (account == null) {
            throw new AccountNotFoundException(
                    "Account not found: " + command.accountNumber());
        }
        switch (command.type()) {
            case DEPOSIT -> {
                account.deposit(command.amount());
                TransactionLog.append(logFile, "DEPOSIT " + command.amount());
            }
            case WITHDRAW -> {
                account.withdraw(command.amount());
                TransactionLog.append(logFile, "WITHDRAW " + command.amount());
            }
            case TRANSFER -> throw new InvalidTransactionException(
                    "Batch transfers are not supported.");
        }
    }

    private Account findAccount(String accountNumber) throws AccountNotFoundException {
        Account account = accounts.findById(Objects.requireNonNull(accountNumber, "accountNumber"));
        if (account == null) {
            throw new AccountNotFoundException("Account not found: " + accountNumber);
        }
        return account;
    }

    @Override
    public void close() {
        try {
            transactionProcessor.stop();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while stopping transaction workers.",
                    exception);
        }
    }
}
