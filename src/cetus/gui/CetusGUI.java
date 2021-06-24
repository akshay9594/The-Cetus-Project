package cetus.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.*;
import java.io.File;
import javax.swing.*;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import cetus.exec.Driver;

public class CetusGUI extends JFrame implements KeyListener, ActionListener{

	private JPanel inputPanel, outputPanel;
	private CetusGUIAboutPanel aboutPanel;
	private CetusGUIConsolePanel consolePanel;
	private CetusGuiSpeedupPanel speedupPanel;
//	private CetusGuiSpeedupPanelClient speedupPanelClient; //server version
//	private CetusGuiDebugPanel updatePanel;
	private JButton openMpButton, openFileButton, saveInputFileButton, 
			saveInputFileAsButton, saveOpenMpFileAsButton, saveOpenMpFileButton, 
			demosButton, openOpenMpButton;
	private int numExamples = 0;
	private JButton[] exampleButtons;
	private String[] demoFileStrings;
	private String[] demoNames;
	private String[] demoFirstLines;
	private String latestFileName = null;
	private Boolean demosOnOff = false;
	
	JCheckBox checkDemos;
	private String
			checkDemosString = "Demos",
			checkDemosTip = "Turn ON/OFF demo examples";
	
	private JLabel label1, label2, label3;
	private JFileChooser fileChooser;
	public static File inputFile = null //= new File(CetusGUITools.user_dir+CetusGUITools.file_sep+"foo.c")
			, defaultOpenMpFile = null, openMpFile = null;
	private JTextArea inputStatusTextArea, openMpStatusTextArea;
	private RSyntaxTextArea inputTextArea, openMpTextArea;
	private RTextScrollPane inputScrollPane, openMpScrollPane;
	private String[] optionsArray;
	public static String consoleSeperator = "\n*******************************************************************************"
			, outputFilePath = CetusGUITools.user_dir+CetusGUITools.file_sep+"cetus_output"
			, openMpFileName, inputFileName, inputFileNameLowerCase;
	private JTabbedPane tabbedPane;
	private int buttonHeight = CetusGUITools.buttonHeight, 
			buttonWidth = CetusGUITools.buttonWidth, 
			buttonTop = CetusGUITools.buttonTop, 
			buttonGap = CetusGUITools.buttonGap, 
			buttonWidthAddtion = CetusGUITools.buttonWidthAddtion, 
			labelHeight = CetusGUITools.labelHeight, 
			exampleButtonTop = buttonTop, 
			exampleIndex = 0, 
			buttonIndex = -1;
	private static CetusGUIOptionsPanel optionsPanel;
	public static String 
			sameInOutDirMessage1 = "Cannot select the same directory as input file. " +
				"Please select a new output directory",
			sameInOutDirMessage2 = "Output file directory cannot be the same as " +
				"input file. Please change it on Option page. ",
			printClass = "[CetusGUI] ";
	public boolean jarOrClass; 
	
	private JRadioButton radioLocalRun, radioServerRun;
	private ButtonGroup groupRadioRun;
	private String 
			radioLocalRunString = "Local",
			radioServerRunString = "Server",
			radioLocalRunTip = "Translate C code on your local machine",
			radioServerRunTip = "Translate C code by Cetus remote server";
	public static int translateWhere = 0; 
	private boolean edited = false;
	
//	public static int 
//		numOptions = (new CetusGUIOptionsPanel()).getNumFullOptions(), numPreferences = numOptions+100;
//	public static String[] preferencesStrings = new String[numPreferences];
	
