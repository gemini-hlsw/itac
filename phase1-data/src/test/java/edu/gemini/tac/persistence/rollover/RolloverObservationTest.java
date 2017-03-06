package edu.gemini.tac.persistence.rollover;

import edu.gemini.tac.persistence.Partner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/data-applicationContext.xml"})
public class RolloverObservationTest {
    @Test
    public void canParseXmlString() throws Exception {
        Partner mockAustralia = new Partner();
        mockAustralia.setAbbreviation("AU");
        Map<String, Partner> partnerMap = new HashMap<String, Partner>();
        partnerMap.put("Australia", mockAustralia);
        String s = "<obs>\n" +
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
                "</obs>\n";
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource src = new InputSource(new StringReader(s));
        Document document = builder.parse(src);
        final Element documentElement = document.getDocumentElement();
        RolloverObservation ro = new RolloverObservation(documentElement, partnerMap);
        Assert.assertNotNull(ro);
        Assert.assertEquals("GS-2012A-Q-10-1", ro.getObservationId());
        Assert.assertEquals(mockAustralia, ro.getPartner());
    }

}
