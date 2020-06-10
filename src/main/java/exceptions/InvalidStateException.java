package exceptions;

public class InvalidStateException extends Exception {

	private static final long serialVersionUID = 4311971342066985629L;

	public InvalidStateException(String rationale) {
		super(rationale);
	}
}