package edu.gemini.tac.itac.database.utils;

import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.database.QueryDataSet;
import org.dbunit.database.search.TablesDependencyHelper;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.util.search.SearchException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: lobrien
 * Date: Nov 15, 2010
 */
public class DbUnitExportAsXml {
    public static void main(String[] args) throws Exception {
        // database connection
        Class driverClass = Class.forName("org.postgresql.Driver");
        Connection jdbcConnection = DriverManager.getConnection(
                "jdbc:postgresql:itac_dev", "itac", "");
        IDatabaseConnection connection = new DatabaseConnection(jdbcConnection);
        FullDatabaseExport(connection);


        String tableName = "p1_documents";
        //DependentsExport(connection, tableName);
        printDependentsOn(connection, tableName, 0);

    }

    private static void DependentsExport(IDatabaseConnection connection, String tableName) throws SearchException, SQLException, DataSetException, IOException {
        //Committee & Dependencies
        String[] depTableNames =
                TablesDependencyHelper.getDependentTables(connection, tableName);
        QueryDataSet depDataset = new QueryDataSet(connection);
        depDataset.addTable(tableName, "select * from " + tableName + " limit 1");
        //IDataSet depDataset = connection.createDataSet(depTableNames);
        for(String tN : depTableNames){
            if(tN.equals(tableName) == false){
                depDataset.addTable(tN);
            }
        }
        FlatXmlDataSet.write(depDataset, new FileOutputStream(tableName + "_and_dependents_partial.xml"));
    }

    static ArrayList<String> printedNames = new ArrayList<String>();

    private static void printDependentsOn(IDatabaseConnection connection, String tableName, int depth) throws SearchException{
        String [] depTableNames = TablesDependencyHelper.getDependentTables(connection, tableName);
        for(String tN : depTableNames){
            for(int i = 0; i < depth; i++){
                System.out.print("   ");
            }
            if(tN.equals(tableName))
            {
                continue;
            }
            if(printedNames.contains(tN) == false){
                System.out.println(tN);
                printedNames.add(tN);
                printDependentsOn(connection, tN, depth + 1);
            }else{
                System.out.println("->" + tN);
            }
        }
    }


    private static void FullDatabaseExport(IDatabaseConnection connection) throws SQLException, IOException, DataSetException {
        // full database export
        IDataSet fullDataSet = connection.createDataSet();
        System.out.println(System.getProperty("user.dir"));
        FlatXmlDataSet.write(fullDataSet, new FileOutputStream("full.xml"));
    }

}
