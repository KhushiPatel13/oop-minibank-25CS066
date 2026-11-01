# MiniBank

MiniBank is a Java 17+ console banking application. It supports savings account
creation, deposits, withdrawals, transfers, statements, batch processing, and an
end-of-day transaction report.

## Build and test

```powershell
mvn test
mvn package
```

The runnable JAR is created at `target/minibank.jar`.

## Run

```powershell
java -jar target/minibank.jar
```

The application loads account state from `data/accounts.bin` when it starts and
saves it on exit. Transaction entries are appended to
`logs/transactions.log`; the report menu option writes
`reports/end-of-day-report.txt`.

## Menu

1. Open a savings account
2. Deposit
3. Withdraw
4. Transfer between accounts
5. View an account statement
6. List accounts
7. Process a batch of `DEPOSIT account amount` or `WITHDRAW account amount` commands
8. Generate the end-of-day report
9. Save and exit

New accounts require a valid email address, a 10-digit Indian mobile number, and
a non-negative opening balance. Account numbers are generated automatically.
