package edu.gemini.tac.exchange;

import edu.gemini.tac.util.ProposalUnwrapper;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Test class for unwrapping functionality of proposal importer.
 * Deals with tar, zip, jar and gz files.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/data-applicationContext.xml"})
public class ImporterUnwrapTest {
    private static final Logger LOGGER = Logger.getLogger(ImporterUnwrapTest.class.getName());

    /**
     * Tests unwrapping of a tar file.
     * @throws java.io.IOException
     */
    @Test
    public void unwrapTarFile() throws IOException {
        unwrapFiles("test.tar");
    }

    /**
     * Tests unwrapping of a zipped file.
     * @throws java.io.IOException
     */
    @Test
    public void unwrapZipFile() throws IOException {
        unwrapFiles("test.zip");
    }

    /**
     * Tests unwrapping of a jar file.
     * @throws java.io.IOException
     */
    @Test
    public void unwrapJarFile() throws IOException {
        unwrapFiles("test.jar");
    }

    /**
     * Tests unwrapping of a gzipped tar file.
     * @throws java.io.IOException
     */
    @Test
    public void unwrapTarGzFile() throws IOException {
        unwrapFiles("test.tar.gz");
    }

    /**
     * Test unwrapping of compressed / tared files, this does *not* test the actual data in the files.
     * @param fileName
     * @throws java.io.IOException
     */
    private void unwrapFiles(final String fileName) throws IOException {
        // unwrap the data contained in the test file, no database interaction so no sessions are needed
        final InputStream inputStream = this.getClass().getResourceAsStream("/edu/gemini/tac/exchange/" + fileName);
        ProposalUnwrapper unwrapper;
        try {
            unwrapper = new ProposalUnwrapper(inputStream, fileName);
        } finally {
            inputStream.close();
        }

        // this is what we expect
        final List<String> expectedResults = new ArrayList<String>();
        expectedResults.add("test1.xml");
        expectedResults.add("test2.xml");
        expectedResults.add("foo/test3.xml");
        expectedResults.add("foo/test4.xml");
        
        final List<String> returnedResults = new ArrayList<String>();
        for (ProposalUnwrapper.Entry f : unwrapper.getEntries()) {
            returnedResults.add(f.getFileName());
        }

        // check that number of results match expected results
        Assert.assertEquals(expectedResults.size(), returnedResults.size());
        Assert.assertTrue(returnedResults.containsAll(expectedResults));

    }
}