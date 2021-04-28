/**

Zhengxiao (Tony) Li

3/21/13
# CetusGuiSpeedupPanel.java
- Whenever start a Graph dialog, all checked as long as input/OpenMP available.
- If user does not check sequential, let user decide if the existing sequential should be used to calculate speedup and efficiency
# PlotData.java
- Randomly position the graph on screen
# TCPClient.java
- Moved server to cetus machine; new IP address for the server


3/19/13
# Finished Graph
 - Wording for buttons
 - Graph 1: running time against number of threads
 - Graph 2: speedup against number of threads
 - Graph 3: efficiency against number of threads

3/18/13
# Workflows for Graph
- If input C file is not available, sequential is disabled and unchecked; else sequential is enabled
- If OpenMP C file is not available, all parallel modes are disabled and unchecked, else they are enabled
- Process all checked modes: 
* if no option is selected, just return; 
* if only sequential is selected, just compile and run input, and record the run time into graph array, no speedup or efficiency recorded; 
* if some parallel modes are also selected, compile and run OpenMP too, and record the run times, speedups and efficiencies; 
* if only some parallel modes are selected, no sequential is selected, compile and run each parallel mode, and record the run times; if sequential run time also exists from previous run, speedups and efficiencys are calculated and recorded
* for any empty graph array, it is a null
* Different mode for compiling C code: sequential and parallel
# Graph
- Added choices of number of threads selection dialog box
- Run and saved Arrays of number of threads, run time, speedup and efficiency for graph purpose for displaying 
- Select all/none for the choices of number of threads
- Functionazed five steps
- Added gcc/g++ option for graph
# Added radio options for using gcc or g++
# Tried to add a second thread to display progress and give user choice to kill main process of compiling and running; but the progress dialog hangs and could not be seen. Failed!!!???
# Compiling and running C file on the second thread were successful, but the main thread does not wait for the second thread, so user may continue to use the main Cetus thread and slow down the second thread which may be running C executable meanwhile.


3/17/13
Makes cetus call executable runnable on Linux:
# You need to add {"/bin/sh","-c"} before command
# Number of cores; $ OMP_NUM_THREADS=2 ./a.out

Workflows for speedup
# Steps
- Compile input C code in sequential mode
- Run input executable in sequential mode
- Compile OpenMP C code
- Run OpenMP executable in parallel mode
- Calculate speedup and efficiency
# Flags to indicate the status of objects: inputFile (if input C file is loaded), openMpFile (if translated OpenMP C file exists), inputExeFile (if input C file has been compiled into executable file), openMpExeFile (if OpenMP C file has been compiled into executable file), newInput (if input C file has been changed), newOpenMP (if OpenMP C file has been changed), seqRunTime (sequential running time, the initial value is negative), parRunTime (parallel running time, the initial value is negative)
# newInput: flag = true, whenever a new input C file is loaded, an existing input C file is saved, or the code in the input text area is saved as a new C file. Flag = false, after input C file gets compiled in [Speedup], or a demo code is loaded into the input text area
# newOpenMP: flag = true, whenever an input C file gets translated into OpenMP C file, an OpenMP C file gets saved, or anything in the OpenMP text area gets saved as a new file. Flag = false, whenever a translated OpenMP C file is invalid, or after translated OpenMP C file gets compiled in [Speedup]
# At the beginning of [Speedup], if no input nor OpenMP C file exists, none of the steps is enabled
# Scenario for input ---- No input C file is loaded: compiling input C is disabled; if no input C executable, running input C executable is disabled, else enabled.
# Scenario for input ---- input C file is loaded: both compiling and running are enabled; If newInput = true, both compiling and running are checked automatically for user; user can uncheck if wanting.
# Scenario for OpenMP ---- No OpenMP C file: compiling is disabled; if no OpenMP executable, running is disabled, else enabled
# Scenario for OpenMP ---- OpenMP C file exists: both compiling and running are enabled; if newOpenMP = true, both compiling and running are checked automatically for user; user can uncheck if wanting
# Scenario for calculating----- if both input and OpenMP C files exist, or both input and OpenMP executable exist, or both input C file and OpenMP executable exist, or both input executable and OpenMP C file exist, calculating option is enabled; if (newInput = true or newOutpu = true) and both input and OpenMP C file exist, calculating option is checked automatically for user; user can uncheck if wanting
# Scenario for different operation systems ---- Linux and Mac users can select number of threads for running parallel executable, starting 1 and up to the number of machine cores; Windows users cannot choose number of threads, which is fixed to the number of machine cores (currently I have not found out a way to specify number of threads when running parallel executables on Windows)
# Scenario for running the five steps ---- whenever compiling input or OpenMP C files is done, newInput or newOpenMP is set to false; whenever a step is done, the option for this step is unchecked automatically for users, meaning users do not need to run this step again next time, unless some previous scenarios happen; 

 */

