package exceptions;

public class InvalidCommandAttributeException extends Exception {

	public InvalidCommandAttributeException(String rationale) {
		super(rationale);
	}
}