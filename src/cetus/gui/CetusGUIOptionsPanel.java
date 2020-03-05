package cetus.gui;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Arrays;
import java.util.Date;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import cetus.exec.Driver;

public class CetusGUIOptionsPanel extends JPanel implements ActionListener{
	
	public static final String defaultOptionsTip = "By default, Cetus parallelizes " +
			"outermost loops with the most powerful options.",
			printClass = "[CetusGUI] ";
//			"\nBy default, Cetus "
//			+ "calls suitable preprocessor based on operating system, "
//			+ "parallelizes outermost loops, "
//			+ "enables scalars and array reduction and transformation, "
//			+ "enables eliminating unreachable branch targets, "
//			+ "and enables model-based loop selection for profitability test\n";
	public final int numAllOptions = 200, numCetusOptions=100, daysBetweenCheck = 7,
			msPerSec = 1000, secPerDay = 86400, msPerDay = msPerSec*secPerDay;
	private int i=0, j=0, k = 0, numUsedCetusOptions = 0, xGap=5, 
			yGap = 5, height1 = 17, height2 = 20, width = 350, layoutBasicY1 = -1,
			ySeperator = 10, layoutAdvY1 = -1, column1 = 5, column2 = 400,
			layoutAdvY2 = -1, layoutBasicY2 = -1;
	private String[] allOptionsStrings = new String[numAllOptions], usedCetusOptionsStrings, 
			allOptionsNameStrings = new String[numAllOptions];
	private final String emptyString = "", optionOffString = emptyString;
	private boolean basicOptionStringOnOffFront = false, advOptionStringOnOffFront = false, 
			basicOptionStringOnOffEnd = false,	advOptionStringOnOffEnd = true;
	private JSeparator separator1;
	
	private String 
			radioAlias0String = "assume no alias exists",
			radioAlias2String = "no alias exists unless proven",
			radioAliasTip = "default: advanced interprocedural analysis";
	
	private JCheckBox checkDebugParserInput;
	private JRadioButton 	radioAlias0, radioAlias2;
	private ButtonGroup groupRadioAlias;

	// -------------- use Cetus remote server or not -----------
	private static JCheckBox checkServer;
	private String optionServerStringPrefixNoEqual = "-server"
//			optionServerStringPrefix = optionServerString+"=",
//			optionServer0String = optionServerStringPrefix+"0",
//			optionServer1String = optionServerStringPrefix+"1"
			;
	private String checkServerText = "use Cetus remote server to do translation",
			checkServerTip = "Use Cetus remote server to do translation";
	
	// -------------- C compiler and OpenMP flag ---------
	private String compilerString = (CetusGUITools.osName.toLowerCase())
			.indexOf("win") >= 0 ? "gcc-4" : "gcc",
			openmpFlagString = "-fopenmp",
			optionCompilerStringPrefix = "-compiler=",
			optionOpenmpStringPrefix = "-ompflag=",
			optionCompilerString = optionCompilerStringPrefix+compilerString,
			optionOpenmpString = optionOpenmpStringPrefix+openmpFlagString
			;
	private JLabel labelCompiler = new JLabel("C compiler");
	private JLabel labelOpenmp = new JLabel("OpenMP");
	private static JTextField textCompiler;
	private static JTextField textOpenmp;
	private JButton buttonSaveCompiler;
	private String buttonCompilerText = "Save",
			buttonSaveCompilerTip = "Save the C compiler and OpenMP flag for C compiler into option file: " + Driver.preferencesFile,
			textCompilerTip = "GNU: gcc/g++; Intel: icc; PGI: pgcc/pgCC; Cygwin: gcc-4/g++-4",
			textOpenmpTip = "GNU: -fopenmp; Intel: -openmp; PGI: -mp; Cygwin: -fopenmp";
			
	// ------------ Advanced Options Panel -------------
	JPanel advOptionsPanel;
	JCheckBox checkAdvOptionsPanel;
	private String
			checkAdvOptionsPanelText = "show advanced options",
			checkAdvOptionsPanelTip = "Check to change advanced options"; 	
	
	// ------------ inline buttons ----------------------
	private String 
			optionInlineMainString = "-tinline=mode=0",
			optionInlineForLoopsOnlyString = "-tinline=foronly=1",
			radioInlineOffString = "do not inline",
			optionInlineMainText = "inline inside main function",
			optionInlineForLoopsOnlyText = "inline inside for loops only",
			radioInlineMainString = (basicOptionStringOnOffFront?optionInlineMainString+" ":emptyString)
				+optionInlineMainText
				+(basicOptionStringOnOffEnd?" ("+optionInlineMainString+")":emptyString),
			radioInlineForLoopsOnlyString = (basicOptionStringOnOffFront?optionInlineForLoopsOnlyString+" ":emptyString)
				+optionInlineForLoopsOnlyText
				+(basicOptionStringOnOffEnd?" ("+optionInlineForLoopsOnlyString+")":emptyString),
			radioInlineTip = "(Experimental) Perform simple subroutine inline expansion tranformation",
			comboInlineTip = new String(radioInlineTip);
	private String[]
			optionInlineStrings = {
				optionOffString,
				optionInlineMainString,
				optionInlineForLoopsOnlyString},
			comboInlineStrings = {
				"*"+radioInlineOffString,
				(advOptionStringOnOffFront?optionInlineMainString+" ":emptyString)
					+optionInlineMainText
					+(advOptionStringOnOffEnd?" ("+optionInlineMainString+")":emptyString),
				(advOptionStringOnOffFront?optionInlineForLoopsOnlyString+" ":emptyString)
					+optionInlineForLoopsOnlyText
					+(advOptionStringOnOffEnd?" ("+optionInlineForLoopsOnlyString+")":emptyString)};
	private JRadioButton radioInlineOff, radioInlineMain, radioInlineForLoopsOnly;
	private ButtonGroup groupRadioInline;
	private JComboBox comboInline;
	int optionInlineOffIndex = CetusGUITools.findStringInStrings(optionOffString,optionInlineStrings);
	int optionInlineMainIndex = CetusGUITools.findStringInStrings(optionInlineMainString,optionInlineStrings);
	int optionInlineForLoopsOnlyIndex = CetusGUITools.findStringInStrings(optionInlineForLoopsOnlyString,optionInlineStrings);

	// --------------- -parallelize-loops ----------------	
	private String
			optionParallelizeLoops0String = "-parallelize-loops=0",
			optionParallelizeLoops1String = "-parallelize-loops=1",
			optionParallelizeLoops2String = "-parallelize-loops=2",
			optionParallelizeLoops3String = "-parallelize-loops=3",
			optionParallelizeLoops4String = "-parallelize-loops=4",
			optionParallelizeLoops2Text = "parallelize all loops in nests",
			checkParallelizeLoops2String = (basicOptionStringOnOffFront?optionParallelizeLoops2String+" ":emptyString)
				+optionParallelizeLoops2Text+(basicOptionStringOnOffEnd?" ("+optionParallelizeLoops2String+")":emptyString), 
			checkParallelizeLoops2Tip = "By default Cetus parallelizes outermost loops", 
			comboParallelizeLoopsTip = "Annotate loops with parallelization decisions";
	private String[] 
			optionParallelizeLoopsStrings = {
				optionParallelizeLoops0String,
				optionParallelizeLoops1String,
				optionParallelizeLoops2String,
				optionParallelizeLoops3String,
				optionParallelizeLoops4String},
			comboParallelizeLoopsStrings = {
				(advOptionStringOnOffFront?optionParallelizeLoops0String+" ":emptyString)
					+"do not parallelize loops"
					+(advOptionStringOnOffEnd?" ("+optionParallelizeLoops0String+")":emptyString), //-parallelize-loops: argument not used, do nothing
				(advOptionStringOnOffFront?optionParallelizeLoops1String+" ":emptyString)
					+"*parallelize outermost loops"
					+(advOptionStringOnOffEnd?" ("+optionParallelizeLoops1String+")":emptyString), 
				(advOptionStringOnOffFront?optionParallelizeLoops2String+" ":emptyString)
					+optionParallelizeLoops2Text
					+(advOptionStringOnOffEnd?" ("+optionParallelizeLoops2String+")":emptyString), 
				(advOptionStringOnOffFront?optionParallelizeLoops3String+" ":emptyString)
					+"parallelize outermost loops w/ report"
					+(advOptionStringOnOffEnd?" ("+optionParallelizeLoops3String+")":emptyString), 
				(advOptionStringOnOffFront?optionParallelizeLoops4String+" ":emptyString)
					+"parallelize all loops w/ report"
					+(advOptionStringOnOffEnd?" ("+optionParallelizeLoops4String+")":emptyString)};
	private JCheckBox checkParallelizeLoops2;
	private JComboBox comboParallelizeLoops;
	int optionParallelizeLoops0Index = CetusGUITools.findStringInStrings(optionParallelizeLoops0String,optionParallelizeLoopsStrings);
	int optionParallelizeLoops1Index = CetusGUITools.findStringInStrings(optionParallelizeLoops1String,optionParallelizeLoopsStrings);
	int optionParallelizeLoops2Index = CetusGUITools.findStringInStrings(optionParallelizeLoops2String,optionParallelizeLoopsStrings);
	int optionParallelizeLoops3Index = CetusGUITools.findStringInStrings(optionParallelizeLoops3String,optionParallelizeLoopsStrings);
	int optionParallelizeLoops4Index = CetusGUITools.findStringInStrings(optionParallelizeLoops4String,optionParallelizeLoopsStrings);
	
	// --------------- -profitable-omp ----------------	
	private String
			optionProfitableOmp0String = "-profitable-omp=0",
			optionProfitableOmp1String = "-profitable-omp=1",
			optionProfitableOmp2String = "-profitable-omp=2",
			checkProfitableOmp0String = (basicOptionStringOnOffFront?optionProfitableOmp0String+" ":emptyString)
				+"profitability test disabled"+(basicOptionStringOnOffEnd?" ("+optionProfitableOmp0String+")":emptyString), 
			checkProfitableOmp0Tip = "Inserts runtime for selecting profitable omp parallel region (ON=1) " +
					"(See the API documentation for more details)", 
			comboProfitableOmpTip = new String(checkProfitableOmp0Tip);
	private String[] 
			optionProfitableOmpStrings = {
				optionProfitableOmp0String,
				optionProfitableOmp1String,
				optionProfitableOmp2String},
			comboProfitableOmpStrings = {
				(advOptionStringOnOffFront?optionProfitableOmp0String+" ":emptyString)
					+"profitability test disabled"
					+(advOptionStringOnOffEnd?" ("+optionProfitableOmp0String+")":emptyString), 
				(advOptionStringOnOffFront?optionProfitableOmp1String+" ":emptyString)
					+"*profitability: model-based loop selection"
					+(advOptionStringOnOffEnd?" ("+optionProfitableOmp1String+")":emptyString), 
				(advOptionStringOnOffFront?optionProfitableOmp2String+" ":emptyString)
					+"profitability: profile-based loop selection"
					+(advOptionStringOnOffEnd?" ("+optionProfitableOmp2String+")":emptyString)};
	private JCheckBox checkProfitableOmp0;
	private JComboBox comboProfitableOmp;
//	int optionProfitableOmpOffIndex = CetusGUITools.findStringInArray(optionProfitableOmpStrings,optionOffString);
	int optionProfitableOmp0Index = CetusGUITools.findStringInStrings(optionProfitableOmp0String,optionProfitableOmpStrings);
	int optionProfitableOmp1Index = CetusGUITools.findStringInStrings(optionProfitableOmp1String,optionProfitableOmpStrings);
	int optionProfitableOmp2Index = CetusGUITools.findStringInStrings(optionProfitableOmp2String,optionProfitableOmpStrings);
	
