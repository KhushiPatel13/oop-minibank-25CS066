package service;

import static util.StatementFormatter.buildStatement;

import java.util.Scanner;

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
import util.Validator;

public class MiniBank {
    public static void main(String[] args) throws ReflectiveOperationException {
        BankInfo bankInfo = new BankInfo("MiniBank", "Main Branch");
        System.out.println(bankInfo);

        Account[] accounts = {
            new SavingsAccount("Asha Patel", 1000, 500),
            new CurrentAccount("Ravi Shah", 0, 500),
            new FixedDepositAccount("Neha Desai", 2500)
        };
        accounts[0].deposit(500);
        accounts[0].withdraw(200);
        accounts[1].deposit(750);

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
