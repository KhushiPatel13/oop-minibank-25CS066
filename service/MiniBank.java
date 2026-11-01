package service;

import static util.StatementFormatter.buildStatement;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Scanner;

import exception.BankException;
import exception.InsufficientFundsException;
import exception.InvalidAmountException;
import model.Account;
import model.BankInfo;
import model.Command;
import model.CurrentAccount;
import model.FixedDepositAccount;
import model.MenuOption;
import model.SavingsAccount;
import model.WithdrawRule;
import util.AnnotationValidator;
import util.CommandParser;
import util.ReportGenerator;
import util.StatePersister;
import util.TransactionLog;
import util.Validator;

public class MiniBank {
    public static void main(String[] args)
            throws ReflectiveOperationException, InterruptedException, IOException,
            ClassNotFoundException {
        BankInfo bankInfo = new BankInfo("MiniBank", "Main Branch");
        System.out.println(bankInfo);

        Account sharedAccount = new CurrentAccount("Concurrent Demo", 0, 0);
        int workerCount = 10;
        int depositsPerWorker = 10_000;
        long amountPerDeposit = 1;
        Thread[] workers = new Thread[workerCount];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new Thread(
                    new AccountWorker(sharedAccount, depositsPerWorker, amountPerDeposit),
                    "AccountWorker-" + (i + 1));
            workers[i].start();
        }
        for (Thread worker : workers) {
            worker.join();
        }
        long expectedBalance = (long) workerCount * depositsPerWorker * amountPerDeposit;
        System.out.println("Concurrent balance: Rs. " + sharedAccount.getBalance()
                + " (expected Rs. " + expectedBalance + ")");

        Account processedAccount = new CurrentAccount("Batch Processing", 1000, 0);
        TransactionProcessor processor = new TransactionProcessor();
        for (int i = 0; i < 10; i++) {
            processor.submit(new AccountTransactionTask(processedAccount, 100, true));
            processor.submit(new AccountTransactionTask(processedAccount, 50, false));
        }
        processor.stop();
        System.out.println("Managed-thread-pool balance: Rs. " + processedAccount.getBalance()
                + " (expected Rs. 1500)");

        ProducerConsumerDemo.run();

        Account transferA = new CurrentAccount("Transfer A", 10_000, 0);
        Account transferB = new CurrentAccount("Transfer B", 10_000, 0);
        Thread transferAToB = new Thread(
                new TransferWorker(transferA, transferB, 1000, 1), "Transfer-A-to-B");
        Thread transferBToA = new Thread(
                new TransferWorker(transferB, transferA, 1000, 1), "Transfer-B-to-A");
        transferAToB.start();
        transferBToA.start();
        transferAToB.join();
        transferBToA.join();
        System.out.println("Opposing transfers completed; combined balance: Rs. "
                + (transferA.getBalance() + transferB.getBalance()));

        Account[] accounts = {
            new SavingsAccount("Asha Patel", 1000, 500),
            new CurrentAccount("Ravi Shah", 0, 500),
            new FixedDepositAccount("Neha Desai", 2500)
        };
        try {
            accounts[0].deposit(500);
            accounts[0].withdraw(200);
            accounts[1].deposit(750);
            accounts[0].transfer(accounts[1], 100);
        } catch (BankException exception) {
            System.out.println("Transaction could not be completed: " + exception.getMessage());
        } finally {
            System.out.println("Transaction processing finished.");
        }

        Path logFile = Path.of("logs", "transactions.log");
        TransactionLog.append(logFile, "DEPOSIT 500");
        TransactionLog.append(logFile, "WITHDRAW 200");
        TransactionLog.append(logFile, "DEPOSIT 750");
        TransactionLog.append(logFile, "WITHDRAW 100");
        TransactionLog.append(logFile, "DEPOSIT 100");

        Path stateFile = Path.of("data", "accounts.bin");
        StatePersister.save(accounts, stateFile);
        Account[] reloadedAccounts = StatePersister.load(stateFile);
        System.out.println("Reloaded accounts:");
        for (Account account : reloadedAccounts) {
            System.out.println(account);
        }

        String report = ReportGenerator.generate(
                Path.of("logs"), Path.of("reports", "end-of-day-report.txt"));
        System.out.println(report);

