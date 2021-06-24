package cetus.exec;

import cetus.analysis.*;
import cetus.codegen.CodeGenPass;
import cetus.codegen.ompGen;
import cetus.gui.CetusGUI;
import cetus.gui.CetusGUITools;
import cetus.gui.ThreadUpdate;
import cetus.hir.PrintTools;
import cetus.hir.Program;
import cetus.hir.SymbolTools;
import cetus.hir.Tools;
import cetus.transforms.*;

import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

import javax.swing.SwingUtilities;

/**
* Implements the command line parser and controls pass ordering.
* Users may extend this class by overriding runPasses
* (which provides a default sequence of passes).  The derived
* class should pass an instance of itself to the run method.
* Derived classes have access to a protected {@link Program Program} object.
*/
public class Driver {

    /**
    * A mapping from option names to option values.
    */
    protected static CommandLineOptionSet options = new CommandLineOptionSet();

    /**
    * Override runPasses to do something with this object.
    * It will contain a valid program when runPasses is called.
    */
    protected Program program;

    /** The filenames supplied on the command line. */
    protected List<String> filenames;

    /** Cetus default option file*/
	public static String preferencesFileName = ".cetus.opt";
	public static String preferencesDirFileName = CetusGUITools.user_home + CetusGUITools.file_sep + preferencesFileName;
	public static File preferencesFile = new File(preferencesDirFileName);

//    /** build number */
//    public static int currentBuild = 120605;
    
    /** version number */
    public static String currentVersion = "1.4.4";
    
    /** URL to build number */
    public static String cetusURL = "https://engineering.purdue.edu/Cetus/";
    public static String cetusInfoURL = "https://engineering.purdue.edu/Cetus/cetusinfo.txt";
    public static String versionInfoString = "";
    public static boolean updateNeeded = false;
    public static final String brokenLink = null;
    public static String onlineVersion = brokenLink;
//    public static String onlineBuild = "0";
//    public static int onlineBuildValue = 0;
    
    /** Compiler version information, GUI added since 1.4.0 */
    public static String version =
            "Cetus "+currentVersion+" - A Source-to-Source Compiler with GUI for C\n" +
            "https://engineering.purdue.edu/Cetus/\n" +
            "Copyright (C) 2002-2016 ParaMount Research Group\n" +
            "Purdue University - School of Electrical & Computer Engineering";
	public static ThreadUpdate t2;

    public static ArrayList<String> autoTurnedOnOptions = new ArrayList<String>();
    
    
    /**
    * Constructor used by derived classes.
    */
    protected Driver() {
        registerOptions();
    }
    
    /**
     * check if there is a new Cetus
    */
    public static boolean checkUpdate() {
    	
    	String[] cetusInfoStrings = CetusGUITools.readURLToArrayStrings(cetusInfoURL);
    	if (cetusInfoStrings != null) onlineVersion = CetusGUITools.getNoPrefixInStrings("-latestversion=", cetusInfoStrings);
    	else System.err.println("Error: reading Cetus info URL failed: "+cetusInfoURL);
    	
		if (onlineVersion == brokenLink) {
			versionInfoString = "Error: reading Cetus latest version number failed. URL: "+cetusInfoURL
					+". Latest version number is unknown. Please visit: "+cetusURL+" directly for more information";
			updateNeeded = false;
		} else if (currentVersion.equals(onlineVersion)) {
			versionInfoString = "You have the latest Cetus of version "+currentVersion+".";
			updateNeeded = false;
		} else {
			versionInfoString = "A newer version of Cetus "+onlineVersion+" is available. Your current version is "
					+currentVersion+". Please update Cetus through: "+cetusURL;
			updateNeeded = true;
		}
		System.out.println(versionInfoString);
    	return updateNeeded;
    }
    