	// --------------- -reduction ----------------	
	private String
			optionReduction0String = "-reduction=0",
			optionReduction1String = "-reduction=1",
			optionReduction2String = "-reduction=2",
			comboReductionTip = "Perform reduction variable analysis";
	private String[] optionReductionStrings = { 
			optionReduction0String,
			optionReduction1String, 
			optionReduction2String };
	private String[] comboReductionStrings = {
			(advOptionStringOnOffFront?optionReduction0String+" ":emptyString)
				+"reduction analysis disabled"
				+(advOptionStringOnOffEnd?" ("+optionReduction0String+")":emptyString),
			(advOptionStringOnOffFront?optionReduction1String+" ":emptyString)
				+"reduction: scalars only"
				+(advOptionStringOnOffEnd?" ("+optionReduction1String+")":emptyString),
			(advOptionStringOnOffFront?optionReduction2String+" ":emptyString)
				+"*reduction: scalars and arrays"
				+(advOptionStringOnOffEnd?" ("+optionReduction2String+")":emptyString)};
	private JComboBox comboReduction;
	
	// --------------- -teliminate-branch ----------------	
	private String
			optionTeliminateBranch0String = "-teliminate-branch=0",
			optionTeliminateBranch1String = "-teliminate-branch=1",
			optionTeliminateBranch2String = "-teliminate-branch=2",
			comboTeliminateBranchTip = "Eliminates unreachable branch targets";
	private String[] optionTeliminateBranchStrings = { 
			optionTeliminateBranch0String, 
			optionTeliminateBranch1String, 
			optionTeliminateBranch2String};
	private String[] comboTeliminateBranchStrings = {
			(advOptionStringOnOffFront ? optionTeliminateBranch0String+" ":emptyString)
				+ "eliminate-branch disabled"
				+(advOptionStringOnOffEnd?" ("+optionTeliminateBranch0String+")":emptyString),
			(advOptionStringOnOffFront ? optionTeliminateBranch1String+" ":emptyString)
				+ "*eliminate-branch enabled"
				+(advOptionStringOnOffEnd?" ("+optionTeliminateBranch1String+")":emptyString),
			(advOptionStringOnOffFront ? optionTeliminateBranch2String+" ":emptyString)
				+ "eliminate-branch: old statements to comments"
				+(advOptionStringOnOffEnd?" ("+optionTeliminateBranch2String+")":emptyString)};
	private JComboBox comboTeliminateBranch;
	
	// --------------- alias ----------------	
	private String
			optionAlias0String = "-alias=0",
			optionAlias1String = "-alias=1",
			optionAlias2String = "-alias=2",
			optionAlias3String = "-alias=3",
			comboAliasTip = "Specify level of alias analysis";
	private String[] optionAliasStrings = { 
			optionAlias0String,
			optionAlias1String,
			optionAlias2String,
			optionAlias3String};
	private String[] comboAliasStrings = {
			(advOptionStringOnOffFront ? optionAlias0String+" ":emptyString)
				+ "alias: assume all locations aliased"
				+ (advOptionStringOnOffEnd?" ("+optionAlias0String+")":emptyString),
			(advOptionStringOnOffFront ? optionAlias1String+" ":emptyString)
				+ "*alias: advanced interprocedural analysis"
				+ (advOptionStringOnOffEnd?" ("+optionAlias1String+")":emptyString),
			(advOptionStringOnOffFront ? optionAlias2String+" ":emptyString)
				+ "alias: assume no alias unless proven"
				+ (advOptionStringOnOffEnd?" ("+optionAlias2String+")":emptyString),
			(advOptionStringOnOffFront ? optionAlias3String+" ":emptyString)
				+ "alias: assume no alias exists" 
				+(advOptionStringOnOffEnd?" ("+optionAlias3String+")":emptyString)};
	private JComboBox comboAlias;
	
	// --------------- -privatize ----------------	
	private String
			optionPrivatize0String = "-privatize=0",
			optionPrivatize1String = "-privatize=1",
			optionPrivatize2String = "-privatize=2",
			comboPrivatizeTip = "Perform scalar/array privatization analysis";
	private String[] optionPrivatizeStrings = { 
			optionPrivatize0String,
			optionPrivatize1String, 
			optionPrivatize2String};
	private String[] comboPrivatizeStrings = {
			(advOptionStringOnOffFront?optionPrivatize0String+" ":emptyString)
				+"privatization analysis disabled"
				+(advOptionStringOnOffEnd?" ("+optionPrivatize0String+")":emptyString),
			(advOptionStringOnOffFront?optionPrivatize1String+" ":emptyString)
				+"privatization: scalars only"
				+(advOptionStringOnOffEnd?" ("+optionPrivatize1String+")":emptyString),
			(advOptionStringOnOffFront?optionPrivatize2String+" ":emptyString)
				+"*privatization: scalars and arrays"
				+(advOptionStringOnOffEnd?" ("+optionPrivatize2String+")":emptyString)};
	private JComboBox comboPrivatize;
	
	// --------------- induction ----------------	
	private String
			optionInduction0String = "-induction=0",
			optionInduction1String = "-induction=1",
			optionInduction2String = "-induction=2",
			optionInduction3String = "-induction=3",
			comboInductionTip = "Perform induction variable substitution";
	private String[] optionInductionStrings = { 
			optionInduction0String,
			optionInduction1String,
			optionInduction2String,
			optionInduction3String};
	private String[] comboInductionStrings = {
			(advOptionStringOnOffFront ? optionInduction0String+" ":emptyString)
				+"induction analysis disabled"
				+(advOptionStringOnOffEnd?" ("+optionInduction0String+")":emptyString),
			(advOptionStringOnOffFront ? optionInduction1String+" ":emptyString)
				+ "induction: linear variables substitution"
				+(advOptionStringOnOffEnd?" ("+optionInduction1String+")":emptyString),
			(advOptionStringOnOffFront ? optionInduction2String+" ":emptyString)
				+ "induction: generalized variables substitution"
				+(advOptionStringOnOffEnd?" ("+optionInduction2String+")":emptyString),
			(advOptionStringOnOffFront ? optionInduction3String+" ":emptyString)
				+ "*induction: zero-trip loops runtime test"
				+(advOptionStringOnOffEnd?" ("+optionInduction3String+")":emptyString)};
	private JComboBox comboInduction;
	
	// --------------- -verbosity -------------
	private String
			optionVerbosity0String = "-verbosity=0",
			optionVerbosity1String = "-verbosity=1",
			optionVerbosity2String = "-verbosity=2",
			optionVerbosity3String = "-verbosity=3",
			optionVerbosity4String = "-verbosity=4",
			comboVerbosityTip = "Degree of status messages (0-4) that you wish to see (default is 0)";
	private String[] optionVerbosityStrings = { 
			optionVerbosity0String,
			optionVerbosity1String,
			optionVerbosity2String,
			optionVerbosity3String,
			optionVerbosity4String};
	private String[] comboVerbosityStrings = {
			(advOptionStringOnOffFront ? optionVerbosity0String+" ":emptyString)
				+ "*verbosity: degree of status messages (0)"
				+(advOptionStringOnOffEnd?" ("+optionVerbosity0String+")":emptyString),
			(advOptionStringOnOffFront ? optionVerbosity1String+" ":emptyString)
				+ "verbosity: degree of status messages (1)"
				+(advOptionStringOnOffEnd?" ("+optionVerbosity1String+")":emptyString),
			(advOptionStringOnOffFront ? optionVerbosity2String+" ":emptyString)
				+ "verbosity: degree of status messages (2)"
				+(advOptionStringOnOffEnd?" ("+optionVerbosity2String+")":emptyString),
			(advOptionStringOnOffFront ? optionVerbosity3String+" ":emptyString)
				+ "verbosity: degree of status messages (3)"
				+(advOptionStringOnOffEnd?" ("+optionVerbosity3String+")":emptyString),
			(advOptionStringOnOffFront ? optionVerbosity4String+" ":emptyString)
				+ "verbosity: degree of status messages (4)"
				+(advOptionStringOnOffEnd?" ("+optionVerbosity4String+")":emptyString)};
	private JComboBox comboVerbosity;
	
	// --------------- ddt (Data Dependence Testing) ----------------	
	private String
			optionDataDepend0String = "-ddt=0",
			optionDataDepend1String = "-ddt=1",
			optionDataDepend2String = "-ddt=2",
			comboDataDependTip = "Perform Data Dependence Testing";
	private String[] optionDataDependStrings = { 
			optionDataDepend0String,
			optionDataDepend1String,
			optionDataDepend2String};
	private String[] comboDataDependStrings = {
			(advOptionStringOnOffFront?optionDataDepend0String+" ":emptyString)
				+"ddt (data dependence test) disabled"
				+(advOptionStringOnOffEnd?" ("+optionDataDepend0String+")":emptyString),
			(advOptionStringOnOffFront?optionDataDepend1String+" ":emptyString)
				+"ddt: banerjee-wolfe (data dependence) test"
				+(advOptionStringOnOffEnd?" ("+optionDataDepend1String+")":emptyString),
			(advOptionStringOnOffFront?optionDataDepend2String+" ":emptyString)
				+"*ddt: range (data dependence) test"
				+(advOptionStringOnOffEnd?" ("+optionDataDepend2String+")":emptyString)};
	private JComboBox comboDataDepend;
	
	// --------------- -range ----------------	
	private String
			optionRange0String = "-range=0",
			optionRange1String = "-range=1",
			optionRange2String = "-range=2",
			comboRangeTip = "Specify the accuracy of symbolic analysis with value ranges";
	private String[] optionRangeStrings = { 
			optionRange0String, 
			optionRange1String, 
			optionRange2String};
	private String[] comboRangeStrings = {
			(advOptionStringOnOffFront ? optionRange0String+" ":emptyString)
				+ "range computation disabled (minimal)"
				+(advOptionStringOnOffEnd?" ("+optionRange0String+")":emptyString),
			(advOptionStringOnOffFront ? optionRange1String+" ":emptyString)
				+ "*range: local range computation"
				+(advOptionStringOnOffEnd?" ("+optionRange1String+")":emptyString),
			(advOptionStringOnOffFront ? optionRange2String+" ":emptyString)
				+ "range: inter-procedural computation (experimental)"
				+(advOptionStringOnOffEnd?" ("+optionRange2String+")":emptyString)};
	private JComboBox comboRange;

	// --------------- -profile-loops -------------
	private String
			optionProfileLoops1String = "-profile-loops=1",
			optionProfileLoops2String = "-profile-loops=2",
			optionProfileLoops3String = "-profile-loops=3",
			optionProfileLoops4String = "-profile-loops=4",
			// -profile-loops=5 or 6 is unsafe, so excluded
			comboProfileLoopsTip = "Inserts loop-profiling calls (=4 is preferred if turned ON)";
	private String[] optionProfileLoopsStrings = { 
			optionOffString,
			optionProfileLoops1String,
			optionProfileLoops2String,
			optionProfileLoops3String,
			optionProfileLoops4String};
	private String[] comboProfileLoopsStrings = {
			"*do not profile-loops",
			(advOptionStringOnOffFront ? optionProfileLoops1String+" ":emptyString)
				+ "profile-loops: every loop"
				+(advOptionStringOnOffEnd?" ("+optionProfileLoops1String+")":emptyString),
			(advOptionStringOnOffFront ? optionProfileLoops2String+" ":emptyString)
				+ "profile-loops: outermost loop"
				+(advOptionStringOnOffEnd?" ("+optionProfileLoops2String+")":emptyString),
			(advOptionStringOnOffFront ? optionProfileLoops3String+" ":emptyString)
				+ "profile-loops: every omp parallel"
				+(advOptionStringOnOffEnd?" ("+optionProfileLoops3String+")":emptyString),
			(advOptionStringOnOffFront ? optionProfileLoops4String+" ":emptyString)
				+ "profile-loops: outermost omp parallel"
				+(advOptionStringOnOffEnd?" ("+optionProfileLoops4String+")":emptyString)}; //default
	private JComboBox comboProfileLoops;	