package cetus.gui;


import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;

public class CetusGuiSpeedupPanelClient extends GUIBasicPanel{

	public static final String dashSeparator = "\n--------------------------------------------\n";
	public static File speedupFile = new File(CetusGUITools.current_dir+CetusGUITools.file_sep+"speedup.txt");
	private static String s1 = "Compile serial and OpenMP C code" 
			+ ", run executables, compute speedup and efficiency" +
			" and print out results.";
	private static String speedupDialogTitle = "Calculate";
	private static String speedupDialogMsg = 
		"Compile and run serial and OpenMP C files and calculate speedup and efficiency";
	private static String[] aboutGuiArray = new String[] {
		"5",
		speedupDialogTitle,speedupDialogMsg,
//		"Comp Input","Compile input C code in sequential mode",
//		"Run Input","Run input executable in sequential mode",
//		"Comp OpenMP","Compile OpenMP C code",
//		"Run OpenMP","Choose number of cores and run OpenMP executable in parallel mode",
//		"Calculate","Calculate speedup and efficiency",
		"Graphs","Draw charts of running time, speedup and efficiency",
		"Save","Save speedup results into: "+speedupFile.toString(),
		"Save As","Save speedup results into a file...",
		"Clear","Clear text area",
		"true","true","false","Text",
		"true","true","false",s1};
	public static int op = (System.getProperty("os.name").toLowerCase()).indexOf("win");
	public static String gcc = op<0?"gcc":"gcc-4";
	public static String gpp = op<0?"g++":"g++-4";
	public static String ext = op<0?".out":".exe";
	public static int numCores = Runtime.getRuntime().availableProcessors(), 
			numThreads = -1;
	public static String aboutMessage = dashSeparator+s1
			+ "\nYou can also choose number of threads " 
			+ "and draw graphs of speedup and efficiency"
			+ "\nYour computer has " + numCores + " processors"
			+ "\nOur Cetus remote server has 16 processors\n"+dashSeparator;
	public static File inputExeFile = null, openMpExeFile = null;
	public static int newInput = 0, newOpenMP = 0;
	public static double seqRunTime = -1.0, parRunTime = -1.0, 
			speedup = -1.0, efficiency = -1.0;
	private JCheckBox checkCompInput = new JCheckBox("Compile input C code in sequential mode");
	private JCheckBox checkRunInput = new JCheckBox("Run input executable in sequential mode");
	private JCheckBox checkCompOpenMP = new JCheckBox("Compile OpenMP C code");
	private JCheckBox checkRunOpenMP = new JCheckBox("Run OpenMP executable in parallel mode");
	private JCheckBox checkCalculate = new JCheckBox("Calculate speedup and efficiency");
	private String[] comboNumThreadsArray;
	private JComboBox comboNumThreads;
	private JRadioButton radioCompGcc, radioCompGpp;
	private ButtonGroup groupRadioComp;
	public static String gccOrGpp = gcc;
	private JCheckBox[] checksNumThreads;
	public static int[] threadsArray; //from 0 (seq) to numCores, except for Windows
	public static double[] runTimeArray; //both seq and par running time
	private JButton buttonSelectAll, buttonSelectNone; //buttons to select all/none for graph configuration dialog box
	
