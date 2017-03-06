package edu.gemini.tac.persistence;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/* v2 : ConstraintSet becomes Proposal.conditions : List[Condition]
        Making this a no-op test.

        This is dead code. Delete on Feb 1, 2012?
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/data-applicationContext.xml"})
public class ConstraintSetTest {
    @Test
    public void nopTest(){}
}
