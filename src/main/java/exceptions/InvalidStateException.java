package exceptions;

public class InvalidStateException extends Exception {

	public InvalidStateException(String rationale) {
		super(rationale);
	}
}