package linker;

import test.beetlekhi.command.Attribute;

import java.util.function.Supplier;

public class LinkedSensor {

	public Supplier<Object> supplier;
	public Attribute attribute;

	public LinkedSensor(Supplier<Object> supplier, Attribute attribute) {
		this.supplier = supplier;
		this.attribute = attribute;
	}

	public Object getCurrentValue() {
		return supplier.get();
	}
}