	CetusGuiSpeedupPanelClient() {
		super(aboutGuiArray);
		textArea.setText(aboutMessage);
		
		checkCompInput.setEnabled(false);
		checkRunInput.setEnabled(false);
		checkCompOpenMP.setEnabled(false);
		checkRunOpenMP.setEnabled(false);
		checkCalculate.setEnabled(false);
		
		//Threads combo box menu
		if (op < 0) {
			comboNumThreadsArray = new String[numCores];			
			for (int i = 0; i < comboNumThreadsArray.length; i++) {
				comboNumThreadsArray[i] = ""+(i+1);
			}
		} else {
			comboNumThreadsArray = new String[]{numCores+""};
		}
		
		comboNumThreads = new JComboBox(comboNumThreadsArray);
		comboNumThreads.setSelectedIndex(comboNumThreadsArray.length-1);
		if (op>=0) comboNumThreads.setToolTipText("On Windows, number of threads available for parallel run is fixed to number of processors.");
		
		//Threads check boxes for graph
		int threadsListSize = comboNumThreadsArray.length;
		checksNumThreads = new JCheckBox[threadsListSize+1];
		checksNumThreads[0] = new JCheckBox("Sequential");
		//checksNumThreads[0].setSelected(true);
		//checksNumThreads[0].setEnabled(false); //Sequential is always forced to be selected
		for (int i = 0; i < threadsListSize; i++) {
			checksNumThreads[i+1] = new JCheckBox(comboNumThreadsArray[i]+" Thread(s)");
			//checksNumThreads[i+1].setSelected(true);
		}

		
		//comboNumThreads.setMaximumSize(new Dimension(50,10));
		//comboNumThreads.setBounds(-1,-1,10,-1);		
		
		
		//radio box for gcc/g++
		radioCompGcc = new JRadioButton("Compile C code by using \""+gcc+"\"", true);
		//radioCompGcc.addActionListener(this);
		//radioCompGcc.setToolTipText("Compile C code by using "+gcc);
		//radioCompGcc.setBounds(3*buttonGap+3*buttonWidth, buttonTop, buttonWidth-buttonWidth/3, buttonHeight);
		
		radioCompGpp = new JRadioButton("Compile C code by using \""+gpp+"\"", false);
		//radioCompGpp.addActionListener(this);
		//radioCompGpp.setToolTipText("Compile C code by using "+gpp);
		//radioCompGpp.setBounds(4*buttonGap+4*buttonWidth-buttonWidth/3, buttonTop, buttonWidth, buttonHeight);
		
		groupRadioComp = new ButtonGroup();
		groupRadioComp.add(radioCompGcc);
		groupRadioComp.add(radioCompGpp);

		buttonSelectAll = new JButton("Select All");
		buttonSelectAll.setToolTipText("Check all enabled choices of number of threads");
		buttonSelectAll.addActionListener(this);
		buttonSelectNone = new JButton("Select None");
		buttonSelectNone.addActionListener(this);
		buttonSelectNone.setToolTipText("Uncheck all enabled choices of number of threads");
				
		
//		System.out.println(aboutMessage);
//		textArea.append(CetusGUIOptionsPanel.defaultOptionsTip);
	}
	
	
	public void actionPerformed(ActionEvent evt) {
		super.actionPerformed(evt);
		Object source = evt.getSource();
		if (source == buttonSelectAll) {
			printMsgEverywhere("\nCheck all enabled choices of number of threads");
			if (checksNumThreads[0].isEnabled()) checksNumThreads[0].setSelected(true);
			for (int i = 1; i < checksNumThreads.length; i++) {
				if (checksNumThreads[i].isEnabled()) checksNumThreads[i].setSelected(true);
			}
			
		} else if (source == buttonSelectNone) {
			printMsgEverywhere("\nUncheck all enabled choices of number of threads");
			if (checksNumThreads[0].isEnabled()) checksNumThreads[0].setSelected(false);
			for (int i = 1; i < checksNumThreads.length; i++) {
				if (checksNumThreads[i].isEnabled()) checksNumThreads[i].setSelected(false);
			}
		}
	}	
	
