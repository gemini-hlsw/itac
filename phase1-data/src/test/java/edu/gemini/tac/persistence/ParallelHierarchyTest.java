package edu.gemini.tac.persistence;

import org.hibernate.SessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.io.FileNotFoundException;


/**
 * The original intent of this test was to make sure that the then-new data model could work with the parallel
 * p1 model.
 *
 * Since we now use the XML DTD as the ground truth, the closest equivalent to this would be the @see v2ProposalImporterTest
 *
 * This is dead code. Delete on February 1, 2012?
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/data-applicationContext.xml"})
public class ParallelHierarchyTest {
    @Test
    public void nopTest() {}
}
