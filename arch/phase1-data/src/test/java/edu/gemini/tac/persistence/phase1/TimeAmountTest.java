package edu.gemini.tac.persistence.phase1;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/data-applicationContext.xml"})
public class TimeAmountTest {
    @Test
    public void canSum(){
        TimeAmount thirtyMin = new TimeAmount(30, TimeUnit.MIN);
        TimeAmount oneHr = new TimeAmount(1, TimeUnit.HR);
        TimeAmount oneAndAHalf = oneHr.sum(thirtyMin);
        Assert.assertEquals(1.5, oneAndAHalf.getValue().doubleValue(), Double.MIN_VALUE);
        Assert.assertEquals(TimeUnit.HR, oneAndAHalf.getUnits());
    }
    
    @Test
    public void convertsCorrectly() {
        TimeAmount oneMinute = new TimeAmount(1, TimeUnit.MIN);
        TimeAmount oneHour = new TimeAmount(1, TimeUnit.HR);
        TimeAmount oneNight = new TimeAmount(1, TimeUnit.NIGHT);
        
        Assert.assertEquals(1.0,            oneMinute.convertTo(TimeUnit.MIN).getDoubleValue(),     0.001);
        Assert.assertEquals(1.0/60.0,       oneMinute.convertTo(TimeUnit.HR).getDoubleValue(),      0.001);
        Assert.assertEquals(1.0/60.0/10,    oneMinute.convertTo(TimeUnit.NIGHT).getDoubleValue(),   0.001);

        Assert.assertEquals(1.0*60.0,       oneHour.convertTo(TimeUnit.MIN).getDoubleValue(),       0.001);
        Assert.assertEquals(1.0,            oneHour.convertTo(TimeUnit.HR).getDoubleValue(),        0.001);
        Assert.assertEquals(1.0/10.0,       oneHour.convertTo(TimeUnit.NIGHT).getDoubleValue(),     0.001);

        Assert.assertEquals(1.0*10.0*60.0,  oneNight.convertTo(TimeUnit.MIN).getDoubleValue(),      0.001);
        Assert.assertEquals(1.0*10,         oneNight.convertTo(TimeUnit.HR).getDoubleValue(),       0.001);
        Assert.assertEquals(1.0,            oneNight.convertTo(TimeUnit.NIGHT).getDoubleValue(),    0.001);
    }

    @Test
    public void doesTrimInPrettyString(){
        TimeAmount third = new TimeAmount(20, TimeUnit.MIN).convertTo(TimeUnit.HR);
        String prettyString = third.getPrettyString();
        Assert.assertEquals("0.33 HR", prettyString);
    }
}
