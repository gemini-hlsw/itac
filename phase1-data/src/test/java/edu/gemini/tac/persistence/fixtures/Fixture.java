package edu.gemini.tac.persistence.fixtures;

import edu.gemini.tac.persistence.Proposal;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import javax.annotation.Resource;

/**
 * The mother of all fixtures.
 */
public class Fixture {
    private static final Logger LOGGER = Logger.getLogger(Fixture.class);

    @Resource(name = "sessionFactory")
    SessionFactory sessionFactory;

    public Proposal getProposalById(Session session, Long proposalId) {
        final Query query = session.createQuery("from Proposal where id = :proposalId").setLong("proposalId", proposalId);
        final Proposal proposal = (Proposal) query.uniqueResult();
        return proposal;
    }
}
