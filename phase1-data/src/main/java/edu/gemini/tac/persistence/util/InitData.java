package edu.gemini.tac.persistence.util;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import java.io.*;

/**
 * Utility to load a bunch of proposals into the database.
 * Uses a given committee id (1000) that has to be loaded when initializing the database.
 */
public class InitData {
    private static final Logger LOGGER = Logger.getLogger(InitData.class.getName());

    protected InitData() {}

    public static void main(String[] args) throws IOException {
    }

    public static void init(String path, String user, String database, String datasets, String envPath) {
        try {
            String[] cmd = {"./initialize_database.sh", user, database, datasets};
            ProcessBuilder builder = new ProcessBuilder(cmd);
            builder.directory(new File(path));
            builder.environment().put("PATH", envPath);

            final Process process = builder.start();
            InputStream is = process.getErrorStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
