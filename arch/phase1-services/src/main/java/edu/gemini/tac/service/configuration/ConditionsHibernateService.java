package edu.gemini.tac.service.configuration;

import edu.gemini.tac.persistence.condition.ConditionBucket;
import edu.gemini.tac.persistence.condition.ConditionSet;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The implementation of @see edu.gemini.tac.service.configuration.IConditionsService in Hibernate.
 * 
 * @author ddawson
 */
@Service(value="conditionsService")
public class ConditionsHibernateService implements IConditionsService {
	@Resource(name = "sessionFactory")
	private SessionFactory sessionFactory;

	/* (non-Javadoc)
	 * @see edu.gemini.tac.service.configuration.IConditionsService#getAllConditionSets()
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<ConditionSet> getAllConditionSets() {
        final Session session = sessionFactory.openSession();
        final Transaction transaction = session.beginTransaction();

        final Query query = session.createQuery("from ConditionSet c");
        final List<ConditionSet> list = query.list();

        transaction.commit();
        session.close();

        return list;
	}

    @Override
    @Transactional
    public void copyConditionSetAndUpdatePercentages(final ConditionSet conditionSet, final String name, final Long[] conditionIds, final Integer[] percentages) {
        final Session session = sessionFactory.getCurrentSession();
        final Query query = session.createQuery("from ConditionSet c");

        final Set<ConditionBucket> conditions = conditionSet.getConditions();

        final ConditionSet updatedConditionSet = new ConditionSet();
        final Set<ConditionBucket> updatedConditions = new HashSet<ConditionBucket>();
        updatedConditionSet.setName(name);
        updatedConditionSet.setConditions(updatedConditions);

        for (ConditionBucket c: conditions) {
            final ConditionBucket updatedCondition = new ConditionBucket();
            updatedCondition.setName(c.getName());
            updatedCondition.setCloudCover(c.getCloudCover());
            updatedCondition.setImageQuality(c.getImageQuality());
            updatedCondition.setSkyBackground(c.getSkyBackground());
            updatedCondition.setWaterVapor(c.getWaterVapor());

            for (int i = 0; i < conditionIds.length; i++) {
                if (conditionIds[i].equals(c.getId()))
                    updatedCondition.setAvailablePercentage(percentages[i]);
            }
            updatedConditions.add(updatedCondition);
        }


        session.saveOrUpdate(updatedConditionSet);
    }

	/* (non-Javadoc)
	 * @see edu.gemini.tac.service.configuration.IConditionsService#updateConditionSet(edu.gemini.tac.persistence.condition.ConditionSet)
	 */
	@Override
	public void updateConditionSet(final ConditionSet conditionSet) {
		final Session session = sessionFactory.openSession();
		final Transaction transaction = session.beginTransaction();
		
		session.saveOrUpdate(conditionSet);
		
		transaction.commit();
		session.close();
	}

	/* (non-Javadoc)
	 * @see edu.gemini.tac.service.configuration.IConditionsService#getConditionSetByName(java.lang.String)
	 */
	@Override
	public ConditionSet getConditionSetByName(final String name) {
        final Session session = sessionFactory.openSession();
        final Transaction transaction = session.beginTransaction();

        final ConditionSet conditionSet = (ConditionSet) session.getNamedQuery("condition.findByName").setString("name", name).uniqueResult();
        
        transaction.commit();
        session.close();
        
		return conditionSet;
	}
	
	/* (non-Javadoc)
	 * @see edu.gemini.tac.service.configuration.IConditionsService#getConditionSetById(java.lang.String)
	 */
	@Override
	public ConditionSet getConditionSetById(final Long id) {
        final Session session = sessionFactory.openSession();
        final Transaction transaction = session.beginTransaction();

        final ConditionSet conditionSet = (ConditionSet) session.getNamedQuery("condition.findById").setLong("id", id).uniqueResult();
        
        transaction.commit();
        session.close();
        
		return conditionSet;
	}
}
