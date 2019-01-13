package gui;

import java.awt.Dimension;

import javax.swing.JFrame;

import linker.LinkedNode;

public class ModuleMonitor {

	private final LinkedNode myNode;
	private final JFrame frame;

	public ModuleMonitor(LinkedNode myNode) {
		this.myNode = myNode;
		frame = new JFrame(myNode.node.getName() + " (" + myNode.getKhiModule()
																.getName() + ")");
	}

	public void display() {
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setPreferredSize(new Dimension(600, 400));
		frame.pack();
		frame.setVisible(true);
	}
}