	public CetusGUI(String Title, int width, int height) {
				
		setTitle(Title);
		setSize(width, height);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		inputPanel = new JPanel();
		//getContentPane().add(inputPanel);
		inputPanel.setToolTipText("Input sequential C code, to translate it into parallel OpenMP code, click [Output]");
		inputPanel.setLayout(new BorderLayout(5, 5)); //new FlowLayout()
		//inputPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

		label1 = new JLabel(" ");
		label1.setPreferredSize(new Dimension(-1,labelHeight));
		//label1.setPreferredSize(new Dimension(-1,buttonHeight*2));
		inputPanel.add(label1,BorderLayout.PAGE_START);
		//inputPanel.add(openFileButton);	
		
		//label3 = new JLabel("         Demos:         ");
		label3 = new JLabel("");
		label3.setPreferredSize(new Dimension(buttonWidth+buttonWidthAddtion,-1));
		label3.setVerticalAlignment(SwingConstants.TOP);
		inputPanel.add(label3,BorderLayout.WEST);
		label3.setVisible(false);
		
//		label2 = new JLabel("You can directly edit your C file in the text area. Clicking [Save C File] will overwrite your original C file.");
//		inputPanel.add(label2,BorderLayout.PAGE_END);
		
		openFileButton = new JButton("Open");
		openFileButton.setBounds(++buttonIndex*(buttonGap+buttonWidth), buttonTop, buttonWidth, buttonHeight);
		//openFileButton.setHorizontalAlignment(10);
		openFileButton.setToolTipText("Open serial input C file");
		//openFileButton.setLayout(new BorderLayout());
		//inputPanel.add(openFileButton,BorderLayout.PAGE_START);
		inputPanel.add(openFileButton);

		saveInputFileButton = new JButton("Save");
		saveInputFileButton.setBounds(++buttonIndex*(buttonGap+buttonWidth), buttonTop, buttonWidth, buttonHeight);
		saveInputFileButton.setToolTipText("Overwrite input/edited C file");
		//inputPanel.add(saveInputFileButton,BorderLayout.LINE_START);
		inputPanel.add(saveInputFileButton);

		saveInputFileAsButton = new JButton("Save As");
		saveInputFileAsButton.setBounds(++buttonIndex*(buttonGap+buttonWidth), buttonTop, buttonWidth, buttonHeight);
		saveInputFileAsButton.setToolTipText("Save input/edited C file as...");
		//inputPanel.add(saveInputFileAsButton,BorderLayout.LINE_END);
		inputPanel.add(saveInputFileAsButton);
		
		demosButton = new JButton();
		demosButton.setText((demosOnOff?"Hide":"Show")+" Demos");			
		demosButton.setBounds(++buttonIndex*(buttonGap+buttonWidth), buttonTop, buttonWidth+buttonWidthAddtion, buttonHeight);
		demosButton.setToolTipText("Turn " + (demosOnOff?"OFF":"ON")+" demo examples");
		demosButton.addActionListener(this);
		inputPanel.add(demosButton);
		//examplesButton.setVisible(false);

		checkDemos = new JCheckBox(checkDemosString, false);
		checkDemos.setToolTipText(checkDemosTip);
		checkDemos.addActionListener(this);
		checkDemos.setBounds(++buttonIndex*(buttonGap+buttonWidth)+buttonWidthAddtion, buttonTop, buttonWidth, buttonHeight);
		
		/**
		 * Demo buttons
		 */
		
		/**
		 * cannot move it into the static method getFileNamesInResource in CetusGUITools.java
		 */
    //System.out.println("Getting demo path"); 
		demoNames = CetusGUITools.getFileNamesInResource(CetusGUI.class, CetusGUITools.examplesLocation);
		
    //System.out.println("Number of demo C code: "+demoNames.length);
//		System.out.println(CetusGUITools.jarPath);
		
		/**
		 * this condition part is for identifying if Cetus is running by cetus.jar or .class (also in
		 * eclipse); the way to find demo C code is different for jar and class.
		 */
		if (demoNames.length==0) { //jar file
			jarOrClass = true;
			demoFileStrings = CetusGUITools.getAllFileNamesUnderTopFolderInJar(CetusGUITools.jarPath, "examples/");
			//System.out.println(CetusGUITools.convertArrayStringsToStringLines(demoFileStrings));
			demoNames = CetusGUITools.getSubStringsLast(demoFileStrings, "/");
			demoFirstLines = CetusGUITools.getFirstLinesInJar(CetusGUITools.jarPath, demoFileStrings);
			//System.out.println(CetusGUITools.convertArrayStringsToStringLines(demoNames));
		} else { //class in eclipse
			jarOrClass = false;
			demoFileStrings = CetusGUITools.combineStringIntoArrayStrings(CetusGUITools.examplesLocation
					, demoNames, 1);
			demoFirstLines = CetusGUITools.getFirstLinesOfResourceFiles(CetusGUI.class, demoFileStrings);
//			System.out.println(CetusGUITools.convertArrayStringsToStringLines(demoNames));
//			System.out.println(CetusGUITools.convertArrayStringsToStringLines(demoFileStrings));
//			System.out.println(CetusGUITools.convertArrayStringsToStringLines(demoFirstLines));	
		}
		
		
		numExamples = demoNames.length;
		exampleButtons = new JButton[numExamples];
		for (int i = 0; i < numExamples; i++) {
			exampleButtons[i] = new JButton(demoNames[i]);
			exampleButtons[i].setBounds(0, exampleButtonTop+(++exampleIndex)*(buttonHeight+buttonGap), buttonWidth+buttonWidthAddtion, buttonHeight);
			exampleButtons[i].setToolTipText(demoFirstLines[i]);
			exampleButtons[i].addActionListener(this);
			inputPanel.add(exampleButtons[i]);
			exampleButtons[i].setVisible(false);
		}

		inputStatusTextArea = new JTextArea();
		inputStatusTextArea.setLineWrap(true);
		inputStatusTextArea.setWrapStyleWord(true);
		inputStatusTextArea.setEditable(false);
		inputStatusTextArea.setBackground(Color.lightGray);
		inputPanel.add(inputStatusTextArea,BorderLayout.PAGE_END);

		inputTextArea = new RSyntaxTextArea();
		inputTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_C);
		//inputTextArea.setAutoIndentEnabled(true); //it is for edit a new text file in the text area; it is always on
		inputTextArea.setEditable(true);
		inputTextArea.addKeyListener(this);
		inputScrollPane = new RTextScrollPane(inputTextArea);
		//inputScrollPane.setFoldIndicatorEnabled(true);
		inputPanel.add(inputScrollPane);

