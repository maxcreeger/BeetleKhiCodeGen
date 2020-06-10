package exceptions;

public class InvalidKhiModuleException extends Exception {

	private static final long serialVersionUID = 4311971342066985629L;

	public InvalidKhiModuleException(String rationale) {
		super(rationale);
	}
}