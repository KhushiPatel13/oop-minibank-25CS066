import java.util.Scanner;

public class MiniBank {
    public static void main(String[] args) {
        BankInfo bankInfo = new BankInfo("MiniBank", "Main Branch");
        System.out.println(bankInfo);

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

record BankInfo(String name, String branch) {}

enum MenuOption {
    OPEN_ACCOUNT,
    DEPOSIT,
    WITHDRAW,
    TRANSFER,
    EXIT
}
