package edu.gemini.tac.persistence.phase1.blueprint.gpi

import javax.persistence._
import scala.collection.JavaConverters._
import edu.gemini.tac.persistence.phase1.blueprint.{BlueprintPair, BlueprintBase}
import edu.gemini.tac.persistence.phase1.Instrument
import edu.gemini.model.p1.mutable.GpiDisperser
import edu.gemini.model.p1.mutable.GpiObservingMode

@Entity
@DiscriminatorValue("GpiBlueprint")
class GpiBlueprint(b: edu.gemini.model.p1.mutable.GpiBlueprint) extends BlueprintBase(b.getId, b.getName, Instrument.GPI) {
  @Enumerated(EnumType.STRING)
  @Column(name = "disperser")
  val disperser: GpiDisperser = b.getDisperser

  @Enumerated(EnumType.STRING)
  @Column(name = "obsMode")
  val obsMode: GpiObservingMode = b.getObservingMode

  def this() = this(new edu.gemini.model.p1.mutable.GpiBlueprint())

  override def getDisplayAdaptiveOptics = BlueprintBase.DISPLAY_NOT_APPLICABLE

  override def getDisplayCamera = BlueprintBase.DISPLAY_NOT_APPLICABLE

  override def getDisplayFocalPlaneUnit = BlueprintBase.DISPLAY_NOT_APPLICABLE

  override def getDisplayDisperser = disperser.value

  override def getDisplayFilter = BlueprintBase.DISPLAY_NOT_APPLICABLE

  override def getDisplayOther = BlueprintBase.DISPLAY_NOT_APPLICABLE

  override def toMutable:BlueprintPair = {
    val choice = new edu.gemini.model.p1.mutable.GpiBlueprintChoice
    val mBlueprint = new edu.gemini.model.p1.mutable.GpiBlueprint
    choice.setGpi(mBlueprint)

    mBlueprint.setId(getBlueprintId)
    mBlueprint.setName(getName)
    mBlueprint.setDisperser(disperser)
    mBlueprint.setObservingMode(obsMode)

    new BlueprintPair(choice, mBlueprint)
  }

  override def getComplementaryInstrumentBlueprint = throw new RuntimeException("Switching sites has no meaning for Gpi blueprints.")

  override def getResourcesByCategory = if (disperser != null) {
    Map[String, java.util.Set[String]]("disperser" -> Set[String](disperser.value()).asJava, "observationMode" -> Set[String](obsMode.value()).asJava).asJava
  } else {
    Map.empty[String, java.util.Set[String]].asJava
  }

  override def isMOS: Boolean = false
}
