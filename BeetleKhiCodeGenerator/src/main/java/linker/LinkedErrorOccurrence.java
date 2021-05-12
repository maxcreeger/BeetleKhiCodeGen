package linker;

import test.beetlekhi.module.Event;
import test.beetlekhi.process.Error;

public class LinkedErrorOccurrence {

	private final Error error;

	private final Event event;
	private final LinkedNode failedNode;
	private final String errorName;

	public LinkedErrorOccurrence(Error error, Event event, ProcessLinker.NodeLinker nodeLinker) {
		this.event = event;
		this.error = error;
		this.failedNode = nodeLinker.lookupLinkedNode(error.getNode());
		this.errorName = error.getName();
	}

	public LinkedNode getFailedNode() {
		return failedNode;
	}

	public String getErrorName() {
		return errorName;
	}

	public Event getEvent() {
		return event;
	}

}