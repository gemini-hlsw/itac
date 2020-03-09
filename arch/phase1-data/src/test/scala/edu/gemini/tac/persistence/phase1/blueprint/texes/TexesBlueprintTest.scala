package edu.gemini.tac.persistence.phase1.blueprint.texes

import edu.gemini.tac.persistence.fixtures.FastHibernateFixture
import org.hibernate.impl.SessionFactoryImpl
import org.junit.Assert
import org.junit.Test
import scala.collection.JavaConversions._
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintPair

/**
 * Tests SpeckleBlueprints
 */
class TexesBlueprintTest extends FastHibernateFixture.BasicLoadOnce {

  @Test
  def testHasPersister = {
    val factory = sessionFactory.asInstanceOf[SessionFactoryImpl]
    Assert.assertNotNull(factory.getEntityPersister("edu.gemini.tac.persistence.phase1.blueprint.texes.TexesBlueprint"))
  }

  @Test
  def testResources = {
    Assert.assertTrue(new TexesBlueprint().getResourcesByCategory.isEmpty)
    val mutableBlueprint = new edu.gemini.model.p1.mutable.TexesBlueprint()
    mutableBlueprint.setDisperser(edu.gemini.model.p1.mutable.TexesDisperser.D_32_LMM)

    Assert.assertEquals("32 l/mm echelle", new TexesBlueprint(mutableBlueprint).getResourcesByCategory.get("disperser").head)
  }

  @Test
  def testToMutable = {
    Assert.assertTrue(new TexesBlueprint().getResourcesByCategory.isEmpty)
    val mutableBlueprint = new edu.gemini.model.p1.mutable.TexesBlueprint()
    mutableBlueprint.setDisperser(edu.gemini.model.p1.mutable.TexesDisperser.D_32_LMM)

    val bp:BlueprintPair = new TexesBlueprint(mutableBlueprint).toMutable
    Assert.assertNotNull(bp.getBlueprintBase)
    Assert.assertNotNull(bp.getBlueprintChoice)
  }

  @Test
  def testCanCreate = {
    val b = new TexesBlueprint()
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
    val mutableBlueprint = new edu.gemini.model.p1.mutable.TexesBlueprint()
    val b = new TexesBlueprint(mutableBlueprint)
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
  def testCanCreateFromModelWithDisperser = {
    val mutableBlueprint = new edu.gemini.model.p1.mutable.TexesBlueprint()
    mutableBlueprint.setDisperser(edu.gemini.model.p1.mutable.TexesDisperser.D_32_LMM)
    val b = new TexesBlueprint(mutableBlueprint)
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
