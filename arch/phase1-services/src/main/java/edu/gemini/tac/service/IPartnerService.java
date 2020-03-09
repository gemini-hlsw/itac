package edu.gemini.tac.service;

import edu.gemini.tac.persistence.Partner;

import java.util.List;
import java.util.Map;

public interface IPartnerService {
	List<Partner> findAllPartners();
    List<Partner> findAllPartnerCountries();
    Partner findForKey(String s);
    Map<String, Partner> getPartnersByName();
    void setNgoContactEmail(String partner, String ngoContactEmail);
    void setPartnerPercentage(String partnerName, double partnerPercentage);
}
