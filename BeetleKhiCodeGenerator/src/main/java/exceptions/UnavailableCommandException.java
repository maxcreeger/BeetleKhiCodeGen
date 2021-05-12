package exceptions;

public class UnavailableCommandException extends Exception {

	public UnavailableCommandException(String rationale) {
		super(rationale);
	}
}