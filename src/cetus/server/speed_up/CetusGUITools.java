package cetus.server.speed_up;

import java.awt.Container;
import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.Enumeration;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

public class CetusGUITools {
	
	public static final String lineSeperator = "\n\n************************************************" +
			"*******************************", osName = System.getProperty("os.name")
			, osMessage = (osName.toLowerCase()).indexOf("win")>=0?"For "+osName+", user must install cygwin " +
			"GCC and set path to cygwin bin to run preprocessor!":"For "+osName+", existing GCC will be used for preprocessor";
	public static final String guideMessage = "You can directly edit your C file in the text area. " +
			"Clicking button [Save] will overwrite your original input C file. " +
			"Click [Options] to change options. " +
			"Click [Compile] to compile C code and show the output C file. " +
			"Click [About] to see Cetus info, help and contact information. " +
			"Click [Console] to see, save and clear all console messages. ";
	
	public static final String user_dir = System.getProperty("user.dir");
	public static final String user_home = System.getProperty("user.home");
    public static final String file_sep = System.getProperty("file.separator"); // ("/" on UNIX)
    public static final String path_sep = System.getProperty("path.separator"); // (":" on UNIX)
    public static final String line_sep = System.getProperty("line.separator"); // ("\n" on UNIX)
    public static String current_dir = user_dir;
    public static final String regexPositiveInteger5OrMoreDigits = "[1-9][0-9][0-9][0-9][0-9]+";
    public static final String regexVersionNumber = "\\d+(\\.\\d+)+";
    public static int buttonHeight = 20, buttonWidth = 100, buttonTop = 4, 
    		buttonGap = 5, buttonWidthAddtion = buttonWidth/2, labelHeight = 24;
    public static String jarPath = System.getProperty("java.class.path", ".");
    public static String examplesLocation = "/examples/";
    public static int cores = Runtime.getRuntime().availableProcessors();
    
    private CetusGUITools() {
    }
    
	public static String readFileToString(File file) {

		StringBuffer fileBuffer = null;
		String fileString = null;
		String line = null;

		try {
			FileReader in = new FileReader(file);
			BufferedReader brd = new BufferedReader(in);
			fileBuffer = new StringBuffer();

			while ((line = brd.readLine()) != null) {
				fileBuffer.append(line).append(line_sep);
			}

			in.close();
			fileString = fileBuffer.toString();
		} catch (IOException e) {
			System.out.println("Error: " + e.getMessage());
			return null;
		}
		return fileString;
	}

	public static String[] readFileToArrayStrings(File file) {

		String line = null;
		ArrayList<String> lines = new ArrayList<String>();
		
		try {
			FileReader in = new FileReader(file);
			BufferedReader brd = new BufferedReader(in);
			while ((line = brd.readLine()) != null) {
				lines.add(line);
			}
			brd.close();
			in.close();
			return lines.toArray(new String[lines.size()]);
		} catch (IOException e) {
			System.out.println("Error: " + e.getMessage());
			return null;
		}
	}

	public static String[] readURLToArrayStrings(String urlString) {

		String line = null;
		ArrayList<String> lines = new ArrayList<String>();
		
		try {			
			URL u = new URL(urlString);
			InputStream ins = u.openStream();
			InputStreamReader isr = new InputStreamReader(ins);
			BufferedReader brd = new BufferedReader(isr);
			while ((line = brd.readLine()) != null) {
				lines.add(line);
			}
			brd.close();
			isr.close();
			ins.close();
			return lines.toArray(new String[lines.size()]);
			
		} catch (IOException e) {
			//System.out.println("Error: " + e.getMessage());
			System.out.println("Error: reading URL failed: "+urlString);
			return null;
		}
	}

	
	public static boolean writeStringToFile(File file, String fileText) {
		try {
			// Create file
			FileWriter fstream = new FileWriter(file);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(fileText);
			// Close the output stream
			out.close();
			return true;
		} catch (Exception e) {// Catch exception if any
			System.err.println("Error: " + e.getMessage());
			return false;
		}
	}   
	
