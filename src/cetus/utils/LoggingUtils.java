package cetus.utils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class LoggingUtils {

    public static void redirectOutput(String... args) {
        String redirectFileStr = null;
        for (String arg : args) {
            if (arg.contains("outputFile=")) {
                String[] argPart = arg.split("=");
                redirectFileStr = argPart[1];
            }
        }

        try {
            PrintStream stream = new PrintStream(new File(redirectFileStr));
            OutputStream originalOutput = System.out;
            PrintStream twoChannelStream = new PrintStream(new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    originalOutput.write(b);
                    stream.write(b);
                }

                @Override
                public void close() throws IOException {
                    originalOutput.close();
                    stream.close();
                }
            });

            System.setOut(twoChannelStream);

        } catch (Exception e) {
            System.out.println("It was not able to replace output stream");
        }
    }
}
