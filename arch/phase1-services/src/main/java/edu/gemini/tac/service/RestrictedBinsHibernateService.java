package edu.gemini.tac.service;

import edu.gemini.tac.persistence.restrictedbin.RestrictedBin;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;


@Service(value="restrictedBinsService")
public class RestrictedBinsHibernateService implements IRestrictedBinsService {
    @Resource(name = "sessionFactory")
    private SessionFactory sessionFactory;

    @Override
    @SuppressWarnings("unchecked")
    public List<RestrictedBin> getAllRestrictedbins() {
        final Session session = sessionFactory.openSession();
        final Transaction transaction = session.beginTransaction();

        final Query query = session.createQuery("from RestrictedBin");
        final List<RestrictedBin> list = query.list();

        transaction.commit();
        session.close();

        return list;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<RestrictedBin> getDefaultRestrictedbins() {
        final Session session = sessionFactory.openSession();
        final Transaction transaction = session.beginTransaction();

        final Query query = session.createQuery("from RestrictedBin where id in(select max(id) from RestrictedBin group by restricted_bin_type) ");
        final List<RestrictedBin> list = query.list();

        transaction.commit();
        session.close();

        return list;
    }
}
