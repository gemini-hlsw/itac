//TODO: Re-implement
//package edu.gemini.tac.service;
//
//import edu.gemini.tac.persistence.daterange.Blackout;
//import edu.gemini.tac.persistence.phase1.Resource;
//import org.apache.commons.lang.Validate;
//import org.apache.log4j.Logger;
//import org.hibernate.Session;
//import org.hibernate.SessionFactory;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import org.hibernate.Query;
//
//import java.util.List;
//import java.util.Set;
//
///**
// * Primarily exists to support the creation of Blackout dates for instruments
// * <p/>
// * Author: lobrien
// * Date: 4/6/11
// */
//@Service("resourceService")
//public class ResourceServiceHibernate implements IResourceService {
//    private static final Logger LOGGER = Logger.getLogger(ResourceServiceHibernate.class.getName());
//
//    @javax.annotation.Resource(name = "sessionFactory")
//    private SessionFactory sessionFactory;
//
//    @Transactional
//    @Override
//    public List<String> getResourceNames() {
//        final Session session = sessionFactory.getCurrentSession();
//        final Query query = session.createQuery("select distinct r.name from Resource r");
//        List<String> results = query.list();
//        return results;
//    }
//
//    @Override
//    public Resource getRepresentativeResource(String name) {
//        Validate.notNull(name, "No name passed in to function");
//        final Session session = sessionFactory.getCurrentSession();
//        final Query query = session.createQuery("from Resource where name ='" + name + "'");
//        List<Resource> list = query.setMaxResults(1).list();
//        Validate.isTrue(list.size() > 0, "Search for Resource named '" + name + "' returned no results");
//        return list.get(0);
//    }
//
//    @Override
//    public void addBlackout(Blackout b) {
//        Validate.notNull(b, "Null Blackout passed to function");
//        final Session session = sessionFactory.getCurrentSession();
//        if (b.getId() != null) {
//            session.update(b);
//            session.saveOrUpdate(b);
//        } else {
//            session.save(b);
//        }
//    }
//
//    @Override
//    public void deleteBlackout(Long id){
//        Validate.notNull(id);
//        final Session session = sessionFactory.getCurrentSession();
//        Blackout b = (Blackout) session.get(Blackout.class, id);
//        if(b != null){
//            session.delete(b);
//        }
//    }
//
//    @Override
//    public List<Blackout> getBlackoutsFor(String instrumentName){
//        Validate.notNull(instrumentName);
//        Validate.notEmpty(instrumentName);
//
//        final Session session = sessionFactory.getCurrentSession();
//        final Query query = session.createQuery("from Blackout b where b.parent.name = '" + instrumentName + "'");
//        List<Blackout> blackouts = (List<Blackout>) query.list();
//        return blackouts;
//    }
//}
