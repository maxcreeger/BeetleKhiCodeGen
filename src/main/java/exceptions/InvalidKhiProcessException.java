package exceptions;

public class InvalidKhiProcessException extends Exception {

	private static final long serialVersionUID = 4311971342066985629L;

	public InvalidKhiProcessException(String rationale) {
		super(rationale);
	}
}