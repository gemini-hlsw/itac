package edu.gemini.tac.persistence.phase1.blueprint.visitor

import edu.gemini.tac.persistence.fixtures.FastHibernateFixture
import org.hibernate.impl.SessionFactoryImpl
import org.junit.Assert
import org.junit.Test
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintPair

/**
 * Tests SpeckleBlueprints
 */
class VisitorGSBlueprintTest extends FastHibernateFixture.BasicLoadOnce {

  @Test
  def testHasPersister = {
    val factory = sessionFactory.asInstanceOf[SessionFactoryImpl]
    Assert.assertNotNull(factory.getEntityPersister("edu.gemini.tac.persistence.phase1.blueprint.visitor.VisitorGSBlueprint"))
  }

  @Test
  def testToMutable = {
    Assert.assertTrue(new VisitorGSBlueprint().getResourcesByCategory.containsKey("Name"))
    val mutableBlueprint = new edu.gemini.model.p1.mutable.VisitorBlueprint()

    val bp:BlueprintPair = new VisitorGSBlueprint(mutableBlueprint).toMutable
    Assert.assertNotNull(bp.getBlueprintBase)
    Assert.assertNotNull(bp.getBlueprintChoice)
  }

  @Test
  def testCanCreate = {
    val b = new VisitorGSBlueprint()
    Assert.assertNotNull(b)
    val s = sessionFactory.openSession()
    try {
      s.saveOrUpdate(b)
    } finally {
      s.close()
    }
    Assert.assertNotNull(b.getId())
  }

  @Test
  def testCanCreateFromModel = {
    val mutableBlueprint = new edu.gemini.model.p1.mutable.VisitorBlueprint()
    val b = new VisitorGSBlueprint(mutableBlueprint)
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
