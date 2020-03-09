package edu.gemini.tac.persistence.rollover;

import edu.gemini.tac.persistence.Partner;
import edu.gemini.tac.persistence.Site;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashMap;
import java.util.Map;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/data-applicationContext.xml"})
public class RolloverReportTest {
    @Test
    public void canParseXml(){
        Partner mockAustralia = new Partner();
        Map<String,Partner> partnerMap = new HashMap<String, Partner>();
        partnerMap.put("Australia", mockAustralia);
        String s = "<rollover site=\"GS\" timestamp=\"1333074873014\" semester=\"2012A\">\n" +
                "<obs>\n" +
                "<id>GS-2012A-Q-10-1</id>\n" +
                "<partner>Australia</partner>\n" +
                "<target>\n" +
                "<ra>0.0</ra>\n" +
                "<dec>0.0</dec>\n" +
                "</target>\n" +
                "<conditions>\n" +
                "<cc>50</cc>\n" +
                "<iq>70</iq>\n" +
                "<sb>50</sb>\n" +
                "<wv>100</wv>\n" +
                "</conditions>\n" +
                "<time>3649496</time>\n" +
                "</obs>\n" +
                "</rollover>";
        RolloverReport rr = RolloverReport.parse(s, partnerMap);
        Assert.assertNotNull(rr);
        Assert.assertEquals(Site.SOUTH, rr.getSite());
        Assert.assertEquals(1, rr.getObservations().size());
        Assert.assertEquals(1, rr.getTotalHours());
    }
}