	@Override
	public void pressButton(int buttonIndex) {
		//
		if (buttonIndex == 0) {
			
			printMsgEverywhere(dashSeparator+"Open speedup calculation dialog box");
			
			if 	(CetusGUI.inputFile == null) {
				printMsgEverywhere("\nNo input C file! Compiling input C file disabled!");
				checkCompInput.setEnabled(false);
				checkCompInput.setSelected(false);
				if (inputExeFile == null) {
					printMsgEverywhere("\nNo input executable file! Running input executable disabled!");
					checkRunInput.setEnabled(false);
					checkRunInput.setSelected(false);
				}
				else {
					printMsgEverywhere("\nYou can run the existing input executable file: "
							+inputExeFile);
					checkRunInput.setEnabled(true);
				}
			} else {
				printMsgEverywhere("\nInput C file is available: " + CetusGUI.inputFile);
				checkCompInput.setEnabled(true);
				checkRunInput.setEnabled(true);
				if (newInput == 1) {
					printMsgEverywhere("\nYou have a new input C file: " + CetusGUI.inputFile);
					checkCompInput.setSelected(true);
					checkRunInput.setSelected(true);
				}
			}
			
			if 	(CetusGUI.openMpFile == null) {
				printMsgEverywhere("\nNo OpenMP C file! Compiling OpenMP C file disabled!");
				checkCompOpenMP.setEnabled(false);
				checkCompOpenMP.setSelected(false);
				if (openMpExeFile == null) {
					printMsgEverywhere("\nNo OpenMP executable file! Running OpenMP executable disabled!");
					checkRunOpenMP.setEnabled(false);
					checkRunOpenMP.setSelected(false);
				}
				else {
					printMsgEverywhere("\nYou can run the existing OpenMP executable file: "
							+openMpExeFile);
					checkRunOpenMP.setEnabled(true);
				}
			} else {
				printMsgEverywhere("\nOpenMP C file is available: " + CetusGUI.openMpFile);
				checkCompOpenMP.setEnabled(true);
				checkRunOpenMP.setEnabled(true);
				if (newOpenMP == 1) {
					printMsgEverywhere("\nYou have a new OpenMP C file: " + CetusGUI.openMpFile);
					checkCompOpenMP.setSelected(true);
					checkRunOpenMP.setSelected(true);
				}
			}

			if (CetusGUI.inputFile != null && CetusGUI.openMpFile != null
					|| inputExeFile != null && openMpExeFile != null
					|| CetusGUI.inputFile != null && openMpExeFile != null
					|| inputExeFile != null && CetusGUI.openMpFile != null)
				checkCalculate.setEnabled(true);

			if ((newInput == 1 || newOpenMP == 1) 
					&& CetusGUI.inputFile != null && CetusGUI.openMpFile != null) {
				printMsgEverywhere("\nYou should calculate new speedup and efficiency");
				checkCalculate.setSelected(true);
			}
			
			Object[] params = {speedupDialogMsg, checkCompInput, checkRunInput, 
					checkCompOpenMP, checkRunOpenMP, checkCalculate,
					"Select number of threads for running OpenMP C executable in parallel mode: ", 
					comboNumThreads, radioCompGcc, radioCompGpp
					};
			int n = JOptionPane.showConfirmDialog(this.getParent(), params, speedupDialogTitle, JOptionPane.OK_CANCEL_OPTION);			
//			int n = JOptionPane.showOptionDialog(this.getParent(),
//			speedupDialogMsg, speedupDialogTitle, 
//			JOptionPane.YES_NO_CANCEL_OPTION, 
//			JOptionPane.QUESTION_MESSAGE, null, 
//			params,  params[0]);
			
			
			numThreads = Integer.parseInt((String) comboNumThreads.getSelectedItem());
			if (radioCompGcc.isSelected()) {
				gccOrGpp = gcc;
			} else if (radioCompGpp.isSelected()) {
				gccOrGpp = gpp;
			}
			


			if (n == 0) {
				// System.out.println("######cores######"+TCPClient.numcore());

				String speedupcmd = "" + " -action=speed_up"; // +" -in_file="+CetusGUI.inputFile.getName()
																// +
																// " -out-file="
																// +
																// CetusGUI.outputFile.getName()
																// + " ";
				if (checkCompInput.isSelected()) {
					// compileInput();
					speedupcmd = speedupcmd + " -compinput=1";
				}

				if (checkRunInput.isSelected()) {

					speedupcmd = speedupcmd + " -runinput=1";
					// runInput();
				}

				if (checkCompOpenMP.isSelected()) {
					speedupcmd = speedupcmd + " -compoutput=1";
					// compileOutput();
				}

				if (checkRunOpenMP.isSelected()) {
					speedupcmd = speedupcmd + " -runoutput=1";
					// runOutput();
				}

				if (checkCalculate.isSelected()) {
					speedupcmd = speedupcmd + " -calculate=1";
					// calculateSpeedup();
				}

				speedupcmd = speedupcmd + " -threads=" + numThreads;
				String return_values[] = TCPClient.speedup(speedupcmd);
				printServerMsg("run_input=" + return_values[0]
						+ "\trun_output=" + return_values[1] + "\n");
				printServerMsg(return_values[2]);
			}
			
//			if (n == 0) {
//				
//				if (checkCompInput.isSelected()) {
//					compileInput();
//				}
//				
//				if (checkRunInput.isSelected()) {
//					runInput();
//				}
//				
//				if (checkCompOpenMP.isSelected()) {
//					compileOpenMP();
//				}
//				
//				if (checkRunOpenMP.isSelected()) {
//					runOpenMP();
//				}
//				
//				if (checkCalculate.isSelected()) {
//					calculateSpeedup();
//				}
//				
//			} 
			
			else {
				printMsgEverywhere("\nSpeedup calcuation cancelled\n");
			}
			
			
			
		} else if (buttonIndex == 1) {
			printMsgEverywhere(dashSeparator+"Open graph configuration dialog box");
			if 	(CetusGUI.inputFile == null) {
				printMsgEverywhere("\nNo input C file! Sequential mode disabled!");
				checksNumThreads[0].setEnabled(false);
				checksNumThreads[0].setSelected(false);
			} else {
				printMsgEverywhere("\nInput C file is available: " + CetusGUI.inputFile);
				checksNumThreads[0].setEnabled(true);
				checksNumThreads[0].setSelected(true);
			}
			
			if 	(CetusGUI.openMpFile == null) {
				printMsgEverywhere("\nNo OpenMP C file! Parallel mode disabled!");
				for (int i = 1; i < checksNumThreads.length; i++) {
					checksNumThreads[i].setEnabled(false);
					checksNumThreads[i].setSelected(false);
				}
			} else {
				printMsgEverywhere("\nOpenMP C file is available: " + CetusGUI.openMpFile);
				for (int i = 1; i < checksNumThreads.length; i++) {
					checksNumThreads[i].setEnabled(true);
					checksNumThreads[i].setSelected(true);
				}
			}
			
			
//			Object[] params = new Object[1+checksNumThreads.length];
//			params[0] = "Check the number of threads you want to run parallel executable on";
//			for (int i = 0; i < checksNumThreads.length; i++) {
//				params[i+1] = checksNumThreads[i];
//			}
			
			Object[] params = {"Check the number of ", 
					"threads you want to run ", "parallel executable on:",
					buttonSelectAll, buttonSelectNone, checksNumThreads,
					radioCompGcc, radioCompGpp};
			
			int n = JOptionPane.showConfirmDialog(this.getParent(), params, "Graph configuration", JOptionPane.OK_CANCEL_OPTION);
			
			if (radioCompGcc.isSelected()) {
				gccOrGpp = gcc;
			} else if (radioCompGpp.isSelected()) {
				gccOrGpp = gpp;
			}
			
			if (n == 0) {
				
				ArrayList<Integer> numThreadsList = new ArrayList<Integer>();
				
				if (checksNumThreads[0].isSelected()) {
					numThreadsList.add(0);
				}
				
				if (op<0) {
					for (int i = 1; i < checksNumThreads.length; i++) {
						if (checksNumThreads[i].isSelected()) 
							numThreadsList.add(i); //for Mac and Linux
					}
				} else {
					if (checksNumThreads[1].isSelected()) 
						numThreadsList.add(numCores); // for Windows
				}
				
				if (numThreadsList.size()==0) {
					printMsgEverywhere("\nYou did not select any mode! Exit!");
					return;
				}
				
				int[] numThreadsArray = new int[numThreadsList.size()];
				for (int i = 0; i < numThreadsArray.length; i++) {
					numThreadsArray[i] = numThreadsList.get(i).intValue();
				}
				
				printMsgEverywhere("\nThe choices of number of threads you selected (0 means sequential): "
						+Arrays.toString(numThreadsArray));
				
				int numChoices = numThreadsArray.length;
				double[] runTimes = new double[numChoices];
				for (int i = 0; i < numChoices; i++) {
					runTimes[i] = -2.0;
				}
				int[] numThreadsPars = null; //parallel threads
				double[] speedups = null;
				double[] efficiencys = null;
				
				if (numThreadsArray[0] == 0) {
					compileInput();
					runInput();
					runTimes[0] = seqRunTime;
					if (numChoices>1) {
						compileOpenMP();
						if (openMpExeFile!=null) {
							numThreadsPars = new int[numChoices-1];
							speedups = new double[numChoices-1];
							efficiencys = new double[numChoices-1];
							for (int i = 1; i < numChoices; i++) {
								numThreads = numThreadsArray[i];
								runOpenMP();
								runTimes[i] = parRunTime;
								calculateSpeedup();
								speedups[i-1] = speedup;
								efficiencys[i-1] = efficiency;
								numThreadsPars[i-1] = numThreads;
							}				
						} else {
							printMsgEverywhere("\nCompiling OpenMP C code failed; will not run OpenMP executable");
						}
					}
				} else {
					int usingExistingSequential = -1;
					if (seqRunTime > 0) {
						String msg = "You did not check sequential mode. " +
								"\nDo you want to use existing sequential time: " 
								+ seqRunTime + " seconds, \nto calcuate speedup and efficiency?";
						printMsgEverywhere("\n"+msg);
						usingExistingSequential = JOptionPane.showConfirmDialog(
								this.getParent(),
								msg, "Using Existing Sequential Time?", 
								JOptionPane.YES_NO_OPTION);
						if (usingExistingSequential == 0) {
							printMsgEverywhere("\nExisting sequential time will " +
									"be used to calcuate speedup and efficiency");
						} else {
							printMsgEverywhere("\nNo speedup or efficiency will be calculated");
						}
					}
					compileOpenMP();
					if (openMpExeFile!=null) {
						for (int i = 0; i < numChoices; i++) {
							numThreads = numThreadsArray[i];
							runOpenMP();
							runTimes[i] = parRunTime;
						}
						if (usingExistingSequential == 0) {
							numThreadsPars = new int[numChoices];
							speedups = new double[numChoices];
							efficiencys = new double[numChoices];
							for (int i = 0; i < numChoices; i++) {
								parRunTime = runTimes[i];
								numThreads = numThreadsArray[i];
								calculateSpeedup();
								speedups[i] = speedup;
								efficiencys[i] = efficiency;
								numThreadsPars[i] = numThreads;
							}
						}
					} else {
						printMsgEverywhere("\nCompiling OpenMP C code failed; will not run OpenMP executable");
					}
				}
				
				PlotData plotRunTimes = null, 
						plotSpeeups = null, 
						plotEfficiencys = null;
				
				printMsgEverywhere("\nNumber of threads array: "
						+Arrays.toString(numThreadsArray));
				printMsgEverywhere("\nRunning times: "
						+Arrays.toString(runTimes));
				plotRunTimes = new PlotData(numThreadsArray, 
						runTimes, 
						"Number of Threads (0 means Sequential)",
						"Running time (seconds)", "RunningTime");				
				
				if (speedups != null) {
					printMsgEverywhere("\nSelected number of threads: "
							+Arrays.toString(numThreadsPars));
					printMsgEverywhere("\nSpeedups: "
							+Arrays.toString(speedups));
					plotSpeeups = new PlotData(numThreadsPars, 
							speedups, 
							"Number of Threads",
							"Speedup = Sequential Time / Parallel Time", 
							"Speedup");	
					printMsgEverywhere("\nEfficiencys: "
							+Arrays.toString(efficiencys));
					plotEfficiencys = new PlotData(numThreadsPars, 
							efficiencys, 
							"Number of Threads",
							"Efficiency (0~1) = Speedup / Number of Threads", 
							"Efficiency");	
				}
				
			} else {
				printMsgEverywhere("\nSpeedup graphs calcuation cancelled\n");
			}
			
		} else if (buttonIndex == 2) {
			System.out.println(shortDashSeparator);
			printMsgEverywhereNewLine("Saving speedup results into file: "+speedupFile.toString());
			boolean success = CetusGUITools.writeStringToFile(speedupFile, textArea.getText());
			if (success==true) {
				printMsgEverywhereNewLine("Saved speedup results into file: "+speedupFile.toString());
			} else {
				printMsgEverywhereNewLine("Error when saving speedup results into file: "+speedupFile.toString());
			}
			
		} else if (buttonIndex == 3) {
			File selectFile = CetusGUITools.saveFile(this, null, null, speedupFile.getName());
			if (selectFile!=null) {
				speedupFile = selectFile;
				String newSaveButtonTip = "Save speedup results into: "+speedupFile.toString();
				buttons[2].setToolTipText(newSaveButtonTip);
				System.out.println(shortDashSeparator);
				printMsgEverywhereNewLine("Saving speedup results into file: "+speedupFile.toString());
				boolean success = CetusGUITools.writeStringToFile(speedupFile, textArea.getText());
				if (success==true) {
					printMsgEverywhereNewLine("Saved speedup results into file: "+speedupFile.toString());
				} else {
					printMsgEverywhereNewLine("Error when saving speedup results into file: "+speedupFile.toString());
				}
			}
			
		} else if (buttonIndex == 4) {
			
//			new PlotData((new int[] { 0, 2, 4 }), (new double[] {
//					8.3, 5.2, 3.1 }), "Number of Threads (0 means Sequential)",
//					"Running time (seconds)", "Running Time");

			printMsgEverywhere(dashSeparator+"Text area cleared");
			textArea.setText(aboutMessage);
		}
	}




