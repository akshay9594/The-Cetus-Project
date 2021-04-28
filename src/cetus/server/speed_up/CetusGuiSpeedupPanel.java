/**

Zhengxiao (Tony) Li

3/21/13
# CetusGuiSpeedupPanel.java
- Whenever start a Graph dialog, all checked as long as input/output available.
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
- If output C file is not available, all parallel modes are disabled and unchecked, else they are enabled
- Process all checked modes: 
* if no option is selected, just return; 
* if only sequential is selected, just compile and run input, and record the run time into graph array, no speedup or efficiency recorded; 
* if some parallel modes are also selected, compile and run output too, and record the run times, speedups and efficiencies; 
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
- Compile output C code with OpenMP
- Run output executable in parallel mode
- Calculate speedup and efficiency
# Flags to indicate the status of objects: inputFile (if input C file is loaded), outputFile (if translated output C file exists), inputExeFile (if input C file has been compiled into executable file), outputExeFile (if output C file has been compiled into executable file), newInput (if input C file has been changed), newOutput (if output C file has been changed), seqRunTime (sequential running time, the initial value is negative), parRunTime (parallel running time, the initial value is negative)
# newInput: flag = true, whenever a new input C file is loaded, an existing input C file is saved, or the code in the input text area is saved as a new C file. Flag = false, after input C file gets compiled in [Speedup], or a demo code is loaded into the input text area
# newOutput: flag = true, whenever an input C file gets translated into output C file, an output C file gets saved, or anything in the output text area gets saved as a new file. Flag = false, whenever a translated output C file is invalid, or after translated output C file gets compiled in [Speedup]
# At the beginning of [Speedup], if no input nor output C file exists, none of the steps is enabled
# Scenario for input ---- No input C file is loaded: compiling input C is disabled; if no input C executable, running input C executable is disabled, else enabled.
# Scenario for input ---- input C file is loaded: both compiling and running are enabled; If newInput = true, both compiling and running are checked automatically for user; user can uncheck if wanting.
# Scenario for output ---- No output C file: compiling is disabled; if no output executable, running is disabled, else enabled
# Scenario for output ---- output C file exists: both compiling and running are enabled; if newOutput = true, both compiling and running are checked automatically for user; user can uncheck if wanting
# Scenario for calculating----- if both input and output C files exist, or both input and output executable exist, or both input C file and output executable exist, or both input executable and output C file exist, calculating option is enabled; if (newInput = true or newOutpu = true) and both input and output C file exist, calculating option is checked automatically for user; user can uncheck if wanting
# Scenario for different operation systems ---- Linux and Mac users can select number of threads for running parallel executable, starting 1 and up to the number of machine cores; Windows users cannot choose number of threads, which is fixed to the number of machine cores (currently I have not found out a way to specify number of threads when running parallel executables on Windows)
# Scenario for running the five steps ---- whenever compiling input or output C files is done, newInput or newOutput is set to false; whenever a step is done, the option for this step is unchecked automatically for users, meaning users do not need to run this step again next time, unless some previous scenarios happen; 

 */

package cetus.server.speed_up;


import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
/*
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
*/
public class CetusGuiSpeedupPanel{// extends GUIBasicPanel{

