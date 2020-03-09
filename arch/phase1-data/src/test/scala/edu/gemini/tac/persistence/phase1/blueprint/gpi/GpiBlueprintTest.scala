package edu.gemini.tac.persistence.phase1.blueprint.gpi

import edu.gemini.tac.persistence.fixtures.FastHibernateFixture
import org.hibernate.impl.SessionFactoryImpl
import org.junit.Assert
import org.junit.Test
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintPair

/**
 * Tests GpiBlueprints
 */
class GpiBlueprintTest extends FastHibernateFixture.BasicLoadOnce {

  @Test
  def testHasPersister = {
    val factory = sessionFactory.asInstanceOf[SessionFactoryImpl]
    Assert.assertNotNull(factory.getEntityPersister("edu.gemini.tac.persistence.phase1.blueprint.gpi.GpiBlueprint"))
  }

  @Test
  def testResources = {
    Assert.assertTrue(new GpiBlueprint().getResourcesByCategory.isEmpty)
  }

  @Test
  def testToMutable = {
    Assert.assertTrue(new GpiBlueprint().getResourcesByCategory.isEmpty)
    val mutableBlueprint = new edu.gemini.model.p1.mutable.GpiBlueprint()

    val bp:BlueprintPair = new GpiBlueprint(mutableBlueprint).toMutable
    Assert.assertNotNull(bp.getBlueprintBase)
    Assert.assertNotNull(bp.getBlueprintChoice)
  }

  @Test
  def testCanCreate = {
    val b = new GpiBlueprint()
    Assert.assertNotNull(b)
    val s = sessionFactory.openSession()
    try {
      s.saveOrUpdate(b)
    } finally {
      s.close()
    }
    Assert.assertNotNull(b.getId())
  }

}
