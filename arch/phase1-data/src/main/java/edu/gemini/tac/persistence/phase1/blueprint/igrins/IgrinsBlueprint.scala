package edu.gemini.tac.persistence.phase1.blueprint.igrins

import edu.gemini.tac.persistence.phase1.Instrument
import edu.gemini.tac.persistence.phase1.blueprint.{BlueprintBase, BlueprintPair}
import javax.persistence._
import scala.collection.JavaConverters._

@Entity
@DiscriminatorValue("IgrinsBlueprint")
class IgrinsBlueprint(b: edu.gemini.model.p1.mutable.IgrinsBlueprint) extends BlueprintBase(b.getId, b.getName, Instrument.IGRINS) {

  def this() = this(new edu.gemini.model.p1.mutable.IgrinsBlueprint())

  override def getDisplay: String = name

  override def getDisplayAdaptiveOptics: String = BlueprintBase.DISPLAY_NOT_APPLICABLE

  override def getDisplayCamera: String = BlueprintBase.DISPLAY_NOT_APPLICABLE

  override def getDisplayFocalPlaneUnit: String = BlueprintBase.DISPLAY_NOT_APPLICABLE

  override def getDisplayDisperser: String = BlueprintBase.DISPLAY_NOT_APPLICABLE

  override def getDisplayFilter: String = BlueprintBase.DISPLAY_NOT_APPLICABLE

  override def getDisplayOther: String = BlueprintBase.DISPLAY_NOT_APPLICABLE

  override def toMutable:BlueprintPair = {
    val choice = new edu.gemini.model.p1.mutable.IgrinsBlueprintChoice
    val mBlueprint = new edu.gemini.model.p1.mutable.IgrinsBlueprint
    choice.setIgrins(mBlueprint)

    mBlueprint.setId(getBlueprintId)
    mBlueprint.setName(getName)
    mBlueprint.setVisitor(true)

    new BlueprintPair(choice, mBlueprint)
  }

  override def getComplementaryInstrumentBlueprint = throw new RuntimeException("Switching sites has no meaning for IGRINS blueprints.")

  override def getResourcesByCategory: java.util.Map[String, java.util.Set[String]] = Map[String, java.util.Set[String]]().asJava

  override def isMOS: Boolean = false
}
