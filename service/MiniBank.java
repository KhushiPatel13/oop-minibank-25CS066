package service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import exception.BankException;
import model.Account;
import model.Command;
import model.MenuOption;
import model.TransactionType;
import util.CommandParser;
import util.StatePersister;

public class MiniBank {
    private static final Path STATE_FILE = Path.of("data", "accounts.bin");
    private static final Path LOG_FILE = Path.of("logs", "transactions.log");
    private static final Path REPORT_FILE = Path.of("reports", "end-of-day-report.txt");

    private MiniBank() {}

    public static void main(String[] args)
            throws IOException, ClassNotFoundException, InterruptedException {
        try (Scanner scanner = new Scanner(System.in)) {
            run(scanner, STATE_FILE, LOG_FILE, REPORT_FILE);
        }
    }

    static void run(Scanner scanner, Path stateFile, Path logFile, Path reportFile)
            throws IOException, ClassNotFoundException, InterruptedException {
        Repository<Account> repository = new Repository<>();
        if (Files.exists(stateFile)) {
            repository.saveAll(StatePersister.load(stateFile));
        }

        BankService bankService = new BankService(repository, logFile, reportFile);
        System.out.println("MiniBank");
        System.out.println(repository.findAll().size() + " account(s) loaded.");
        try {
            runMenu(scanner, bankService);
        } finally {
            try {
                bankService.saveState(stateFile);
                System.out.println("Account data saved.");
            } finally {
                bankService.close();
            }
        }
    }

    private static void runMenu(Scanner scanner, BankService bankService)
            throws InterruptedException {
        boolean running = true;
        while (running) {
            printMenu();
            String selected = readLine(scanner, "Choose an option: ");
            if (selected == null) {
                break;
            }

            try {
                int number = Integer.parseInt(selected.trim());
                if (number < 1 || number > MenuOption.values().length) {
                    System.out.println("Please choose an option from 1 to "
                            + MenuOption.values().length + ".");
                    continue;
                }

                MenuOption option = MenuOption.values()[number - 1];
                switch (option) {
                    case OPEN_ACCOUNT -> openAccount(scanner, bankService);
                    case DEPOSIT -> deposit(scanner, bankService);
                    case WITHDRAW -> withdraw(scanner, bankService);
                    case TRANSFER -> transfer(scanner, bankService);
                    case STATEMENT -> statement(scanner, bankService);
                    case LIST_ACCOUNTS -> listAccounts(bankService);
                    case PROCESS_BATCH -> processBatch(scanner, bankService);
                    case REPORT -> System.out.println(bankService.generateReport());
                    case EXIT -> {
                        System.out.println("Goodbye!");
                        running = false;
                    }
                }
            } catch (BankException | IOException exception) {
                System.out.println("Operation failed: " + exception.getMessage());
            } catch (IllegalArgumentException exception) {
                System.out.println("Invalid input: " + exception.getMessage());
            }
        }
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("1. Open account");
        System.out.println("2. Deposit");
        System.out.println("3. Withdraw");
        System.out.println("4. Transfer");
        System.out.println("5. Account statement");
        System.out.println("6. List accounts");
        System.out.println("7. Process transaction batch");
        System.out.println("8. End-of-day report");
        System.out.println("9. Exit");
    }

    private static void openAccount(Scanner scanner, BankService bankService)
            throws BankException {
        String name = readLine(scanner, "Customer name: ");
        String email = readLine(scanner, "Email: ");
        String mobile = readLine(scanner, "10-digit mobile: ");
        String openingBalance = readLine(scanner, "Opening balance in rupees: ");
        if (name == null || email == null || mobile == null || openingBalance == null) {
            return;
        }

        Account account = bankService.openAccount(
                name, email, mobile, parseAmount(openingBalance));
        System.out.println("Account opened: " + account.getAccountNumber());
    }

    private static void deposit(Scanner scanner, BankService bankService)
            throws BankException, IOException {
        String accountNumber = readLine(scanner, "Account number: ");
        String amount = readLine(scanner, "Deposit amount: ");
        if (accountNumber == null || amount == null) {
            return;
        }
        bankService.deposit(accountNumber.trim(), parseAmount(amount));
        System.out.println("Deposit completed.");
    }

    private static void withdraw(Scanner scanner, BankService bankService)
            throws BankException, IOException {
        String accountNumber = readLine(scanner, "Account number: ");
        String amount = readLine(scanner, "Withdrawal amount: ");
        if (accountNumber == null || amount == null) {
            return;
        }
        bankService.withdraw(accountNumber.trim(), parseAmount(amount));
        System.out.println("Withdrawal completed.");
    }

    private static void transfer(Scanner scanner, BankService bankService)
            throws BankException, IOException {
        String from = readLine(scanner, "From account number: ");
        String to = readLine(scanner, "To account number: ");
        String amount = readLine(scanner, "Transfer amount: ");
        if (from == null || to == null || amount == null) {
            return;
        }
        bankService.transfer(from.trim(), to.trim(), parseAmount(amount));
        System.out.println("Transfer completed.");
    }

    private static void statement(Scanner scanner, BankService bankService)
            throws BankException {
        String accountNumber = readLine(scanner, "Account number: ");
        if (accountNumber != null) {
            System.out.println(bankService.statement(accountNumber.trim()));
        }
    }

    private static void listAccounts(BankService bankService) {
        ArrayList<Account> accounts = bankService.listAccounts();
        if (accounts.isEmpty()) {
            System.out.println("No accounts are open.");
            return;
        }
        for (Account account : accounts) {
            System.out.println(account);
        }
    }

    private static void processBatch(Scanner scanner, BankService bankService)
            throws BankException, IOException, InterruptedException {
        String countInput = readLine(scanner, "Number of transactions: ");
        if (countInput == null) {
            return;
        }
        int count = Integer.parseInt(countInput.trim());
        if (count < 1 || count > 1000) {
            throw new IllegalArgumentException("Enter a batch size from 1 to 1000.");
        }

        List<Command> commands = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String line = readLine(scanner, "Transaction " + (i + 1)
                    + " (DEPOSIT|WITHDRAW account amount): ");
            if (line == null) {
                return;
            }
            Command command = CommandParser.parse(line);
            if (command.type() == TransactionType.TRANSFER) {
                throw new IllegalArgumentException(
                        "Batch transfers are not supported; use the transfer option.");
            }
            commands.add(command);
        }

        bankService.processBatch(commands);
        System.out.println(commands.size() + " transaction(s) processed.");
    }

    private static String readLine(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.hasNextLine() ? scanner.nextLine() : null;
    }

    private static long parseAmount(String input) {
        try {
            return Long.parseLong(input.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Amount must be a whole number.", exception);
        }
    }
}
