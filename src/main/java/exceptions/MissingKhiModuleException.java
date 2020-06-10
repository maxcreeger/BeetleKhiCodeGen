package exceptions;

public class MissingKhiModuleException extends Exception {

	private static final long serialVersionUID = 4311971342066985649L;

	public MissingKhiModuleException(String rationale) {
		super(rationale);
	}
}