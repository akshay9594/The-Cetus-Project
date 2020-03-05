package cetus.gui;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.swing.SwingUtilities;

public class CetusGUIConsolePanel extends GUIBasicPanel{

	public static File consoleFile = new File(CetusGUITools.current_dir+CetusGUITools.file_sep+"console.txt");
	private static String[] aboutGuiArray = new String[] {
		"3",
		"Save","Save console message to: "+consoleFile.toString(),
		"Save As","Save console message to a new file",
		"Clear","Clear all message",
		"true","true","false","About Text",
		"true","true","false","Console Status"};
	private String aboutMessage = "";
	
//	CetusGUIAboutPanel(String[] guiStringArray) {
//		super(guiStringArray);
//		// TODO Auto-generated constructor stub
//	}
	
	CetusGUIConsolePanel() {
		super(aboutGuiArray);
		redirectSystemStreams();
//		System.out.println("\n----------------------\nDisplay version information in [About]");		
		textArea.setText(aboutMessage);		
	}
	
	
	public void pressButton(int buttonIndex) {
		if (buttonIndex == -1) {
			System.out.println("Error");
		}

		else if (buttonIndex == 0) {
			System.out.println(shortDashSeparator);
			System.out.println("Saving console message to file: "+consoleFile.toString());
			statusArea.setText("Saving console message to file: "+consoleFile.toString());
			boolean success = CetusGUITools.writeStringToFile(consoleFile, textArea.getText());
			if (success==true) {
				statusArea.setText("Saved console message to file: "+consoleFile.toString());
				System.out.println("Saved console message to file: "+consoleFile.toString());
			} else {
				statusArea.setText("Error when saving console message to file: "+consoleFile.toString());
				System.out.println("Error when saving console message to file: "+consoleFile.toString());
			}					
		}
		
		else if (buttonIndex == 1) {
			File selectFile = CetusGUITools.saveFile(this, null, null, consoleFile.getName());
			if (selectFile!=null) {
				consoleFile = selectFile;
				String newSaveButtonTip = "Save console message to: "+consoleFile.toString();
				buttons[0].setToolTipText(newSaveButtonTip);
				System.out.println(shortDashSeparator);
				System.out.println("Saving console message to file: "+consoleFile.toString());
				statusArea.setText("Saving console message to file: "+consoleFile.toString());
				boolean success = CetusGUITools.writeStringToFile(consoleFile, textArea.getText());
				if (success==true) {
					statusArea.setText("Saved console message to file: "+consoleFile.toString());
					System.out.println("Saved console message to file: "+consoleFile.toString());
				} else {
					statusArea.setText("Error when saving console message to file: "+consoleFile.toString());
					System.out.println("Error when saving console message to file: "+consoleFile.toString());
				}					
			}
			
			//textArea.setText("Save console...");
		}
		
		else if (buttonIndex == 2) {
			textArea.setText("");
			statusArea.setText("Console message cleared");
		}
	}

	private void updateTextArea(final String text) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				textArea.append(text);
			}
		});
	}

	private void redirectSystemStreams() {
		OutputStream out = new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				updateTextArea(String.valueOf((char) b));
			}

			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				updateTextArea(new String(b, off, len));
			}

			@Override
			public void write(byte[] b) throws IOException {
				write(b, 0, b.length);
			}
		};

		System.setOut(new PrintStream(out, true));
		System.setErr(new PrintStream(out, true));
	}
	
}