		optionsPanel = new CetusGUIOptionsPanel();
		
		outputPanel = new JPanel();
		outputPanel.setToolTipText("OpenMP code");
		outputPanel.setLayout(new BorderLayout(5, 5)); //new FlowLayout()
		label2 = new JLabel(" ");
		label2.setPreferredSize(new Dimension(-1,labelHeight));
		outputPanel.add(label2,BorderLayout.PAGE_START);
		
		openMpButton = new JButton("OpenMP");
		openMpButton.setBounds(0, buttonTop, buttonWidth, buttonHeight);
		openMpButton.setToolTipText("Translate input C code from serial to parallel OpenMP");
		outputPanel.add(openMpButton);

		openOpenMpButton = new JButton("Open");
		openOpenMpButton.setBounds(buttonGap+buttonWidth, buttonTop, buttonWidth, buttonHeight);
		openOpenMpButton.setToolTipText("Open existing OpenMP code (you can test it with Cetus [speedup])");
		openOpenMpButton.addActionListener(this);
		outputPanel.add(openOpenMpButton);

		saveOpenMpFileButton = new JButton("Save");
		saveOpenMpFileButton.setBounds(2*buttonGap+2*buttonWidth, buttonTop, buttonWidth, buttonHeight);
		saveOpenMpFileButton.setToolTipText("Save (edited) OpenMP code ...");
		outputPanel.add(saveOpenMpFileButton);

		saveOpenMpFileAsButton = new JButton("Save As");
		saveOpenMpFileAsButton.setBounds(3*buttonGap+3*buttonWidth, buttonTop, buttonWidth, buttonHeight);
		saveOpenMpFileAsButton.setToolTipText("Save (edited) OpenMP code as...");
		outputPanel.add(saveOpenMpFileAsButton);
		
		radioLocalRun = new JRadioButton(radioLocalRunString, true);
		radioLocalRun.addActionListener(this);
		radioLocalRun.setToolTipText(radioLocalRunTip);
		radioLocalRun.setBounds(4*buttonGap+4*buttonWidth, buttonTop, buttonWidth-buttonWidth/3, buttonHeight);
		
		radioServerRun = new JRadioButton(radioServerRunString, false);
		radioServerRun.addActionListener(this);
		radioServerRun.setToolTipText(radioServerRunTip);
		radioServerRun.setBounds(5*buttonGap+5*buttonWidth-buttonWidth/3, buttonTop, buttonWidth, buttonHeight);
		
		groupRadioRun = new ButtonGroup();
		groupRadioRun.add(radioLocalRun);
		groupRadioRun.add(radioServerRun);
//		outputPanel.add(radioLocalRun);
//		outputPanel.add(radioServerRun);
		
		openMpTextArea = new RSyntaxTextArea();
		openMpTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_C);
		openMpTextArea.setEditable(true);
		openMpScrollPane = new RTextScrollPane(openMpTextArea);
		outputPanel.add(openMpScrollPane);

		openMpStatusTextArea = new JTextArea();
		openMpStatusTextArea.setLineWrap(true);
		openMpStatusTextArea.setWrapStyleWord(true);
		openMpStatusTextArea.setEditable(false);
		openMpStatusTextArea.setBackground(Color.lightGray);
		openMpStatusTextArea.setText("Click [Translate] to translate C code");
		outputPanel.add(openMpStatusTextArea,BorderLayout.PAGE_END);

		aboutPanel = new CetusGUIAboutPanel();		
		consolePanel = new CetusGUIConsolePanel();
		speedupPanel = new CetusGuiSpeedupPanel();
//		speedupPanelClient = new CetusGuiSpeedupPanelClient();
//		updatePanel = new CetusGuiDebugPanel();
		
		tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Input", null, inputPanel, "Input sequential C code, to translate it into parallel OpenMP code, go to [Output]");
		tabbedPane.addTab("Options", null, optionsPanel, "Cetus Options");
		tabbedPane.addTab("Output", null, outputPanel, "Translate input sequential C code into parallel OpenMP code");
		tabbedPane.addTab("Speedup", null, speedupPanel, "Compile and run serial and OpenMP code, calculate speedup and efficiency, and display graphs");
		tabbedPane.addTab("Console", null, consolePanel, "Console Message and Information");
		tabbedPane.addTab("About", null, aboutPanel, "Help and About");
		
		
