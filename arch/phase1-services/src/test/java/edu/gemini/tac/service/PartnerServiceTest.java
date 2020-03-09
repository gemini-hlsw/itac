package edu.gemini.tac.service;

import edu.gemini.tac.persistence.Partner;
import edu.gemini.tac.persistence.fixtures.FastHibernateFixture;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/services-applicationContext.xml"})
public class PartnerServiceTest extends FastHibernateFixture {
	@Resource(name = "partnerService")
	IPartnerService service;

	@Test
	public void canGetPartners() {
		final List<Partner> partners = service.findAllPartners();
		assertNotNull(partners);
		assertEquals(11, partners.size());
        //Confirm they're not LIE
        for(Partner p : partners){
            assertNotNull(p.getId());
            assertNotNull(p.getAbbreviation());
        }
	}


    @Test
    public void getPartnerCountries() {
        final List<Partner> partnerCountries = service.findAllPartnerCountries();
        assertNotNull(partnerCountries);
        assertEquals(6, partnerCountries.size());
    }

    @Test
    public void canGetASpecificCountry(){
        final Partner p = service.findForKey("BR");
        assertNotNull(p);
        assertEquals("BR", p.getPartnerCountryKey().toUpperCase());
    }

    @Test
    public void canAssignNgoContactEmail(){
        final Partner p = service.findForKey("BR");
        final String firstEmail = p.getNgoFeedbackEmail();
        assertFalse(firstEmail.equals("foo@bar.com"));
        service.setNgoContactEmail("Brazil", "foo@bar.com");
        final String newEmail = service.findForKey("BR").getNgoFeedbackEmail();
        assertTrue(newEmail.equals("foo@bar.com"));
    }
}
