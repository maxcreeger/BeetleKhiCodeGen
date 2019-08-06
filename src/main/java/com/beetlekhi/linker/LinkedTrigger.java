package com.beetlekhi.linker;

import com.beetlekhi.module.Event;
import com.beetlekhi.process.EventListener;

public class LinkedTrigger {
	public EventListener eventListener;
	public Event event;
	public LinkedOperation nextLinkedOperation;

	public LinkedTrigger(EventListener eventListener, Event event, LinkedOperation nextLinkedOperation) {
		this.event = event;
		this.eventListener = eventListener;
		this.nextLinkedOperation = nextLinkedOperation;
	}
}