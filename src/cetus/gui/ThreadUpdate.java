package cetus.gui;

import cetus.exec.Driver;

public class ThreadUpdate extends Thread {

	private int waitTimeMilliseconds;
	private boolean guiMsgDialogNeeded = false;
	
	public ThreadUpdate () {
		waitTimeMilliseconds = 10000;
	}
	
	public ThreadUpdate (int waitTimeSeconds) {
		waitTimeMilliseconds = waitTimeSeconds*1000;
	}

	public ThreadUpdate (int waitTimeSeconds, boolean guiNeeded) {
		this(waitTimeSeconds);
		guiMsgDialogNeeded = guiNeeded;
	}
	
	public void run() {
		try {
			//System.out.println("[Thread2] Check new version in "+waitTimeMilliseconds/1000+" seconds...");
			Thread.sleep(waitTimeMilliseconds);
			Driver.checkUpdate();
			if (guiMsgDialogNeeded) CetusGUI.checkUpdateSaveOptions();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			System.out.println("Thread: "+this.getId()+", "+this.getName()+ "; Checking Update (New Version) Interrupted!");
			//e.printStackTrace();
		}
	}

}
