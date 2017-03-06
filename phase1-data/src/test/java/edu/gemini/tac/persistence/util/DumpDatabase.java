package edu.gemini.tac.persistence.util;


import edu.gemini.tac.persistence.Committee;
import edu.gemini.tac.persistence.fixtures.HibernateFixture;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.*;

/**
 * Utility to create and load
 */
public class DumpDatabase {
    
    public static final String DUMP_WITHOUT_PROPOSALS = "committees";
    public static final String DUMP_WITH_PROPOSALS    = "committeesAndProposals";
    public static final String DUMP_WITH_QUEUES = "committeesAndProposalsAndQueue";
    public static final String DUMP_2012B_PRE_MEETING = "2014Aproposals";
    public static final String DUMP_2012B_POST_MEETING = "2012Bqueues";

    private static final Logger LOGGER = Logger.getLogger(DumpDatabase.class.getName());

    public static void main(String[] args) throws IOException {
        // Spring setup
        XmlBeanFactory beanFactory = new XmlBeanFactory(new ClassPathResource("/data-applicationContext.xml"));
        PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
        ppc.setLocation(new ClassPathResource("/itac.properties"));
        ppc.postProcessBeanFactory(beanFactory);
        SessionFactory sessionFactory = (SessionFactory) beanFactory.getBean("sessionFactory");

        createDump(sessionFactory, args[0], DUMP_WITHOUT_PROPOSALS, args[1], args[2]);
        createDump(sessionFactory, args[0], DUMP_WITH_PROPOSALS, args[1], args[2]);
        createDump(sessionFactory, args[0], DUMP_WITH_QUEUES, args[1], args[2]);
        createDumpWithImportData(sessionFactory, args[0], DUMP_2012B_PRE_MEETING, args[1], args[2]);
        copyDump(args[0], DUMP_2012B_POST_MEETING, args[1], args[2]);
    }

    public static void copyDump(String outdirName, String dump, String databaseUser, String databaseName){

    }
    
    public static void dump(String outDirName, String outFileName, String databaseUser, String databaseName) {
        try {

            final File dir = new File(outDirName);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            final String[] cmdParts = {"pg_dump", "-U"+databaseUser, "-Fcustom", "-f"+dir.getAbsolutePath()+File.separator+outFileName+".dump", databaseName};
            final StringBuffer cmdLine = new StringBuffer();
            for (String s : cmdParts) {
                cmdLine.append(s).append(" ");
            }

            System.out.println("creating dump  : " + cmdLine.toString());
            //System.out.println("path : " + System.getenv("PATH"));

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
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a dump that only uses the database elements that are created in the HibernateFixture.
     * @param sessionFactory
     * @param outDirName
     * @param dump
     * @param databaseUser
     * @param databaseName
     */
    private static void createDump(SessionFactory sessionFactory, String outDirName, String dump, String databaseUser, String databaseName) {

        System.out.println("==== CREATING FIXTURE " + dump + " ====") ;
        HibernateFixture fixture = new HibernateFixture(sessionFactory);
        fixture.teardown(); // empty all tables (otherwise partners and other stuff will be inserted twice)
        fixture.before();
        if (dump.equals(DUMP_WITH_QUEUES)) {
            fixture.createCommitteeAndQueueFixture();
        } else if (dump.equals(DUMP_WITH_PROPOSALS)) {
            fixture.createCommitteeAndProposalsFixture();
        } else if (dump.equals(DUMP_WITHOUT_PROPOSALS)) {
            fixture.createEmptyCommitteesFixture();
        }
        dump(outDirName, dump, databaseUser, databaseName);
        System.out.println("==== DONE ====");
    }

    /**
     * Creates a dump using a very basic HibernateFixture (committees) and an imported set of proposals from
     * the resources.
     * @param sessionFactory
     * @param outDirName
     * @param dump
     * @param databaseUser
     * @param databaseName
     */
    private static void createDumpWithImportData(SessionFactory sessionFactory, String outDirName, String dump, String databaseUser, String databaseName) {
        System.out.println("==== CREATING IMPORT FIXTURE " + dump + " ====") ;
        HibernateFixture fixture = new HibernateFixture(sessionFactory);
        fixture.teardown(); // empty all tables (otherwise partners and other stuff will be inserted twice)
        fixture.before();
        fixture.createEmptyCommitteesFixture();
        Committee committee = fixture.getCommittees().get(0);
        String fileName = "/edu/gemini/tac/persistence/fixtures/"+dump+".tar.gz";
        InputStream inputStream = DumpDatabase.class.getClass().getResourceAsStream(fileName);
        Validate.notNull(inputStream);
        LoadData.importProposalsFromStream(inputStream, fileName, committee.getId(), sessionFactory);
        //inputStream.close();
        dump(outDirName, dump, databaseUser, databaseName);
        System.out.println("==== DONE ====");
    }

}
