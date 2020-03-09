package edu.gemini.tac.exchange;

import edu.gemini.tac.persistence.fixtures.FastHibernateFixture;
import edu.gemini.tac.util.ProposalSchemaValidator;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.FileInputStream;
import java.io.IOException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/data-applicationContext.xml"})
public class ProposalSchemaValidatorTest {
    private static final Logger LOGGER = Logger.getLogger(ProposalSchemaValidatorTest.class.getName());

    @Test
    public void validateProposal() throws IOException {
//        ProposalSchemaValidator.validate(new FileInputStream("/users/fnussber/Downloads/2012B_test_proposals/ToO_proposal.xml"));
        ProposalSchemaValidator.validate(this.getClass().getResourceAsStream("testImport1.xml"));
    }

}
