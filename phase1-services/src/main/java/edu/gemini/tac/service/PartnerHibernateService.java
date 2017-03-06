package edu.gemini.tac.service;

import edu.gemini.tac.persistence.Partner;
import org.apache.commons.lang.Validate;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("partnerService")
public class PartnerHibernateService implements IPartnerService {
	@Resource(name = "sessionFactory")
	private SessionFactory sessionFactory;

    /**                                          *
     * @return a sorted list of all partners
     */
	@Override
    @Transactional
	@SuppressWarnings("unchecked")
	public List<Partner> findAllPartners() {
		final Query query = sessionFactory.getCurrentSession().getNamedQuery("partner.findAllPartners");
		final List<Partner> list = query.list();
        for(Partner p : list){
            Hibernate.initialize(p);
        }
        Collections.sort(list);

        return list;
	}

    /**                                          *
     * @return a sorted list of all partners that are countries
     */
    @Override
    @Transactional
	@SuppressWarnings("unchecked")
    public List<Partner> findAllPartnerCountries() {
        final Query query = sessionFactory.getCurrentSession().getNamedQuery("partner.findAllPartnerCountries");
        final List<Partner> list = query.list();
        Collections.sort(list);

        return list;
    }

    /**
     *
     * @param countryKey -- the key for the country, e.g., BR
     * @return
     */
    @Override
    @Transactional
    public Partner findForKey(String countryKey) {
        final Query query = sessionFactory.getCurrentSession().getNamedQuery("partner.findByKey").setParameter("key", countryKey.toUpperCase());
        final Partner p = (Partner) query.uniqueResult();
        return p;
    }


	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

    @Override
    @Transactional
    public Map<String, Partner> getPartnersByName(){
        final List<Partner> partners = findAllPartners();
        Map<String,Partner> partnerMap = new HashMap<String, Partner>();
        for(Partner p : partners){
            partnerMap.put(p.getName(), p);
            //Special exception for rollover reports server use-case
            if(p.getName().compareToIgnoreCase("United States of America")==0){
                partnerMap.put("United States", p);
            }
        }
        return partnerMap;
    }

    @Override
    @Transactional
    public void setNgoContactEmail(String partnerName, String email){
        Validate.notEmpty(partnerName);
        Validate.notEmpty(email);
        Validate.isTrue(getPartnersByName().containsKey(partnerName), "No partner found for name '" + partnerName + "'");
        final Partner partner = getPartnersByName().get(partnerName);
        partner.setNgoFeedbackEmail(email);
        Session session = sessionFactory.getCurrentSession();
        session.saveOrUpdate(partner);
    }

    @Override
    @Transactional
    public void setPartnerPercentage(String partnerName, double partnerPercentage){
        Validate.notEmpty(partnerName);
        Validate.notNull(partnerPercentage);
        Validate.isTrue(getPartnersByName().containsKey(partnerName), "No partner found for name '" + partnerName + "'");
        final Partner partner = getPartnersByName().get(partnerName);
        partner.setPercentageShare(partnerPercentage);
        Session session = sessionFactory.getCurrentSession();
        session.saveOrUpdate(partner);
    }
}