	// --------------- -ompGen -------------
	private String
			optionOmpGen0String = "-ompGen=0",
			optionOmpGen1String = "-ompGen=1",
			optionOmpGen2String = "-ompGen=2",
			optionOmpGen3String = "-ompGen=3",
			optionOmpGen4String = "-ompGen=4",
			comboOmpGenTip = "Generate new OpenMP pragma and handle existing OpenMP pragrams";
	private String[] optionOmpGenStrings = {
			optionOmpGen0String,
			optionOmpGen1String,
			optionOmpGen2String,
			optionOmpGen3String,
			optionOmpGen4String};
	private String[] comboOmpGenStrings = {
			(advOptionStringOnOffFront ? optionOmpGen0String+" ":emptyString)
				+ "ompGen disabled, no OpenMP pragma generated"
				+(advOptionStringOnOffEnd?" ("+optionOmpGen0String+")":emptyString),
			(advOptionStringOnOffFront ? optionOmpGen1String+" ":emptyString)
				+ "*ompGen: comment out existing OpenMP pragmas"
				+(advOptionStringOnOffEnd?" ("+optionOmpGen1String+")":emptyString),
			(advOptionStringOnOffFront ? optionOmpGen2String+" ":emptyString)
				+ "ompGen: remove existing OpenMP pragmas"
				+(advOptionStringOnOffEnd?" ("+optionOmpGen2String+")":emptyString),
			(advOptionStringOnOffFront ? optionOmpGen3String+" ":emptyString)
				+ "ompGen: remove existing OpenMP & Cetus pragmas"
				+(advOptionStringOnOffEnd?" ("+optionOmpGen3String+")":emptyString),
			(advOptionStringOnOffFront ? optionOmpGen4String+" ":emptyString)
				+ "ompGen: keep all pragmas" 
				+(advOptionStringOnOffEnd?" ("+optionOmpGen4String+")":emptyString)}; //default
	private JComboBox comboOmpGen;		

	// ------------------ notes for default options ------------
	private JLabel labelNotesDefaultOptions;
	private String 
			labelNotesDefaultOptionsText = "* Indicates default options",
			labelNotesDefaultOptionsTip = labelNotesDefaultOptionsText
				+". They are the most common or powerful options";

	// ------------------ -preserve-KR-function ---------
	private JCheckBox checkPreserveKR;
	private String optionPreserveKRString = "-preserve-KR-function",
			checkPreserveKRTip = "Preserves K&R-style function declaration",
			checkPreserveKRText = (advOptionStringOnOffFront?optionPreserveKRString+" ":emptyString)
				+"Preserves K&R-style function"
				+(advOptionStringOnOffEnd?" ("+optionPreserveKRString+")":emptyString);
	
	// ------------------ -version ---------------
	private JCheckBox checkVersion;
	private String optionVersionString = "-version",
			checkVersionTip = "Print the version information",
			checkVersionText = (advOptionStringOnOffFront?optionVersionString+" ":emptyString)
				+checkVersionTip
				+(advOptionStringOnOffEnd?" ("+optionVersionString+")":emptyString);
	
	// ------------------ -callgraph ---------------
	private JCheckBox checkCallgraph;
	private String optionCallgraphString = "-callgraph",
			checkCallgraphTip = "Print the static call graph to stdout",
			checkCallgraphText = (advOptionStringOnOffFront?optionCallgraphString+" ":emptyString)
				+checkCallgraphTip
				+(advOptionStringOnOffEnd?" ("+optionCallgraphString+")":emptyString);

	// ------------------ -normalize-loops ---------------
	private JCheckBox checkNormalizeLoops;
	private String optionNormalizeLoopsString = "-normalize-loops",
			checkNormalizeLoopsTip = "Normalize for loops so they begin at 0 and have a step of 1",
			checkNormalizeLoopsText = (advOptionStringOnOffFront?
				optionNormalizeLoopsString+" ":emptyString)+"Normalize for loops"
				+(advOptionStringOnOffEnd?" ("+optionNormalizeLoopsString+")":emptyString);

	// ------------------ -normalize-return-stmt ---------------
	private JCheckBox checkNormalizeReturnStmt;
	private String optionNormalizeReturnStmtString = "-normalize-return-stmt",
			checkNormalizeReturnStmtTip = "Normalize return statements for all procedures",
			checkNormalizeReturnStmtText = (advOptionStringOnOffFront?
				optionNormalizeReturnStmtString+" ":emptyString)+"Normalize return statements"
				+(advOptionStringOnOffEnd?" ("+optionNormalizeReturnStmtString+")":emptyString);
	
	// ------------------ -tsingle-call ---------------
	private JCheckBox checkTsingleCall;
	private String optionTsingleCallString = "-tsingle-call",
			checkTsingleCallTip = "Transform all statements so they contain at most one function call",
			checkTsingleCallText = (advOptionStringOnOffFront?
				optionTsingleCallString+" ":emptyString)+"Transform to max one function call"
				+(advOptionStringOnOffEnd?" ("+optionTsingleCallString+")":emptyString);

	// ------------------ -tsingle-declarator ---------------
	private JCheckBox checkTsingleDeclarator;
	private String optionTsingleDeclaratorString = "-tsingle-declarator",
			checkTsingleDeclaratorTip = "Transform all variable declarations so they contain at most one declarator",
			checkTsingleDeclaratorText = (advOptionStringOnOffFront?
				optionTsingleDeclaratorString+" ":emptyString)+"Transform to max one declarator"
				+(advOptionStringOnOffEnd?" ("+optionTsingleDeclaratorString+")":emptyString);

	// ------------------ -tsingle-return ---------------
	private JCheckBox checkTsingleReturn;
	private String optionTsingleReturnString = "-tsingle-return",
			checkTsingleReturnTip = "Transform all procedures so they have a single return statement",
			checkTsingleReturnText = (advOptionStringOnOffFront?
				optionTsingleReturnString+" ":emptyString)+"Transform procedures to single return"
				+(advOptionStringOnOffEnd?" ("+optionTsingleReturnString+")":emptyString);
	
	// ----------------- preprocessor -------------
	private JCheckBox checkPreprocessor;
	private String optionPreprocessorString = (CetusGUITools.osName.toLowerCase()).indexOf("win")>=0?
			"-preprocessor=\"cpp-4.exe -E\"":"-preprocessor=\"cpp -C -I.\"",
			checkPreprocessorTip = "Change preprocessor command and options if needed. Default: "
				+optionPreprocessorString,
			checkPreprocessorText = "Preprocessor";

//	private JLabel labelPreprocessor;
	private JTextField textPreprocessor;
	private JButton buttonPreprocessor;
	private String 
//			labelPreprocessorText = "Preprocessor:",
//			labelPreprocessorTip = "Change preprocessor command. " +
//					"The default one is determined based on operating system.",
			buttonPreprocessorText = "Apply",
			buttonPreprocessorTip = "Save preprocessor command and options",
			textPreprocessorTip = "Default: "+ optionPreprocessorString;
//			textPreprocessorDefaultText = (CetusGUITools.osName.toLowerCase()).indexOf("win")>=0?
//					"cpp-4.exe -E":optionOffString;
	
	// ----------------- Reset options -------------
	private JButton buttonResetOptions;
	private String 
			buttonResetOptionsText = "Reset Cetus Options",
			buttonResetOptionsTip = "Reset all above options to default";
			
	// ----------------- output folder -------------
	private JButton buttonOutDir;
	private String 
			optionOutDirStringPrefix = "-outdir=",
			buttonOutDirTip = "Click to change output directory; current: "+CetusGUI.outputFilePath, 
			buttonOutDirText = "Change Output Directory";
	private File fileOutDir = null;
	
	// ----------------- CheckUpdate -------------
	private JButton buttonCheckUpdate;
	private String 
			optionCheckUpdateStringPrefix = "-update=",
			buttonCheckUpdateText = "Check Update",
			buttonCheckUpdateTip = "Check if a new Cetus is available online";

	// ----------------- PrintOptions -------------
	private JButton buttonPrintOptions;
	private String 
			buttonPrintOptionsText = "Print Options",
			buttonPrintOptionsTip = "Print all options in memory onto console in array format (for debug purpose)";

	// ------------------ AutoLoadOptions ---------------
	private JCheckBox checkAutoLoadOptions;
	private String 
			optionAutoLoadOptions1String = "-autoloadoptions=1",
			optionAutoLoadOptions0String = "-autoloadoptions=0",
			checkAutoLoadOptionsTip = "Automatically load default option file when starting Cetus GUI ("+Driver.preferencesFile.toString()+")",
			checkAutoLoadOptionsText = "Auto load options when starting Cetus";
	
	// ----------------- SaveOptions -------------
	private JButton buttonSaveOptions;
	private String 
			buttonSaveOptionsText = "Save Options",
			buttonSaveOptionsTip = "Save options to "+Driver.preferencesFile.toString();
			
	// ----------------- LoadOptions -------------
	private JButton buttonLoadOptions;
	private String 
			buttonLoadOptionsText = "Load Options",
			buttonLoadOptionsTip = "Load options from "+Driver.preferencesFile.toString();
			
	// ----------------- SaveOptionsAs -------------
	private JButton buttonSaveOptionsAs;
	private String 
			buttonSaveOptionsAsText = "Save Options As",
			buttonSaveOptionsAsTip = "Save options to user file";
			
	// ----------------- LoadOptionsFrom -------------
	private JButton buttonLoadOptionsFrom;
	private String 
			buttonLoadOptionsFromText = "Load Options From",
			buttonLoadOptionsFromTip = "Load options from user file";
			
	
	// ------------------ notes for options to help ------------
	private JLabel labelNotesOptionToHelp;
	private String 
			labelNotesOptionToHelpText = "For options' description, see [About]->Help",
			labelNotesOptionToHelpTip = labelNotesOptionToHelpText;

	
	
	
	
	
	
	
	
	

	
	
	
	
	
	
	
	
	
	
	
	
	// --------------- cetus Option index ----------------------
	private int cetusOptionIndex = 0,
			optionPreprocessorIndex = ++cetusOptionIndex, optionParallelizeLoopsIndex = ++cetusOptionIndex, 
			optionAliasIndex = ++cetusOptionIndex, optionPrivatizeIndex = ++cetusOptionIndex, 
			optionReductionIndex = ++cetusOptionIndex, optionInductionIndex = ++cetusOptionIndex,
			optionTeliminateBranchIndex = ++cetusOptionIndex,optionProfitableOmpIndex = ++cetusOptionIndex,
			optionInlineIndex = ++cetusOptionIndex, optionOutDirIndex = ++cetusOptionIndex, 
			optionPreserveKRIndex = ++cetusOptionIndex, optionVerbosityIndex = ++cetusOptionIndex, 
			optionVersionIndex = ++cetusOptionIndex,  optionCallgraphIndex = ++cetusOptionIndex,
			optionDataDependIndex = ++cetusOptionIndex, optionRangeIndex = ++cetusOptionIndex, 
			optionNormalizeLoopsIndex = ++cetusOptionIndex, optionNormalizeReturnStmtIndex = ++cetusOptionIndex, 
			optionProfileLoopsIndex = ++cetusOptionIndex, optionTsingleCallIndex = ++cetusOptionIndex, 
			optionTsingleDeclaratorIndex = ++cetusOptionIndex, optionTsingleReturnIndex = ++cetusOptionIndex, 
			optionOmpGenIndex = ++cetusOptionIndex, optionDebugParserInputIndex = ++cetusOptionIndex
			
			
			
			;

	// --------------- GUI Option index ----------------------
	private int 
			guiOptionIndex = numCetusOptions,
			optionAutoLoadOptionsIndex = ++guiOptionIndex,
			optionCheckUpdateIndex = ++guiOptionIndex,
			optionServerIndex = ++guiOptionIndex,
			optionCompilerIndex = ++guiOptionIndex,
			optionOpenmpIndex = ++guiOptionIndex
			
			;

			
			
	public CetusGUIOptionsPanel(){
//		for (i=0;i<numAllOptions;i++) {
//			allOptionsStrings[i]=optionOffString; //
//		}
		initializeAllOptionNames();
		initializeDefaultCetusOptionsArray();
		initializeDefaultGuiOptionsArray();
		initializeAllOptionsPanel();
		
//		System.out.println("*** allOptionsNameStrings: "+Arrays.toString(allOptionsNameStrings));
//		String[] tempOptionStrings = loadOptionsFromFile(CetusGUI.preferencesFile);
//		allOptionsStrings = CetusGUITools.matchCopyStrings (allOptionsNameStrings, tempOptionStrings, optionOffString);
//		System.out.println("*** allOptionsStrings: "+Arrays.toString(allOptionsStrings));
		
	}
	
