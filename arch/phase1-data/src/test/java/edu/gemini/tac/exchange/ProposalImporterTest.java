package edu.gemini.tac.exchange;

import edu.gemini.tac.persistence.Committee;
import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.fixtures.FastHibernateFixture;
import edu.gemini.tac.persistence.phase1.proposal.PhaseIProposal;
import edu.gemini.tac.persistence.queues.Banding;
import edu.gemini.tac.persistence.queues.ScienceBand;
import edu.gemini.tac.util.ProposalImporter;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.List;

import static org.junit.Assert.assertEquals;


/**
 * Test class for import and export functionality from and to xml based phase1 proposals
 * used by the different tac committees to prepare the queue.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/data-applicationContext.xml"})
public class ProposalImporterTest extends FastHibernateFixture.Basic {
    private static final Logger LOGGER = Logger.getLogger(ProposalImporterTest.class.getName());

    private Long committeeId;

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Before
    public void myBefore() {
        committeeId = getCommittee(0).getId();
    }

    @Test
    public void importProposal() throws IOException {
        LOGGER.log(Level.DEBUG, "importSingleDocument()");

        ProposalImporter importer = new ProposalImporter(tmp.newFolder("pdfs"));
        long start = System.currentTimeMillis();
        importProposal(importer, "testImport1.xml", committeeId);
        long dur = System.currentTimeMillis() - start;
        LOGGER.log(Level.DEBUG, "Duration of import was " + dur + "ms");
        checkResults(
                new ProposalImporter.State[]{ProposalImporter.State.IMPORTED},
                importer.getResults());
    }

    @Test
    public void replaceProposal() throws IOException {
        LOGGER.log(Level.DEBUG, "replaceProposal()");

        final ProposalImporter importer1 = new ProposalImporter(tmp.newFolder("pdfs"));
        importProposal(importer1, "testImport1.xml", committeeId);
        checkResults(
                new ProposalImporter.State[]{ProposalImporter.State.IMPORTED},
                importer1.getResults());

        final ProposalImporter importer2 = new ProposalImporter(true, tmp.newFolder("pdfs"));
        importProposal(importer2, "testImport1.xml", committeeId);
        checkResults(
                new ProposalImporter.State[]{ProposalImporter.State.IMPORTED},
                importer2.getResults());
    }

    @Test (expected = RuntimeException.class)
    public void rejectDuplicateProposalKeyForPartner() throws IOException {
        LOGGER.log(Level.DEBUG, "rejectDuplicatePropsalKeyForPartner()");
        final ProposalImporter importer = new ProposalImporter(tmp.newFolder("pdfs"));
        importProposal(importer, "testImport1.xml", committeeId);  // same proposal key for same partner -> NO WAY MAN!
        importProposal(importer, "testImport1.xml", committeeId);  // this will fail horribly, badly, wrongly, anythingly
    }

    @Test
    public void merge2Proposals() throws IOException {
        LOGGER.log(Level.DEBUG, "merge2Proposals()");

        final ProposalImporter importer = new ProposalImporter(tmp.newFolder("pdfs"));
        importProposal(importer, "testImport1.xml", committeeId);  // same proposal key for two different partners -> merge
        importProposal(importer, "testImport2.xml", committeeId);
        Assert.assertEquals(2, importer.getSuccessfulCount());
        Assert.assertEquals(2, importer.getResults().size());
        checkResults(
                new ProposalImporter.State[]
                        {ProposalImporter.State.IMPORTED,
                         ProposalImporter.State.MERGED
                        },
                importer.getResults());
    }

    @Test
    public void merge4Proposals() throws Exception {
        LOGGER.log(Level.DEBUG, "merge4Proposals()");

        final ProposalImporter importer = new ProposalImporter(tmp.newFolder("pdfs"));
        importProposal(importer, "testImport1.xml", committeeId);  // same proposal key for four different partners -> merge and add
        importProposal(importer, "testImport2.xml", committeeId);
        importProposal(importer, "testImport3.xml", committeeId);
        importProposal(importer, "testImport4.xml", committeeId);
        Assert.assertEquals(4, importer.getSuccessfulCount());
        Assert.assertEquals(4, importer.getResults().size());
        checkResults(
                new ProposalImporter.State[]
                        {ProposalImporter.State.IMPORTED,
                         ProposalImporter.State.MERGED,
                         ProposalImporter.State.ADDED2JOINT,
                         ProposalImporter.State.ADDED2JOINT
                        },
                importer.getResults());
    }

    @Test
    public void mergeWithExistingProposal() throws IOException {
        LOGGER.log(Level.DEBUG, "mergeWithExistingProposal()");

        // create proposal
        final ProposalImporter importer1 = new ProposalImporter(tmp.newFolder("pdfs"));
        importProposal(importer1, "testImport1.xml", committeeId);
        checkResults(
                new ProposalImporter.State[]
                        {ProposalImporter.State.IMPORTED,
                        },
                importer1.getResults());

        // merge with existing proposal
        final ProposalImporter importer2 = new ProposalImporter(tmp.newFolder("pdfs"));
        importProposal(importer2, "testImport2.xml", committeeId);
        importProposal(importer2, "testImport3.xml", committeeId);
        checkResults(
                new ProposalImporter.State[]
                        {ProposalImporter.State.MERGED,
                         ProposalImporter.State.ADDED2JOINT
                        },
                importer2.getResults());
    }

    @Test
    public void addToExistingJointProposal() throws IOException {
        LOGGER.log(Level.DEBUG, "addToExistingJointProposal()");

        // remember initial number of proposals in database
        final int startProposalCount = getProposalCount(committeeId);

        // create joint proposal
        final ProposalImporter importer1 = new ProposalImporter(tmp.newFolder("pdfs"));
        importProposal(importer1, "testImport1.xml", committeeId);
        importProposal(importer1, "testImport2.xml", committeeId);
        checkResults(
                new ProposalImporter.State[]
                        {ProposalImporter.State.IMPORTED,
                                ProposalImporter.State.MERGED
                        },
                importer1.getResults());
        Assert.assertEquals(3, getProposalCount(committeeId) - startProposalCount);

        // add to existing joint proposal
        final ProposalImporter importer2 = new ProposalImporter(tmp.newFolder("pdfs"));
        importProposal(importer2, "testImport3.xml", committeeId);
        importProposal(importer2, "testImport4.xml", committeeId);
        checkResults(
                new ProposalImporter.State[]
                        {ProposalImporter.State.ADDED2JOINT,
                         ProposalImporter.State.ADDED2JOINT
                        },
                importer2.getResults());
        Assert.assertEquals(5, getProposalCount(committeeId) - startProposalCount);
    }

    @Test
    public void replaceExistingProposal() throws IOException {
        LOGGER.log(Level.DEBUG, "replaceExistingProposal()");

        // remember initial number of proposals in database
        final int startProposalCount = getProposalCount(committeeId);

        // create proposal
        final ProposalImporter importer1 = new ProposalImporter(tmp.newFolder("pdfs"));
        importProposal(importer1, "testImport1.xml", committeeId);
        checkResults(
                new ProposalImporter.State[]
                        {ProposalImporter.State.IMPORTED,
                        },
                importer1.getResults());
        Assert.assertEquals(1, getProposalCount(committeeId) - startProposalCount);

        // replace existing proposal
        final ProposalImporter importer2 = new ProposalImporter(true, tmp.newFolder("pdfs"));
        importProposal(importer2, "testImport1.xml", committeeId);
        checkResults(
                new ProposalImporter.State[]
                        {ProposalImporter.State.IMPORTED,
                        },
                importer2.getResults());
        Assert.assertEquals(1, getProposalCount(committeeId) - startProposalCount);
    }

    @Test
    public void replaceExistingJointProposal() throws IOException {
        LOGGER.log(Level.DEBUG, "replaceExistingJointProposal()");

        // remember initial number of proposals in database
        final int startProposalCount = getProposalCount(committeeId);

        // create joint proposal
        final ProposalImporter importer1 = new ProposalImporter(tmp.newFolder("pdfs"));
        importProposal(importer1, "testImport1.xml", committeeId);
        importProposal(importer1, "testImport2.xml", committeeId);
        checkResults(
                new ProposalImporter.State[]
                        {ProposalImporter.State.IMPORTED,
                                ProposalImporter.State.MERGED
                        },
                importer1.getResults());
        Assert.assertEquals(3, getProposalCount(committeeId) - startProposalCount);

        // replace existing joint proposal
        final ProposalImporter importer2 = new ProposalImporter(true, tmp.newFolder("pdfs"));
        importProposal(importer2, "testImport1.xml", committeeId);
        importProposal(importer2, "testImport2.xml", committeeId);
        checkResults(
                new ProposalImporter.State[]
                        {ProposalImporter.State.IMPORTED,
                         ProposalImporter.State.MERGED
                        },
                importer2.getResults());
        Assert.assertEquals(3, getProposalCount(committeeId) - startProposalCount);
    }

    // compare expected results with actual results, do some nice printout
    private void checkResults(ProposalImporter.State[] expected, List<ProposalImporter.Result> results) {
        Assert.assertEquals(expected.length, results.size());
        for (int i = 0; i < results.size(); i++) {
            LOGGER.log(Level.DEBUG, "Result[" + i + "]=" + results.get(i).getState().name() + " (expected=" + expected[i].name() + ")");
        }
        for (int i = 0; i < results.size(); i++) {
            Assert.assertEquals(expected[i], results.get(i).getState());
        }
    }

    /**
     * Reads a tared and zipped version of four proposals (testImport1.xml .. testImport4.xml).
     */
    @Test
    public void testReadMultipleFiles() throws IOException {
        final ProposalImporter importer = new ProposalImporter(tmp.newFolder("pdfs"));
        final String fileName = "/edu/gemini/tac/exchange/testImport.tar.gz";
        final InputStream inputStream = getClass().getResourceAsStream(fileName);
        final Session session = getSessionFactory().openSession();
        try {
            Committee committee = (Committee) session.createQuery("from Committee where id = "+committeeId).uniqueResult();
            importer.importDocuments(session, fileName, inputStream, committee);
            assertEquals(0, importer.getFailedCount());
            assertEquals(4, importer.getSuccessfulCount());
            assertEquals(4, importer.getResults().size());
            assertEquals(ProposalImporter.State.IMPORTED, importer.getResults().get(0).getState());
            assertEquals(ProposalImporter.State.MERGED, importer.getResults().get(1).getState());
            assertEquals(ProposalImporter.State.ADDED2JOINT, importer.getResults().get(2).getState());
            assertEquals(ProposalImporter.State.ADDED2JOINT, importer.getResults().get(3).getState());
        } finally {
            inputStream.close();
            session.close();
        }
    }

    /**
     * Tries to read a tared and zipped version of a completely broken xml file.
     * This test is mainly here to make sure that exceptions bubble all up to the top so that the caller
     * can handle the transactions accordingly.
     */
    @Test(expected = RuntimeException.class)
    public void doesNotImportBrokenTarFile() throws IOException {
        final ProposalImporter importer = new ProposalImporter(tmp.newFolder("pdfs"));
        // this tar file contains the same proposal twice and must therefore fail
        // (same submission key for same partner twice!)
        final String fileName = "/edu/gemini/tac/exchange/testImportBroken.tar.gz";
        final InputStream inputStream = getClass().getResourceAsStream(fileName);
        final org.hibernate.classic.Session session = getSessionFactory().openSession();
        try {
            Committee committee = (Committee) session.createQuery("from Committee where id = "+committees.get(0).getId()).uniqueResult();
            importer.importDocuments(session, fileName, inputStream, committee);
        } finally {
            inputStream.close();
            session.close();
        }
    }

    // cloning is essential for the north/south splitting of mixed proposals
    @Test
    @Transactional
    public void canSaveClone() throws IOException, JAXBException {
        final JAXBContext jaxbContext = JAXBContext.newInstance( "edu.gemini.model.p1.mutable" );
        final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        final edu.gemini.model.p1.mutable.Proposal mutableProposal = (edu.gemini.model.p1.mutable.Proposal) unmarshaller.unmarshal(getClass().getResourceAsStream("testMixedImport1.xml"));
        final PhaseIProposal phaseIProposal = PhaseIProposal.fromMutable(mutableProposal, getPartners());
        final PhaseIProposal clone = phaseIProposal.memberClone();
        getSessionFactory().getCurrentSession().saveOrUpdate(clone);
        getSessionFactory().getCurrentSession().flush();
    }

    @Test
    public void doesSplitMixedProposals() throws IOException {
        LOGGER.log(Level.DEBUG, "split mixed proposal");

        ProposalImporter importer = new ProposalImporter(tmp.newFolder("pdfs"));
        importProposal(importer, "testMixedImport1.xml", committeeId);
        checkResults(
                new ProposalImporter.State[]{
                        ProposalImporter.State.IMPORTED,
                        ProposalImporter.State.IMPORTED
                },
                importer.getResults());
    }

    @Test
    public void doesSplitComplexMixedProposals() throws IOException {
        LOGGER.log(Level.DEBUG, "split mixed proposal");

        ProposalImporter importer = new ProposalImporter(tmp.newFolder("pdfs"));
        importProposal(importer, "testMixedImportComplex.xml", committeeId);
        checkResults(
                new ProposalImporter.State[]{
                        ProposalImporter.State.IMPORTED,
                        ProposalImporter.State.IMPORTED
                },
                importer.getResults());
    }

    @Test
    public void doesSplitAndMergeMixedProposals() throws IOException {
        LOGGER.log(Level.DEBUG, "split mixed proposal");

        ProposalImporter importer = new ProposalImporter(tmp.newFolder("pdfs"));
        importProposal(importer, "testMixedImport1.xml", committeeId);
        importProposal(importer, "testMixedImport2.xml", committeeId);
        checkResults(
                new ProposalImporter.State[]{
                        ProposalImporter.State.IMPORTED,
                        ProposalImporter.State.IMPORTED,
                        ProposalImporter.State.MERGED,
                        ProposalImporter.State.MERGED
                },
                importer.getResults());
    }

    @Test
    public void canReplaceBandedProposal() throws IOException{
        LOGGER.log(Level.DEBUG, "Replace banded proposal");
        // remember initial number of proposals in database
        final int startProposalCount = getProposalCount(committeeId);

        // create proposal
        final ProposalImporter importer1 = new ProposalImporter(tmp.newFolder("pdfs"));
        importProposal(importer1, "testImport1.xml", committeeId);
        checkResults(
                new ProposalImporter.State[]
                        {ProposalImporter.State.IMPORTED,
                        },
                importer1.getResults());
        Assert.assertEquals(1, getProposalCount(committeeId) - startProposalCount);
        bandProposal(importer1.getResults().get(0).getProposalId());


        // replace existing proposal
        final ProposalImporter importer2 = new ProposalImporter(true, tmp.newFolder("pdfs"));
        importProposal(importer2, "testImport1.xml", committeeId);
        checkResults(
                new ProposalImporter.State[]
                        {ProposalImporter.State.IMPORTED,
                        },
                importer2.getResults());
        Assert.assertEquals(1, getProposalCount(committeeId) - startProposalCount);

    }

    @Test
    public void canReplaceBandedJointProposal() throws IOException{
        LOGGER.log(Level.DEBUG, "replaceExistingJointProposal()");

          // remember initial number of proposals in database
          final int startProposalCount = getProposalCount(committeeId);

          // create joint proposal
          final ProposalImporter importer1 = new ProposalImporter(tmp.newFolder("pdfs"));
          importProposal(importer1, "testImport1.xml", committeeId);
          importProposal(importer1, "testImport2.xml", committeeId);
          checkResults(
                  new ProposalImporter.State[]
                          {ProposalImporter.State.IMPORTED,
                                  ProposalImporter.State.MERGED
                          },
                  importer1.getResults());
          Assert.assertEquals(3, getProposalCount(committeeId) - startProposalCount);
        final Long componentProposalId = importer1.getResults().get(0).getProposalId();
        final Long proposalId = getJointProposalParentOf(componentProposalId).getId();
        bandProposal(proposalId);

          // replace existing joint proposal
          final ProposalImporter importer2 = new ProposalImporter(true, tmp.newFolder("pdfs"));
          importProposal(importer2, "testImport1.xml", committeeId);
          importProposal(importer2, "testImport2.xml", committeeId);
          checkResults(
                  new ProposalImporter.State[]
                          {ProposalImporter.State.IMPORTED,
                           ProposalImporter.State.MERGED
                          },
                  importer2.getResults());
          Assert.assertEquals(3, getProposalCount(committeeId) - startProposalCount);

    }

    private Proposal getJointProposalParentOf(long componentProposalId){
        Session session = sessionFactory.openSession();
        Proposal p = (Proposal) session.createQuery("from Proposal where id = " + componentProposalId).uniqueResult();
        return p.getJointProposal();
    }

    private Banding bandProposal(Long proposalId){
        Session session = sessionFactory.openSession();
        Proposal p = (Proposal) session.createQuery(
                "from Proposal where id = " + proposalId).
                uniqueResult();
        Banding b = new Banding(p, ScienceBand.BAND_ONE);
        session.save(b);
        session.flush();
        session.close();
        return b;
    }


