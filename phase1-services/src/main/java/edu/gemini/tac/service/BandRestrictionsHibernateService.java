package edu.gemini.tac.service;

import edu.gemini.tac.persistence.bandrestriction.BandRestrictionRule;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service(value="bandRestrictionsService")
public class BandRestrictionsHibernateService implements IBandRestrictionService {
	@Resource(name = "sessionFactory")
	private SessionFactory sessionFactory;

	@Override
	@SuppressWarnings("unchecked")
	public List<BandRestrictionRule> getAllBandRestrictions() {
        final Session session = sessionFactory.openSession();
        final Transaction transaction = session.beginTransaction();

        final Query query = session.createQuery("from BandRestrictionRule");
        final List<BandRestrictionRule> list = query.list();

        transaction.commit();
        session.close();

        return list;
	}
}
