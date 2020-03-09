package edu.gemini.tac.persistence.phase1.blueprint.visitor

import edu.gemini.tac.persistence.phase1.blueprint.{BlueprintPair, BlueprintBase}
import edu.gemini.model.p1.mutable.Site
import scala.collection.JavaConverters._
import javax.persistence.Transient

trait VisitorBlueprint extends BlueprintBase {
  val site:Site
  val customName:String

  override def getDisplayAdaptiveOptics = BlueprintBase.DISPLAY_NOT_APPLICABLE

  override def getDisplayCamera = BlueprintBase.DISPLAY_NOT_APPLICABLE

  override def getDisplayFocalPlaneUnit = BlueprintBase.DISPLAY_NOT_APPLICABLE

  override def getDisplayDisperser = BlueprintBase.DISPLAY_NOT_APPLICABLE

  override def getDisplayFilter = BlueprintBase.DISPLAY_NOT_APPLICABLE

  override def getDisplayOther = BlueprintBase.DISPLAY_NOT_APPLICABLE

  override def getComplementaryInstrumentBlueprint = throw new RuntimeException("Switching sites has no meaning for Visitor blueprints.")

  override def getResourcesByCategory = Map[String, java.util.Set[String]]("Name" -> Set[String](customName).asJava).asJava

  override def toMutable: BlueprintPair = {
    val choice = new edu.gemini.model.p1.mutable.VisitorBlueprintChoice
    val mBlueprint = new edu.gemini.model.p1.mutable.VisitorBlueprint
    mBlueprint.setSite(site)
    mBlueprint.setCustomName(customName)
    mBlueprint.setId(getBlueprintId)
    mBlueprint.setName(getName)

    choice.setVisitor(mBlueprint)

    new BlueprintPair(choice, mBlueprint)
  }

  override def isMOS: Boolean = false
}
