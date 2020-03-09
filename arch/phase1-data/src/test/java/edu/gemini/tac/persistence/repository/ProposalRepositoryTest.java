package edu.gemini.tac.persistence.repository;

import edu.gemini.tac.persistence.fixtures.FastHibernateFixture;
import edu.gemini.tac.persistence.fixtures.HibernateFixture;
import edu.gemini.tac.persistence.phase1.proposal.PhaseIProposal;
import org.apache.commons.lang.Validate;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Resource;

public class ProposalRepositoryTest extends FastHibernateFixture {
    @Resource
    ProposalRepository proposalRepository;

    @Before
    public void before() {
        super.teardown();
        super.before();
        phaseIProposals = getPhaseIProposals();

    }

    @Test
    public void canFindById() {
        final PhaseIProposal proposal = proposalRepository.findById(phaseIProposals.get(0).getId());
        Validate.notNull(proposal);
    }
}
