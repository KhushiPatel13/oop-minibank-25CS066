package exception;

public class InvalidTransactionException extends BankException {
    private static final long serialVersionUID = 1L;

    public InvalidTransactionException(String message) {
        super(message);
    }
}