        try (TransactionResource resource = new TransactionResource()) {
            System.out.println("Transaction resource active: " + resource.isOpen());
        }

        try {
            accounts[0].withdraw(Long.MAX_VALUE);
        } catch (InsufficientFundsException exception) {
            System.out.println("Withdrawal declined. Short by Rs. " + exception.getShortfall()
                    + ".");
        } catch (InvalidAmountException exception) {
            System.out.println("Please enter a valid withdrawal amount.");
        }

        try {
            accounts[0].deposit(0);
        } catch (InvalidAmountException exception) {
            System.out.println("Deposit declined: " + exception.getMessage());
        }

        System.out.println("Account balances and interest rates:");
        for (Account account : accounts) {
            System.out.println(account + ", interest rate=" + account.interestRate()
                    + "%, yearly interest=Rs. " + account.yearlyInterest());
            if (account instanceof SavingsAccount savingsAccount) {
                System.out.println("Savings minimum balance: " + savingsAccount.getMinBalance());
            }
        }
        System.out.println("First two accounts equal: " + accounts[0].equals(accounts[1]));
        System.out.println("First object is an Account: " + (accounts[0] instanceof Account));

        WithdrawRule anonymousRule = new WithdrawRule() {
            @Override
            public boolean allow(Account account, long amount) {
                return account.canWithdraw(amount);
            }
        };
        WithdrawRule lambdaRule = (account, amount) -> account.canWithdraw(amount);
        System.out.println("Withdrawal allowed (anonymous): "
                + anonymousRule.allow(accounts[0], 100));
        System.out.println("Withdrawal allowed (lambda): "
                + lambdaRule.allow(accounts[0], 100));

        System.out.println("Validation examples:");
        System.out.println("Mobile: " + Validator.isValidMobile("9876543210")
                + " / " + Validator.isValidMobile("12345"));
        System.out.println("Email: " + Validator.isValidEmail("asha@example.com")
                + " / " + Validator.isValidEmail("not-an-email"));
        System.out.println("PAN: " + Validator.isValidPan("ABCDE1234F")
                + " / " + Validator.isValidPan("bad-pan"));
        System.out.println("IFSC: " + Validator.isValidIfsc("SBIN0001234")
                + " / " + Validator.isValidIfsc("bad-ifsc"));

        Command command = CommandParser.parse("DEPOSIT AC0001 500");
        System.out.println("Parsed command: " + command.type() + ", "
                + command.accountNumber() + ", " + command.amount());
        System.out.println(buildStatement(accounts[0]));

        Account invalidAccount = new SavingsAccount("Validation sample", 100, 0);
        var balanceField = Account.class.getDeclaredField("balance");
        balanceField.setAccessible(true);
        balanceField.setLong(invalidAccount, -1);
        for (String error : AnnotationValidator.validate(invalidAccount)) {
            System.out.println("Annotation validation error: " + error);
        }

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.println();
                System.out.println("1. Open account");
                System.out.println("2. Deposit");
                System.out.println("3. Withdraw");
                System.out.println("4. Transfer");
                System.out.println("5. Exit");
                System.out.print("Choose an option: ");

                if (!scanner.hasNextInt()) {
                    if (!scanner.hasNext()) {
                        System.out.println();
                        break;
                    }
                    System.out.println("Please enter a number from 1 to 5.");
                    scanner.next();
                    continue;
                }

                int selectedNumber = scanner.nextInt();
                if (selectedNumber < 1 || selectedNumber > MenuOption.values().length) {
                    System.out.println("Please enter a number from 1 to 5.");
                    continue;
                }

                MenuOption choice = MenuOption.values()[selectedNumber - 1];
                String message = switch (choice) {
                    case OPEN_ACCOUNT -> "Open account - to be implemented in a later lab";
                    case DEPOSIT -> "Deposit - to be implemented in a later lab";
                    case WITHDRAW -> "Withdraw - to be implemented in a later lab";
                    case TRANSFER -> "Transfer - to be implemented in a later lab";
                    case EXIT -> "Goodbye!";
                };
                System.out.println(message);

                if (choice == MenuOption.EXIT) {
                    break;
                }
            }
        }
    }
}