//		add(tabbedPane, BorderLayout.WEST);
		add(tabbedPane);
		

		
	}

//	public CetusGUI(String[] args) {
//		this("Cetus", 600, 500);
//		initUI();
//	}
	
	public CetusGUI() {
		this("Cetus - PAROT Research Group, University of Delaware - Dept. of Electrical & Computer Engineering", 800, 600);
		initUI();
		optionsPanel.autoCheckUpdateSaveLoadOptions(); //The reason that put autoLoadOptions() 
		//here is to make sure the print out message for loading options appears after Cetus main messages
//		if (Driver.updateNeeded) { //update tab shown only if a new update is available
//			tabbedPane.addTab("Update", null, updatePanel, Driver.versionInfoString);
//			int indexTabUpdate = tabbedPane.indexOfComponent(updatePanel);
//			tabbedPane.setBackgroundAt(indexTabUpdate, new Color(0,255,0));
//		}
	}

	public final void initUI() {
		
//		System.out.println("12345".matches(CetusGUITools.regexPositiveInteger5OrMoreDigits));
//		System.out.println("3.12.563.5698.12".matches(CetusGUITools.regexVersionNumber));
//		System.out.println("********** User working directory: "+CetusGUITools.user_dir);
//		System.out.println("********** User home directory: "+CetusGUITools.user_home+CetusGUITools.file_sep+preferencesFileName);
		
		//redirectSystemStreams();
		System.out.println(Driver.versionInfoString);
		System.out.println(CetusGUITools.osMessage);
		System.out.println(CetusGUITools.guideMessage);
		System.out.println(CetusGuiSpeedupPanel.aboutMessage);
		
		inputStatusTextArea.setText(CetusGUITools.osMessage);
		inputStatusTextArea.append("\n"+CetusGUITools.guideMessage);
//		inputTextArea.setText(CetusGUITools.osMessage);
//		inputTextArea.append("\n"+CetusGUITools.guideMessage);
		

//		optionsArray = new String[numOptions];
//		for (i=0;i<numOptions;i++) {
//			optionsArray[i]="-";
//		}

//		//---------- default input file: foo.c ---------------- 
//		System.out.println("----------------");
//		inputStatusTextArea.append("\n----------------");
//		if (inputFile.exists()) {
//			System.out.println("Loading default sample input C file: "+inputFile.getAbsolutePath().toString()+" ... ");
//			inputStatusTextArea.append("\nLoading default sample input C file: "+inputFile.getAbsolutePath().toString()+" ... ");
//			String text = CetusGUITools.readFileToString(inputFile);
//			inputTextArea.setText(text);
//			System.out.println("Successful!");
//			inputStatusTextArea.append("Successful!");
//		} else {
//			String noSampleCFileMessage1 = "The default sample input C file " + inputFile.toString()
//				+ " should be put in the same directory as the Cetus tool: "+ new File("").getAbsolutePath();
//			String noSampleCFileMessage2 = "No default sample input C file exists! Loading default sample input C file failed";
//			System.out.println(noSampleCFileMessage1);
//			System.out.println(noSampleCFileMessage2);
//			inputStatusTextArea.append("\n"+noSampleCFileMessage1);
//			inputStatusTextArea.append("\n"+noSampleCFileMessage2);
//			inputTextArea.append("\n"+noSampleCFileMessage1);
//			inputTextArea.append("\n"+noSampleCFileMessage2);
//		}

		openFileButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) 
			{
				//CetusGUITools.getFilesDirs(inputPanel, "C files (*.c)", "C");
				File selectFile = CetusGUITools.getFile(inputPanel, "C files (*.c)", "C");
				if (selectFile!=null) {
					inputFile = selectFile;
					latestFileName = inputFile.getName();
					System.out.println(consoleSeperator);
					System.out.println("Opening input file: "+inputFile.toString());
					inputStatusTextArea.setText("Opening input file: "+inputFile.toString());
//					inputTextArea.setText("Opening input file: " + inputFile.toString());
					String text = CetusGUITools.readFileToString(inputFile);
					if (text!=null) {
						edited = false;
						CetusGuiSpeedupPanel.newInput = 1;
						inputTextArea.setText(text);
						inputStatusTextArea.setText("Opened input file: "+inputFile.toString());
						System.out.println("Opened input file: "+inputFile.toString());
					} else {
						printDialogMsg(inputStatusTextArea, tabbedPane, "Error when opening input file: "+inputFile.toString());
					}					
				}

			}
		});

		saveInputFileButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent event) {
				if (inputFile==null) {
					String pleaseLoadMsg = "Please open an input C file before saving or click [Save As] to save the text area into a new file";
//					inputStatusTextArea.setText(pleaseLoadMsg);
//					System.out.println(pleaseLoadMsg);
//					JOptionPane.showMessageDialog(tabbedPane, pleaseLoadMsg);
					printDialogMsg(inputStatusTextArea, tabbedPane, pleaseLoadMsg);
					return;
				}
				System.out.println(consoleSeperator);
				System.out.println("Saving input C file: "+inputFile.toString());
				inputStatusTextArea.setText("Saving input C file: "+inputFile.toString());
				boolean success = CetusGUITools.writeStringToFileAddNewLine(inputFile, inputTextArea.getText());
				//inputStatusTextArea.setText("Saved input C file: "+inputFile.toString());
				if (success==true) {
					edited = false;
					CetusGuiSpeedupPanel.newInput = 1;
					inputStatusTextArea.setText("Saved input C file: "+inputFile.toString());
					System.out.println("Saved input C file: "+inputFile.toString());
//					printDialogMsg(inputStatusTextArea, tabbedPane, "Saved input C file: "+inputFile.toString());
				} else {
//					inputStatusTextArea.setText("Error when saving: "+inputFile.toString());
//					System.out.println("Error when saving: "+inputFile.toString());
					printDialogMsg(inputStatusTextArea, tabbedPane, "Error when saving: "+inputFile.toString());
				}					
			}
		});

		saveInputFileAsButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent event) 
			{
				File selectFile = CetusGUITools.saveFile(inputPanel, "C files (*.c)", "C", latestFileName);
				if (selectFile!=null) {
					inputFile = selectFile;
					latestFileName = inputFile.getName();
					System.out.println(consoleSeperator);
					System.out.println("Saving input C file as: "+inputFile.toString());
					inputStatusTextArea.setText("Saving input C file as: "+inputFile.toString());
					boolean success = CetusGUITools.writeStringToFileAddNewLine(inputFile, inputTextArea.getText());
					if (success==true) {
						edited = false;
						CetusGuiSpeedupPanel.newInput = 1;
						inputStatusTextArea.setText("Saved input C file as: "+inputFile.toString());
						System.out.println("Saved input C file as: "+inputFile.toString());
					} else {
//						inputStatusTextArea.setText("Error when saving: "+inputFile.toString());
//						System.out.println("Error when saving: "+inputFile.toString());
						printDialogMsg(inputStatusTextArea, tabbedPane, "Error when saving input C file as: "+inputFile.toString());
					}					
				}
			}
		});
		
		saveOpenMpFileButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent event) {
				if (openMpFile==null) {
					String pleaseRunMsg = "Please translate C code first or click [Save As] to save the text area into a new file";
					printDialogMsg(openMpStatusTextArea, tabbedPane, pleaseRunMsg);
					return;
				}
				System.out.println(consoleSeperator);
				System.out.println("Saving OpenMP code: "+openMpFile.toString());
				openMpStatusTextArea.setText("Saving OpenMP code: "+openMpFile.toString());
				boolean success = CetusGUITools.writeStringToFileAddNewLine(openMpFile, openMpTextArea.getText());
				if (success==true) {
					openMpStatusTextArea.setText("Saved OpenMP code: "+openMpFile.toString());
					System.out.println("Saved OpenMP code: "+openMpFile.toString());
					CetusGuiSpeedupPanel.newOpenMP = 1;
				} else {
					printDialogMsg(openMpStatusTextArea, tabbedPane, "Error when saving OpenMP code: "+openMpFile.toString());
				}		
			}
		});

		saveOpenMpFileAsButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent event) 
			{
				File selectFile = CetusGUITools.saveFile(outputPanel, "C files (*.c)", "C"
						, openMpFile==null?null:openMpFile.getName());
				if (selectFile!=null) {
					openMpFile = selectFile;
					System.out.println(consoleSeperator);
					System.out.println("Saving OpenMP code as: "+openMpFile.toString());
					openMpStatusTextArea.setText("Saving OpenMP code as: "+openMpFile.toString());
					boolean success = CetusGUITools.writeStringToFileAddNewLine(openMpFile, openMpTextArea.getText());
					if (success==true) {
						openMpStatusTextArea.setText("Saved OpenMP code as: "+openMpFile.toString());
						System.out.println("Saved OpenMP code as: "+openMpFile.toString());
						CetusGuiSpeedupPanel.newOpenMP = 1;
					} else {
						printDialogMsg(openMpStatusTextArea, tabbedPane, "Error when saving OpenMP code as: "+openMpFile.toString());
					}
				}
			}
		});
		
		openMpButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				
				if (inputFile==null) {
					printDialogMsg(openMpStatusTextArea, tabbedPane, "Please open an input C file or save a demo C code before proceeding");
					return;
				} else if (!inputFile.exists()){
					String inputFileNotExistMsg = "Input file: "+ inputFile.getAbsolutePath().toString() + " does not exist!";
					openMpStatusTextArea.setText(inputFileNotExistMsg);
					System.out.println(inputFileNotExistMsg);
					JOptionPane.showMessageDialog(tabbedPane, inputFileNotExistMsg);
					return;
				} else {
					openMpStatusTextArea.setText("Input file is: "+ inputFile);
					System.out.println("Input file is: "+ inputFile.getAbsolutePath().toString());
				}
				
				if (outputFilePath.equalsIgnoreCase(inputFile.getParent())){
					String sameInOutDirMessage2CurrentDir = printClass
						+ sameInOutDirMessage2
						+ "\nInput File Dir: " + inputFile.getParent()
						+ "\nOutput File Dir: " + outputFilePath;
					openMpStatusTextArea.setText(sameInOutDirMessage2CurrentDir);
					System.out.println(sameInOutDirMessage2CurrentDir);
					
					JOptionPane.showMessageDialog(tabbedPane
						, sameInOutDirMessage2CurrentDir);
					
					tabbedPane.setSelectedComponent(optionsPanel);
					optionsPanel.checkAdvOptionsPanel.setSelected(true);
					optionsPanel.advOptionsPanel.setVisible(true);
					return;
				}
				
				/**
				 * if input text area is changed, user may want to save it before proceeding
				 */
				if (edited){
					System.out.println("Input text area has been edited. Do you want to save your change before translation?");
					int editedDo = JOptionPane.showOptionDialog(
							outputPanel.getParent(),
						    "Input text area has been edited. Do you want to save your change before translation?",
						    "Save Edit",
						    JOptionPane.YES_NO_CANCEL_OPTION,
						    JOptionPane.QUESTION_MESSAGE,
						    null,
						    (new String[]{"Yes","No","Check Input", "Cancel"}),
						    "Check Input");
					System.out.println("Selected option: "+editedDo);
					if (editedDo == JOptionPane.YES_OPTION) {
						openMpStatusTextArea.setText("Translation after saving input file: "+inputFile);
						System.out.println("Translation after saving input file: "+inputFile);
						saveInputFileButton.doClick();
					} else if (editedDo == JOptionPane.NO_OPTION){
						openMpStatusTextArea.setText("Translation without saving edited input file: "+inputFile);
						System.out.println("Translation without saving edited input file: "+inputFile);
					} else if (editedDo == JOptionPane.CANCEL_OPTION) {
						openMpStatusTextArea.setText("Check input");
						System.out.println("Check input");
						tabbedPane.setSelectedComponent(inputPanel);
						return;
					} else {
						openMpStatusTextArea.setText("Translation cancelled");
						System.out.println("Translation cancelled");
						return;
					}
				}
				
				openMpFileName = inputFile.getName();
				openMpFile = new File(outputFilePath+CetusGUITools.file_sep+openMpFileName);
				System.out.println("OpenMP code is: "+ openMpFile.toString());

				if (openMpFile.exists()) {
					System.out.println("Deleting previous OpenMP code: "+openMpFile.toString());
					openMpFile.delete();
					System.out.println("Deleted previous OpenMP code: "+openMpFile.toString());
				} else {
					System.out.println("No same OpenMP code exists and no delete needed");
				}
				openMpTextArea.setText("");
				System.out.println("Cleared [OpenMP] text area");
				
				optionsArray = optionsPanel.getOptionsArray();
