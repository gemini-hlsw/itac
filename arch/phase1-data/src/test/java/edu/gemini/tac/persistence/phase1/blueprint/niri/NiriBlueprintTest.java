package edu.gemini.tac.persistence.phase1.blueprint.niri;

import edu.gemini.tac.persistence.fixtures.FastHibernateFixture;
import org.hibernate.classic.Session;
import org.hibernate.impl.SessionFactoryImpl;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests NiriBlueprints
 */
public class NiriBlueprintTest extends FastHibernateFixture.BasicLoadOnce {

    @Test
    public void testHasPersister(){
        final SessionFactoryImpl factory = (SessionFactoryImpl) sessionFactory;
        Assert.assertNotNull(factory.getEntityPersister("edu.gemini.tac.persistence.phase1.blueprint.niri.NiriBlueprint"));
    }

    @Test
    public void testCanCreate() {
        NiriBlueprint b = new NiriBlueprint();
        Assert.assertNotNull(b);
        Session s = sessionFactory.openSession();
        try{
            s.saveOrUpdate(b);
        }finally{
            s.close();
        }
        Assert.assertNotNull(b.getId());
    }

}
