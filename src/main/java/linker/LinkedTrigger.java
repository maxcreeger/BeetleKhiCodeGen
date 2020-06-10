package linker;

import test.beetlekhi.module.Event;
import test.beetlekhi.process.EventListener;

public class LinkedTrigger {
	public EventListener eventListener;
	public Event event;
	public linker.LinkedOperation nextLinkedOperation;

	public LinkedTrigger(EventListener eventListener, Event event, linker.LinkedOperation nextLinkedOperation) {
		this.event = event;
		this.eventListener = eventListener;
		this.nextLinkedOperation = nextLinkedOperation;
	}
}