	private void calculateSpeedup() {
		printMsgEverywhere("\n"+dashSeparator+"Calculating speedup and efficiency...");
		if (seqRunTime > 0 && parRunTime > 0 && numThreads > 0) {
			speedup = seqRunTime/parRunTime;
			efficiency = speedup/numThreads;
			printMsgEverywhere(
				"\nSequential running time in seconds = " + String.format("%1$,.3f", seqRunTime)
				+ "\nParallel running time in seconds = " + parRunTime
				+ "\nNumber of threads used = " + numThreads
				+ "\nSpeedup = " + String.format("%1$,.3f", speedup)
				+ "\nEfficiency = " + String.format("%1$,.3f", efficiency)+"\n");
			checkCalculate.setSelected(false);
		} else {
			String msg = "\nFailed! Please run input and OpenMP executables first!";
			JOptionPane.showMessageDialog(this.getParent(),msg);
			printMsgEverywhere(msg);
		}
	}

	private void runOpenMP() {
		printMsgEverywhere("\n"+dashSeparator+"Running OpenMP C executable in parallel mode...");
		parRunTime = runExe(openMpExeFile, numThreads);
		if (parRunTime > 0) checkRunOpenMP.setSelected(false);
	}

	private void compileOpenMP() {
		printMsgEverywhere("\n"+dashSeparator+"Compiling OpenMP C code...");
		openMpExeFile = compileCFile(CetusGUI.openMpFile, 1);
		if (openMpExeFile != null) {
			newOpenMP = 0;
			checkCompOpenMP.setSelected(false);
		}
	}

