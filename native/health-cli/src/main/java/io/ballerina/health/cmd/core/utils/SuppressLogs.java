package io.ballerina.health.cmd.core.utils;

import java.io.OutputStream;
import java.io.PrintStream;

public class SuppressLogs {


        public static void main(String[] args) {
            // Create a custom print stream that discards the output
            PrintStream nullPrintStream = new PrintStream(new NullOutputStream());

            // Save the original standard output and error streams
            PrintStream originalOut = System.out;
            PrintStream originalErr = System.err;

            // Redirect the standard output and error streams to the custom print stream
            System.setOut(nullPrintStream);
            System.setErr(nullPrintStream);

            // Perform your application logic
            // ...

            // Restore the original standard output and error streams
            System.setOut(originalOut);
            System.setErr(originalErr);
        }

        public static class NullOutputStream extends OutputStream {
            @Override
            public void write(int b) {
                // Do nothing, discard the output
            }
        }

}