	private void initializeAllOptionNames() {
		
		for (i=0;i<numAllOptions;i++) {
			allOptionsNameStrings[i]="$"; //
		}
		
		// option names
		allOptionsNameStrings[optionPreprocessorIndex]="-preprocessor";
		allOptionsNameStrings[optionParallelizeLoopsIndex]="-parallelize-loops";
		allOptionsNameStrings[optionAliasIndex]="-alias";
		allOptionsNameStrings[optionPrivatizeIndex]="-privatize";
		allOptionsNameStrings[optionReductionIndex]="-reduction";
		allOptionsNameStrings[optionInductionIndex]="-induction";
		allOptionsNameStrings[optionTeliminateBranchIndex]="-teliminate-branch";
		allOptionsNameStrings[optionProfitableOmpIndex]="-profitable-omp";
		allOptionsNameStrings[optionInlineIndex]="-tinline";
		allOptionsNameStrings[optionOutDirIndex]="-outdir";
		allOptionsNameStrings[optionPreserveKRIndex]="-preserve-KR-function";
		allOptionsNameStrings[optionVerbosityIndex]="-verbosity";
		allOptionsNameStrings[optionVersionIndex]="-version";
		allOptionsNameStrings[optionCallgraphIndex]="-callgraph";
		allOptionsNameStrings[optionDataDependIndex]="-ddt";
		allOptionsNameStrings[optionRangeIndex]="-range";
		allOptionsNameStrings[optionNormalizeLoopsIndex]="-normalize-loops";
		allOptionsNameStrings[optionNormalizeReturnStmtIndex]="-normalize-return-stmt";
		allOptionsNameStrings[optionProfileLoopsIndex]="-profile-loops";
		allOptionsNameStrings[optionTsingleCallIndex]="-tsingle-call";
		allOptionsNameStrings[optionTsingleDeclaratorIndex]="-tsingle-declarator";
		allOptionsNameStrings[optionTsingleReturnIndex]="-tsingle-return";
		allOptionsNameStrings[optionOmpGenIndex]="-ompGen";
		allOptionsNameStrings[optionDebugParserInputIndex]="-debug_parser_input";
		
		//------ gui options ----------
		allOptionsNameStrings[optionAutoLoadOptionsIndex]="-autoloadoptions";
		allOptionsNameStrings[optionCheckUpdateIndex]="-update";
		allOptionsNameStrings[optionServerIndex]=optionServerStringPrefixNoEqual;
		allOptionsNameStrings[optionCompilerIndex]="-compiler";
		allOptionsNameStrings[optionOpenmpIndex]="-ompflag";
	}
	
	private void initializeDefaultCetusOptionsArray(){
		
		//System.out.println("Initialize default Cetus options...");
		//allOptionsStrings = new String[numAllOptions];
		for (i=0;i<numCetusOptions;i++) {
			allOptionsStrings[i]=optionOffString; //
		}
		
		allOptionsStrings[optionPreprocessorIndex] = optionPreprocessorString;
		allOptionsStrings[optionParallelizeLoopsIndex]=optionParallelizeLoops1String;
		allOptionsStrings[optionReductionIndex]=optionReduction2String;
		allOptionsStrings[optionTeliminateBranchIndex]=optionTeliminateBranch1String;
		allOptionsStrings[optionProfitableOmpIndex]=optionProfitableOmp1String;
		
		allOptionsStrings[optionAliasIndex]=optionAlias1String;	
		allOptionsStrings[optionPrivatizeIndex]=optionPrivatize2String;	
		allOptionsStrings[optionInductionIndex]=optionInduction3String;	
		allOptionsStrings[optionVerbosityIndex]=optionVerbosity0String;	
		allOptionsStrings[optionDataDependIndex]=optionDataDepend2String;	
		allOptionsStrings[optionRangeIndex]=optionRange1String;	
		allOptionsStrings[optionOmpGenIndex]=optionOmpGen1String;	
		allOptionsStrings[optionOutDirIndex]=optionOutDirStringPrefix+CetusGUI.outputFilePath;	
//		System.out.println("**************"+allOptionsStrings[optionOutDirIndex]);
		
//		allOptionsStrings[optionIndex]=optionString;

		
	}
	
	private void initializeDefaultGuiOptionsArray(){
		
		//System.out.println("Initialize default GUI options...");
		for (i=numCetusOptions;i<numAllOptions;i++) {
			allOptionsStrings[i]=optionOffString; //
		}
		allOptionsStrings[optionAutoLoadOptionsIndex]=optionAutoLoadOptions1String;	
		allOptionsStrings[optionCheckUpdateIndex]=optionCheckUpdateStringPrefix+"1";
		allOptionsStrings[optionServerIndex]=optionServerStringPrefixNoEqual+"=0";
		allOptionsStrings[optionCompilerIndex]=optionCompilerString;
		allOptionsStrings[optionOpenmpIndex]=optionOpenmpString;
		
		
	}
	