    /**
    * Register default legal set of options and default values for Driver.
    * Only registers options can have values set.
    */
    public static void registerOptions() {
    	autoTurnedOnOptions = new ArrayList<String>();
        options.add(options.ANALYSIS, // TODO: obsolete?
            "callgraph",
            "Print the static call graph to stdout");
        options.add(options.UTILITY,
            "parser",
            "cetus.base.grammars.CetusCParser",
            "parsername",
            "Name of parser to be used for parsing source file");
        options.add(options.UTILITY, // TODO: somebody needs to work on this.
            "dump-options",
            "Create file options.cetus with default options");
        options.add(options.UTILITY,
            "dump-system-options",
            "Create system wide file options.cetus with default options");
        options.add(options.UTILITY,
            "load-options",
            "Load options from file options.cetus");
        options.add(options.UTILITY,
            "expand-user-header",
            "Expand user (non-standard) header file #includes into code");
        options.add(options.UTILITY,
            "expand-all-header",
            "Expand all header file #includes into code");
        options.add(options.UTILITY,
            "help",
            "Print this message");
        options.add(options.TRANSFORM, 
            "induction", //not always on, but if on without value, "3" assigned automatically
            "3", //move option dependences analysis up since -parallelize-loops=1 is default now
            "3",
            "N",
            "Perform induction variable substitution (ON=3)\n"
            + "      =0 force to disable\n" //in case -parallelize-loops is on, but user wants to disable this option
            + "      =1 enable substitution of linear induction variables\n"
            + "      =2 enable substitution of generalized induction variables\n"
            + "      =3 enable insertion of runtime test for zero-trip loops");
        options.add(options.UTILITY,
            "outdir",
            "cetus_output",
            "dirname",
            "Set the output directory name (default is cetus_output)");
        options.add(options.TRANSFORM,
            "normalize-loops",
            "Normalize for loops so they begin at 0 and have a step of 1");
        if ((System.getProperty("os.name").toLowerCase()).indexOf("win")>=0)
        	options.add(options.UTILITY,
	            "preprocessor",
	            "cpp-4.exe -E",
	            "command",
	            "Set the preprocessor command to use");
        else
        	options.add(options.UTILITY,
	            "preprocessor",
	            "cpp -C -I.",
	            "command",
	            "Set the preprocessor command to use");
        options.add(options.ANALYSIS,
            "privatize", //not always on, but if on without value, "2" assigned automatically
            "2", //move option dependences analysis up since -parallelize-loops=1 is default now
            "2",
            "N",
            "Perform scalar/array privatization analysis (ON=2)\n"
            + "      =0 force to disable\n" //in case -parallelize-loops is on, but user wants to disable this option
            + "      =1 enable only scalar privatization\n"
            + "      =2 enable scalar and array privatization");
        options.add(options.ANALYSIS,
            "reduction", //not always on, but if on without value, "2" assigned automatically
            "2", //move option dependences analysis up since -parallelize-loops=1 is default now
            "2",
            "N",
            "Perform reduction variable analysis (ON=2)\n"
            + "      =0 force to disable\n" //in case -parallelize-loops is on, but user wants to disable this option
            + "      =1 enable only scalar reduction analysis\n"
            + "      =2 enable array reduction analysis and transformation");
        options.add(options.UTILITY, // TODO: this may not be consistent
            "skip-procedures",
            "proc1,proc2,...",
            "Causes all passes that observe this flag to skip the listed procedures");
        options.add(options.TRANSFORM,
            "tsingle-call",
            "Transform all statements so they contain at most one function call");
        options.add(options.TRANSFORM,
            "tinline",
            "mode=0|1|2|3|4:depth=0|1:pragma=0|1:debug=0|1:foronly=0|1:complement=0|1:functions=foo,bar,...",
            "(Experimental) Perform simple subroutine inline expansion tranformation\n"
            + "   mode\n"
            + "      =0 inline inside main function (default)\n"
            + "      =1 inline inside selected functions provided in the \"functions\" sub-option\n"
            + "      =2 inline selected functions provided in the \"functions\" sub-option, when invoked\n"
            + "      =3 inline according to the \"inlinein\" pragmas\n"
            + "      =4 inline according to both \"inlinein\" and \"inline\" pragmas\n"
            + "   depth\n"
            + "      =0 perform inlining recursively i.e. within callees (and their callees) as well (default)\n"
            + "      =1 perform 1-level inlining \n"
            + "   pragma\n"
            + "      =0 do not honor \"noinlinein\" and \"noinline\" pragmas\n"
            + "      =1 honor \"noinlinein\" and \"noinline\" pragmas (default)\n"
            + "   debug\n"
            + "      =0 remove inlined (and other) functions if they are no longer executed (default)\n"
            + "      =1 do not remove the inlined (and other) functions even if they are no longer executed\n"
            + "   foronly\n"
            + "      =0 try to inline all function calls depending on other options (default)\n"
            + "      =1 try to inline function calls inside for loops only \n"
            + "   complement\n"
            + "      =0 consider the functions provided in the command line with \"functions\" sub-option (default)\n"
            + "      =1 consider all functions except the ones provided in the command line with \"functions\" sub-option\n"
            + "   functions\n"
            + "      =[comma-separated list] consider the provided functions. \n"
            + "      (Note 1: This sub-option is meaningful for modes 1 and 2 only) \n"
            + "      (Note 2: It is used with \"complement\" sub-option to determine which functions should be considered.)");

        options.add(options.TRANSFORM,
            "tsingle-declarator",
            "Transform all variable declarations so they contain at most one declarator");
        options.add(options.TRANSFORM,
            "tsingle-return",
            "Transform all procedures so they have a single return statement");
        options.add(options.UTILITY,
            "verbosity", //by default on with "0", but if on without value, "1" assigned automatically
            "0",
//            "1", //1 is general default for on without value
            "N",
            "Degree of status messages (0-4) that you wish to see (ON=1)");
        options.add(options.UTILITY,
            "debug_parser_input",
            "Print a single preprocessed input file before sending to parser and exit");
        options.add(options.UTILITY,
            "debug_preprocessor_input",
            "Print a single pre-annotated input file before sending to preprocessor and exit");
        options.add(options.UTILITY,
            "version",
            "Print the version information");
        options.add(options.ANALYSIS,
            "ddt", //not always on, but if on without value, "2" assigned automatically
            "2", //move option dependences analysis up since -parallelize-loops=1 is default now
            "2",
            "N",
            "Perform Data Dependence Testing (ON=2)\n"
            + "      =0 force to disable\n" //in case -parallelize-loops is on, but user wants to disable this option
            + "      =1 banerjee-wolfe test\n"
            + "      =2 range test");
        options.add(options.ANALYSIS,
            "parallelize-loops",
            "1", // now this option is turned on by default. (=0 to turn off)
            "N",
            "Annotate loops with Parallelization decisions (ON=1)\n"
            + "      =0 do not parallelize\n"
            + "      =1 parallelizes outermost loops\n"
            + "      =2 parallelizes all loops in nests\n"
            + "      =3 parallelizes outermost loops with report\n"
            + "      =4 parallelizes all loops with report");
        options.add(options.CODEGEN,
            "ompGen",
            "1", //move option dependences analysis up since -parallelize-loops=1 is default now
//            "1",
            "N",
            "Generate new OpenMP pragma and handle existing OpenMP pragrams (ON=1)\n"
            + "      =0 force to disable\n"
            + "      =1 comment out existing OpenMP pragmas\n"
            + "      =2 remove existing OpenMP pragmas\n"
            + "      =3 remove existing OpenMP and Cetus pragmas\n"
            + "      =4 keep all pragmas");
        options.add(options.TRANSFORM,
            "profile-loops",
            null,
            "4",
            "N",
            "Inserts loop-profiling calls (ON=4), =5|6 may be unsafe\n"
            + "      =1 every loop          =2 outermost loop\n"
            + "      =3 every omp parallel  =4 outermost omp parallel\n"
            + "      =5 every omp for       =6 outermost omp for");

        options.add(options.UTILITY, // TODO: needs check for usefulness
            "macro",
            "Sets macros for the specified names with comma-separated list (no space is allowed)\n" +
            "e.g., -macro=ARCH=i686,OS=linux");
        options.add(options.ANALYSIS,
            "alias",
            "1", // this is demand-driven with option default value = 1
            "N",
            "Specify level of alias analysis\n"
        + "      =0 assume all locations are aliased\n"
        + "      =1 advanced interprocedural analysis (default)\n"
        + "         Uses interprocedural points-to analysis\n"
        + "      =2 assume no alias exists when points-to analysis is too conservative\n"
        + "      =3 assume no alias exists"
        );
        options.add(options.TRANSFORM,
            "normalize-return-stmt",
            "Normalize return statements for all procedures");
        options.add(options.ANALYSIS,
            "range",
            "1", // this is demand-driven with option default value = 1
            "N",
            "Specify the accuracy of symbolic analysis with value ranges\n"
            + "      =0 disable range computation (minimal symbolic analysis)\n"
            + "      =1 enable local range computation (default)\n"
            + "      =2 enable inter-procedural computation (experimental)");
        options.add(options.UTILITY,
            "preserve-KR-function",
            "Preserves K&R-style function declaration");
        options.add(options.TRANSFORM,
            "teliminate-branch",
            "1", //move option dependences analysis up since -parallelize-loops=1 is default now
//            "1", //1 is default value-on
            "N",
            "Eliminates unreachable branch targets (ON=1)\n"
            + "      =0 disable\n"
            + "      =1 enable\n"
            + "      =2 leave old statements as comments");
        options.add(options.CODEGEN,
            "profitable-omp",
            "1", //move option dependences analysis up since -parallelize-loops=1 and -ompGen=1 are default 
//            "1", //1 is default value-on
            "N",
            "Inserts runtime for selecting profitable omp parallel region (ON=1)"
            + " (See the API documentation for more details)\n"
            + "      =0 disable\n"
            + "      =1 Model-based loop selection\n"
            + "      =2 Profile-based loop selection");

        options.add(options.TRANSFORM,
                    "loop_interchange", 
                     "Exchanges the order of two iteration variables used by a nested loop");
    }

