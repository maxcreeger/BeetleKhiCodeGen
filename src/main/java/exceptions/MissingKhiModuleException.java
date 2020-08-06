package exceptions;

public class MissingKhiModuleException extends Exception {

	public MissingKhiModuleException(String rationale) {
		super(rationale);
	}
}