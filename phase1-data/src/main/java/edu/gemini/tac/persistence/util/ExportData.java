package edu.gemini.tac.persistence.util;

import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;

import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;

/**
 * It basically exists in order to dump the database into
 * the flat xml file that DbUnit understands.  I do this from time to time in order to
 * build test fixtures that make sense.
 * 
 * @author ddawson
 *
 */
public class ExportData {
    protected ExportData() {}

	public static void main(String[] args) throws Exception
	{
		// database connection
		Connection jdbcConnection = DriverManager.getConnection(
				"jdbc:postgresql:itac_test", "itac", "");
		IDatabaseConnection connection = new DatabaseConnection(jdbcConnection);
//		ITableFilter filter = new DatabaseSequenceFilter(connection);
//		IDataSet dataSet = new FilteredDataSet(filter, connection.createDataSet());

		// full database export
		IDataSet fullDataSet = connection.createDataSet();
		FlatXmlDataSet.write(fullDataSet, new FileOutputStream("phase1database.xml"));
	}
}
