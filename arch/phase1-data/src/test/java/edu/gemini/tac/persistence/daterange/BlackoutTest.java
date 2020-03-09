package edu.gemini.tac.persistence.daterange;



import edu.gemini.shared.util.DateRange;
import edu.gemini.tac.persistence.phase1.Instrument;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.text.ParseException;
import java.util.Date;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/data-applicationContext.xml"})
public class BlackoutTest {
    @Resource(name = "sessionFactory")
    SessionFactory sessionFactory;

    @Test
    public void canBeInstantiated() throws ParseException {
        Date start = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000 * 21);
        Date end = new Date(System.currentTimeMillis());
        DateRange dr = new DateRange(start, end);

        DateRangePersister drp = new DateRangePersister(dr);

        Blackout b = new Blackout();
        b.setInstrument(Instrument.GMOS_N);
        b.setDateRangePersister(drp);

        Assert.assertNotNull(b);
    }

    @Test
    public void canBeSaved() throws ParseException {
        Date start = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000 * 21);
        Date end = new Date(System.currentTimeMillis());
        DateRange dr = new DateRange(start, end);

        DateRangePersister drp = new DateRangePersister(dr);

        Blackout b = new Blackout();
        b.setInstrument(Instrument.FLAMINGOS2);
        b.setDateRangePersister(drp);

        Session s = sessionFactory.openSession();
        try{
            s.save(b);
            Long id = b.getId();
            Assert.assertTrue(id > 0);
        }finally {
            s.close();
        }
    }
}
