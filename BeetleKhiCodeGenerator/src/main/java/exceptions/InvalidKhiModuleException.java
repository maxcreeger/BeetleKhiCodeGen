package exceptions;

public class InvalidKhiModuleException extends Exception {

	public InvalidKhiModuleException(String rationale) {
		super(rationale);
	}
}