    /**
    * Returns the value of the given key or null * if the value is not set.
    * Key values are set on the command line as <b>-option_name=value</b>.
    *
    * @param key The key to search
    * @return the value of the given key or null if the value is not set.
    */
    public static String getOptionValue(String key) {
        return options.getValue(key);
    }

    /**
    * Returns the set of  procedure names that should be excluded from
    * transformations. These procedure names are specified with the
    * skip-procedures command line option by providing a comma-separated list
    * of names.
    * @return set of procedure names that should be excluded from
    *         transformations
    */
    public static HashSet getSkipProcedureSet() {
        HashSet<String> proc_skip_set = new HashSet<String>();
        String s = getOptionValue("skip-procedures");
        if (s != null) {
            String[] proc_names = s.split(",");
            proc_skip_set.addAll(Arrays.asList(proc_names));
        }
        return proc_skip_set;
    }

    protected void parseOption(String opt) {
        opt = opt.trim();
        // empty line
        if (opt.length() < 2) {
            return;
        }
        int eq = opt.indexOf('=');
        if (eq == -1) { // if value is not set
            // registered option
            if (options.contains(opt)) {
                // no value on the option line, so set it to null
                setOptionValue(opt, null);
            } else {
                System.err.println("ignoring unrecognized option " + opt);
            }
        } else { // if value is set
            String option_name = opt.substring(0, eq);
            if (options.contains(option_name)) {
                if (option_name.equals("preprocessor")) {
                    setOptionValue(option_name,
                                   opt.substring(eq + 1).replace("\"", ""));
                } else {
                    // use the value from the command line
                    setOptionValue(option_name, opt.substring(eq + 1));
                }
            } else {
                System.err.println("ignoring unrecognized option "
                                   + option_name);
            }
        }
    }