	public void initializeAllOptionsPanel() {
		
		//setLayout(new GridLayout(10, 2));
		//setToolTipText(defaultOptionsTip);
		//setLayout(new CardLayout());
		//setLayout(new BorderLayout(5, 5));
		//setLayout(new FlowLayout());
		//setLayout(new BoxLayout());
		setLayout(null);
		
		// -------------- radio buttons for inline ------------------
//		allOptionsStrings[comboInlineIndex] = optionInlineMainString;
//		allOptionsStrings[comboInlineIndex] = optionInlineForLoopsOnlyString;
		radioInlineOff = new JRadioButton(radioInlineOffString,	
			allOptionsStrings[optionInlineIndex].equals(optionOffString));
		radioInlineOff.addActionListener(this);
		radioInlineMain = new JRadioButton(radioInlineMainString,
			allOptionsStrings[optionInlineIndex].equals(optionInlineMainString));
		radioInlineMain.addActionListener(this);
		radioInlineForLoopsOnly = new JRadioButton(radioInlineForLoopsOnlyString,
			allOptionsStrings[optionInlineIndex].equals(optionInlineForLoopsOnlyString));
		radioInlineForLoopsOnly.addActionListener(this);
		groupRadioInline = new ButtonGroup();
		groupRadioInline.add(radioInlineOff);
		groupRadioInline.add(radioInlineMain);
		groupRadioInline.add(radioInlineForLoopsOnly);
		radioInlineOff.setToolTipText(radioInlineTip);
		radioInlineMain.setToolTipText(radioInlineTip);
		radioInlineForLoopsOnly.setToolTipText(radioInlineTip);
//		radioInlineOff.setBounds(column1, (mainButtonIndex)*(yGap+height), 100, height);
//		radioInlineMain.setBounds(column1+xGap+100, (mainButtonIndex)*(yGap+height), 200, height);
//		radioInlineForLoopsOnly.setBounds(column1+2*xGap+300, (++mainButtonIndex)*(yGap+height), 200, height);
		radioInlineOff.setBounds(column1, (++layoutBasicY1)*(yGap+height1)+yGap, width, height1);
		radioInlineMain.setBounds(column1, (++layoutBasicY1)*(yGap+height1)+yGap, width, height1);
		radioInlineForLoopsOnly.setBounds(column1, (++layoutBasicY1)*(yGap+height1)+yGap, width, height1);
		add(radioInlineOff);
		add(radioInlineMain);
		add(radioInlineForLoopsOnly);
		
		checkParallelizeLoops2 = new JCheckBox(checkParallelizeLoops2String, 
				allOptionsStrings[optionParallelizeLoopsIndex].equals(optionParallelizeLoops2String));
		checkParallelizeLoops2.setToolTipText(checkParallelizeLoops2Tip);
		checkParallelizeLoops2.addActionListener(this);
		checkParallelizeLoops2.setBounds(column2, (++layoutBasicY2)*(yGap+height2), width, height2);
		add(checkParallelizeLoops2);

		// -------------- profitable omp ----------------------------
		checkProfitableOmp0 = new JCheckBox(checkProfitableOmp0String, 
				allOptionsStrings[optionProfitableOmpIndex].equals(optionProfitableOmp0String));
		checkProfitableOmp0.setToolTipText(checkProfitableOmp0Tip);
		checkProfitableOmp0.addActionListener(this);
		checkProfitableOmp0.setBounds(column2, (++layoutBasicY2)*(yGap+height2), width, height2);
		add(checkProfitableOmp0);
		
		// --------------- use Cetus remote server ------------------------
		checkServer = new JCheckBox(checkServerText,allOptionsStrings[optionServerIndex].equals(optionServerStringPrefixNoEqual+"=1"));
		checkServer.setToolTipText(checkServerTip);
		checkServer.addActionListener(this);
		checkServer.setBounds(column2, (++layoutBasicY2)*(yGap+height2), width, height2);
		add(checkServer);
		//checkServer.setEnabled(false);

		// -------------- check box to turn on/off advanced options ------------
		checkAdvOptionsPanel = new JCheckBox(checkAdvOptionsPanelText, false);
		checkAdvOptionsPanel.setToolTipText(checkAdvOptionsPanelTip);
		checkAdvOptionsPanel.addActionListener(this);
		checkAdvOptionsPanel.setBounds(column1, (++layoutBasicY1)*(yGap+height2), width, height2);
		add(checkAdvOptionsPanel);
		
		// ------------- Separator --------------------
		separator1 = new JSeparator();
		separator1.setBounds(column1, (++layoutBasicY1)*(yGap+height2)+10, 1600, height2);
		add(separator1);
		
		// -------------- panel for advanced options ---------------------------
		advOptionsPanel = new JPanel();
		advOptionsPanel.setBounds(column1, (++layoutBasicY1)*(yGap+height2), 1600, 1200);
		advOptionsPanel.setLayout(null);
		advOptionsPanel.setVisible(false);
		add(advOptionsPanel);
		
		// -------------- combo box for parallelize loops options ---------------
		comboParallelizeLoops = new JComboBox(comboParallelizeLoopsStrings);
		comboParallelizeLoops.setSelectedIndex(CetusGUITools.findStringInStrings(allOptionsStrings[optionParallelizeLoopsIndex], 
				optionParallelizeLoopsStrings));
		comboParallelizeLoops.setToolTipText(comboParallelizeLoopsTip);
		comboParallelizeLoops.addActionListener(this);
		comboParallelizeLoops.setBounds(column1, (++layoutAdvY1)*(yGap+height2), width, height2);
		advOptionsPanel.add(comboParallelizeLoops);

		// -------------- combo box for Profitable Omp options ---------------
		comboProfitableOmp = new JComboBox(comboProfitableOmpStrings);
		comboProfitableOmp.setSelectedIndex(CetusGUITools.findStringInStrings(allOptionsStrings[optionProfitableOmpIndex], 
				optionProfitableOmpStrings));
		comboProfitableOmp.setToolTipText(comboProfitableOmpTip);
		comboProfitableOmp.addActionListener(this);
		comboProfitableOmp.setBounds(column1, (++layoutAdvY1)*(yGap+height2), width, height2);
		advOptionsPanel.add(comboProfitableOmp);
		
		// -------------- combo box for reduction options ---------------
		comboReduction = new JComboBox(comboReductionStrings);
		comboReduction.setSelectedIndex(CetusGUITools.findStringInStrings(allOptionsStrings[optionReductionIndex], 
				optionReductionStrings));
		comboReduction.setToolTipText(comboReductionTip);
		comboReduction.addActionListener(this);
		comboReduction.setBounds(column1, (++layoutAdvY1)*(yGap+height2), width, height2);
		advOptionsPanel.add(comboReduction);
		
		// -------------- combo box for Teliminate Branch options ---------------
		comboTeliminateBranch = new JComboBox(comboTeliminateBranchStrings);
		comboTeliminateBranch.setSelectedIndex(CetusGUITools.findStringInStrings(allOptionsStrings[optionTeliminateBranchIndex], 
				optionTeliminateBranchStrings));
		comboTeliminateBranch.setToolTipText(comboTeliminateBranchTip);
		comboTeliminateBranch.addActionListener(this);
		comboTeliminateBranch.setBounds(column1, (++layoutAdvY1)*(yGap+height2), width, height2);
		advOptionsPanel.add(comboTeliminateBranch);
		
		// -------------- combo box for alias options ---------------
		comboAlias = new JComboBox(comboAliasStrings);
		comboAlias.setSelectedIndex(CetusGUITools.findStringInStrings(allOptionsStrings[optionAliasIndex], 
				optionAliasStrings));
		comboAlias.setToolTipText(comboAliasTip);
		comboAlias.addActionListener(this);
		comboAlias.setBounds(column1, (++layoutAdvY1)*(yGap+height2), width, height2);
		advOptionsPanel.add(comboAlias);
		
		// -------------- combo box for inline options --------------------------
		comboInline = new JComboBox(comboInlineStrings);
		comboInline.setSelectedIndex(CetusGUITools.findStringInStrings(allOptionsStrings[optionInlineIndex], 
				optionInlineStrings));
		comboInline.setToolTipText(comboInlineTip);
		comboInline.addActionListener(this);
		comboInline.setBounds(column1, (++layoutAdvY1)*(yGap+height2), width, height2);
		advOptionsPanel.add(comboInline);
		
		// -------------- combo box for privatize options --------------------------
		comboPrivatize = new JComboBox(comboPrivatizeStrings);
		comboPrivatize.setSelectedIndex(CetusGUITools.findStringInStrings(allOptionsStrings[optionPrivatizeIndex], 
				optionPrivatizeStrings));
		comboPrivatize.setToolTipText(comboPrivatizeTip);
		comboPrivatize.addActionListener(this);
		comboPrivatize.setBounds(column1, (++layoutAdvY1)*(yGap+height2), width, height2);
		advOptionsPanel.add(comboPrivatize);

		// -------------- combo box for induction options --------------------------
		comboInduction = new JComboBox(comboInductionStrings);
		comboInduction.setSelectedIndex(CetusGUITools.findStringInStrings(allOptionsStrings[optionInductionIndex], 
				optionInductionStrings));
		comboInduction.setToolTipText(comboInductionTip);
		comboInduction.addActionListener(this);
		comboInduction.setBounds(column1, (++layoutAdvY1)*(yGap+height2), width, height2);
		advOptionsPanel.add(comboInduction);

		// -------------- combo box for verbosity options ---------------
		comboVerbosity = new JComboBox(comboVerbosityStrings);
		comboVerbosity.setSelectedIndex(CetusGUITools.findStringInStrings(allOptionsStrings[optionVerbosityIndex], 
				optionVerbosityStrings));
		comboVerbosity.setToolTipText(comboVerbosityTip);
		comboVerbosity.addActionListener(this);
		comboVerbosity.setBounds(column1, (++layoutAdvY1)*(yGap+height2), width, height2);
		advOptionsPanel.add(comboVerbosity);
		
		// -------------- combo box for Data Dependence Testing options --------------------------
		comboDataDepend = new JComboBox(comboDataDependStrings);
		comboDataDepend.setSelectedIndex(CetusGUITools.findStringInStrings(allOptionsStrings[optionDataDependIndex], 
				optionDataDependStrings));
		comboDataDepend.setToolTipText(comboDataDependTip);
		comboDataDepend.addActionListener(this);
		comboDataDepend.setBounds(column1, (++layoutAdvY1)*(yGap+height2), width, height2);
		advOptionsPanel.add(comboDataDepend);

		// -------------- combo box for Range options ---------------
		comboRange = new JComboBox(comboRangeStrings);
		comboRange.setSelectedIndex(CetusGUITools.findStringInStrings(allOptionsStrings[optionRangeIndex], 
				optionRangeStrings));
		comboRange.setToolTipText(comboRangeTip);
		comboRange.addActionListener(this);
		comboRange.setBounds(column1, (++layoutAdvY1)*(yGap+height2), width, height2);
		advOptionsPanel.add(comboRange);
		
		// -------------- combo box for profile-loops options ---------------
		comboProfileLoops = new JComboBox(comboProfileLoopsStrings);
		comboProfileLoops.setSelectedIndex(CetusGUITools.findStringInStrings(allOptionsStrings[optionProfileLoopsIndex], 
				optionProfileLoopsStrings));
		comboProfileLoops.setToolTipText(comboProfileLoopsTip);
		comboProfileLoops.addActionListener(this);
		comboProfileLoops.setBounds(column1, (++layoutAdvY1)*(yGap+height2), width, height2);
		advOptionsPanel.add(comboProfileLoops);
		
		// -------------- combo box for -ompGen options ---------------
		comboOmpGen = new JComboBox(comboOmpGenStrings);
		comboOmpGen.setSelectedIndex(CetusGUITools.findStringInStrings(allOptionsStrings[optionOmpGenIndex], 
				optionOmpGenStrings));
		comboOmpGen.setToolTipText(comboOmpGenTip);
		comboOmpGen.addActionListener(this);
		comboOmpGen.setBounds(column1, (++layoutAdvY1)*(yGap+height2), width, height2);
		advOptionsPanel.add(comboOmpGen);
		
		// ---------------- notes for default options -----------------
		labelNotesDefaultOptions = new JLabel(labelNotesDefaultOptionsText);
		labelNotesDefaultOptions.setToolTipText(labelNotesDefaultOptionsTip);
		labelNotesDefaultOptions.setBounds(column1, (++layoutAdvY1)*(yGap+height2), width, height2);
		advOptionsPanel.add(labelNotesDefaultOptions);
		
		
		// -------------- options on the second column ----------------------
		
		// -------------- check box for preserve-KR-function --------------------
		checkPreserveKR = new JCheckBox(checkPreserveKRText, 
				allOptionsStrings[optionPreserveKRIndex].equals(optionPreserveKRString));
		checkPreserveKR.setToolTipText(checkPreserveKRTip);
		checkPreserveKR.addActionListener(this);
		checkPreserveKR.setBounds(column2, (++layoutAdvY2)*(yGap+height2), width, height2);
		advOptionsPanel.add(checkPreserveKR);
		
//		// -------------- check box for -version --------------------
//		checkVersion = new JCheckBox(checkVersionText, 
//				allOptionsStrings[optionVersionIndex].equals(optionVersionString));
//		checkVersion.setToolTipText(checkVersionTip);
//		checkVersion.addActionListener(this);
//		checkVersion.setBounds(column2, (++layoutAdvY2)*(yGap+height2), width, height2);
//		advOptionsPanel.add(checkVersion);
		
		// -------------- check box for -callgraph --------------------
		checkCallgraph = new JCheckBox(checkCallgraphText, 
				allOptionsStrings[optionCallgraphIndex].equals(optionCallgraphString));
		checkCallgraph.setToolTipText(checkCallgraphTip);
		checkCallgraph.addActionListener(this);
		checkCallgraph.setBounds(column2, (++layoutAdvY2)*(yGap+height2), width, height2);
		advOptionsPanel.add(checkCallgraph);
		
		// -------------- check box for -normalize-loops --------------------
		checkNormalizeLoops = new JCheckBox(checkNormalizeLoopsText, 
				allOptionsStrings[optionNormalizeLoopsIndex].equals(optionNormalizeLoopsString));
		checkNormalizeLoops.setToolTipText(checkNormalizeLoopsTip);
		checkNormalizeLoops.addActionListener(this);
		checkNormalizeLoops.setBounds(column2, (++layoutAdvY2)*(yGap+height2), width, height2);
		advOptionsPanel.add(checkNormalizeLoops);
		
		// -------------- check box for -normalize-return-stmt --------------------
		checkNormalizeReturnStmt = new JCheckBox(checkNormalizeReturnStmtText, 
				allOptionsStrings[optionNormalizeReturnStmtIndex].equals(optionNormalizeReturnStmtString));
		checkNormalizeReturnStmt.setToolTipText(checkNormalizeReturnStmtTip);
		checkNormalizeReturnStmt.addActionListener(this);
		checkNormalizeReturnStmt.setBounds(column2, (++layoutAdvY2)*(yGap+height2), width, height2);
		advOptionsPanel.add(checkNormalizeReturnStmt);
		
		// -------------- check box for -tsingle-call --------------------
		checkTsingleCall = new JCheckBox(checkTsingleCallText, 
				allOptionsStrings[optionTsingleCallIndex].equals(optionTsingleCallString));
		checkTsingleCall.setToolTipText(checkTsingleCallTip);
		checkTsingleCall.addActionListener(this);
		checkTsingleCall.setBounds(column2, (++layoutAdvY2)*(yGap+height2), width, height2);
		advOptionsPanel.add(checkTsingleCall);
		
		// -------------- check box for -tsingle-declarator --------------------
		checkTsingleDeclarator = new JCheckBox(checkTsingleDeclaratorText, 
				allOptionsStrings[optionTsingleDeclaratorIndex].equals(optionTsingleDeclaratorString));
		checkTsingleDeclarator.setToolTipText(checkTsingleDeclaratorTip);
		checkTsingleDeclarator.addActionListener(this);
		checkTsingleDeclarator.setBounds(column2, (++layoutAdvY2)*(yGap+height2), width, height2);
		advOptionsPanel.add(checkTsingleDeclarator);
		
		// -------------- check box for -tsingle-return --------------------
		checkTsingleReturn = new JCheckBox(checkTsingleReturnText, 
				allOptionsStrings[optionTsingleReturnIndex].equals(optionTsingleReturnString));
		checkTsingleReturn.setToolTipText(checkTsingleReturnTip);
		checkTsingleReturn.addActionListener(this);
		checkTsingleReturn.setBounds(column2, (++layoutAdvY2)*(yGap+height2), width, height2);
		advOptionsPanel.add(checkTsingleReturn);
		
		// --------------- preprocessor ------------------- 
		checkPreprocessor = new JCheckBox(checkPreprocessorText, false);
		checkPreprocessor.setToolTipText(checkPreprocessorTip);
		checkPreprocessor.addActionListener(this);
		checkPreprocessor.setBounds(column2, (++layoutAdvY2)*(yGap+height2), (width-2*xGap)/20*7, height2);
		advOptionsPanel.add(checkPreprocessor);
		
//		labelPreprocessor = new JLabel(labelPreprocessorText);
//		labelPreprocessor.setToolTipText(labelPreprocessorTip);
//		//labelPreprocessor.addActionListener(this);
//		labelPreprocessor.setBounds(column2, (++layoutAdvY2)*(yGap+height2), width/7*2, height2);
//		advOptionsPanel.add(labelPreprocessor);

		textPreprocessor = new JTextField(allOptionsStrings[optionPreprocessorIndex]);
		textPreprocessor.setToolTipText(textPreprocessorTip);
		textPreprocessor.setBounds(column2+(width-2*xGap)/20*7+xGap, (layoutAdvY2)*(yGap+height2), (width-2*xGap)/20*8, height2);
		advOptionsPanel.add(textPreprocessor);
		textPreprocessor.setVisible(false);
		
		buttonPreprocessor = new JButton(buttonPreprocessorText);
		buttonPreprocessor.setToolTipText(buttonPreprocessorTip);
		buttonPreprocessor.addActionListener(this);
		buttonPreprocessor.setBounds(column2+(width-2*xGap)/20*15+2*xGap, (layoutAdvY2)*(yGap+height2), (width-2*xGap)/20*5, height2);
		advOptionsPanel.add(buttonPreprocessor);
		buttonPreprocessor.setVisible(false);
		
		// --------------- Reset options ------------------
		buttonResetOptions = new JButton(buttonResetOptionsText);
		buttonResetOptions.setToolTipText(buttonResetOptionsTip);
		buttonResetOptions.addActionListener(this);
		buttonResetOptions.setBounds(column2, (++layoutAdvY2)*(yGap+height2), (width-xGap)/2, height2);
		advOptionsPanel.add(buttonResetOptions);

		// --------------- output directory ------------------
		buttonOutDir = new JButton(buttonOutDirText);
		buttonOutDir.setToolTipText(buttonOutDirTip);
		buttonOutDir.addActionListener(this);
		buttonOutDir.setBounds(column2+(width-xGap)/2+xGap, (layoutAdvY2)*(yGap+height2), (width-xGap)/2, height2);
		advOptionsPanel.add(buttonOutDir);

		// --------------- CheckUpdate ------------------
		buttonCheckUpdate = new JButton(buttonCheckUpdateText);
		buttonCheckUpdate.setToolTipText(buttonCheckUpdateTip);
		buttonCheckUpdate.addActionListener(this);
		buttonCheckUpdate.setBounds(column2, (++layoutAdvY2)*(yGap+height2), (width-xGap)/2, height2);
		advOptionsPanel.add(buttonCheckUpdate);
		
		// --------------- PrintOptions ------------------
		buttonPrintOptions = new JButton(buttonPrintOptionsText);
		buttonPrintOptions.setToolTipText(buttonPrintOptionsTip);
		buttonPrintOptions.addActionListener(this);
		buttonPrintOptions.setBounds(column2+(width-xGap)/2+xGap, (layoutAdvY2)*(yGap+height2), (width-xGap)/2, height2);
		advOptionsPanel.add(buttonPrintOptions);
		
		
		// --------------- C Compiler ------------------- 
		labelCompiler.setBounds(column2, (++layoutAdvY2)*(yGap+height2), (width-2*xGap)/20*5, height2);
		labelCompiler.setToolTipText("C compiler for speedup calcuation and graph");
		advOptionsPanel.add(labelCompiler);

		textCompiler = new JTextField(compilerString);
		textCompiler.setToolTipText(textCompilerTip);
		textCompiler.setBounds(column2+(width-4*xGap)/20*5+xGap, (layoutAdvY2)*(yGap+height2), (width-4*xGap)/20*3, height2);
		//textCompiler.addActionListener(this);
		advOptionsPanel.add(textCompiler);

		labelOpenmp.setBounds(column2+(width-4*xGap)/20*8+2*xGap, (layoutAdvY2)*(yGap+height2), (width-4*xGap)/20*4, height2);
		labelOpenmp.setToolTipText("OpenMP flag for C compiler including '-'");
		advOptionsPanel.add(labelOpenmp);

		textOpenmp = new JTextField(openmpFlagString);
		textOpenmp.setToolTipText(textOpenmpTip);
		textOpenmp.setBounds(column2+(width-4*xGap)/20*12+3*xGap, (layoutAdvY2)*(yGap+height2), (width-4*xGap)/20*4, height2);
		//textOpenmp.addActionListener(this);
		advOptionsPanel.add(textOpenmp);

		buttonSaveCompiler = new JButton(buttonCompilerText);
		buttonSaveCompiler.setToolTipText(buttonSaveCompilerTip);
		buttonSaveCompiler.addActionListener(this);
		buttonSaveCompiler.setBounds(column2+(width-4*xGap)/20*16+4*xGap, (layoutAdvY2)*(yGap+height2), (width-4*xGap)/20*5, height2);
		advOptionsPanel.add(buttonSaveCompiler);
		

		
		
		
		
		
		// -------------- check box for AutoLoadOptions --------------------
		checkAutoLoadOptions = new JCheckBox(checkAutoLoadOptionsText, 
				!allOptionsStrings[optionAutoLoadOptionsIndex].equals(optionAutoLoadOptions0String));
		checkAutoLoadOptions.setToolTipText(checkAutoLoadOptionsTip);
		checkAutoLoadOptions.addActionListener(this);
		checkAutoLoadOptions.setBounds(column2, (++layoutAdvY2)*(yGap+height2), width, height2);
		advOptionsPanel.add(checkAutoLoadOptions);

		// --------------- SaveOptions ------------------
		buttonSaveOptions = new JButton(buttonSaveOptionsText);
		buttonSaveOptions.setToolTipText(buttonSaveOptionsTip);
		buttonSaveOptions.addActionListener(this);
		buttonSaveOptions.setBounds(column2, (++layoutAdvY2)*(yGap+height2), (width-xGap)/2, height2);
		advOptionsPanel.add(buttonSaveOptions);
		
		// --------------- LoadOptions ------------------
		buttonLoadOptions = new JButton(buttonLoadOptionsText);
		buttonLoadOptions.setToolTipText(buttonLoadOptionsTip);
		buttonLoadOptions.addActionListener(this);
		buttonLoadOptions.setBounds(column2+(width-xGap)/2+xGap, (layoutAdvY2)*(yGap+height2), (width-xGap)/2, height2);
		advOptionsPanel.add(buttonLoadOptions);
		
		// --------------- SaveOptionsAs ------------------
		buttonSaveOptionsAs = new JButton(buttonSaveOptionsAsText);
		buttonSaveOptionsAs.setToolTipText(buttonSaveOptionsAsTip);
		buttonSaveOptionsAs.addActionListener(this);
		buttonSaveOptionsAs.setBounds(column2, (++layoutAdvY2)*(yGap+height2), (width-xGap)/2, height2);
		advOptionsPanel.add(buttonSaveOptionsAs);
		
		// --------------- LoadOptionsFrom ------------------
		buttonLoadOptionsFrom = new JButton(buttonLoadOptionsFromText);
		buttonLoadOptionsFrom.setToolTipText(buttonLoadOptionsFromTip);
		buttonLoadOptionsFrom.addActionListener(this);
		buttonLoadOptionsFrom.setBounds(column2+(width-xGap)/2+xGap, (layoutAdvY2)*(yGap+height2), (width-xGap)/2, height2);
		advOptionsPanel.add(buttonLoadOptionsFrom);
		

		
		
		
		
		
		// ---------------- notes for options to help -----------------
		labelNotesOptionToHelp = new JLabel(labelNotesOptionToHelpText);
		labelNotesOptionToHelp.setToolTipText(labelNotesOptionToHelpTip);
		labelNotesOptionToHelp.setBounds(column2, (++layoutAdvY2)*(yGap+height2), width, height2);
		advOptionsPanel.add(labelNotesOptionToHelp);
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
//		radioAlias0 = new JRadioButton(radioAlias0String,allOptionsStrings[comboAliasIndex].equals("-alias=0"));
//		radioAlias0.addActionListener(this);
//		radioAlias2 = new JRadioButton(radioAlias2String,allOptionsStrings[comboAliasIndex].equals("-alias=2"));
//		radioAlias2.addActionListener(this);
//		groupRadioAlias = new ButtonGroup();
//		groupRadioAlias.add(radioAlias0);
//		groupRadioAlias.add(radioAlias2);
//		radioAlias0.setToolTipText(radioAliasTip);
//		radioAlias2.setToolTipText(radioAliasTip);
//		radioAlias0.setBounds(column1, yGap+height, width, height);
//		radioAlias2.setBounds(column1, 2*yGap+2*height, width, height);
//		add(radioAlias0);
//		add(radioAlias2);
//        
// 		checkDebugParserInput = new JCheckBox("debug_parser_input", allOptionsStrings[checkDebugParserInputIndex].equals("-debug_parser_input"));
//		checkDebugParserInput.setToolTipText("Print a single preprocessed input file before sending to parser and exit");
//		checkDebugParserInput.addActionListener(this);
//		//add(checkDebugParserInput);
		
	}
	
