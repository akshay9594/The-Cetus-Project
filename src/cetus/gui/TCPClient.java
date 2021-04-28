

package cetus.gui;
import java.io.*;
import java.net.*;


//parhub.ecn.purdue.edu purd1234
public class TCPClient {
  //  public static void main(String[] args) throws IOException {
	private static int CETUS = 1;
	private static int CORE = 2;
	private static int SPEEDUP = 3;
	public static String hostname = "parhub.ecn.purdue.edu";//"localhost";//"sslab05.cs.purdue.edu";
	
	public static String[] speedup(String cmdin){
		
		Socket kkSocket = null;
        PrintWriter out = null;
        BufferedReader in = null;
        //int result[] = new int[] {-1,-1};
        try {
        	System.out.println("Connecting Cetus remote server...");
            kkSocket = new Socket( hostname, 4444);
            out = new PrintWriter(kkSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(kkSocket.getInputStream()));
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host");
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection");
            e.printStackTrace();
            return null;
        }
        
        String cmd = SPEEDUP + "\n" +CetusGUI.inputFile.getName() +"\n"+ cmdin;
	    out.println(cmd);
	    String returnvalue[] = new String[] {"-1","-1",""};
	    String fromServer;
	    try {
			while ((fromServer = in.readLine()) != null) {
				System.out.println(fromServer);
				returnvalue[2]= returnvalue[2]+fromServer+"\n";
				if(fromServer.length()>12){
				if(fromServer.substring(0, 12).equals("output_time=")){
					returnvalue[1]=fromServer.substring(12, fromServer.length());
					
				}else if(fromServer.substring(0, 11).equals("input_time=")){
					returnvalue[0]=fromServer.substring(11, fromServer.length());
					
				}
				
				
				
				}
				
			}
			out.close();
	        in.close();
	        kkSocket.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	    
	    
		return returnvalue;
	}
	
	public static int numcore(){
		
		Socket kkSocket = null;
        PrintWriter out = null;
        BufferedReader in = null;
        
        try {
        	System.out.println("Connecting Cetus remote server...");
            kkSocket = new Socket( hostname, 4444);
            out = new PrintWriter(kkSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(kkSocket.getInputStream()));
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host");
            e.printStackTrace();
            return -1;
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection");
            e.printStackTrace();
            return -1;
       }
        
        out.println(CORE);
        
		try {
	        
			int cores = Integer.parseInt(in.readLine());
			out.close();
	        in.close();
	        kkSocket.close();
	        return cores;
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;
	}
	
	
	public static void request(String[] args) throws IOException{
		
		int i = args.length;
        Socket kkSocket = null;
        PrintWriter out = null;
        BufferedReader in = null;
        if(args.length<1){return;}
        String filePath = args[i-1];
        File file = new File(filePath);
        String fileName= file.getName();
        String fileS = readfile(filePath,file);
        int length = fileS.length();
        String flags= "";
        // /home/cetus/wwwcetus/public/WebApp/cetus_output
        String outdir = "output/"+file.getName();
		for (i=0;i<args.length-2;i++){
			
			if(args[i].contains("-outdir")){
				outdir = args[i].substring(8,args[i].length());
			continue;
		}else if (args[i].contains("-preprocessor")){
			continue;
			
		}
			flags = flags+ " " + args[i];
			
		}
        fileS = fileName+"\n"+flags+"\n"+fileS;
        fileS = CETUS+"\n"+length+"\n"+fileS;
        //System.out.println(fileS.length());
        //System.out.println(fileS);
        try {
        	System.out.println("Connecting Cetus remote server...");
            kkSocket = new Socket( hostname, 4444);
            out = new PrintWriter(kkSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(kkSocket.getInputStream()));
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host");
            e.printStackTrace();
            return;
           // System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection");
            e.printStackTrace();
            return;
            //System.exit(1);
        }

        
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        String fromServer;
        String fromUser;
        out.println(fileS);

        if(!CetusGUI.openMpFile.exists()){
        	CetusGUI.openMpFile.getParentFile().mkdirs();
        	CetusGUI.openMpFile.createNewFile();
        }
        FileWriter fstream = new FileWriter(CetusGUI.openMpFile);
        
		BufferedWriter fileout = new BufferedWriter(fstream);
		//fileout.write(filein);
		boolean read = false;
        while ((fromServer = in.readLine()) != null) {
            if(fromServer.equals("file_start")){
            	read = true;
            	continue;
            }
            
            if(fromServer.equals("file_end")){
            	read = false;
            	continue;
            }
            if(read&&(!fromServer.equals("file_end"))){
            	
            	fileout.write(fromServer+"\n");
            	//System.out.println(fromServer);
            }else{
            	System.out.println(fromServer);
            }
            

           // if (fromServer.equals("Bye."))
           //     break;
		    
       /*     fromUser = stdIn.readLine();
	    if (fromUser != null) {
                System.out.println("Client: " + fromUser);
                out.println(file);
	    }*/
        }
        fileout.close();

        out.close();
        in.close();
        stdIn.close();
        kkSocket.close();
    }
    
    private static String readfile(String filePath, File file){
    	
    	long length = file.length();
    	
        StringBuffer fileData = new StringBuffer((int) length+30);
        
        //fileData.insert(0, b);
        BufferedReader reader;
		try {
			reader = new BufferedReader(
			        new FileReader(filePath));
		
        char[] buf = new char[1024];
        int numRead=0;
        while((numRead=reader.read(buf)) != -1){
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[1024];
        }
        reader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return fileData.toString();
    	
    	
    }
    }



