package exceptions;

public class UnavailableCommandException extends Exception {

	private static final long serialVersionUID = 4311971342066985629L;

	public UnavailableCommandException(String rationale) {
		super(rationale);
	}
}