// ProposalBuilder does not work for this kind of test (problems with investigators), not sure if it's worth getting this to work
//    @Test
//    @Transactional
//    public void canDo() throws IOException {
//        Proposal p = new QueueProposalBuilder(getPrincipalInvestigators().get(0)).
////                setCommittee(ProposalBuilder.dummyCommittee).
////                setPartner(ProposalBuilder.dummyPartnerUS).
//                setCommittee(getCommittee(0)).
//                setPartner(getPartner(0)).
//                addObservation("0:0:0.0", "0:0:0.0", 1, TimeUnit.HR).
//                create(sessionFactory.getCurrentSession());
//        ProposalExporterImpl exporter = new ProposalExporterImpl();
//        byte[] xml = exporter.getAsXml(p);
//        System.out.println(xml.toString());
//
//        ProposalImporter importer = new ProposalImporter();
//        importer.importSingleDocument(sessionFactory.getCurrentSession(), "test.xml", new ByteArrayInputStream(xml), getCommittee(0));
//    }

    @Resource(name = "sessionFactory")
    SessionFactory sessionFactory;
    private void importProposal(ProposalImporter importer, String fileName, Long committeeId) throws IOException {
        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();
        InputStream inputStream = getClass().getResourceAsStream(fileName);
        try {
            Committee committee = (Committee) session.createQuery("from Committee where id = " + committeeId).uniqueResult();
            Validate.notNull(committee);
            importer.importDocuments(session, fileName, inputStream, committee);
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw new RuntimeException(e); // rethrow after rolling transaction back -> make sure test fails
        } finally {
            inputStream.close();
            session.close();
        }
    }

    public int getProposalCount(Long committeeId) {
        Session session = sessionFactory.openSession();
        BigInteger count = (BigInteger) session.createSQLQuery(
                "select count(*) from proposals").
                uniqueResult();
        session.close();
        return count.intValue();
    }
}
