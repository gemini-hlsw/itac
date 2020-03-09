package edu.gemini.tac.persistence.daterange;

import edu.gemini.shared.util.DateRange;
import junit.framework.Assert;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.annotation.Resource;
import java.text.ParseException;
import java.util.Date;

/**
 * Exercise DateRangePersister
 *
 * Author: lobrien
 * Date: 4/5/11
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/data-applicationContext.xml"})
public class DateRangePersisterTest {
    @Resource(name = "sessionFactory")
    SessionFactory sessionFactory;

    @Test
    public void canBeInstantiated() throws ParseException {
        Date start = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000 * 21);
        Date end = new Date(System.currentTimeMillis());
        DateRange dr = new DateRange(start, end);

        DateRangePersister drp = new DateRangePersister(dr);
        Assert.assertNotNull(drp);
        Assert.assertEquals(dr, drp.getDateRange());
    }

    @Test
    public void canBeSaved(){

        Date start = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000 * 21);
        Date end = new Date(System.currentTimeMillis());
        DateRange dr = new DateRange(start, end);

        Session session = sessionFactory.openSession();
        try {
            DateRangePersister drp = new DateRangePersister(dr);
            session.save(drp);
            Long id = drp.getId();
            Assert.assertNotNull(id);
            Assert.assertTrue(id.longValue() > 0);
        } finally {
            session.close();
        }
    }

    @Test
    public void canBeRetrieved(){
        Date start = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000 * 21);
        Date end = new Date(System.currentTimeMillis());
        DateRange dr = new DateRange(start, end);

        Session session = sessionFactory.openSession();
        try {
            DateRangePersister drp = new DateRangePersister(dr);
            session.save(drp);
            Long id = drp.getId();
            Assert.assertNotNull(id);
            Assert.assertTrue(id.longValue() > 0);

            DateRangePersister retrieved = (DateRangePersister) session.get(DateRangePersister.class, id.longValue());
            Assert.assertNotNull(retrieved);
            Assert.assertEquals(retrieved, drp);
        } finally {
            session.close();
        }
    }
}