	public static int writeArrayStringsToFile (File file, String[] strings) {

		PrintWriter out;
		int numStringsWritten = 0;
		try {
			out = new PrintWriter(new FileWriter(file));
			// Write each string in the array on a separate line
			for (String s : strings) {
				out.println(s);
				numStringsWritten++;
			}
			out.close();
		} catch (IOException e) {
			numStringsWritten = -1;
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return numStringsWritten;
	}

	public static int findPrefixInStrings(String prefix, String[] strings){
		int index = 0;
		
		for (index = 0; index < strings.length; index ++) {
			if (strings[index].startsWith(prefix)) return index;
		}
		
		return -1;
	}

	public static int findSubStringInStrings(String subString, String[] strings){
		int index = 0;
		
		for (index = 0; index < strings.length; index ++) {
			if (strings[index].indexOf(subString)>=0) return index;
		}
		
		return -1;
	}

	public static int findStringInStrings(String string, String[] strings){
		int index = 0;
		
		for (index = 0; index < strings.length; index ++) {
			if (strings[index].equals(string)) return index;
		}
		
		return -1;
	}

//	public static int findSubStringsInString(String[] subStrings, String string){
//		int index = 0;
//		
//		for (index = 0; index < subStrings.length; index ++) {
//			if (string.indexOf(subStrings[index])>=0) return index;
//		}
//		
//		return -1;
//	}
	
//	public static int findSubStringsInStringLast(String[] subStrings, String string){
//		int index = 0;
//		
//		for (index = subStrings.length-1; index >= 0; index --) {
//			if (string.indexOf(subStrings[index])>=0) return index;
//		}
//		
//		return -1;
//	}
	
	public static File getDir(Container parent, String extDescription, String ext){
		JFileChooser fileChooser = new JFileChooser(current_dir);
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		
		if (extDescription!=null && !extDescription.equals("")&& ext!=null && !ext.equals("")) {
			FileFilter filter = new FileNameExtensionFilter(extDescription, ext);
			fileChooser.setFileFilter(filter);
		}

		File file;
        int returnVal = fileChooser.showOpenDialog(parent);

		if (returnVal == JFileChooser.APPROVE_OPTION) {
			file = fileChooser.getSelectedFile();
			//file = fileChooser.getCurrentDirectory();
			// This is where a real application would open the file.
			System.out.println("Opening a directory: " + file.toString());
			//System.out.println("Opening a directory: " + file.toPath());
			current_dir = file.toString();
		} else {
			file = null; //new File("");
			System.out.println("Cancelled opening a directory");
			//System.out.println("Opening a directory: " + file.toPath());
		}
		return file;

	}
	
	public static File getFile(Container parent, String extDescription, String ext){
		JFileChooser fileChooser = new JFileChooser(current_dir);
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		if (extDescription!=null && !extDescription.equals("")&& ext!=null && !ext.equals("")) {
			FileFilter filter = new FileNameExtensionFilter(extDescription, ext);
			fileChooser.setFileFilter(filter);
		}
		
		File file;
        int returnVal = fileChooser.showOpenDialog(parent);

		if (returnVal == JFileChooser.APPROVE_OPTION) {
			file = fileChooser.getSelectedFile();
			//file = fileChooser.getCurrentDirectory();
			// This is where a real application would open the file.
			System.out.println("Selected a file: " + file.toString());
			//System.out.println("Opening a directory: " + file.toPath());
			current_dir = file.getParent().toString();
		} else {
			file = null; //new File("");
			System.out.println("Cancelled opening a file");
			//System.out.println("Opening a directory: " + file.toPath());
		}
		return file;

	}
	
	public static File[] getFilesDirs(Container parent, String extDescription, String ext){
		JFileChooser fileChooser = new JFileChooser(current_dir);
		fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		fileChooser.setMultiSelectionEnabled(true);
		if (extDescription!=null && !extDescription.equals("")&& ext!=null && !ext.equals("")) {
			FileFilter filter = new FileNameExtensionFilter(extDescription, ext);
			fileChooser.setFileFilter(filter);
		}
		
		File[] filesDirs;
        int returnVal = fileChooser.showOpenDialog(parent);

		if (returnVal == JFileChooser.APPROVE_OPTION) {
			filesDirs = fileChooser.getSelectedFiles();
			System.out.println("Selected multiple dirs and files: ");
			for (File eachFile : filesDirs){
				System.out.println(eachFile.toString());				
			}
			current_dir = filesDirs[0].getParent().toString();
		} else {
			filesDirs = null;
			System.out.println("Cancelled opening multiple dirs and files");
		}
		return filesDirs;

	}

	public static File saveFile(Container parent, String extDescription, String ext, String defaultFileName){
		JFileChooser fileChooser = new JFileChooser(current_dir) {
			@Override
			public void approveSelection(){
			    File f = getSelectedFile();
			    if(f.exists() && getDialogType() == SAVE_DIALOG){
			        int result = JOptionPane.showConfirmDialog(this,"The file exists, overwrite?","Existing file",JOptionPane.YES_NO_CANCEL_OPTION);
			        switch(result){
			            case JOptionPane.YES_OPTION:
			                super.approveSelection();
			                return;
			            case JOptionPane.NO_OPTION:
			                return;
			            case JOptionPane.CLOSED_OPTION:
			                return;
			            case JOptionPane.CANCEL_OPTION:
			                cancelSelection();
			                return;
			        }
			    }
			    super.approveSelection();
			}
		};
		
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		if (defaultFileName!=null && defaultFileName != "") {
			String defaultFileString = current_dir+file_sep+defaultFileName;
			System.out.println("Default file to be saved: "+defaultFileString);
			File defaultFile = new File(defaultFileString);
			fileChooser.setSelectedFile(defaultFile);
		}
		if (extDescription!=null && !extDescription.equals("")&& ext!=null && !ext.equals("")) {
			FileFilter filter = new FileNameExtensionFilter(extDescription, ext);
			fileChooser.setFileFilter(filter);
		}
		
		File file;
        int returnVal = fileChooser.showSaveDialog(parent);

		if (returnVal == JFileChooser.APPROVE_OPTION) {
			file = fileChooser.getSelectedFile();
			//file = fileChooser.getCurrentDirectory();
			// This is where a real application would open the file.
			System.out.println("Specified a file to be saved: " + file.toString());
			//System.out.println("Opening a directory: " + file.toPath());
			current_dir = file.getParent().toString();
		} else {
			file = null; //new File("");
			System.out.println("Cancelled saving a file");
			//System.out.println("Opening a directory: " + file.toPath());
		}
		return file;
	}
	
	public static String getNoPrefixInStrings(String prefix, String[] strings) {
		int index = findPrefixInStrings(prefix,strings);
		if (index>=0) {
			String optionValue = new String(strings[index]);
//			System.out.println(optionValue.replaceFirst(option, ""));
//			System.out.println(optionStrings[index]);
			return optionValue.replaceFirst(prefix, "");			
		} else {
			return null;
		}
	}
	
	
	
	public static void visitURL (String urlString) {
		
		Desktop dt = Desktop.getDesktop();
		URI uri;
		try {
			uri = new URI(urlString);
			dt.browse(uri.resolve(uri));
			System.out.println("Visiting: "+urlString);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public static String[] matchCopyStrings (String[] subStrings, String[] strings, String defaultString) {
		String[] newStrings = new String[subStrings.length];
		for (int i=0; i<newStrings.length; i++) {
			newStrings[i] = defaultString;
		}
//		boolean found = false;
		for (int i=0; i<strings.length; i++) {
//			found = false;
			for (int index = 0; index < subStrings.length; index ++) {
				if (strings[i].startsWith(subStrings[index])) {
					newStrings[index]=strings[i];
//					found = true;
					break;
				}
			}
//			if (found==true) {
//				System.out.println("*** allOptionsNameStrings: "+Arrays.toString(subStrings));
//				System.out.println("*** allOptionsStrings: "+Arrays.toString(newStrings));
//			}
			//if (found==false) System.out.println("did not find String: "+strings[i]);
		}
		return newStrings;
	}
	
	/**
	 * remove blank strings in string array
	 * @param strings
	 * @param defaultString
	 * @return string array
	 */
	
	public static String[] compressStrings (String[] strings, String defaultString) {
		ArrayList<String> compressedStrings = new ArrayList<String>();
		for (int i=0; i<strings.length; i++) {
			if (!strings[i].equals(defaultString)) {
				compressedStrings.add(strings[i]);
			}
		}
		return compressedStrings.toArray(new String[compressedStrings.size()]);
	}
	
	public static String readFileInJarToString(String jarFileName, String txtFileName) {
	
		JarFile jarFile = null;
		JarEntry entry = null;
		InputStream input = null;
		InputStreamReader isr = null;
		BufferedReader brd = null;
		StringBuffer fileBuffer = null;
		String line = null;		
		String fileString = null;
		
		try {
			jarFile = new JarFile(jarFileName);
			entry = jarFile.getJarEntry(txtFileName);
			input = jarFile.getInputStream(entry);
			isr = new InputStreamReader(input);
			brd = new BufferedReader(isr);
			fileBuffer = new StringBuffer();

			while ((line = brd.readLine()) != null) {
				fileBuffer.append(line).append(line_sep);
			}

			fileString = fileBuffer.toString();
			jarFile.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return fileString;
	}
	
	public static String[] readFileInJarToArrayStrings(String jarFileName, String txtFileName, int numLines) {
		
		JarFile jarFile = null;
		JarEntry entry = null;
		InputStream input = null;
		InputStreamReader isr = null;
		BufferedReader brd = null;
		String line = null;	
		ArrayList<String> lines = new ArrayList<String>();
		//String fileString = null;
		
		try {
			//System.out.println("jarFileName: " + jarFileName);
			jarFile = new JarFile(jarFileName);
			entry = jarFile.getJarEntry(txtFileName);
			input = jarFile.getInputStream(entry);
			isr = new InputStreamReader(input);
			brd = new BufferedReader(isr);

			if (numLines == 0) {
				while ((line = brd.readLine()) != null) {
					lines.add(line);
				}				
			} else if (numLines > 0) {
				for (int i = 0; i < numLines; i++) {
					line = brd.readLine();
					lines.add(line);
				}
			} else {
				return null;
			}
			brd.close();
			jarFile.close();
			return lines.toArray(new String[lines.size()]);
			
		} catch (IOException e) {
			System.out.println("Error: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
	
	public static String convertArrayStringsToStringLines(String[] arr) {
		
		if (arr==null) return null;
		
		StringBuffer buffer = new StringBuffer();
		for(String s : arr) {
		    buffer.append(s).append(line_sep);
		}
		return buffer.toString();
	}
	
	public static String convertArrayStringsToStringSpaces(String[] arr) {
		
		if (arr==null) return null;
		
		StringBuffer buffer = new StringBuffer();
		for(String s : arr) {
		    buffer.append(s).append(" ");
		}
		return buffer.toString();
	}
	
	public static String[] getFileNamesInJar (String jarFileName, String ext) {
		//JarFile jarFile = null;
		//Enumeration en = null;

		try {
			JarFile jarFile = new JarFile(jarFileName);
			Enumeration<JarEntry> en = jarFile.entries();
			ArrayList<String> list = new ArrayList<String>();
			
			while (en.hasMoreElements()) {
				JarEntry entry = (JarEntry) en.nextElement();
				String name = entry.getName();
				//System.out.println(name);
				
				if (ext==null) {
					//System.out.println(name);
					list.add(name);
				} else if (ext!=null && name.endsWith(ext)) {
					//System.out.println(name);
					list.add(name);
				}
			}
			
			return list.toArray(new String[list.size()]);
			
		} catch (IOException e) {
			System.out.println("Error: " + e.getMessage());
			e.printStackTrace();
			return null;
		}		
		
	}
	
	public static String[] getAllFileNamesUnderTopFolderInJar (String jarFileName, String folder) {
		//JarFile jarFile = null;
		//Enumeration en = null;

		try {
			JarFile jarFile = new JarFile(jarFileName);
			Enumeration<JarEntry> en = jarFile.entries();
			ArrayList<String> list = new ArrayList<String>();
			
			while (en.hasMoreElements()) {
				JarEntry entry = (JarEntry) en.nextElement();
				String name = entry.getName();
				//System.out.println(name);
				
				if (folder==null) {
					//System.out.println(name);
					list.add(name);
				} else if (folder!=null && name.lastIndexOf(folder)==0 
					&& name.length()>(folder.length()+name.lastIndexOf(folder))) {
					//System.out.println(name);
					list.add(name);
				}
			}
			
			return list.toArray(new String[list.size()]);
			
		} catch (IOException e) {
			System.out.println("Error: " + e.getMessage());
			e.printStackTrace();
			return null;
		}		
		
	}

	public static String[] getAllFileNamesUnderFolderInJar (String jarFileName, String folder) {
		//JarFile jarFile = null;
		//Enumeration en = null;

		try {
			JarFile jarFile = new JarFile(jarFileName);
			Enumeration<JarEntry> en = jarFile.entries();
			ArrayList<String> list = new ArrayList<String>();
			
			while (en.hasMoreElements()) {
				JarEntry entry = (JarEntry) en.nextElement();
				String name = entry.getName();
				//System.out.println(name);
				
				if (folder==null) {
					//System.out.println(name);
					list.add(name);
				} else if (folder!=null && name.lastIndexOf(folder)>=0 
					&& name.length()>(folder.length()+name.lastIndexOf(folder))) {
					//System.out.println(name);
					list.add(name);
				}
			}
			
			return list.toArray(new String[list.size()]);
			
		} catch (IOException e) {
			System.out.println("Error: " + e.getMessage());
			e.printStackTrace();
			return null;
		}		
		
	}
	
	public static String[] getSubStringsLast(String[] arr, String sep) {
		
		String[] sub = new String[arr.length];
		
		for (int i = 0; i < arr.length; i++) {
			int ind = arr[i].lastIndexOf(sep);
			sub[i] = arr[i].substring(ind+1);
		}
		
		return sub;
		
	}
	
	public static String[] getFirstLinesInJar (String jarFileName, String[] files) {
		String[] firstLines = new String[files.length];
		for (int i = 0; i < files.length; i++) {
			firstLines[i] = readFileInJarToArrayStrings(jarFileName, files[i], 1)[0];
		}
			
		return firstLines;
	}
	
	public static String[] getFileNamesInResource(Class clazz, String folder) {
		
		//System.out.println(input.toString());
		InputStream input = clazz.getResourceAsStream(folder);
		InputStreamReader isr = new InputStreamReader(input);
		BufferedReader brd = new BufferedReader(isr);
		String line = null;
		ArrayList<String> lines = new ArrayList<String>();
		try {
			while ((line = brd.readLine()) != null) {
				//System.out.println(line);
				lines.add(line);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (lines==null) return null;
		else return lines.toArray(new String[lines.size()]);

	}

	public static String[] combineStringIntoArrayStrings(String a, String[] b, int dir) {
		String[] c = new String[b.length];
		if (dir == 1) {
			for (int i = 0; i < b.length; i++) {
				c[i] = a + b[i];
			}
		} else if (dir == 2) {
			for (int i = 0; i < b.length; i++) {
				c[i] = b[i] + a;
			}	
		}
		
		return c;
	}

	
	public static String[] getResourceFile(Class clazz, String fileName, int numLines) {
		
		try {
			//InputStream input = clazz.getClassLoader().getResourceAsStream(fileName); //wrong
			InputStream input = clazz.getResourceAsStream(fileName);
			InputStreamReader isr = new InputStreamReader(input);
			BufferedReader brd = new BufferedReader(isr);
			String line;
			ArrayList<String> lines = new ArrayList<String>();
			
			if (numLines == 0) {
				while ((line = brd.readLine()) != null) {
					lines.add(line);
				}				
			} else if (numLines > 0) {
				for (int i = 0; i < numLines; i++) {
					line = brd.readLine();
					lines.add(line);
				}
			} else {
				return null;
			}
			brd.close();
			
			return lines.toArray(new String[lines.size()]);
			
		} catch (IOException e) {
			System.out.println("Error: " + e.getMessage());
			return null;
		}
		
	}
	
	
	public static String getFirstLineOfResourceFile(Class clazz, String fileName) {
		//
		
		try {			
			//InputStream input = clazz.getClassLoader().getResourceAsStream(fileName); //wrong
			InputStream input = clazz.getResourceAsStream(fileName);
			InputStreamReader isr = new InputStreamReader(input);
			BufferedReader brd = new BufferedReader(isr);
			String line = brd.readLine();
			input.close();
			isr.close();
			brd.close();
			return line;
		} catch (IOException e) {
			System.out.println("Error: " + e.getMessage());
			return null;
		}
		
	}

	public static String[] getFirstLinesOfResourceFiles(Class clazz, String[] fileNames) {
		// 
		String[] lines = new String[fileNames.length];
		for (int i = 0; i < fileNames.length; i++) {
			lines[i] = getFirstLineOfResourceFile(clazz, fileNames[i]);
		}
		return lines;
	}

	public static String[] getProcessMessages (Process p) {
		try {
			ArrayList<String> lines = new ArrayList<String>();
			BufferedReader input = new BufferedReader(new InputStreamReader(
					p.getInputStream()));
			String line = input.readLine();
			while (line != null) {
				lines.add(line);
				line = input.readLine();
			}
			
			BufferedReader error = new BufferedReader(new InputStreamReader(
					p.getErrorStream()));
			line = error.readLine();
			while (line != null) {
				lines.add(line);
				line = error.readLine();
			}
			
			input.close();
			error.close();
			
			return lines.toArray(new String[lines.size()]);
		} catch (IOException e) {
			System.out.println("Error: " + e.getMessage());
		}
		return null;
	}
	
}