    public void actionPerformed(ActionEvent evt) {
        Object source = evt.getSource();
        k = 0;
        int comboIndex = 0;
        
//        switch ((String) source) {
//        
//        }
        
        if (source.equals(emptyString)) {
			
		} else if (source.equals(checkAdvOptionsPanel)) {
			//k = comboParallelizeLoopsIndex;
			advOptionsPanel.setVisible(checkAdvOptionsPanel.isSelected());
			System.out.println("Turned "+(checkAdvOptionsPanel.isSelected()?"ON":"OFF")+" advanced options");
			return;
		} else if (source.equals(radioInlineOff)) {
			k = optionInlineIndex;
			comboInline.setSelectedIndex(optionInlineOffIndex);
		} else if (source.equals(radioInlineMain)) {
			k = optionInlineIndex;
			comboInline.setSelectedIndex(optionInlineMainIndex);
		} else if (source.equals(radioInlineForLoopsOnly)) {
			k = optionInlineIndex;
			comboInline.setSelectedIndex(optionInlineForLoopsOnlyIndex);
		} else if (source.equals(comboInline)) {
			k = optionInlineIndex;
			allOptionsStrings[k] = optionInlineStrings[comboInline.getSelectedIndex()];
			if (allOptionsStrings[k].equals(optionOffString)) {
				radioInlineOff.setSelected(true);
			} else if (allOptionsStrings[k].equals(optionInlineMainString)) {
				radioInlineMain.setSelected(true);
			} else if (allOptionsStrings[k].equals(optionInlineForLoopsOnlyString)) {
				radioInlineForLoopsOnly.setSelected(true);
			} else {
				groupRadioInline.clearSelection();
			}
		} else if (source.equals(checkParallelizeLoops2)) {
			k = optionParallelizeLoopsIndex;
			comboParallelizeLoops.setSelectedIndex(checkParallelizeLoops2.isSelected()?
					optionParallelizeLoops2Index:optionParallelizeLoops1Index);
		} else if (source.equals(comboParallelizeLoops)) {
			k = optionParallelizeLoopsIndex;
			allOptionsStrings[k] = optionParallelizeLoopsStrings[comboParallelizeLoops.getSelectedIndex()];
			checkParallelizeLoops2.setSelected(allOptionsStrings[k].equals(optionParallelizeLoops2String));
		} else if (source.equals(checkProfitableOmp0)) {
			k = optionProfitableOmpIndex;
			comboProfitableOmp.setSelectedIndex(checkProfitableOmp0.isSelected()?
					optionProfitableOmp0Index:optionProfitableOmp1Index);
		} else if (source.equals(comboProfitableOmp)) {
			k = optionProfitableOmpIndex;
			allOptionsStrings[k] = optionProfitableOmpStrings[comboProfitableOmp.getSelectedIndex()];
			checkProfitableOmp0.setSelected(allOptionsStrings[k].equals(optionProfitableOmp0String));
		} else if (source.equals(comboReduction)) {
			k = optionReductionIndex;
			allOptionsStrings[k] = optionReductionStrings[comboReduction.getSelectedIndex()];
		} else if (source.equals(comboTeliminateBranch)) {
			k = optionTeliminateBranchIndex;
			allOptionsStrings[k] = optionTeliminateBranchStrings[comboTeliminateBranch.getSelectedIndex()];
		
//		} else if (source.equals(radioAlias0)) {
//			k = optionAliasIndex;
//			comboAlias.setSelectedIndex(radioAlias0.isSelected()?0:2);
//		} else if (source.equals(radioAlias2)) {
//			k = optionAliasIndex;
//			comboAlias.setSelectedIndex(radioAlias2.isSelected()?2:0);
		} else if (source.equals(comboAlias)) {
			k = optionAliasIndex;
			allOptionsStrings[k] = optionAliasStrings[comboAlias.getSelectedIndex()];
//			if (comboAlias.getSelectedIndex()==0) {
//				radioAlias0.setSelected(true);
//			} else if (comboAlias.getSelectedIndex()==2) {
//				radioAlias2.setSelected(true);
//			} else {
//				groupRadioAlias.clearSelection();
//			}
			
		} else if (source.equals(comboPrivatize)) {
			k = optionPrivatizeIndex;
			allOptionsStrings[k] = optionPrivatizeStrings[comboPrivatize.getSelectedIndex()];
		} else if (source.equals(comboInduction)) {
			k = optionInductionIndex;
			allOptionsStrings[k] = optionInductionStrings[comboInduction.getSelectedIndex()];
		} else if (source.equals(checkPreserveKR)) {
			k = optionPreserveKRIndex;
			allOptionsStrings[k] = checkPreserveKR.isSelected()?optionPreserveKRString:optionOffString;
		} else if (source.equals(comboVerbosity)) {
			k = optionVerbosityIndex;
			allOptionsStrings[k] = optionVerbosityStrings[comboVerbosity.getSelectedIndex()];
		} else if (source.equals(checkVersion)) {
			k = optionVersionIndex;
			allOptionsStrings[k] = checkVersion.isSelected()?optionVersionString:optionOffString;
		} else if (source.equals(checkCallgraph)) {
			k = optionCallgraphIndex;
			allOptionsStrings[k] = checkCallgraph.isSelected()?optionCallgraphString:optionOffString;
		} else if (source.equals(comboDataDepend)) {
			k = optionDataDependIndex;
			allOptionsStrings[k] = optionDataDependStrings[comboDataDepend.getSelectedIndex()];
		} else if (source.equals(comboRange)) {
			k = optionRangeIndex;
			allOptionsStrings[k] = optionRangeStrings[comboRange.getSelectedIndex()];
		} else if (source.equals(checkNormalizeLoops)) {
			k = optionNormalizeLoopsIndex;
			allOptionsStrings[k] = checkNormalizeLoops.isSelected()?optionNormalizeLoopsString:optionOffString;
		} else if (source.equals(checkNormalizeReturnStmt)) {
			k = optionNormalizeReturnStmtIndex;
			allOptionsStrings[k] = checkNormalizeReturnStmt.isSelected()?optionNormalizeReturnStmtString:optionOffString;
		} else if (source.equals(comboProfileLoops)) {
			k = optionProfileLoopsIndex;
			allOptionsStrings[k] = optionProfileLoopsStrings[comboProfileLoops.getSelectedIndex()];
		} else if (source.equals(checkTsingleCall)) {
			k = optionTsingleCallIndex;
			allOptionsStrings[k] = checkTsingleCall.isSelected()?optionTsingleCallString:optionOffString;
		} else if (source.equals(checkTsingleDeclarator)) {
			k = optionTsingleDeclaratorIndex;
			allOptionsStrings[k] = checkTsingleDeclarator.isSelected()?optionTsingleDeclaratorString:optionOffString;
		} else if (source.equals(checkTsingleReturn)) {
			k = optionTsingleReturnIndex;
			allOptionsStrings[k] = checkTsingleReturn.isSelected()?optionTsingleReturnString:optionOffString;
		} else if (source.equals(comboOmpGen)) {
			k = optionOmpGenIndex;
			allOptionsStrings[k] = optionOmpGenStrings[comboOmpGen.getSelectedIndex()];
		} else if (source.equals(checkPreprocessor)) {
			//k = comboParallelizeLoopsIndex;
			textPreprocessor.setVisible(checkPreprocessor.isSelected());
			buttonPreprocessor.setVisible(checkPreprocessor.isSelected());
			System.out.println("Turn "+(checkPreprocessor.isSelected()?"ON":"OFF")+" Preprocessor Option");
			return;
		} else if (source.equals(buttonPreprocessor)) {
			k = optionPreprocessorIndex;
			allOptionsStrings[k] = textPreprocessor.getText().equals(emptyString)?
					optionOffString:textPreprocessor.getText();
			setPreprocessorTip();
		} else if (source.equals(buttonResetOptions)) {
			System.out.println(printClass+"Reset Cetus options to default");
			initializeDefaultCetusOptionsArray();
			setCetusOptions();
			return;
		} else if (source.equals(buttonOutDir)) {
			k = optionOutDirIndex;
			fileOutDir = CetusGUITools.getDir(this.getParent(), "C files (*.c)", "C");
			while (fileOutDir!=null && CetusGUI.inputFile !=null 
					&& fileOutDir.toString().equalsIgnoreCase(CetusGUI.inputFile.getParent())){
				String sameInOutDirMessage1CurrentDir = printClass
						+ CetusGUI.sameInOutDirMessage1
						+ "\nInput File Dir: " + CetusGUI.inputFile.getParent()
						+ "\nOutput File Dir: " + fileOutDir.toString();
				System.out.println(sameInOutDirMessage1CurrentDir);
				JOptionPane.showMessageDialog(this.getParent()
						, sameInOutDirMessage1CurrentDir);				
				fileOutDir = CetusGUITools.getDir(this.getParent(), "C files (*.c)", "C");
			}
			if (fileOutDir!=null) {
				CetusGUI.outputFilePath = fileOutDir.toString();
				allOptionsStrings[k]= optionOutDirStringPrefix+CetusGUI.outputFilePath;
				setOutDirButtonTip();
			}
			else {
				System.out.println("The output directory was NOT changed");
			}
			//return;
		} else if (source.equals(buttonCheckUpdate)) {
			k = optionCheckUpdateIndex;
			Driver.checkUpdate();
			if (Driver.updateNeeded) {
				String[] tempOptionStrings = loadOptionsFromFile(Driver.preferencesFile);
				String updateString = processUpdateDialog(tempOptionStrings[k]);
				allOptionsStrings[k] = updateString;
				tempOptionStrings[k] = updateString;
				saveOptionsToFile(Driver.preferencesFile, tempOptionStrings);
			} else {
				//System.out.println(Driver.versionInfoString);
				JOptionPane.showMessageDialog(this.getParent(), Driver.versionInfoString);
			}
		} else if (source.equals(buttonPrintOptions)) {
			String[] compressedOptions = CetusGUITools.compressStrings (allOptionsStrings, optionOffString);
			System.out.println("*** All options in memory now: "+Arrays.toString(compressedOptions));
			return;
		} else if (source.equals(checkAutoLoadOptions)) {
			k = optionAutoLoadOptionsIndex;
			allOptionsStrings[k] = checkAutoLoadOptions.isSelected()?optionAutoLoadOptions1String:optionAutoLoadOptions0String;
			String[] tempOptionStrings = loadOptionsFromFile(Driver.preferencesFile);
			tempOptionStrings[k]=allOptionsStrings[k];
			saveOptionsToFile(Driver.preferencesFile, tempOptionStrings);
			System.out.println("Auto-load option "+tempOptionStrings[k]
					+" was saved into default option file: "+Driver.preferencesFile.toString());
			//JOptionPane.showMessageDialog(this.getParent(), "Please click ["+buttonSaveOptionsText+"] to make option ["+checkAutoLoadOptionsText+"] to take effect");							
			//buttonSaveOptions.doClick();
		} else if (source.equals(buttonSaveOptions)) {
			saveOptionsToFile(Driver.preferencesFile, allOptionsStrings);
			return;
		} else if (source.equals(buttonLoadOptions)) {
			loadSetOptions(Driver.preferencesFile);
			return;
		} else if (source.equals(buttonSaveOptionsAs)) {
			//System.out.println(this.getParent());
			File userOptionFile = CetusGUITools.saveFile(this.getParent(), null, null, Driver.preferencesFile.getName());
			if (userOptionFile!=null) {
				System.out.println("Saving options to file: "+userOptionFile.toString());
				saveOptionsToFile(userOptionFile, allOptionsStrings);
			}
			return;
		} else if (source.equals(buttonLoadOptionsFrom)) {
			File userOptionFile = CetusGUITools.getFile(this.getParent(), null, null);
			if (userOptionFile!=null) {
				loadSetOptions(userOptionFile);
			}
			return;
		} else if (source.equals(checkServer)) {
			k = optionServerIndex;
			allOptionsStrings[k] = checkServer.isSelected()?
					(optionServerStringPrefixNoEqual+"=1"):(optionServerStringPrefixNoEqual+"=0");
		} else if (source.equals(buttonSaveCompiler)) {
			allOptionsStrings[optionCompilerIndex] = optionCompilerStringPrefix+textCompiler.getText();
			allOptionsStrings[optionOpenmpIndex] = optionOpenmpStringPrefix+textOpenmp.getText();
			System.out.println("Set C compiler to: " + textCompiler.getText());
			System.out.println("Set OpenMP flag for C compiler to: " + textOpenmp.getText());

			String[] tempOptionStrings = loadOptionsFromFile(Driver.preferencesFile);
			tempOptionStrings[optionCompilerIndex] = optionCompilerStringPrefix+textCompiler.getText();
			tempOptionStrings[optionOpenmpIndex] = optionOpenmpStringPrefix+textOpenmp.getText();
			saveOptionsToFile(Driver.preferencesFile, tempOptionStrings);
			return;
			
			
			

			
			
			
			
			

			
			
			
			
		} else if (source.equals(checkDebugParserInput)) {
			k = optionDebugParserInputIndex;
			allOptionsStrings[k] = checkDebugParserInput.isSelected()?"-debug_parser_input":optionOffString;
		}
		System.out.println("-------- Set option "+allOptionsNameStrings[k]+" to "
			+(allOptionsStrings[k].equals(optionOffString)?"OFF":allOptionsStrings[k])); // "+ source.toString() + "
    }
    
