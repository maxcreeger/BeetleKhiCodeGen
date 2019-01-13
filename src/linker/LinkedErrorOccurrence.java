package linker;

import test.beetlekhi.Error;
import test.beetlekhi.Event;

public class LinkedErrorOccurrence {
	/**
	 * 
	 */
	private final LinkedOperation linkedOperation;
	Error error;
	Event event;

	public LinkedErrorOccurrence(LinkedOperation linkedOperation, Error error, Event event) {
		this.linkedOperation = linkedOperation;
		this.event = event;
		this.error = error;
		this.linkedOperation.linkedErrors.add(this);
	}
}