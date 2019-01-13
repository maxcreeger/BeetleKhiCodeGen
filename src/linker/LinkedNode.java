package linker;

import generator.ProcessOverview;

import java.util.ArrayList;
import java.util.List;

import test.beetlekhi.Khimodule;
import test.beetlekhi.Node;

public class LinkedNode {

	public final Node node;
	private final Khimodule khiModule;
	public final List<LinkedCommand> availableCommands = new ArrayList<>();
	public final List<LinkedTrigger> availableTriggers = new ArrayList<>();
	final ProcessOverview root;

	public LinkedNode(ProcessOverview root, Node node, Khimodule khiModule) {
		super();
		this.node = node;
		this.khiModule = khiModule;
		this.root = root;
		root.add(this);
	}

	public void add(LinkedCommand linkedCommand) {
		availableCommands.add(linkedCommand);
	}

	public void add(LinkedTrigger linkedTrigger) {
		availableTriggers.add(linkedTrigger);
	}

	public Khimodule getKhiModule() {
		return khiModule;
	}
}