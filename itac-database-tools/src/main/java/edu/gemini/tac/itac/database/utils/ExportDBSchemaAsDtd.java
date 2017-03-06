package edu.gemini.tac.itac.database.utils;

import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.xml.FlatDtdDataSet;

public class ExportDBSchemaAsDtd {
    public static void main(String[] args) throws Exception
    {
		System.out.println("args:" + args);
		Options options = new Options();
		options.addOption("d", "database", true, "Target database to export the schema from, ie, itac_dev, itac_test, itac_qa...");
		options.addOption("f", "outputFile", true, "File to write to");
		
		HelpFormatter formatter = new HelpFormatter();
		if (args.length < 2){
			formatter.printHelp( "ImportProposals", options );
			System.exit(-1);
		}

		
		final CommandLineParser parser = new PosixParser();
		final CommandLine line = parser.parse( options, args );
		
		System.out.println("exporting database " + line.getOptionValue("d"));
        // database connection
        Connection jdbcConnection = DriverManager.getConnection(
                "jdbc:postgresql:" + line.getOptionValue("d"),  "itac", "");
        IDatabaseConnection connection = new DatabaseConnection(jdbcConnection);

        // write DTD file
        FlatDtdDataSet.write(connection.createDataSet(), new FileOutputStream(line.getOptionValue("f")));
    }
}