    /**set options according to options strings*/
    public void setCetusOptions() {
    	System.out.println(printClass+"Set Cetus options according to option array of strings...");
    	
		try {
    	
	    	radioInlineOff.setSelected(allOptionsStrings[optionInlineIndex].equals(optionOffString));
	    	radioInlineMain.setSelected(allOptionsStrings[optionInlineIndex].equals(optionInlineMainString));
	    	radioInlineForLoopsOnly.setSelected(allOptionsStrings[optionInlineIndex].equals(optionInlineForLoopsOnlyString));
	    	checkParallelizeLoops2.setSelected(allOptionsStrings[optionParallelizeLoopsIndex].equals(optionParallelizeLoops2String));
	    	checkProfitableOmp0.setSelected(allOptionsStrings[optionProfitableOmpIndex].equals(optionProfitableOmp0String));
			comboParallelizeLoops.setSelectedIndex(CetusGUITools.findStringInStrings(allOptionsStrings[optionParallelizeLoopsIndex], 
					optionParallelizeLoopsStrings));
			comboProfitableOmp.setSelectedIndex(CetusGUITools.findStringInStrings(allOptionsStrings[optionProfitableOmpIndex], 
					optionProfitableOmpStrings));
			comboReduction.setSelectedIndex(CetusGUITools.findStringInStrings(allOptionsStrings[optionReductionIndex], 
					optionReductionStrings));
			comboTeliminateBranch.setSelectedIndex(CetusGUITools.findStringInStrings(allOptionsStrings[optionTeliminateBranchIndex], 
					optionTeliminateBranchStrings));
			comboAlias.setSelectedIndex(CetusGUITools.findStringInStrings(allOptionsStrings[optionAliasIndex], 
					optionAliasStrings));
			comboInline.setSelectedIndex(CetusGUITools.findStringInStrings(allOptionsStrings[optionInlineIndex], 
					optionInlineStrings));
			comboPrivatize.setSelectedIndex(CetusGUITools.findStringInStrings(allOptionsStrings[optionPrivatizeIndex], 
					optionPrivatizeStrings));
			comboInduction.setSelectedIndex(CetusGUITools.findStringInStrings(allOptionsStrings[optionInductionIndex], 
					optionInductionStrings));
			comboVerbosity.setSelectedIndex(CetusGUITools.findStringInStrings(allOptionsStrings[optionVerbosityIndex], 
					optionVerbosityStrings));
			comboDataDepend.setSelectedIndex(CetusGUITools.findStringInStrings(allOptionsStrings[optionDataDependIndex], 
					optionDataDependStrings));
			comboRange.setSelectedIndex(CetusGUITools.findStringInStrings(allOptionsStrings[optionRangeIndex], 
					optionRangeStrings));
			comboProfileLoops.setSelectedIndex(CetusGUITools.findStringInStrings(allOptionsStrings[optionProfileLoopsIndex], 
					optionProfileLoopsStrings));
			comboOmpGen.setSelectedIndex(CetusGUITools.findStringInStrings(allOptionsStrings[optionOmpGenIndex], 
					optionOmpGenStrings));
			checkPreserveKR.setSelected(allOptionsStrings[optionPreserveKRIndex].equals(optionPreserveKRString));
			checkCallgraph.setSelected(allOptionsStrings[optionCallgraphIndex].equals(optionCallgraphString));
			checkNormalizeLoops.setSelected(allOptionsStrings[optionNormalizeLoopsIndex].equals(optionNormalizeLoopsString));
			checkNormalizeReturnStmt.setSelected(allOptionsStrings[optionNormalizeReturnStmtIndex].equals(optionNormalizeReturnStmtString));
			checkTsingleCall.setSelected(allOptionsStrings[optionTsingleCallIndex].equals(optionTsingleCallString));
			checkTsingleDeclarator.setSelected(allOptionsStrings[optionTsingleDeclaratorIndex].equals(optionTsingleDeclaratorString));
			checkTsingleReturn.setSelected(allOptionsStrings[optionTsingleReturnIndex].equals(optionTsingleReturnString));
			
			textPreprocessor.setText(allOptionsStrings[optionPreprocessorIndex]);
			setPreprocessorTip();
			setOutDirFromOptions();
			
		} catch (Throwable t) {
			String msg = "Option file "+Driver.preferencesFile+" may have been corrupted. " +
					"\nCetus options are reset to default. Please save the option file again";
			JOptionPane.showMessageDialog(this.getParent(), msg);
			t.printStackTrace();
			// if option value is out of bounds, reset to default options
			
			System.out.println(msg);
			System.out.println("Initialize default Cetus options...");
			initializeDefaultCetusOptionsArray();
			setCetusOptions();
			//this.revalidate();
			
		}
		
		//System.out.println(printClass+"Set Cetus options done!");
    }
    