    /**
    * Parses command line options to Cetus.
    *
    * @param args The String array passed to main by the system.
    */
    protected void parseCommandLine(String[] args) {
        /* print a useful message if there are no arguments */
        if (args.length == 0) {
            printUsage();
            Tools.exit(1);
        }
        // keeps track of dangling preprocessor values
        // e.g., args[n] = -preprocessor="cpp
        //       args[n+1] = -EP"
        boolean preprocessor = false;
        int i; /* used after loop; don't put inside for loop */
        for (i = 0; i < args.length; ++i) {
            String opt = args[i];
            // options start with "-"
            if (opt.charAt(0) != '-') {
                /* not an option -- skip to handling options and filenames */
                break;
            }
            int eq = opt.indexOf('=');
            if (eq == -1) { // if value is not set
                String option_name = opt.substring(1);
                if (options.contains(option_name)) { // registered option
                    preprocessor = false;
                    // no value on the command line, so just set it to "1"
                    // setValue(name) will search for predefined value
                    // --> see setValue(String) for more information.
                    options.setValue(option_name);
                } else if (preprocessor) {
                    // found dangling preprocessor option
                    setOptionValue("preprocessor",
                                   getOptionValue("preprocessor")
                                   + " " + opt.replace("\"",""));
                } else {
                    System.err.println("Unrecognized option " +
                                       option_name);
                }
            } else { // if value is set
                String option_name = opt.substring(1, eq);
                if (options.contains(option_name)) {
                    if (option_name.equals("preprocessor")) {
                        preprocessor = true;
                        setOptionValue(option_name,
                                       opt.substring(eq + 1).replace("\"",""));
                    } else {
                        preprocessor = false;
                        // use the value from the command line
                        // replace "0" with null to keep pass-invocation
                        // convention
                        String value = opt.substring(eq+1);
                        //converting all "0" into "null" causes "-option=0 (force to disable)" not work well
                        //needs to move this part to after option dependence test
//                        if (value.equals("0") && !option_name.equals("verbosity")) { //verbosity cannot be null
//                            value = null;
//                        }
                        // if "=" exists, but no value, assign default values
                        // if value "=null", assign null to it.
                        if (value==null || value.equals("") || value.equals(" ")) options.setValue(option_name);
                        else if (value.equals("null")) setOptionValue(option_name, null);
                        else setOptionValue(option_name, value);
                    }
                } else if (preprocessor) {
                    setOptionValue("preprocessor",
                                   getOptionValue("preprocessor")
                                   + " " + opt.replace("\"",""));
                } else {
                    System.err.println("ignoring unrecognized option " +
                                       option_name);
                }
            }
            if (getOptionValue("help") != null ||
                getOptionValue("usage") != null) {
                printUsage();
                Tools.exit(0);
            }
            if (getOptionValue("version") != null) {
                printVersion();
                Tools.exit(0);
            }
            if (getOptionValue("dump-options") != null) {
                setOptionValue("dump-options", null);
                dumpOptionsFile();
                Tools.exit(0);
            }
            if (getOptionValue("dump-system-options") != null) {
                setOptionValue("dump-system-options", null);
                dumpSystemOptionsFile();
                Tools.exit(0);
            }
            // load options file and then proceed with rest of command line
            // options
            if (getOptionValue("load-options") != null) {
                // load options should not be set in options file
                setOptionValue("load-options", null);
                loadOptionsFile();
                // prevent reentering this handler
                setOptionValue("load-options", null);
            }
        }
        // end of arguments without a file name
        if (i >= args.length) {
            System.err.println("No input files provided!");
            Tools.exit(1);
        }
        // The purpose of this wildcard expansion is to ease the use of IDE
        // environment which usually doesn't handle wildcards.
        int num_file_args = args.length-i;
        filenames = new ArrayList<String>(num_file_args);
        for (int j = 0; j < num_file_args; ++j, ++i) {
            if (args[i].contains("*") || args[i].contains("?")) {
                File parent =
                        (new File(args[i])).getAbsoluteFile().getParentFile();
                for (File file : parent.listFiles(new RegexFilter(args[i]))) {
                    filenames.add(file.getAbsolutePath());
                }
            } else {
                filenames.add(args[i]);
            }
        }
        if (filenames.isEmpty()) {
            System.err.println("No input files provided!");
            Tools.exit(1);
        }
    }

