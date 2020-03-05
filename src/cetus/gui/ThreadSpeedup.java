package cetus.gui;

import javax.swing.JOptionPane;

import cetus.exec.Driver;

public class ThreadSpeedup extends Thread {

	private String msg;
	private Process p;
	private int mode = 0;
	private CetusGuiSpeedupPanel panel;
	
	public ThreadSpeedup (String newMsg, Process newP, int newMode, CetusGuiSpeedupPanel newPanel) {
		msg = newMsg;
		p = newP;
		mode = newMode;
		panel = newPanel;
	}
	
	public void run() {
		
		String processType = "";
		if (mode == 0) {
			processType = "Compiling";
		} else if (mode == 1) {
			processType = "Running";
		}
		
		try {
			
//			Object[] possibleValues = { "First", "Second", "Third" };
//			Object selectedValue = JOptionPane.showInputDialog(null,
//			"Choose one", "Input",
//			JOptionPane.INFORMATION_MESSAGE, null,
//			possibleValues, possibleValues[0]);
			
			int kill = JOptionPane.showOptionDialog(panel.getParent(),
				    "Executing: " + msg,
				    "Executing...",
				    JOptionPane.OK_CANCEL_OPTION,
				    JOptionPane.INFORMATION_MESSAGE,
				    null,
				    new String[] {"Stop", "Cancel"},
				    "Stop");
			if (kill == 0) {
				p.destroy();
				panel.printMsgEverywhere("\n" + processType + " gets cancelled.");
				if (mode == 1) panel.printMsgEverywhere("\nRunning time may not be correct!");
			} else {
				panel.printMsgEverywhere("\nLet "+ processType + " run until finished.");
			}
//					JOptionPane.showConfirmDialog(panel,
//							"Confirmation dialog box text message.",
//							"Confirmation Dialog Box", JOptionPane.OK_CANCEL_OPTION,
//							JOptionPane.INFORMATION_MESSAGE);
					
			Thread.sleep(1);		
			
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt(); // very important
			panel.printMsgEverywhere("\n" + processType + " finished.");
		}
		
	}

	
}
