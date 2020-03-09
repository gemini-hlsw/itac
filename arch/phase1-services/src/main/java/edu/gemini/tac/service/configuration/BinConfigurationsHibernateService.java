package edu.gemini.tac.service.configuration;

import edu.gemini.tac.persistence.bin.BinConfiguration;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service(value="binConfigurationsService")
public class BinConfigurationsHibernateService implements IBinConfigurationsService {
	@Resource(name = "sessionFactory")
	private SessionFactory sessionFactory;

	@Override
	@SuppressWarnings("unchecked")
	public List<BinConfiguration> getAllBinConfigurations() {
        final Session session = sessionFactory.openSession();
        final Transaction transaction = session.beginTransaction();

        final Query query = session.createQuery("from BinConfiguration bc");
        final List<BinConfiguration> list = query.list();

        transaction.commit();
        session.close();

        return list;
	}
	
	public void updateBinConfiguration(final BinConfiguration binConfiguration) {
		final Session session = sessionFactory.openSession();
		final Transaction transaction = session.beginTransaction();
		
		session.saveOrUpdate(binConfiguration);
		
		transaction.commit();
		session.close();
	}

	/* (non-Javadoc)
	 * @see edu.gemini.tac.service.configuration.IConditionsService#getConditionSetByName(java.lang.String)
	 */
	@Override
	public BinConfiguration getBinConfigurationByName(final String name) {
        final Session session = sessionFactory.openSession();
        final Transaction transaction = session.beginTransaction();

        final BinConfiguration binConfiguration = (BinConfiguration) session.getNamedQuery("binConfiguration.findByName").setString("name", name).uniqueResult();
        
        transaction.commit();
        session.close();
        
		return binConfiguration;
	}
	
	/* (non-Javadoc)
	 * @see edu.gemini.tac.service.configuration.IBinConfigurationsService#getBinConfigurationById(java.lang.Long)
	 */
	@Override
	public BinConfiguration getBinConfigurationById(final Long id) {
        final Session session = sessionFactory.openSession();
        final Transaction transaction = session.beginTransaction();

        final BinConfiguration binConfiguration = (BinConfiguration) session.getNamedQuery("binConfiguration.findById").setLong("id", id).uniqueResult();
        
        transaction.commit();
        session.close();
        
		return binConfiguration;
	}

}
