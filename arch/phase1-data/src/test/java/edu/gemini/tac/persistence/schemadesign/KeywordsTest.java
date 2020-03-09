package edu.gemini.tac.persistence.schemadesign;

import edu.gemini.model.p1.mutable.Keyword;
import edu.gemini.tac.persistence.Committee;
import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.fixtures.FastHibernateFixture;
import edu.gemini.tac.persistence.fixtures.HibernateFixture;
import edu.gemini.tac.persistence.fixtures.builders.ProposalBuilder;
import edu.gemini.tac.persistence.fixtures.builders.QueueProposalBuilder;
import edu.gemini.tac.persistence.phase1.TimeUnit;
import edu.gemini.tac.persistence.phase1.proposal.PhaseIProposal;
import edu.gemini.tac.persistence.repository.ProposalRepository;
import org.hibernate.classic.Session;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class KeywordsTest extends FastHibernateFixture {
    @Resource
    private ProposalRepository proposalRepository;
    private Long builtProposalId;

    @Test
    public void proposalsCanHaveMultipleKeywords() {
        
        
        PhaseIProposal p = getPhaseIProposal(5);
        Set<Keyword> newKeys = new HashSet<Keyword>();
        newKeys.add(Keyword.ASTROBIOLOGY);
        newKeys.add(Keyword.BINARIES);
        newKeys.add(Keyword.BULGES);
        p.setKeywords(newKeys);
        final Set<Keyword> keywords = p.getKeywords();
        assertNotNull(keywords);
        assertEquals(3, keywords.size());
        assertTrue(keywords.contains(Keyword.ASTROBIOLOGY));
        assertTrue(keywords.contains(Keyword.BINARIES));
        assertTrue(keywords.contains(Keyword.BULGES));
    }

    @Test
    public void keywordsCanBeUpdated() {
        Session session = getSessionFactory().openSession();
        session.getTransaction().begin();

        final Set<Keyword> setKeywords = new HashSet<Keyword>() {{
            add(Keyword.ABSORPTION_LINES);
        }};

        final Proposal proposal = new QueueProposalBuilder().
                setCommittee(getCommittee(0)).
                setPartner(getPartner("US")).
                setKeywords(setKeywords).
                addObservation("Vega", "00:00:00.000", "00:00:00.000", 1, TimeUnit.HR).
                create(session);
        builtProposalId = proposal.getId();

            try {
                final Proposal proposalById = getProposalById(session, builtProposalId);
                PhaseIProposal p = proposalById.getPhaseIProposal();
                final Long phaseIProposalId = p.getId();
                Set<Keyword> keywords = p.getKeywords();

                assertNotNull(keywords);
                assertTrue(! keywords.contains(Keyword.BLACK_HOLE_PHYSICS));
                assertEquals(1, keywords.size());
                keywords.add(Keyword.BLACK_HOLE_PHYSICS);

                session.saveOrUpdate(p);
                session.getTransaction().commit();
                session.close();

                session = getSessionFactory().openSession();
                session.getTransaction().begin();
                p = (PhaseIProposal) session.createQuery("from PhaseIProposal where id = :id").setLong("id", phaseIProposalId).uniqueResult();

                keywords = p.getKeywords();

                assertNotNull(keywords);
                assertEquals(2, keywords.size());
                assertTrue(keywords.contains(Keyword.BLACK_HOLE_PHYSICS));
            } finally {
                session.getTransaction().commit();
                session.close();
            }
        }
        public Set<Keyword> testVariations() {
        Keyword[] ks = Keyword.class.getEnumConstants();
        Set<Keyword> keywords = new HashSet<Keyword>(ks.length);
        for (Keyword k : ks) {
            keywords.add(k);
        }
        return keywords;
    }
}