	public static final String dashSeparator = "\n--------------------------------------------\n";
	protected static String s1 = "Compile input and output C code" 
			+ ", run executables, compute speedup and efficiency" +
			" and print out results.";
	protected static String speedupDialogTitle = "Speedup";
	protected static String speedupDialogMsg = 
		"Compile and run input and output C files and calculate speedup and efficiency";
	protected static String[] aboutGuiArray = new String[] {
		"3",
		speedupDialogTitle,speedupDialogMsg,
//		"Comp Input","Compile input C code in sequential mode",
//		"Run Input","Run input executable in sequential mode",
//		"Comp Output","Compile output C code with OpenMP",
//		"Run Output","Choose number of cores and run output executable in parallel mode",
//		"Calculate","Calculate speedup and efficiency",
		"Graphs","Draw charts of running time, speedup and efficiency",
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
	public static File inputExeFile = null, outputExeFile = null;
	public static int newInput = 0, newOutput = 0;
	public static double seqRunTime = -1.0, parRunTime = -1.0, 
			speedup = -1.0, efficiency = -1.0;
	/*protected JCheckBox checkCompInput = new JCheckBox("Compile input C code in sequential mode");
	protected JCheckBox checkRunInput = new JCheckBox("Run input executable in sequential mode");
	protected JCheckBox checkCompOutput = new JCheckBox("Compile output C code with OpenMP");
	protected JCheckBox checkRunOutput = new JCheckBox("Run output executable in parallel mode");
	protected JCheckBox checkCalculate = new JCheckBox("Calculate speedup and efficiency");
	*/
	protected String[] comboNumThreadsArray;
	/*protected JComboBox comboNumThreads;
	protected JRadioButton radioCompGcc, radioCompGpp;
	protected ButtonGroup groupRadioComp;*/
	public static String gccOrGpp = "g++";
	//protected JCheckBox[] checksNumThreads;
	public static int[] threadsArray; //from 0 (seq) to numCores, except for Windows
	public static double[] runTimeArray; //both seq and par running time
	//protected JButton buttonSelectAll, buttonSelectNone; //buttons to select all/none for graph configuration dialog box
	
	CetusGuiSpeedupPanel() {
	}
//		super(aboutGuiArray);
//		textArea.setText(aboutMessage);
//		
//		checkCompInput.setEnabled(false);
//		checkRunInput.setEnabled(false);
//		checkCompOutput.setEnabled(false);
//		checkRunOutput.setEnabled(false);
//		checkCalculate.setEnabled(false);
//		
		//Threads combo box menu
//		if (op < 0) {
//			comboNumThreadsArray = new String[numCores];			
//			for (int i = 0; i < comboNumThreadsArray.length; i++) {
//				comboNumThreadsArray[i] = ""+(i+1);
//			}
//		} else {
//			comboNumThreadsArray = new String[]{numCores+""};
//		}
		
//		comboNumThreads = new JComboBox(comboNumThreadsArray);
//		comboNumThreads.setSelectedIndex(comboNumThreadsArray.length-1);
//		if (op>=0) comboNumThreads.setToolTipText("On Windows, number of threads available for parallel run is fixed to number of processors.");
//		
		//Threads check boxes for graph
//		int threadsListSize = comboNumThreadsArray.length;
//		checksNumThreads = new JCheckBox[threadsListSize+1];
//		checksNumThreads[0] = new JCheckBox("Sequential");
		//checksNumThreads[0].setSelected(true);
//		//checksNumThreads[0].setEnabled(false); //Sequential is always forced to be selected
//		for (int i = 0; i < threadsListSize; i++) {
//			checksNumThreads[i+1] = new JCheckBox(comboNumThreadsArray[i]+" Thread(s)");
//			//checksNumThreads[i+1].setSelected(true);
//		}

		
		//comboNumThreads.setMaximumSize(new Dimension(50,10));
		//comboNumThreads.setBounds(-1,-1,10,-1);		
		
		
		//radio box for gcc/g++
	//	radioCompGcc = new JRadioButton("Compile C code by using \""+gcc+"\"", true);
		//radioCompGcc.addActionListener(this);
		//radioCompGcc.setToolTipText("Compile C code by using "+gcc);
		//radioCompGcc.setBounds(3*buttonGap+3*buttonWidth, buttonTop, buttonWidth-buttonWidth/3, buttonHeight);
		
	//	radioCompGpp = new JRadioButton("Compile C code by using \""+gpp+"\"", false);
		//radioCompGpp.addActionListener(this);
		//radioCompGpp.setToolTipText("Compile C code by using "+gpp);
		//radioCompGpp.setBounds(4*buttonGap+4*buttonWidth-buttonWidth/3, buttonTop, buttonWidth, buttonHeight);
		
//		groupRadioComp = new ButtonGroup();
//		groupRadioComp.add(radioCompGcc);
//		groupRadioComp.add(radioCompGpp);
//
//		buttonSelectAll = new JButton("Select All");
//		buttonSelectAll.setToolTipText("Check all enabled choices of number of threads");
//		buttonSelectAll.addActionListener(this);
//		buttonSelectNone = new JButton("Select None");
//		buttonSelectNone.addActionListener(this);
//		buttonSelectNone.setToolTipText("Uncheck all enabled choices of number of threads");
//				
		
//		System.out.println(aboutMessage);
//		textArea.append(CetusGUIOptionsPanel.defaultOptionsTip);
//	}
	
	
//	public void actionPerformed(ActionEvent evt) {
//		//super.actionPerformed(evt);
//		Object source = evt.getSource();
//		if (source == buttonSelectAll) {
//			printMsgEverywhere("\nCheck all enabled choices of number of threads");
//			if (checksNumThreads[0].isEnabled()) checksNumThreads[0].setSelected(true);
//			for (int i = 1; i < checksNumThreads.length; i++) {
//				if (checksNumThreads[i].isEnabled()) checksNumThreads[i].setSelected(true);
//			}
//			
//		} else if (source == buttonSelectNone) {
//			printMsgEverywhere("\nUncheck all enabled choices of number of threads");
//			if (checksNumThreads[0].isEnabled()) checksNumThreads[0].setSelected(false);
//			for (int i = 1; i < checksNumThreads.length; i++) {
//				if (checksNumThreads[i].isEnabled()) checksNumThreads[i].setSelected(false);
//			}
//		}
//	}	
//	
//	@Override
//	public void pressButton(int buttonIndex) {
//		//
//		if (buttonIndex == 0) {
//			
//			printMsgEverywhere(dashSeparator+"Open speedup calculation dialog box");
//			
//			if 	(CetusGUI.inputFile == null) {
//				printMsgEverywhere("\nNo input C file! Compiling input C file disabled!");
//				checkCompInput.setEnabled(false);
//				checkCompInput.setSelected(false);
//				if (inputExeFile == null) {
//					printMsgEverywhere("\nNo input executable file! Running input executable disabled!");
//					checkRunInput.setEnabled(false);
//					checkRunInput.setSelected(false);
//				}
//				else {
//					printMsgEverywhere("\nYou can run the existing input executable file: "
//							+inputExeFile);
//					checkRunInput.setEnabled(true);
//				}
//			} else {
//				printMsgEverywhere("\nInput C file is available: " + CetusGUI.inputFile);
//				checkCompInput.setEnabled(true);
//				checkRunInput.setEnabled(true);
//				if (newInput == 1) {
//					printMsgEverywhere("\nYou have a new input C file: " + CetusGUI.inputFile);
//					checkCompInput.setSelected(true);
//					checkRunInput.setSelected(true);
//				}
//			}
//			
//			if 	(CetusGUI.outputFile == null) {
//				printMsgEverywhere("\nNo output C file! Compiling output C file disabled!");
//				checkCompOutput.setEnabled(false);
//				checkCompOutput.setSelected(false);
//				if (outputExeFile == null) {
//					printMsgEverywhere("\nNo output executable file! Running output executable disabled!");
//					checkRunOutput.setEnabled(false);
//					checkRunOutput.setSelected(false);
//				}
//				else {
//					printMsgEverywhere("\nYou can run the existing output executable file: "
//							+outputExeFile);
//					checkRunOutput.setEnabled(true);
//				}
//			} else {
//				printMsgEverywhere("\nOutput C file is available: " + CetusGUI.outputFile);
//				checkCompOutput.setEnabled(true);
//				checkRunOutput.setEnabled(true);
//				if (newOutput == 1) {
//					printMsgEverywhere("\nYou have a new output C file: " + CetusGUI.outputFile);
//					checkCompOutput.setSelected(true);
//					checkRunOutput.setSelected(true);
//				}
//			}
//
//			if (CetusGUI.inputFile != null && CetusGUI.outputFile != null
//					|| inputExeFile != null && outputExeFile != null
//					|| CetusGUI.inputFile != null && outputExeFile != null
//					|| inputExeFile != null && CetusGUI.outputFile != null)
//				checkCalculate.setEnabled(true);
//
//			if ((newInput == 1 || newOutput == 1) 
//					&& CetusGUI.inputFile != null && CetusGUI.outputFile != null) {
//				printMsgEverywhere("\nYou should calculate new speedup and efficiency");
//				checkCalculate.setSelected(true);
//			}
//			
//			Object[] params = {speedupDialogMsg, checkCompInput, checkRunInput, 
//					checkCompOutput, checkRunOutput, checkCalculate,
//					"Select number of threads for running output C executable in parallel mode: ", 
//					comboNumThreads, radioCompGcc, radioCompGpp
//					};
//			int n = JOptionPane.showConfirmDialog(this.getParent(), params, speedupDialogTitle, JOptionPane.OK_CANCEL_OPTION);			
////			int n = JOptionPane.showOptionDialog(this.getParent(),
////			speedupDialogMsg, speedupDialogTitle, 
////			JOptionPane.YES_NO_CANCEL_OPTION, 
////			JOptionPane.QUESTION_MESSAGE, null, 
////			params,  params[0]);
//			numThreads = Integer.parseInt((String) comboNumThreads.getSelectedItem());
//			if (radioCompGcc.isSelected()) {
//				gccOrGpp = gcc;
//			} else if (radioCompGpp.isSelected()) {
//				gccOrGpp = gpp;
//			}
//			
//			if (n == 0) {
				
	
	
//	
//	
//				if (checkCompInput.isSelected()) {
//					compileInput();
//				}
//				
//				if (checkRunInput.isSelected()) {
//					runInput();
//				}
//				
//				if (checkCompOutput.isSelected()) {
//					compileOutput();
//				}
//				
//				if (checkRunOutput.isSelected()) {
//					runOutput();
//				}
//				
//				if (checkCalculate.isSelected()) {
//					calculateSpeedup();
//				}
				
//			} else {
//				printMsgEverywhere("\nSpeedup calcuation cancelled\n");
//			}
//			
			
//			
//		} else if (buttonIndex == 1) {
//			printMsgEverywhere(dashSeparator+"Open graph configuration dialog box");
//			if 	(CetusGUI.inputFile == null) {
//				printMsgEverywhere("\nNo input C file! Sequential mode disabled!");
//				checksNumThreads[0].setEnabled(false);
//				checksNumThreads[0].setSelected(false);
//			} else {
//				printMsgEverywhere("\nInput C file is available: " + CetusGUI.inputFile);
//				checksNumThreads[0].setEnabled(true);
//				checksNumThreads[0].setSelected(true);
//			}
//			
//			if 	(CetusGUI.outputFile == null) {
//				printMsgEverywhere("\nNo output C file! Parallel mode disabled!");
//				for (int i = 1; i < checksNumThreads.length; i++) {
//					checksNumThreads[i].setEnabled(false);
//					checksNumThreads[i].setSelected(false);
//				}
//			} else {
//				printMsgEverywhere("\nOutput C file is available: " + CetusGUI.outputFile);
//				for (int i = 1; i < checksNumThreads.length; i++) {
//					checksNumThreads[i].setEnabled(true);
//					checksNumThreads[i].setSelected(true);
//				}
//			}
//			
//			
////			Object[] params = new Object[1+checksNumThreads.length];
////			params[0] = "Check the number of threads you want to run parallel executable on";
////			for (int i = 0; i < checksNumThreads.length; i++) {
////				params[i+1] = checksNumThreads[i];
////			}
//			
//			Object[] params = {"Check the number of ", 
//					"threads you want to run ", "parallel executable on:",
//					buttonSelectAll, buttonSelectNone, checksNumThreads,
//					radioCompGcc, radioCompGpp};
//			
//			int n = JOptionPane.showConfirmDialog(this.getParent(), params, "Graph configuration", JOptionPane.OK_CANCEL_OPTION);
//			if (radioCompGcc.isSelected()) {
//				gccOrGpp = gcc;
//			} else if (radioCompGpp.isSelected()) {
//				gccOrGpp = gpp;
//			}
//			
//			if (n == 0) {
//				
//				ArrayList<Integer> numThreadsList = new ArrayList<Integer>();
//				
//				if (checksNumThreads[0].isSelected()) {
//					numThreadsList.add(0);
//				}
//				
//				if (op<0) {
//					for (int i = 1; i < checksNumThreads.length; i++) {
//						if (checksNumThreads[i].isSelected()) 
//							numThreadsList.add(i); //for Mac and Linux
//					}
//				} else {
//					if (checksNumThreads[1].isSelected()) 
//						numThreadsList.add(numCores); // for Windows
//				}
//				
//				if (numThreadsList.size()==0) {
//					printMsgEverywhere("\nYou did not select any mode! Exit!");
//					return;
//				}
//				
//				int[] numThreadsArray = new int[numThreadsList.size()];
//				for (int i = 0; i < numThreadsArray.length; i++) {
//					numThreadsArray[i] = numThreadsList.get(i).intValue();
//				}
//				
//				printMsgEverywhere("\nThe choices of number of threads you selected (0 means sequential): "
//						+Arrays.toString(numThreadsArray));
//				
//				int numChoices = numThreadsArray.length;
//				double[] runTimes = new double[numChoices];
//				int[] numThreadsPars = null; //parallel threads
//				double[] speedups = null;
//				double[] efficiencys = null;
//				
//				if (numThreadsArray[0] == 0) {
//					compileInput();
//					runInput();
//					runTimes[0] = seqRunTime;
//					if (numChoices>1) {
//						numThreadsPars = new int[numChoices-1];
//						speedups = new double[numChoices-1];
//						efficiencys = new double[numChoices-1];
//						compileOutput();					
//						for (int i = 1; i < numChoices; i++) {
//							numThreads = numThreadsArray[i];
//							runOutput();
//							runTimes[i] = parRunTime;
//							calculateSpeedup();
//							speedups[i-1] = speedup;
//							efficiencys[i-1] = efficiency;
//							numThreadsPars[i-1] = numThreads;
//						}
//					}
//				} else {
//					int usingExistingSequential = -1;
//					if (seqRunTime > 0) {
//						String msg = "You did not check sequential mode. " +
//								"\nDo you want to use existing sequential time: " 
//								+ seqRunTime + " seconds, \nto calcuate speedup and efficiency?";
//						printMsgEverywhere("\n"+msg);
//						usingExistingSequential = JOptionPane.showConfirmDialog(
//								this.getParent(),
//								msg, "Using Existing Sequential Time?", 
//								JOptionPane.YES_NO_OPTION);
//						if (usingExistingSequential == 0) {
//							printMsgEverywhere("\nExisting sequential time will " +
//									"be used to calcuate speedup and efficiency");
//						} else {
//							printMsgEverywhere("\nNo speedup or efficiency will be calculated");
//						}
//					}
//					compileOutput();
//					for (int i = 0; i < numChoices; i++) {
//						numThreads = numThreadsArray[i];
//						runOutput();
//						runTimes[i] = parRunTime;
//					}
//					if (usingExistingSequential == 0) {
//						numThreadsPars = new int[numChoices];
//						speedups = new double[numChoices];
//						efficiencys = new double[numChoices];
//						for (int i = 0; i < numChoices; i++) {
//							parRunTime = runTimes[i];
//							numThreads = numThreadsArray[i];
//							calculateSpeedup();
//							speedups[i] = speedup;
//							efficiencys[i] = efficiency;
//							numThreadsPars[i] = numThreads;
//						}
//					}
//				}
//				
//				PlotData plotRunTimes = null, 
//						plotSpeeups = null, 
//						plotEfficiencys = null;
//				
//				printMsgEverywhere("\nNumber of threads array: "
//						+Arrays.toString(numThreadsArray));
//				printMsgEverywhere("\nRunning times: "
//						+Arrays.toString(runTimes));
//				plotRunTimes = new PlotData(numThreadsArray, 
//						runTimes, 
//						"Number of Threads (0 means Sequential)",
//						"Running time (seconds)", "RunningTime");				
//				
//				if (speedups != null) {
//					printMsgEverywhere("\nSelected number of threads: "
//							+Arrays.toString(numThreadsPars));
//					printMsgEverywhere("\nSpeedups: "
//							+Arrays.toString(speedups));
//					plotSpeeups = new PlotData(numThreadsPars, 
//							speedups, 
//							"Number of Threads",
//							"Speedup = Sequential Time / Parallel Time", 
//							"Speedup");	
//					printMsgEverywhere("\nEfficiencys: "
//							+Arrays.toString(efficiencys));
//					plotEfficiencys = new PlotData(numThreadsPars, 
//							efficiencys, 
//							"Number of Threads",
//							"Efficiency (0~1) = Speedup / Number of Threads", 
//							"Efficiency");	
//				}
//				
//			} else {
//				printMsgEverywhere("\nSpeedup graphs calcuation cancelled\n");
//			}
//			
//			
//		} else if (buttonIndex == 2) {
//			
////			new PlotData((new int[] { 0, 2, 4 }), (new double[] {
////					8.3, 5.2, 3.1 }), "Number of Threads (0 means Sequential)",
////					"Running time (seconds)", "Running Time");
//
//			printMsgEverywhere(dashSeparator+"Text area cleared");
//			textArea.setText(aboutMessage);
//		}
//	}

	protected void calculateSpeedup() {
		printMsgEverywhere("\n"+dashSeparator+"Calculating speedup and efficiency...");
		if (seqRunTime > 0 && parRunTime > 0 && numThreads > 0) {
			speedup = seqRunTime/parRunTime;
			efficiency = speedup/numThreads;
			//printMsgEverywhere(
			System.out.println(
				"\nSequential running time in seconds = " + String.format("%1$,.3f", seqRunTime)
				+ "\nParallel running time in seconds = " + parRunTime
				+ "\nNumber of threads used = " + numThreads
				+ "\nSpeedup = " + String.format("%1$,.3f", speedup)
				+ "\nEfficiency = " + String.format("%1$,.3f", efficiency)+"\n");
			//checkCalculate.setSelected(false);
		} else {
			String msg = "\nFailed! Please run input and output executables first!";
			//JOptionPane.showMessageDialog(this.getParent(),msg);
			printMsgEverywhere(msg);
		}
	}

	protected void runOutput() {
		printMsgEverywhere("\n"+dashSeparator+"Running output C executable in parallel mode...");
		parRunTime = runExe(outputExeFile, numThreads);
		
		if (parRunTime > 0) {printMsgEverywhere("\n"+"output_time="+parRunTime+"\n");}
	}

	protected void compileOutput() {
		printMsgEverywhere("\n"+dashSeparator+"Compiling output C code in OpenMP mode...");
		outputExeFile = compileCFile(CetusGUI.outputFile, 1);
		newOutput = 0;
		//if (outputExeFile != null) {
		//	checkCompOutput.setSelected(false);
		//}
	}

	protected void runInput() {
		printMsgEverywhere("\n"+dashSeparator+"Running input C executable in sequential mode...");		
		seqRunTime = runExe(inputExeFile, 0);
		if (seqRunTime > 0) {
			//checkRunInput.setSelected(false);
			printMsgEverywhere("\n"+"input_time="+seqRunTime+"\n");}
		}
	

	protected void compileInput() {
		printMsgEverywhere("\n"+dashSeparator+"Compiling input C code in sequential mode...");
		inputExeFile = compileCFile(CetusGUI.inputFile, 0);
		newInput = 0;
		//if (inputExeFile != null) {
		//	checkCompInput.setSelected(false);
		//}
	}
	
	public void printMsgEverywhere (String msg) {
		//textArea.append(msg);
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
				//setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				//Runtime.getRuntime().exec(compCode).waitFor();
				Process p = Runtime.getRuntime().exec(compCode);			
//				Thread t = new ThreadSpeedup(compCode, p, 0, this);
//				t.start();
				p.waitFor();
//				t.interrupt();
				String[] processMsgs = CetusGUITools.getProcessMessages(p);
				String processMsgsString = CetusGUITools.convertArrayStringsToStringLines(processMsgs);
				printMsgEverywhere("\n"+processMsgsString);
				//setCursor(null);
				if (cExeFile.exists()) {
					printMsgEverywhere("\nCompiling C code was successful!\n");
					return cExeFile;
				} else {
					printMsgEverywhere("\nCompiling C code failed!\n");
		//			JOptionPane.showMessageDialog(this.getParent(),"Compiling C code failed!");
				}
				
			} else {
				String msg = "\nNo C file! Compiling C code failed!";
		//		JOptionPane.showMessageDialog(this.getParent(),msg);
				printMsgEverywhere(msg);
			}
		}
		catch (Exception ep) {
			//setCursor(null);
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
		//		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				
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
//				setCursor(null);
				String[] processMsgs = CetusGUITools.getProcessMessages(p);
				String processMsgsString = CetusGUITools.convertArrayStringsToStringLines(processMsgs);
				printMsgEverywhere("\n"+processMsgsString);
				double duration = (endTime - startTime)/1000.0;
				printMsgEverywhere("\nRunning time = "
						+ String.format("%1$,.3f", duration) + " (seconds)");
				return duration;
				
			} else {
				
				String msg = "\nExecutable file does not exist! Please compile C code first!";
			//	JOptionPane.showMessageDialog(this.getParent(),msg);
				printMsgEverywhere(msg);
				return -2.0;
			}
			
		}
		catch (Exception ep) {
		//	setCursor(null);
			printMsgEverywhere("\nRunning C executable failed\n");
			printMsgEverywhere("\n"+ep);
		}
		
		return -3.0;
	}

}
