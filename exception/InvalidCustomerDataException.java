package exception;

public class InvalidCustomerDataException extends BankException {
    private static final long serialVersionUID = 1L;

    public InvalidCustomerDataException(String message) {
        super(message);
    }
}