	private void runInput() {
		printMsgEverywhere("\n"+dashSeparator+"Running input C executable in sequential mode...");		
		seqRunTime = runExe(inputExeFile, 0);
		if (seqRunTime > 0) checkRunInput.setSelected(false);
	}

	private void compileInput() {
		printMsgEverywhere("\n"+dashSeparator+"Compiling input C code in sequential mode...");
		inputExeFile = compileCFile(CetusGUI.inputFile, 0);
		if (inputExeFile != null) {
			newInput = 0;
			checkCompInput.setSelected(false);
		}
	}
	
	public void printMsgEverywhere (String msg) {
		textArea.append(msg);
		statusArea.setText(msg);
		System.out.print(msg);
	}

	public void printMsgEverywhereNewLine (String msg) {
		textArea.append("\n"+msg);
		statusArea.setText(msg);
		System.out.println(msg);
	}
	
	private void printServerMsg(String msg) {
		textArea.append(msg);
		//statusArea.setText(msg);
		System.out.print(msg);
		
	}
	
	public File compileCFile(File cFile, int mode) {
		
		try {
			if (cFile!=null) {
				String compCommand = "";
				if (mode == 0) {
					compCommand = gccOrGpp+" -o ";
				} else if (mode == 1){
					compCommand = gccOrGpp+" -fopenmp -o ";
				} else {
					printMsgEverywhere("\nWrong mode: "+ mode +", exit!\n");
					return null;
				}
				String msg = "\nCompiling C file: "+cFile+"\n";
				printMsgEverywhere(msg);
				int cExtIndex = cFile.toString().toLowerCase().lastIndexOf(".");
				String cMain = cFile.toString().substring(0, cExtIndex);
				String cExe = cMain + ext;
				File cExeFile = new File(cExe);
				if (cExeFile.exists()) {
					cExeFile.delete();
					printMsgEverywhere("Deleted previous C executable: "+cExeFile+"\n");
				}
				
				String compCode = compCommand + cExe + " "+ cFile.toString();
				printMsgEverywhere(compCode);
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				//Runtime.getRuntime().exec(compCode).waitFor();
				Process p = Runtime.getRuntime().exec(compCode);			
//				Thread t = new ThreadSpeedup(compCode, p, 0, this);
//				t.start();
				p.waitFor();
//				t.interrupt();
				String[] processMsgs = CetusGUITools.getProcessMessages(p);
				String processMsgsString = CetusGUITools.convertArrayStringsToStringLines(processMsgs);
				printMsgEverywhere("\n"+processMsgsString);
				setCursor(null);
				if (cExeFile.exists()) {
					printMsgEverywhere("\nCompiling C code was successful!\n");
					return cExeFile;
				} else {
					printMsgEverywhere("\nCompiling C code failed!\n");
					JOptionPane.showMessageDialog(this.getParent(),"Compiling C code failed!");
				}
				
			} else {
				String msg = "\nNo C file! Compiling C code failed!";
				JOptionPane.showMessageDialog(this.getParent(),msg);
				printMsgEverywhere(msg);
			}
		}
		catch (Exception ep) {
			setCursor(null);
			printMsgEverywhere("\nCompiling C code failed!\n");
			printMsgEverywhere("\n"+ep);
		}
		
		return null;
	}
	
