package linker;

import test.beetlekhi.module.Event;
import test.beetlekhi.process.EventListener;

public class LinkedTrigger {

	public EventListener eventListener;
	public Event event;
	public LinkedOperation nextLinkedOperation;

	public LinkedTrigger(EventListener eventListener, Event event, LinkedOperation nextLinkedOperation) {
		this.event = event;
		this.eventListener = eventListener;
		this.nextLinkedOperation = nextLinkedOperation;
	}

	public String getEmitterNode() {
		return eventListener.getNode();
	}

	public EventListener getEventListener() {
		return eventListener;
	}

	public Event getEvent() {
		return event;
	}

	public LinkedOperation getNextLinkedOperation() {
		return nextLinkedOperation;
	}
}