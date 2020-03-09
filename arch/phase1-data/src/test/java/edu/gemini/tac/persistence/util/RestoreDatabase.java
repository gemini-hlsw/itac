package edu.gemini.tac.persistence.util;


import org.apache.log4j.Logger;

import java.io.*;

/**
 * Utility to create and load
 */
public class RestoreDatabase {

    private static final Logger LOGGER = Logger.getLogger(RestoreDatabase.class.getName());

    public static void main(String[] args) throws IOException {
        System.out.println("==== RESTORE DATABASE ====");
        restore(args[0], args[1], args[2], args[3]);
        System.out.println("==== RESTORE DATABASE DONE ====");
    }

    public static void restore(String inDirName, String inFileName, String databaseUser, String databaseName) {
        try {
            final File dir = new File(inDirName);

            final String[] cmdParts = {"pg_restore", "-c", "-U"+databaseUser, "-d"+databaseName, dir.getAbsolutePath()+File.separator+inFileName+".dump"};
            final StringBuffer cmdLine = new StringBuffer();
            for (String s : cmdParts) {
                cmdLine.append(s).append(" ");
            }

            System.out.println("cmd  : " + cmdLine.toString());
            System.out.println("path : " + System.getenv("PATH"));

            final ProcessBuilder builder = new ProcessBuilder(cmdParts);
            final Process process = builder.start();
            final InputStream is = process.getErrorStream();
            final InputStreamReader isr = new InputStreamReader(is);
            final BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        } catch (Exception e) {
            LOGGER.error(
                    String.format(
                    "Could not restore database with inDirName=%s, inFileName=%s, databaseUser=%s, databaseName=%s",
                            inDirName, inFileName, databaseUser, databaseName));
            throw new RuntimeException(e);
        }
    }


}
