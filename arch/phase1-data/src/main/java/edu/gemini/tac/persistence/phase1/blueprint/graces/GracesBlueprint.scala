package edu.gemini.tac.persistence.phase1.blueprint.graces

import javax.persistence._
import scala.collection.JavaConverters._
import edu.gemini.tac.persistence.phase1.blueprint.{BlueprintPair, BlueprintBase}
import edu.gemini.tac.persistence.phase1.Instrument
import edu.gemini.model.p1.mutable.{GracesReadMode, GracesFiberMode}

@Entity
@DiscriminatorValue("GracesBlueprint")
class GracesBlueprint(b: edu.gemini.model.p1.mutable.GracesBlueprint) extends BlueprintBase(b.getId, b.getName, Instrument.GRACES) {
  @Enumerated(EnumType.STRING)
  @Column(name = "fibers")
  val fiberMode: GracesFiberMode = b.getFiberMode

  @Enumerated(EnumType.STRING)
  @Column(name = "readMode")
  val readMode: GracesReadMode = b.getReadMode

  def this() = this(new edu.gemini.model.p1.mutable.GracesBlueprint())

  override def getDisplayAdaptiveOptics = BlueprintBase.DISPLAY_NOT_APPLICABLE

  override def getDisplayCamera = BlueprintBase.DISPLAY_NOT_APPLICABLE

  override def getDisplayFocalPlaneUnit = BlueprintBase.DISPLAY_NOT_APPLICABLE

  override def getDisplayDisperser = BlueprintBase.DISPLAY_NOT_APPLICABLE

  override def getDisplayFilter = BlueprintBase.DISPLAY_NOT_APPLICABLE

  override def getDisplayOther = fiberMode.value()

  override def toMutable:BlueprintPair = {
      val choice = new edu.gemini.model.p1.mutable.GracesBlueprintChoice
      val mBlueprint = new edu.gemini.model.p1.mutable.GracesBlueprint
      mBlueprint.setFiberMode(fiberMode)
      mBlueprint.setReadMode(readMode)
      choice.setGraces(mBlueprint)

      mBlueprint.setId(getBlueprintId)
      mBlueprint.setName(getName)

      new BlueprintPair(choice, mBlueprint)
    }

  override def getComplementaryInstrumentBlueprint = throw new RuntimeException("Switching sites has no meaning for Graces blueprints.")

  override def getResourcesByCategory = if (fiberMode != null) {
      Map[String, java.util.Set[String]]("fiberMode" -> Set[String](fiberMode.value()).asJava, "readMode" -> Set[String](readMode.value()).asJava).asJava
    } else {
      Map.empty[String, java.util.Set[String]].asJava
    }

  override def isMOS: Boolean = false
}
