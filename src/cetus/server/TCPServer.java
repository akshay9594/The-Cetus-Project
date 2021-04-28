package cetus.server;

import java.net.*;
import java.io.*;

public class TCPServer {
	private static int port = 4444, maxConnections = 0;
	private static String CETUS = "1";
	private static String CORES = "2";
	private static String SPEEDUP = "3";

	public static void main(String[] args) throws IOException {
		// private static int port=4444, maxConnections=0;
		int i = 0;
		try {
			ServerSocket serverSocket = new ServerSocket(4444);

			Socket clientSocket;

			while ((i++ < maxConnections) || (maxConnections == 0)) {
				doComms connection;
				
				System.out.println("*** Connection #" + i);

				clientSocket = serverSocket.accept();

				doComms conn_c = new doComms(clientSocket);
				Thread t = new Thread(conn_c);
				t.start();
			}
		} catch (IOException e) {
			System.err.println("Accept failed.");
			e.printStackTrace();
			// System.exit(1);
		}
	}

	public static String convertStreamToString(java.io.InputStream is) {
		java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
		return s.hasNext() ? s.next() : "";
	}

	private static String readfile(String filePath, File file) {

		long length = file.length();

		StringBuffer fileData = new StringBuffer((int) length + 30);

		// fileData.insert(0, b);
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(filePath));

			char[] buf = new char[1024];
			int numRead = 0;
			while ((numRead = reader.read(buf)) != -1) {
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

	static class doComms implements Runnable {
		private Socket clientSocket;

		// private String line, input;

		doComms(Socket clientSocket) {
			this.clientSocket = clientSocket;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				// String dirName = String.valueOf(System.currentTimeMillis());
				// InetAddress user_ip = clientSocket.getInetAddress();
				// InetAddress user_localip = clientSocket.getLocalAddress();
				InetAddress user_dir = clientSocket.getInetAddress();
				String user_dir_full = "/cetus_server/users" + user_dir;
				// String user_dir = "";
				// System.out.println("user_ip:"+user_ip+"local-ip"+user_localip+"  "+clientSocket.getRemoteSocketAddress());
				PrintWriter out = new PrintWriter(
						clientSocket.getOutputStream(), true);
				BufferedReader in = new BufferedReader(new InputStreamReader(
						clientSocket.getInputStream()));
				// String inputLine, outputLine;
				// KnockKnockProtocol kkp = new KnockKnockProtocol();

				// outputLine = kkp.processInput(null);
				String cmd = in.readLine(); // read in cmd
				System.out.println(cmd);
				if (cmd.equals(CETUS)) {

					int filelen = Integer.parseInt(in.readLine());
					System.out.println("File size: "+filelen);
					String name = in.readLine();
					String fileName = user_dir_full + "/" + name;
					String outdir = user_dir_full + "/output/" + name;
					String flags = in.readLine();

					StringBuffer cmdin = new StringBuffer(filelen);

					char[] buf = new char[1024];
					int numRead = 0;
					int count = 0;
					while ((numRead = in.read(buf)) != -1 && count < filelen) {
						String readData = String.valueOf(buf, 0, numRead);
						// System.out.println(readData);
						cmdin.append(readData);
						count += numRead;
						// System.out.println(count);
						if (count >= filelen) {
							break;
						}
						buf = new char[1024];
					}
					String filein = cmdin.toString();

					System.out.println("name=" + fileName + "\n"); // + filein

					// Create file
					// fileName
					File file = new File(fileName);
					if (!file.exists()) {
						file.getParentFile().mkdirs();
						file.createNewFile();
					}
					File outfiledir = new File(user_dir_full + "/output/");// ("./"+user_dir+"/output");
					if (!outfiledir.exists()) {
						outfiledir.getParentFile().mkdirs();
						outfiledir.mkdir();
						// outfile.createNewFile();
					}
					FileWriter fstream = new FileWriter(fileName);
					BufferedWriter fileout = new BufferedWriter(fstream);
					fileout.write(filein);
					// Close the output stream
					fileout.close();

					System.out.println("java -jar /apps/cetus/r22/cetus.jar "
							+ flags + " -outdir=" + user_dir_full + "/output"
							+ " " + fileName);
					Process proc = Runtime.getRuntime().exec(
							"java -jar /apps/cetus/r22/cetus.jar "
									+ " -outdir=" + user_dir_full + "/output"
									+ " " + flags + " " + fileName);

					// Then retreive the process output
					InputStream inputStream = proc.getInputStream();
					String terminal_out = convertStreamToString(inputStream);
					// StringWriter writer = new StringWriter();
					// IOUtils.copy(inputStream, writer, encoding);
					// String theString = writer.toString();
					InputStream errStream = proc.getErrorStream();
					String terminal_err = convertStreamToString(errStream);
					System.out.println(terminal_out);
					System.out.println("err" + terminal_err);

					// String filepath ="/cetus-server"+user_dir+"/output"+
					// fileName;
					File outfile = new File(outdir);

					String outfilearray = readfile(outdir, outfile);
					outfilearray = terminal_out + "\n" + terminal_err + "\n"
							+ "file_start\n" + outfilearray + "file_end\n";
					out.println(outfilearray);

				} else if (cmd.equals(CORES)) {
					out.println(Runtime.getRuntime().availableProcessors());

				} else if (cmd.equals(SPEEDUP)) {

					String fileName = in.readLine();
					String speedcmd = in.readLine();
					speedcmd = " -in_file=./" + fileName
							+ " -out_file=./output/" + fileName + speedcmd;
					System.out.println("java -jar cetus_speed_up.jar "
							+ speedcmd);
					Process proc = Runtime.getRuntime().exec(
							"java -jar cetus_speed_up.jar " + speedcmd);
					// out.println(speedcmd);

					InputStream inputStream = proc.getInputStream();
					String terminal_out = convertStreamToString(inputStream);
					// StringWriter writer = new StringWriter();
					// IOUtils.copy(inputStream, writer, encoding);
					// String theString = writer.toString();
					InputStream errStream = proc.getErrorStream();
					String terminal_err = convertStreamToString(errStream);
					System.out.println(terminal_out);
					System.out.println("err\n" + terminal_err);
					out.print(terminal_out);
					out.print(terminal_err);

				}

				out.close();
				in.close();
				clientSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// serverSocket.close();
		}
	}

}