//				optionsArray[optionsArray.length-2] = "-outdir="+outputFilePath;
				optionsArray[optionsArray.length-1] = inputFile.toString();
				//arrayOptions = new String[] {preprocessor,"-parallelize-loops","-",inputFile.toString()};
				System.out.println(consoleSeperator);
				
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		    	try {
		    		if (CetusGUIOptionsPanel.useServer()) {//use Cetus remote server to process
		    			printOpenMpMsg("*** "+radioServerRunTip+" ***");
		    			TCPClient.request(optionsArray);
		    		}
		    		else {//use your local machine to process
		    			printOpenMpMsg("*** "+radioLocalRunTip+" ***");
						System.out.println("Translating C code by command: " 
								+"\njava -jar " + CetusGUITools.jarPath +" " 
								+ CetusGUITools.convertArrayStringsToStringSpaces(optionsArray)
								//+ Arrays.toString(optionsArray)
								+"\nOpenMP code is " + openMpFile.toString()
								+". Please click tab [OpenMP] for the OpenMP code!");
						
						openMpStatusTextArea.setText("Translating C code by command: " 
								+"\njava -jar " + CetusGUITools.jarPath +" " 
								+ CetusGUITools.convertArrayStringsToStringSpaces(optionsArray)
								//+ Arrays.toString(optionsArray)
								+"\nOpenMP code is " + openMpFile.toString());
		    			(new SubDriver()).run(optionsArray);
		    		}
//		    		else {
//		    			printOpenMpMsg("Error: translate engine is undefined");
//		    		}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		    	setCursor(null);
		    	
				String openMpTextString = CetusGUITools.readFileToString(openMpFile);
				if (openMpTextString!=null && openMpTextString.length()>0){
					openMpTextArea.setText(openMpTextString);
					if (CetusGUIOptionsPanel.useServer()) {
						openMpStatusTextArea.setText("Finished translating C code by Cetus remote server" 
								+ "\nPlease check above OpenMP text area for the OpenMP code: "
								+ openMpFile.toString() + ". Check [Console] for all messages");
						System.out.println(
								consoleSeperator + "\nFinished translating C code by Cetus remote server" 
								+ "\nPlease check [OpenMP] text area for the OpenMP code: " 
								+ openMpFile.toString());
					} else {
						openMpStatusTextArea.setText("Finished translating C code by command: " 
								+ "\njava -jar " + CetusGUITools.jarPath +" " 
								+ CetusGUITools.convertArrayStringsToStringSpaces(optionsArray)
								//+ Arrays.toString(optionsArray) 
								+ "\nPlease check above OpenMP text area for the OpenMP code: "
								+ openMpFile.toString() + ". Check [Console] for all messages");
						System.out.println(
								consoleSeperator + "\nFinished translating C code by command: " 
								+ "\njava -jar " + CetusGUITools.jarPath +" " 
								+ CetusGUITools.convertArrayStringsToStringSpaces(optionsArray)
								//+ Arrays.toString(optionsArray) 
								+ "\nPlease check [OpenMP] text area for the OpenMP code: " 
								+ openMpFile.toString());
					}
					CetusGuiSpeedupPanel.newOpenMP = 1;
				} else {
					String openMpFileErrorMsg = "Error translating C code." 
							+ "\nThe OpenMP code:  "	+ openMpFile.toString() 
							+ " may not be correct. " + "\nCheck [Console] for all messages";
					JOptionPane.showMessageDialog(tabbedPane, openMpFileErrorMsg);
					if (CetusGUIOptionsPanel.useServer()) {
						System.out.println(
								consoleSeperator + "\nError translating C code by Cetus remote server." 
								+ "\nThe OpenMP code: "	+ openMpFile.toString() 
								+ " may not be correct. ");
						openMpStatusTextArea.setText("Error translating C code by Cetus remote server." 
								+ "\nThe OpenMP code: "	+ openMpFile.toString() 
								+ " may not be correct. " + "Check [Console] for all messages");
					} else {
						System.out.println(
								consoleSeperator + "\nError translating C code by command: " 
								+ "\njava -jar " + CetusGUITools.jarPath +" " 
								+ CetusGUITools.convertArrayStringsToStringSpaces(optionsArray) 
								+ "\nThe OpenMP code: "	+ openMpFile.toString() 
								+ " may not be correct. ");
						openMpStatusTextArea.setText("Error translating C code by command: " 
								+ "\njava -jar " + CetusGUITools.jarPath +" " 
								+ CetusGUITools.convertArrayStringsToStringSpaces(optionsArray) 
								+ "\nThe OpenMP code: "	+ openMpFile.toString() 
								+ " may not be correct. " + "Check [Console] for all messages");
					}
					//tabbedPane.setSelectedComponent(consolePanel);
					openMpFile = null;
					CetusGuiSpeedupPanel.newOpenMP = 0;
				}
				System.out.println(consoleSeperator);
			}
		});

	}
	
	public static void printDialogMsg (Component statusArea, Component dialogParent, String msg) {
		((JTextArea) statusArea).setText(msg);
		System.out.println(msg);
		JOptionPane.showMessageDialog(dialogParent, msg);					
	}
	
	public static void checkUpdateSaveOptions () {
		optionsPanel.checkUpdateSaveOptions ();
	}

	@Override
	public void actionPerformed(ActionEvent evt) {
		//
		Object source = evt.getSource();
		
		for (int i = 0; i < numExamples; i++) {
			if (source.equals(exampleButtons[i])) {
				pressDemoButton(i);
				return;
			}
		}
		
		if (source.equals(checkDemos)) {
			demosOnOff = checkDemos.isSelected(); 
			label3.setVisible(demosOnOff);
			for (int i = 0; i < numExamples; i++) {
				exampleButtons[i].setVisible(demosOnOff);
			}			
			String info = "Turned "+(demosOnOff?"ON":"OFF")+" demo examples";
			System.out.println(info);
			inputStatusTextArea.setText(info);
			return;
		} else if (source.equals(demosButton)) {
			demosOnOff = !demosOnOff;
			label3.setVisible(demosOnOff);
			for (int i = 0; i < numExamples; i++) {
				exampleButtons[i].setVisible(demosOnOff);
			}			
			demosButton.setText((demosOnOff?"Hide":"Show")+" Demos");			
			demosButton.setToolTipText("Turn " + (demosOnOff?"OFF":"ON")+" demo examples");
			String info = "Turned "+(demosOnOff?"ON":"OFF")+" demo examples";
			System.out.println(info);
			inputStatusTextArea.setText(info);
			
//			int exampleIndex = examplesDialog();
//			if (exampleIndex<0) return;
//			System.out.println("Example " + (exampleIndex + 1));
//			inputTextArea.setText("Example " + (exampleIndex + 1));
			return;
		} else if (source.equals(radioLocalRun)) {
			translateWhere = 0;
			openMpStatusTextArea.setText(radioLocalRunTip);
			System.out.println(radioLocalRunTip);
		} else if (source.equals(radioServerRun)) {
			translateWhere = 1;
			openMpStatusTextArea.setText(radioServerRunTip);
			System.out.println(radioServerRunTip);
		} else if (source.equals(openOpenMpButton)) {
			File selectFile = CetusGUITools.getFile(outputPanel, "C files (*.c)", "C");
			if (selectFile!=null) {
				openMpFile = selectFile;
				System.out.println(consoleSeperator);
				printOpenMpMsg("Opening OpenMP code: "+openMpFile.toString());
				String openMpTextString = CetusGUITools.readFileToString(openMpFile);
				if (openMpTextString!=null && openMpTextString.length()>0){
					openMpTextArea.setText(openMpTextString);
					printOpenMpMsg("Opened OpenMP code: "+openMpFile.toString());
					CetusGuiSpeedupPanel.newOpenMP = 1;
				} else {
					printOpenMpMsg("Error when opening OpenMP code: "+openMpFile.toString());
				}
			}
		}
		
		
		
	}
	