	public double runExe (File cExeFile, int numThreads) {
		
		try {
			if (cExeFile!=null) {
				
				String[] runExe;
				
				if (numThreads == 0) {
					printMsgEverywhere("\nRunning C executable in sequential mode: "+cExeFile+"\n");
					runExe = new String[]{cExeFile.toString()};					
				} else if (numThreads > 0) {
					printMsgEverywhere("\nRunning C executable in parallel mode: "+cExeFile);
					printMsgEverywhere("\n"+System.getProperty("os.name") + ", using " + numThreads + " threads\n");
					if (op<0) {
						runExe = new String[]{"/bin/sh","-c",
								"OMP_NUM_THREADS="+numThreads+" "+cExeFile.toString()}; 
					} else {
						runExe = new String[]{cExeFile.toString()};	
					}					
				} else {
					printMsgEverywhere("\nWrong number of threads!");
					return -1.0;
				}

				printMsgEverywhere(CetusGUITools.convertArrayStringsToStringLines(runExe));
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				
				long startTime = System.currentTimeMillis() ; //.nanoTime();
//				Process p = null;
//				ThreadSpeedupProcess pro = new ThreadSpeedupProcess(
//						CetusGUITools.convertArrayStringsToStringLines(runExe), p, 1, this);
//				pro.start();
				Process p = Runtime.getRuntime().exec(runExe);
//				Thread t = new ThreadSpeedup(CetusGUITools.convertArrayStringsToStringLines(runExe), p, 1, this);
//				t.start();
				p.waitFor();
				long endTime = System.currentTimeMillis();
//				t.interrupt();
				setCursor(null);
				String[] processMsgs = CetusGUITools.getProcessMessages(p);
				String processMsgsString = CetusGUITools.convertArrayStringsToStringLines(processMsgs);
				printMsgEverywhere("\n"+processMsgsString);
				double duration = (endTime - startTime)/1000.0;
				printMsgEverywhere("\nRunning time = "
						+ String.format("%1$,.3f", duration) + " (seconds)");
				return duration;
				
			} else {
				
				String msg = "\nExecutable file does not exist! Please compile C code first!";
				JOptionPane.showMessageDialog(this.getParent(),msg);
				printMsgEverywhere(msg);
				return -2.0;
			}
			
		}
		catch (Exception ep) {
			setCursor(null);
			printMsgEverywhere("\nRunning C executable failed\n");
			printMsgEverywhere("\n"+ep);
		}
		
		return -3.0;
	}

}
