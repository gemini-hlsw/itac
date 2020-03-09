package edu.gemini.tac.persistence.repository;

import edu.gemini.tac.persistence.phase1.proposal.PhaseIProposal;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Repository
@Transactional
public class ProposalRepository {
    private static final Logger LOGGER = Logger.getLogger(ProposalRepository.class);

    @Resource(name = "sessionFactory")
    private SessionFactory sessionFactory;

    public PhaseIProposal findById(final Long id) {
        final Session currentSession = sessionFactory.getCurrentSession();
        final Query query = currentSession.getNamedQuery("phaseIProposal.findById").setLong("id", id);

        return (PhaseIProposal) query.uniqueResult();
    }
}