    public void setOutDirFromOptions() {
//    	String tempOptionOutDirString = new String(allOptionsStrings[optionOutDirIndex]);
//    	CetusGUI.outputFilePath=tempOptionOutDirString.replaceFirst(optionOutDirStringPrefix, "");
    	CetusGUI.outputFilePath=allOptionsStrings[optionOutDirIndex].split("=")[1];
    	setOutDirButtonTip();
    }

    public void setGuiOptions() {
    	System.out.println(printClass+"Set Cetus GUI options...");
    	
		try {
			checkAutoLoadOptions.setSelected(!allOptionsStrings[optionAutoLoadOptionsIndex].equals(optionAutoLoadOptions0String));
			checkServer.setSelected(allOptionsStrings[optionServerIndex].equals(optionServerStringPrefixNoEqual+"=1"));
			textCompiler.setText(allOptionsStrings[optionCompilerIndex].split("=")[1]);
			textOpenmp.setText(allOptionsStrings[optionOpenmpIndex].split("=")[1]);
		} catch (Throwable t) {
			JOptionPane.showMessageDialog(this.getParent(), "Option file "+Driver.preferencesFile+" may have been corrupted. " +
					"\nGUI options are reset to default. Please save the option file again");
			t.printStackTrace();
			System.out.println("Initialize default GUI options...");
			initializeDefaultGuiOptionsArray();
			setGuiOptions();
		}
    }
    
	public String[] getOptionsArray () {
		numUsedCetusOptions = 0;
		for (i=0;i<numCetusOptions;i++) {
			if (!allOptionsStrings[i].equals(optionOffString)) {
				numUsedCetusOptions++;
			}
		}
		
		System.out.println("Number of options used for Cetus = "+numUsedCetusOptions);
		
		usedCetusOptionsStrings = new String[numUsedCetusOptions+1]; //one more option:
		//for input file; it will be set in CetusGUI.java
		j = 0;
		for (i=0;i<numCetusOptions;i++) {
			if (!allOptionsStrings[i].equals(optionOffString)) {
				usedCetusOptionsStrings[j]=allOptionsStrings[i];
				j++;
			}
		}
		
		return usedCetusOptionsStrings;
	}
	
	public String[] getAllOptionsStrings() {
		return allOptionsStrings;
	}
	
	public int getNumAllOptions () {
		return numAllOptions;
	}
	
	public File getOutDir(){
		return fileOutDir;
	}

	/** if AutoLoadOption is checked, automatically load saved options when starting Cetus GUI*/
	public void autoCheckUpdateSaveLoadOptions () {

		//cetusStartCheckUpdate(allOptionsStrings);
		//JOptionPane.showMessageDialog(this.getParent(), "Hello, you selected: "+showUpdateDialog());
		
		if (Driver.preferencesFile.exists()) {
			// ---------- change update option in option file -----------------
			String[] tempOptionStrings = loadOptionsFromFile(Driver.preferencesFile);
//			tempOptionStrings[optionCheckUpdateIndex] = cetusCheckUpdate (tempOptionStrings[optionCheckUpdateIndex]);
//			saveOptionsToFile(Driver.preferencesFile, tempOptionStrings);
			
			// ---------- load option file from disk if auto-load is checked ---------
			if (!tempOptionStrings[optionAutoLoadOptionsIndex].equals(optionAutoLoadOptions0String)) {
//			if (CetusGUITools.findStringInArray(tempOptionStrings, optionAutoLoadOptions1String)>=0) {
				System.out.println(checkAutoLoadOptionsTip);
				loadSetOptions(Driver.preferencesFile);
			} else { //--------- copy update option and auto load option from temp option Strings to official option Strings
				allOptionsStrings[optionCheckUpdateIndex] = tempOptionStrings[optionCheckUpdateIndex];
				allOptionsStrings[optionAutoLoadOptionsIndex] = tempOptionStrings[optionAutoLoadOptionsIndex];//meaningless
				checkAutoLoadOptions.setSelected(false);
			}
		} else { // ----------- no existing option file, check update and save one first -------------
//			allOptionsStrings[optionCheckUpdateIndex] = cetusCheckUpdate (allOptionsStrings[optionCheckUpdateIndex]);
			saveOptionsToFile(Driver.preferencesFile, allOptionsStrings);
		}

	}
	
	public void checkUpdateSaveOptions () {
		String[] tempOptionStrings = loadOptionsFromFile(Driver.preferencesFile);
		String updateString = cetusCheckUpdate (tempOptionStrings[optionCheckUpdateIndex]);
		allOptionsStrings[optionCheckUpdateIndex] = updateString;
		tempOptionStrings[optionCheckUpdateIndex] = updateString;
		saveOptionsToFile(Driver.preferencesFile, tempOptionStrings);
//		allOptionsStrings[optionCheckUpdateIndex] = cetusCheckUpdate (allOptionsStrings[optionCheckUpdateIndex]);
//		saveOptionsToFile(Driver.preferencesFile, allOptionsStrings);		
	}
	
	public boolean loadSetOptions (File optionFile) {
		
		if (optionFile.exists()) { //CetusGUI.preferencesFile
			allOptionsStrings = loadOptionsFromFile(optionFile);
			setCetusOptions();
			setGuiOptions();
			return true;
		} else {
			JOptionPane.showMessageDialog(this.getParent(), "The option file "+optionFile
				+" does not exist. Please click ["+buttonSaveOptionsText+"] to save it first");
			return false;
		}
	}
	
	private void setOutDirButtonTip() {
		System.out.println("The output directory has been set to: "
				+CetusGUI.outputFilePath);
		buttonOutDirTip = "Click to change output directory; current: "+CetusGUI.outputFilePath;
		buttonOutDir.setToolTipText(buttonOutDirTip);
	}

	private void setPreprocessorTip() {
		System.out.println("Set preprocessor to: "+allOptionsStrings[optionPreprocessorIndex]);
		checkPreprocessorTip = "Change preprocessor command and options if needed. \nCurrent: "
				+allOptionsStrings[optionPreprocessorIndex];
		checkPreprocessor.setToolTipText(checkPreprocessorTip);
	}
	
	public int showUpdateDialog() {
		System.out.println(Driver.versionInfoString);
		Object[] options = {"Visit Cetus website",
                "Remind me in "+daysBetweenCheck+" days",
                "Remind me for next update"};
		return JOptionPane.showOptionDialog(this.getParent(),
			    "Your current version is: "+Driver.currentVersion+". A new version "+Driver.onlineVersion+" is available. Update?",
			    "Cetus Update",
			    JOptionPane.YES_NO_CANCEL_OPTION,
			    JOptionPane.QUESTION_MESSAGE,
			    null,
			    options,
			    options[0]);
	}
	
	public String processUpdateDialog (String updateString) {
		int userSelectN = showUpdateDialog();
		//String updateString = allOptionsStrings[optionCheckUpdateIndex];
		if (userSelectN == 0) {
			CetusGUITools.visitURL(Driver.cetusURL);
		} else if (userSelectN == 1) {
			int today = (int) ((new Date()).getTime()/msPerDay);
			System.out.println("*** Today is day "+today+", remind me in "+daysBetweenCheck+" days");
			updateString=optionCheckUpdateStringPrefix+(today+daysBetweenCheck);
		} else if (userSelectN == 2) {
			System.out.println("*** Do not remind me of version "+Driver.onlineVersion);
			updateString=optionCheckUpdateStringPrefix+Driver.onlineVersion;
		}
		return updateString;
		//		allOptionsStrings[optionCheckUpdateIndex]=optionCheckUpdateString+"1";

	}
	
	public String cetusCheckUpdate (String updateString) {
		if (Driver.updateNeeded) {
//			System.out.println("*** A new version "+Driver.onlineVersion
//					+" is available. Your current version is: "+Driver.currentVersion+".");
			String updateValue = new String(updateString);//CetusGUITools.getOptionValue(optionCheckUpdateString, options);
			updateValue = updateValue.replaceFirst(optionCheckUpdateStringPrefix, emptyString);
			//System.out.println("************************* "+updateValue);
			if (updateValue.matches(CetusGUITools.regexVersionNumber)||updateValue.equals(Driver.brokenLink)) {
				//System.out.println("************************* This is a version number");
				//updateValue = "1.3.1"; //for test
				if (!updateValue.equals(Driver.onlineVersion)) {
					System.out.println("*** A new version is available: "+Driver.onlineVersion
							+". Your current version is: "+Driver.currentVersion
							+". You have missed more than one version");
					updateString = processUpdateDialog(updateString);
				} else System.out.println("*** You skipped version: "+Driver.onlineVersion);
			} else if (updateValue.matches(CetusGUITools.regexPositiveInteger5OrMoreDigits)) {
				//updateValue = "15660"; //for test
				//System.out.println("************************* This is a date number");
				int today = (int) ((new Date()).getTime()/msPerDay);
				int plannedCheckDay = Integer.parseInt(updateValue);
				if (today >= plannedCheckDay) {
					System.out.println("*** A new version is available: "+Driver.onlineVersion
							+". Your current version is: "+Driver.currentVersion
							+". It has been "+daysBetweenCheck+" days or more since last update check.");
					updateString = processUpdateDialog(updateString);
				} else System.out.println("*** Cetus will automatically check update again in "+(plannedCheckDay-today)+" days.");
			} else if (!updateValue.equals("0")) {
				//System.out.println("************************* Always check");
				updateString = processUpdateDialog(updateString);
			}
		} //else System.out.println("*** Your current version "+Driver.currentVersion+" is the latest one.");

		return updateString;
	}
	
	private String[] loadOptionsFromFile (File file) {
		String[] tempOptionStrings = CetusGUITools.readFileToArrayStrings(file);
		System.out.println(buttonLoadOptionsTip+". The number of options read: "+tempOptionStrings.length);
		return CetusGUITools.matchCopyStrings (allOptionsNameStrings, tempOptionStrings, optionOffString);
	}
	
	private int saveOptionsToFile (File file, String[] options) {
		String[] compressedOptions = CetusGUITools.compressStrings (options, optionOffString);
		CetusGUITools.writeArrayStringsToFile(file, compressedOptions);
		System.out.println(buttonSaveOptionsTip+". The number of options written: "+compressedOptions.length);
		return compressedOptions.length;
	}

	public static boolean useServer() {
		return checkServer.isSelected();
	}
	
	public static String getCompiler() {
		return textCompiler.getText();
	}

	public static String getOpenmpFlag() {
		return textOpenmp.getText();
	}

}
