package edu.gemini.tac.persistence.fixtures;

import edu.gemini.tac.persistence.util.DumpDatabase;
import edu.gemini.tac.persistence.util.RestoreDatabase;
import org.apache.commons.lang.Validate;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Fast fixture.
 * Configure fixtures as needed by overloading the before and beforeClass methods.
 */
public class FastHibernateFixture extends HibernateFixture {

    // After loading the database we need to clean the cache!
    private static boolean cacheIsDirty = false;


    public static class Basic extends FastHibernateFixture {
        @Before public void before() {
            loadDump(DumpDatabase.DUMP_WITHOUT_PROPOSALS);
            clearCacheIfDirty();
        }
    }

    public static class BasicLoadOnce extends FastHibernateFixture {
        @BeforeClass public static void beforeClass() {
            loadDump(DumpDatabase.DUMP_WITHOUT_PROPOSALS);
        }
        @Before public void before() {
            clearCacheIfDirty();
        }
    }

    public static class WithProposals extends FastHibernateFixture {
        @Before public void before() {
            loadDump(DumpDatabase.DUMP_WITH_PROPOSALS);
            clearCacheIfDirty();
        }
    }

    public static class WithProposalsLoadOnce extends FastHibernateFixture {
        @BeforeClass public static void beforeClass() {
            loadDump(DumpDatabase.DUMP_WITH_PROPOSALS);
        }
        @Before public void before() {
            clearCacheIfDirty();
        }
    }

    public static class WithQueues extends FastHibernateFixture {
        @Before public void before() {
            loadDump(DumpDatabase.DUMP_WITH_QUEUES);
            clearCacheIfDirty();
        }
    }

    public static class WithQueuesLoadOnce extends FastHibernateFixture {
        @BeforeClass public static void beforeClass() {
            loadDump(DumpDatabase.DUMP_WITH_QUEUES);
        }
        @Before public void before() {
            clearCacheIfDirty();
        }
    }

    public static class With2012BProposalsLoadOnce extends FastHibernateFixture {
        @BeforeClass public static void beforeClass() {
            loadDump(DumpDatabase.DUMP_2012B_PRE_MEETING);
        }
        @Before public void before() {
            clearCacheIfDirty();
        }
    }

    public static class With2012BQueuesLoadOnce extends FastHibernateFixture {
        @BeforeClass public static void beforeClass() {
            loadDump(DumpDatabase.DUMP_2012B_POST_MEETING);
        }
        @Before public void before() {
            clearCacheIfDirty();
        }
    }

    // Override before from HibernateFixture
    @Before public void before() {
        loadDump(DumpDatabase.DUMP_WITH_QUEUES);
    }

    // Override after from HibernateFixture
    @After public void after() {
        // nothing needs to be done here
    }

    /**
     * Loads a dump from the file system.
     * NOTE: These dumps are re-created every time maven is run with the profile initDatabase and they are
     * therefore always up-to-date and reflect the latest database schema.
     * @param dump
     */
    protected static void loadDump(String dump) {
        try {
            Properties properties = getProperties();
            String dumpDirectory = properties.getProperty("itac.dbdump.directory");
            String dumpUser = properties.getProperty("itac.dbdump.user");
            String database = properties.getProperty("itac.database.name");
            RestoreDatabase.restore(dumpDirectory, dump, dumpUser, database);
            // Mark cache as dirty in order to enforce eviction of all current data from cache
            // in the before method of the next test case that is going to run.
            // NOTE: clearCacheIfDirty() uses the non-static member sessionFactory and can therefore
            // not be called directly from this static method, this is way we have to take a detour here..
            cacheIsDirty = true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Properties getProperties() throws IOException {
        Properties properties = new Properties();
        InputStream is = FastHibernateFixture.class.getResourceAsStream("/itac.properties");
        try {
            Validate.notNull(is);
            properties.load(is);
        } finally {
            is.close();
        }
        return properties;
    }

    protected void clearCacheIfDirty() {
        if (cacheIsDirty) {
            sessionFactory.getCache().evictDefaultQueryRegion();
            sessionFactory.getCache().evictQueryRegions();
            sessionFactory.getCache().evictEntityRegions();
            cacheIsDirty = false;
        }
    }

}