    /**
    * Parses all of the files listed in <var>filenames</var>
    * and creates a {@link Program Program} object.
    */
    @SuppressWarnings({"unchecked", "cast"})
    protected void parseFiles() {
        program = new Program();
        Class class_parser;
        try {
            class_parser = getClass().getClassLoader().loadClass(
                           getOptionValue("parser"));
            CetusParser cparser =
                    (CetusParser)class_parser.getConstructor().newInstance();
            for (String file : filenames) {
                program.addTranslationUnit(cparser.parseFile(file,options));
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Failed to load parser: " +
                               getOptionValue("parser"));
            Tools.exit(1);
        } catch(Exception e) {
            System.err.println("Failed to initialize parser");
            Tools.exit(1);
        }
        // It is more natural to include these two steps in this method.
        // Link IDExpression => Symbol object for faster future access.
        SymbolTools.linkSymbol(program);
        // Convert the IR to a new one with improved annotation support
        TransformPass.run(new AnnotationParser(program));
    }

/*
    protected void parseFiles_old() {
        try {
          program = new Program();
          Parser parser = new Parser();
          for (String file : filenames) {
              program.addTranslationUnit(parser.parse(file));
          }
        } catch (IOException e) {
            System.err.println("I/O error parsing files");
            System.err.println(e);
        Tools.exit(1);
        } catch (Exception e) {
            System.err.println(
                    "Miscellaneous exception while parsing files: " + e);
            e.printStackTrace();
            Tools.exit(1);
        }
        // It is more natural to include these two steps in this method.
        // Link IDExpression => Symbol object for faster future access.
        SymbolTools.linkSymbol(program);
        // Convert the IR to a new one with improved annotation support
        TransformPass.run(new AnnotationParser(program));
    }
*/

