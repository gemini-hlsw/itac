package edu.gemini.tac.persistence.util;

import edu.gemini.tac.persistence.Committee;
import edu.gemini.tac.util.ProposalImporter;
import edu.gemini.tac.util.ProposalUnwrapper;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;

import org.springframework.core.io.FileSystemResource;

import java.io.*;
import java.util.List;

/**
 * Utility to load a bunch of proposals into the database.
 * Uses a given committee id (1000) that has to be loaded when initializing the database.
 */
public class LoadData {

    private static final org.apache.log4j.Logger LOGGER = Logger.getLogger(LoadData.class.getName());

    protected LoadData() {}

    public static void main(String[] args) throws IOException {

        if (args.length != 2) {
            throw new RuntimeException("invalid number of arguments");
        }

        String configFile = args[0];
        String inputFile = args[1];

        System.out.println("Reading configuration from file: " + configFile);
        System.out.println("Importing data from file:        " + inputFile);

        XmlBeanFactory beanFactory = new XmlBeanFactory(new ClassPathResource("/data-applicationContext.xml"));
        PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
        ppc.setLocation(new FileSystemResource(configFile));
        ppc.postProcessBeanFactory(beanFactory);

        SessionFactory sessionFactory = (SessionFactory) beanFactory.getBean("sessionFactory");
        InputStream inputStream = new FileInputStream(inputFile);
        importProposalsFromStream(inputStream, inputFile, 1000L, sessionFactory);
    }

    public static List<ProposalImporter.Result> importProposalsFromStream(
            InputStream inputStream,
            String inputFile,
            Long committeeId,
            SessionFactory sessionFactory) {

        ProposalImporter importer = new ProposalImporter(new File("/tmp"));
        ProposalUnwrapper unwrapper = null;
        try {
           unwrapper = new ProposalUnwrapper(inputStream, inputFile);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e2) {
                throw new RuntimeException(e2.getMessage(), e2);
            }
        }

        Session  session = sessionFactory.openSession();
        for (ProposalUnwrapper.Entry file : unwrapper.getEntries()) {

            LOGGER.log(Level.ALL, "importing: " + file.getFileName());

            Transaction tx = session.beginTransaction();
            Committee committee = (Committee) session.createQuery("from Committee c where c.id=:committeeId").setParameter("committeeId", committeeId).uniqueResult();
            try {
                importer.importSingleDocument(session, file, committee);
                tx.commit();
            } catch (Exception e) {
                LOGGER.log(Level.ALL, e);
                tx.rollback();
                LOGGER.error("ignoring proposal: " + e.getMessage(), e);
            }
        }
        session.close();

        LOGGER.log(Level.ALL, importer.getStatistics());

        return importer.getResults();
    }

}
