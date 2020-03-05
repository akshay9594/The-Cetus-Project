package cetus.server.speed_up;

import java.io.File;
import java.io.IOException;


//import cetus.gui.CetusGuiSpeedupPanel;

//import cetus.gui.CetusGuiSpeedupPanel;

/*
 * flags -outdir= file
 * -action=(speed_up or graph)
 * -threads=(number of threads)
 * 
 * 
 * 
 */

public class CetusGUI{// extends JFrame{

	
	protected static File inputFile = null, defaultOutputFile = null, outputFile = null;
	protected CetusGuiSpeedupPanel runPanel;
	protected final static int SPEED_UP = 0;
	protected final static int GRAPH = 1;
	protected final static int NO = 0;
	protected final static int YES = 1;
	protected static int action;
	protected static int threads;
	protected static boolean checkCompInput = false;
	protected static boolean checkRunInput = false;
	protected static boolean checkCompOutput = false;
	protected static boolean checkRunOutput = false;
	protected static boolean checkCalculate = false;
	
	//runPanel = new CetusGuiSpeedupPanel();
	protected CetusGUI(){
		runPanel = new CetusGuiSpeedupPanel();
		runPanel.numThreads = threads;
		if(checkCompInput){
			runPanel.compileInput();
		}
		if(checkRunInput){
			runPanel.runInput();
		}
		if(checkCompOutput){
			runPanel.compileOutput();
		}
		if(checkRunOutput){
			runPanel.runOutput();
		}
		if(checkCalculate){
			//System.out.println("here");
			runPanel.calculateSpeedup();
		}
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException{
		// TODO Auto-generated method stub
		
		 //inputFile = new File(args[1]);
		 //outputFile = new File(args[2]);
		 
		 for(String flags : args){
			// System.out.println(flags.substring(10, flags.length()));
			 
			 if(flags.substring(0, 9).equals("-in_file=")){
				 
					inputFile = new File(flags.substring(9, flags.length()));
					 
				 }
			 else if(flags.substring(0, 10).equals("-out_file=")){
				outputFile = new File(flags.substring(10, flags.length()));
				 
			 }else if(flags.substring(0, 8).equals("-action=")){
				 if(flags.substring(8, flags.length()).equals("speed_up")){
					 action = SPEED_UP;
				 }else{
					 action = GRAPH;
				 }
				 
			 }else if(flags.substring(0, 9).equals("-threads=")){
				 
				 
				 threads = Integer.parseInt(flags.substring(9, flags.length()));
			 }else if(flags.substring(0,11).equals("-compinput=")){
				 if(Integer.parseInt(flags.substring(11,flags.length()))==1){
					 checkCompInput = true;
				 }
			 }else if(flags.substring(0,10).equals("-runinput=")){
				 if(Integer.parseInt(flags.substring(10,flags.length()))==1){
					 checkRunInput = true;
				 }
			 }else if(flags.substring(0,12).equals("-compoutput=")){
				 if(Integer.parseInt(flags.substring(12,flags.length()))==1){
					 checkCompOutput = true;
				 }
			 }else if(flags.substring(0,11).equals("-runoutput=")){
				 if(Integer.parseInt(flags.substring(11,flags.length()))==1){
					 checkRunOutput = true;
					// System.out.print(checkRunI);
				 }
			 }else if(flags.substring(0,11).equals("-calculate=")){
				if(Integer.parseInt(flags.substring(11,flags.length()))==1){
					 checkCalculate = true;
					 
				 }
			 }
			 
			 
		 }
		 
		 new CetusGUI() ;
		 
	}

}
