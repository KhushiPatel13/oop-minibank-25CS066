package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import exception.InsufficientFundsException;
import exception.InvalidCustomerDataException;
import model.Account;
import model.Command;
import model.TransactionType;
import util.StatePersister;

class BankServiceTest {
    @Test
    void depositIncreasesBalance() throws Exception {
        Path directory = Files.createTempDirectory("minibank-deposit-test");
        try (BankService service = createService(directory)) {
            Account account = service.openAccount(
                    "Asha Patel", "asha@example.com", "9876543210", 100);

            service.deposit(account.getAccountNumber(), 50);

            assertEquals(150, account.getBalance());
        } finally {
            deleteTree(directory);
        }
    }

    @Test
    void withdrawalOverBalanceThrowsInsufficientFunds() throws Exception {
        Path directory = Files.createTempDirectory("minibank-withdraw-test");
        try (BankService service = createService(directory)) {
            Account account = service.openAccount(
                    "Asha Patel", "asha@example.com", "9876543210", 100);

            assertThrows(InsufficientFundsException.class,
                    () -> service.withdraw(account.getAccountNumber(), 101));
            assertEquals(100, account.getBalance());
        } finally {
            deleteTree(directory);
        }
    }

    @Test
    void concurrentDepositsKeepTheBalanceCorrect() throws Exception {
        Path directory = Files.createTempDirectory("minibank-concurrent-test");
        try (BankService service = createService(directory)) {
            Account account = service.openAccount(
                    "Asha Patel", "asha@example.com", "9876543210", 0);
            int threadCount = 10;
            int depositsPerThread = 2_000;
            AtomicReference<Throwable> failure = new AtomicReference<>();
            List<Thread> threads = new ArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                Thread thread = new Thread(() -> {
                    try {
                        for (int deposit = 0; deposit < depositsPerThread; deposit++) {
                            account.deposit(1);
                        }
                    } catch (Exception exception) {
                        failure.compareAndSet(null, exception);
                    }
                });
                threads.add(thread);
                thread.start();
            }
            for (Thread thread : threads) {
                thread.join();
            }

            if (failure.get() != null) {
                throw new AssertionError("Concurrent deposit failed.", failure.get());
            }
            assertEquals((long) threadCount * depositsPerThread, account.getBalance());
        } finally {
            deleteTree(directory);
        }
    }

    @Test
    void processBatchUsesTheThreadPoolAndConcurrentAccountRegistry() throws Exception {
        Path directory = Files.createTempDirectory("minibank-batch-test");
        try (BankService service = createService(directory)) {
            Account account = service.openAccount(
                    "Asha Patel", "asha@example.com", "9876543210", 0);
            List<Command> commands = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                commands.add(new Command(
                        TransactionType.DEPOSIT, account.getAccountNumber(), 5));
            }

            service.processBatch(commands);

            assertEquals(500, account.getBalance());
        } finally {
            deleteTree(directory);
        }
    }

    @Test
    void openAccountRejectsInvalidContactDetails() throws Exception {
        Path directory = Files.createTempDirectory("minibank-validation-test");
        try (BankService service = createService(directory)) {
            assertThrows(InvalidCustomerDataException.class,
                    () -> service.openAccount("Asha Patel", "bad-email", "123", 0));
        } finally {
            deleteTree(directory);
        }
    }

    @Test
    void persistedAccountsLoadAndNewIdsRemainUnique() throws Exception {
        Path directory = Files.createTempDirectory("minibank-state-test");
        Path stateFile = directory.resolve("data/accounts.bin");
        String oldAccountNumber;
        try (BankService service = createService(directory)) {
            Account account = service.openAccount(
                    "Asha Patel", "asha@example.com", "9876543210", 100);
            oldAccountNumber = account.getAccountNumber();
            service.saveState(stateFile);
        }

        try (BankService service = createServiceWithState(directory, stateFile)) {
            Account nextAccount = service.openAccount(
                    "Ravi Shah", "ravi@example.com", "9876543211", 0);
            assertEquals(2, service.listAccounts().size());
            assertNotEquals(oldAccountNumber, nextAccount.getAccountNumber());
        } finally {
            deleteTree(directory);
        }
    }

    @Test
    void reportCanBeGeneratedBeforeTransactionsExist() throws Exception {
        Path directory = Files.createTempDirectory("minibank-empty-report-test");
        try (BankService service = createService(directory)) {
            String report = service.generateReport();

            assertTrue(report.contains("Total deposits: Rs. 0"));
            assertTrue(Files.exists(directory.resolve("reports/report.txt")));
        } finally {
            deleteTree(directory);
        }
    }

    @Test
    void menuLoadsAccountsRunsOperationsAndSavesOnExit() throws Exception {
        Path directory = Files.createTempDirectory("minibank-menu-test");
        Path stateFile = directory.resolve("data/accounts.bin");
        Path logFile = directory.resolve("logs/transactions.log");
        Path reportFile = directory.resolve("reports/report.txt");
        String accountNumber;
        try (BankService service = createService(directory)) {
            Account account = service.openAccount(
                    "Asha Patel", "asha@example.com", "9876543210", 100);
            accountNumber = account.getAccountNumber();
            service.saveState(stateFile);
        }

        String input = "2\n" + accountNumber + "\n25\n5\n" + accountNumber + "\n9\n";
        try {
            MiniBank.run(new Scanner(input), stateFile, logFile, reportFile);

            Account[] savedAccounts = StatePersister.load(stateFile);
            assertEquals(125, savedAccounts[0].getBalance());
            assertTrue(Files.readString(logFile).contains("DEPOSIT 25"));
        } finally {
            deleteTree(directory);
        }
    }

    private static BankService createService(Path directory) {
        return new BankService(new Repository<>(),
                directory.resolve("logs/transactions.log"),
                directory.resolve("reports/report.txt"));
    }

    private static BankService createServiceWithState(Path directory, Path stateFile)
            throws Exception {
        Repository<Account> repository = new Repository<>();
        repository.saveAll(StatePersister.load(stateFile));
        return new BankService(repository,
                directory.resolve("logs/transactions.log"),
                directory.resolve("reports/report.txt"));
    }

    private static void deleteTree(Path directory) throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