    /**
    * Prints the list of options that Cetus accepts.
    */
    public String printUsage() {
        String usage = "\n\ncetus.exec.Driver [option]... [file]...\n";
        usage += options.getUsage();
        System.err.println(usage);
        return usage;
    }

    /**
    * dump default options to file options.cetus in working directory
    * do not overwrite if file already exists.
    */
    public void dumpOptionsFile()  {
        // check for options.cetus in working directory
        // registerOptions();
        File optionsFile = new File("options.cetus");
        // create file options.cetus
        try {
            if (optionsFile.createNewFile()) {
                // populate options.cetus
                FileOutputStream fo = new FileOutputStream(optionsFile);
                PrintStream ps = new PrintStream(fo);
                ps.println(options.dumpOptions().trim());
                ps.close();
                fo.close();
            }
        } catch (IOException e) {
            System.err.println("Error: Failed to dump options.cetus");
        }
    }

    public void dumpSystemOptionsFile()  {
        // check for options.cetus in working directory
        // registerOptions();
        String homePath = System.getProperty("user.home");
        File optionsFile = new File(homePath,"options.cetus");
        // create file options.cetus
        try {
            if (optionsFile.createNewFile()) {
                // populate options.cetus
                FileOutputStream fo = new FileOutputStream(optionsFile);
                PrintStream ps = new PrintStream(fo);
                ps.println(options.dumpOptions().trim());
                ps.close();
                fo.close();
            }
        } catch (IOException e) {
            System.err.println(
                    "Error: Failed to dump system wide options.cetus");
        }
    }

    /**
    * load options.cetus
    * search order is working directory and then home directory
    */
    public void loadOptionsFile() {
        // check working directory for options.cetus
        // check home directory for options.cetus
        File optionsFile = new File("options.cetus");
        //dumpOptionsFile();
        if (!optionsFile.exists()) {
            String homePath = System.getProperty("user.home");
            optionsFile = new File(homePath,"options.cetus");
        }
        if (!optionsFile.exists()) {
            System.err.println("Error: Failed to load options.cetus");
            System.err.println(
                "Use option -dump-options or -dump-system-options"
                + " to create options.cetus with default values");
            Tools.exit(1);
        }
        // load file contents
        try {
            FileReader fr = new FileReader(optionsFile);
            BufferedReader br = new BufferedReader(fr);
            String line;
            // Read lines
            while ((line=br.readLine()) != null) {
                // Remove comments
                if (line.startsWith("#"))
                    continue;
                // load option
                parseOption(line);
            }
        } catch (Exception e) {
            System.err.println("Error while loading options file");
            Tools.exit(1);
        }
    }

