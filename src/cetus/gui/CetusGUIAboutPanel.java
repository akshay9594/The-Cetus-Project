package cetus.gui;

import java.io.File;

import cetus.exec.Driver;

public class CetusGUIAboutPanel extends GUIBasicPanel{

	public static final String shortDashSeparator = "\n-------------\n";
	private static String[] aboutGuiArray = new String[] {
		"4",
		"About","Cetus Basic Info",
		"Help","Help and Options Info",
		"Licenses","Cetus, ANTLR and RSyntaxTextArea licenses",
		"Contact","Contact Cetus Team",
		"true","true","false","About Text",
		"true","true","false","About Status"};
	private String aboutMessage = Driver.versionInfoString+"\n"+Driver.version+ shortDashSeparator 
		+ "The binary version is only for personal and academic use, not for commercial use."
		+ shortDashSeparator +CetusGUITools.osMessage + shortDashSeparator +CetusGUITools.guideMessage
		+shortDashSeparator+CetusGUIOptionsPanel.defaultOptionsTip
		+" Click [Help] to see the description of Cetus command line options";
	private String helpMessage = "This is the description of Cetus command " +
			"line options. The corresponding options are available on " +
			"[Option] to set."+shortDashSeparator;
	
	private String aSeperator = "\n---------------\n";
	
	CetusGUIAboutPanel() {
		super(aboutGuiArray);
		textArea.setText(aboutMessage);
		textArea.append(aSeperator);
		if ((new File("resources/about/about.txt")).exists()) {
			textArea.append(CetusGUITools.readFileToString(new File("resources/about/about.txt")));
		} else {
			textArea.append(CetusGUITools.readFileInJarToString(CetusGUITools.jarPath,"about/about.txt"));
		}
	}

	@Override
	public void pressButton(int buttonIndex) {
		//
		if (buttonIndex == 0) {
			System.out.println(shortDashSeparator+"Display About information in [About]");
			//textArea.setText(Driver.version);
			textArea.setText(aboutMessage);
			textArea.append(aSeperator);
			if ((new File("resources/about/about.txt")).exists()) {
				textArea.append(CetusGUITools.readFileToString(new File("resources/about/about.txt")));
			} else {
				textArea.append(CetusGUITools.readFileInJarToString(CetusGUITools.jarPath,"about/about.txt"));
			}
		} 
		else if (buttonIndex == 1) {
			System.out.println(shortDashSeparator+"Display Help information in [About]");		
			textArea.setText(helpMessage);
			textArea.append((new SubDriver()).printUsage());
		} 
		else if (buttonIndex == 2) {
			System.out.println(shortDashSeparator+"Display Contact information in [About]");		
			textArea.setText("\n--------------------------\nCetus license\n--------------------------\n");
			if ((new File("resources/licenses/cetus_license.txt")).exists()) {
				textArea.append(CetusGUITools.readFileToString(new File("resources/licenses/cetus_license.txt")));
			} else {
				textArea.append(CetusGUITools.readFileInJarToString(CetusGUITools.jarPath,"licenses/cetus_license.txt"));
			}
			
			textArea.append("\n--------------------------\nAntlr license\n--------------------------\n");
			if ((new File("resources/licenses/antlr_license.txt")).exists()) {
				textArea.append(CetusGUITools.readFileToString(new File("resources/licenses/antlr_license.txt")));
			} else {
				textArea.append(CetusGUITools.readFileInJarToString(CetusGUITools.jarPath,"licenses/antlr_license.txt"));
			}
			
			textArea.append("\n--------------------------\nRSyntaxTextArea License\n--------------------------\n");
			if ((new File("resources/licenses/RSyntaxTextArea.License.txt")).exists()) {
				textArea.append(CetusGUITools.readFileToString(new File("resources/licenses/RSyntaxTextArea.License.txt")));
			} else {
				textArea.append(CetusGUITools.readFileInJarToString(CetusGUITools.jarPath,"licenses/RSyntaxTextArea.License.txt"));
			}
		}
		else if (buttonIndex == 3) {
			System.out.println(shortDashSeparator+"Display Contact information in [About]");		
			textArea.setText("Send your feedback to: cetus@ecn.purdue.edu");
		}
	}


}
