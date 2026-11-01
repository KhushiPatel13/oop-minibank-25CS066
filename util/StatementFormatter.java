package util;

import model.Account;

public class StatementFormatter {
    private StatementFormatter() {}

    public static String buildStatement(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null.");
        }

        StringBuilder statement = new StringBuilder();
        statement.append("Account Statement\n")
                .append("Account Number: ").append(account.getAccountNumber()).append('\n')
                .append("Owner: ").append(account.getOwnerName()).append('\n')
                .append("Balance: Rs. ").append(account.getBalance());
        return statement.toString();
    }
}