    /**
    * Prints the compiler version.
    */
    public void printVersion() {
        System.err.println(version);
    }

    /**
    * Runs this driver with args as the command line.
    *
    * @param args The command line from main.
    */
    public void run(String[] args) {
    	System.out.println(CetusGUI.consoleSeperator +
    			"\nOptions initialized implicitly before parsing command line: "
    			+autoTurnedOnOptions+"\n");
        parseCommandLine(args);
        parseFiles();
        if (getOptionValue("parse-only") != null) {
            System.err.println("parsing finished and parse-only option set");
            Tools.exit(0);
        }
        runPasses();
        PrintTools.printlnStatus("Printing...", 1);
        try {
            program.print();
            // process indentation for output C file
            String outDir = getOptionValue("outdir");
            for (String fileString : filenames) {
            	//System.out.println("**************"+fileString);
            	File outFile = new File(outDir, (new File(fileString)).getName());
            	Tools.processIndent(outFile);
            }
        } catch (IOException e) {
            System.err.println("could not write output files: " + e);
            Tools.exit(1);
        }
    }

    /**
    * Runs analysis and optimization passes on the program.
    */
    public void runPasses() {
    	
//    	PrintTools.printlnStatus("[Driver] print all options before option dependences analysis:\n"+options.dumpOptions(),4);

    	//-------------- dependences test is not needed any more, since -parallelize-loops=1 is default now
    	// so all 7 prerequisites (plus profitable-omp) for parallelize-loops are moved into Cetus initialization now.  
//        /* check for option dependences */
//        /* in each set of option strings, the first option requires the
//           rest of the options to be set for it to run effectively */
//        String[][] pass_prerequisites = {
//            // inliner internally handles it -- no need for pass dependency
//            //{"inline",
//            //    "tsingle-call","tsingle-return"},
//            {"parallelize-loops", 
//            	"teliminate-branch","alias","ddt","privatize","reduction","induction","ompGen"},
//            //{"loop-interchange", //loop-interchange not supported now 11/14/2012
//            //    "ddt"}
//        };
//        for (String[] pass_prerequisite : pass_prerequisites) {
//            if (getOptionValue(pass_prerequisite[0]) != null && !getOptionValue(pass_prerequisite[0]).equals("0")) {//use "0" as a value for disable
//            	PrintTools.printlnStatus("\n[Driver] turning on prerequested options for -"+pass_prerequisite[0]
//            			+"="+getOptionValue(pass_prerequisite[0])+":",0);
//                for (int j = 1; j < pass_prerequisite.length; ++j) {
//                    if (getOptionValue(pass_prerequisite[j]) == null) {
//                        System.out.println("[Driver] turning on required pass "
//                            + pass_prerequisite[j] + " for "
//                            + pass_prerequisite[0]);
//                        options.setValue(pass_prerequisite[j]);
//                    } else PrintTools.printlnStatus("[Driver] prerequested option -"+pass_prerequisite[j]
//                    		+"="+getOptionValue(pass_prerequisite[j])+" exists",0);
//                }
//                PrintTools.printlnStatus("",0);
//            }
//        }
        
        PrintTools.printlnStatus("[Driver] print all options :\n"+options.dumpOptions(),4);
        
        if (getOptionValue("teliminate-branch") != null && !getOptionValue("teliminate-branch").equals("0")) {
            TransformPass.run(new BranchEliminator(program));
        }
        if (getOptionValue("callgraph") != null) {
            CallGraph cg = new CallGraph(program);
            cg.print(System.out);
        }
        if (getOptionValue("tsingle-declarator") != null) {
            TransformPass.run(new SingleDeclarator(program));
        }
        if (getOptionValue("tsingle-call") != null) {
            TransformPass.run(new SingleCall(program));
        }
        if (getOptionValue("tsingle-return") != null) {
            TransformPass.run(new SingleReturn(program));
        }
        if (getOptionValue("tinline") != null) {
            TransformPass.run(new InlineExpansionPass(program));
        }
        if (getOptionValue("normalize-loops") != null) {
            TransformPass.run(new LoopNormalization(program));
        }
        if (getOptionValue("normalize-return-stmt") != null) {
            TransformPass.run(new NormalizeReturn(program));
        }
        if (getOptionValue("induction") != null && !getOptionValue("induction").equals("0")) {
            TransformPass.run(new IVSubstitution(program));
        }

        if (getOptionValue("privatize") != null && !getOptionValue("privatize").equals("0")) {
            AnalysisPass.run(new ArrayPrivatization(program));
        }

      
        if (getOptionValue("ddt") != null && !getOptionValue("ddt").equals("0")) {
            AnalysisPass.run(new DDTDriver(program));
        }
        if (getOptionValue("reduction") != null && !getOptionValue("reduction").equals("0")) {
            AnalysisPass.run(new Reduction(program));
        }
/*
        if (getOptionValue("openmp") != null) {
            AnalysisPass.run(new OmpAnalysis(program));
        }
      
*/
        if (getOptionValue("loop_interchange") != null) {
            TransformPass.run(new LoopInterchange(program));
        }

        if (getOptionValue("parallelize-loops") != null && !getOptionValue("parallelize-loops").equals("0")) {
            AnalysisPass.run(new LoopParallelizationPass(program));
        }
        if (getOptionValue("ompGen") != null && !getOptionValue("ompGen").equals("0")) {
            CodeGenPass.run(new ompGen(program));
        }
      

/*
        if (getOptionValue("loop-tiling") != null) {
            AnalysisPass.run(new LoopTiling(program));
        }
*/
        if (getOptionValue("profile-loops") != null) {
            TransformPass.run(new LoopProfiler(program));
        }
    }