//	public int examplesDialog() {
//		System.out.println("Please select an example");
//		Object[] options = {"Example 1",
//                "Example 2",
//                "Example 3",
//                "Example 4"};
//		//setLayout(new GridLayout(3,1));
//		return JOptionPane.showOptionDialog(this.getParent(),
//			    "Please select an example",
//			    "Cetus Examples",
//			    JOptionPane.YES_NO_CANCEL_OPTION,
//			    JOptionPane.QUESTION_MESSAGE,
//			    null,
//			    options,
//			    options[0]);
//	}

	public void pressDemoButton (int i) {
		String[] demoCodeArray;
		if (jarOrClass) demoCodeArray = CetusGUITools.readFileInJarToArrayStrings(CetusGUITools.jarPath
				, demoFileStrings[i], 0);
		else demoCodeArray = CetusGUITools.getResourceFile(CetusGUI.class, demoFileStrings[i], 0);
		String demoCodeString = CetusGUITools.convertArrayStringsToStringLines(demoCodeArray);
		inputTextArea.setText(demoCodeString);
		latestFileName = demoNames[i];
		inputFile = null;
		String info = "Load demo C code: "+demoNames[i]
				+". Please click [Save As] to save the demo code before translation!";
		System.out.println(info);
		inputStatusTextArea.setText(info);
		edited = false;
		CetusGuiSpeedupPanel.newInput = 0;
		//JOptionPane.showMessageDialog(tabbedPane, info);
//		File selectFile = CetusGUITools.saveFile(inputPanel, "C files (*.c)", "C", latestFileName);
//		if (selectFile!=null) {
//			inputFile = selectFile;
//			System.out.println(consoleSeperator);
//			System.out.println("Saving demo C file as: "+inputFile.toString());
//			inputStatusTextArea.setText("Saving demo C file as: "+inputFile.toString());
//			boolean success = CetusGUITools.writeStringToFile(inputFile, inputTextArea.getText());
//			if (success==true) {
//				inputStatusTextArea.setText("Saved demo C file as: "+inputFile.toString());
//				System.out.println("Saved demo C file as: "+inputFile.toString());
//			} else {
////				inputStatusTextArea.setText("Error when saving: "+inputFile.toString());
////				System.out.println("Error when saving: "+inputFile.toString());
//				printDialogMsg(inputStatusTextArea, tabbedPane, "Error when saving demo C file as: "+inputFile.toString());
//			}					
//		}		
	}

	@Override
	public void keyPressed(KeyEvent arg0) {
		// TODO Auto-generated method stub
		//inputStatusTextArea.append("Hello");
		
	}

	@Override
	public void keyReleased(KeyEvent arg0) {
		// TODO Auto-generated method stub
		//inputStatusTextArea.append("Kill");
		
	}

	@Override
	public void keyTyped(KeyEvent arg0) {
		// TODO Auto-generated method stub
		//typeCounter++;
		//inputStatusTextArea.append(" "+typeCounter);
		if (edited == false) inputStatusTextArea.append(" *** edited");
		edited = true;
	}
	
	// public static void main(String[] args) {
	//
	// OptionsGUI newGUI = new OptionsGUI(args);
	// // newGUI.initUI(args);
	// newGUI.setVisible(true);
	//
	// }
	
	public void printOpenMpMsg (String msg) {
		openMpStatusTextArea.setText(msg);
		System.out.println(msg);
	}


}
