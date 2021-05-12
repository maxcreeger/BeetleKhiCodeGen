package exceptions;

public class InvalidKhiProcessException extends Exception {

	public InvalidKhiProcessException(String rationale) {
		super(rationale);
	}
}