    /**
    * Sets the value of the option represented by <i>key</i> to
    * <i>value</i>.
    *
    * @param key The option name.
    * @param value The option value.
    */
    public static void setOptionValue(String key, String value) {
        options.setValue(key, value);
    }

    public static boolean isIncluded(
            String name, String hir_type, String hir_name) {
        return options.isIncluded(name, hir_type, hir_name);
    }


    /**
    * Implementation of file filter for handling wild card character and other
    * special characters to generate regular expressions out of a string.
    */
    private static class RegexFilter implements FileFilter {
        /** Regular expression */
        private String regex;

        /**
        * Constructs a new filter with the given input string
        * @param str String to construct regular expression out of
        */
        public RegexFilter(String str) {
            regex = str.replaceAll("\\.", "\\\\.") // . => \.
            .replaceAll("\\?", ".")                // ? => .
            .replaceAll("\\*", ".*");              // * => .*
        }

        @Override
        public boolean accept(File f) {
            return f.getName().matches(regex);
        }
    }
    
    
    /**
    * Entry point for Cetus; creates a new Driver object,
    * and calls run on it with args.
    *
    * @param args Command line options.
     * @throws IOException 
    */
    public static void main(String[] args) {
    	
    	//checkUpdate();
		if (args.length > 0 && args[0].toLowerCase().equals("-gui")) {
//    	if (args.length == 0 || args[0].toLowerCase().equals("-gui")) {
			t2 = new ThreadUpdate(10, true);
			t2.start(); //thread 2 for other things, like version check
//			CetusGUI newGUI = new CetusGUI(args);
//			newGUI.setVisible(true);
			System.out.println("Starting Cetus GUI...");
			Tools.exitThrowsException(true); //GUI will not exit upon exception
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					CetusGUI newGUI = new CetusGUI();
					//newGUI.pack();
					newGUI.setVisible(true);
				}
			});
		} else {
			//System.out.println(args.length);
//			t2 = new Thread2(0);
//			t2.start(); //thread 2 for other things, like version check
			System.out.println("Start Cetus GUI with \"-gui\" as the first arg, " +
					"e.g. \"./cetus -gui\" or \"java -jar cetus.jar -gui\".");
			System.out.println();
			(new Driver()).run(args);
			checkUpdate();
			//t2.interrupt(); //if thread 2 for checking new version has not finished after Cetus finished, interrupt it.
		}
    }

}
