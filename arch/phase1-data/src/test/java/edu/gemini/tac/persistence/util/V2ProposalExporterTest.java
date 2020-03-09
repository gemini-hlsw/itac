package edu.gemini.tac.persistence.util;

import edu.gemini.model.p1.mutable.Proposal;
import edu.gemini.tac.persistence.fixtures.HibernateFixture;
import edu.gemini.tac.persistence.phase1.proposal.PhaseIProposal;
import edu.gemini.tac.util.HibernateToMutableConverter;
import edu.gemini.tac.util.V2ProposalExporter;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class V2ProposalExporterTest extends HibernateFixture {
    private static final Logger LOGGER = Logger.getLogger(HibernateFixture.class);

    Long proposalId;

    @Before
    public void before() {
        super.teardown();
        super.before();
        createCommitteeAndProposalsFixture();
        proposalId = importDemoFile();
    }

    @After
    public void teardown() {
        super.teardown();
    }

    @Test
    public void writeToTmp() {
        final V2ProposalExporter v2ProposalExporter = new V2ProposalExporter();

        final Session session = getSessionFactory().openSession();
        session.getTransaction().begin();
        try {
            final Query query = session.createQuery("from PhaseIProposal p " +
                    "join fetch p.targets t " +
                    "join fetch t.coordinates c " +
                    "where p.id = :id").setLong("id", proposalId);
            final PhaseIProposal hp = (PhaseIProposal) query.uniqueResult();
            final Proposal proposal = HibernateToMutableConverter.toMutable(hp);
            v2ProposalExporter.export(proposal);

            LOGGER.info("Proposal exported to /tmp/crap.xml");
            Assert.assertTrue(true);
        } finally {
            session.close();
        }
    }
}
