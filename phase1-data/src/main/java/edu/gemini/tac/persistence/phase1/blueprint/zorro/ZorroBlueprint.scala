package edu.gemini.tac.persistence.phase1.blueprint.zorro

import javax.persistence._

import edu.gemini.model.p1.mutable.ZorroMode

import scala.collection.JavaConverters._
import edu.gemini.tac.persistence.phase1.blueprint.{BlueprintBase, BlueprintPair}
import edu.gemini.tac.persistence.phase1.Instrument

@Entity
@DiscriminatorValue("ZorroBlueprint")
class ZorroBlueprint(b: edu.gemini.model.p1.mutable.ZorroBlueprint) extends BlueprintBase(b.getId, b.getName, Instrument.ZORRO) {
  @Enumerated(EnumType.STRING)
  @Column(name = "visitor_site")
  val mode: ZorroMode = b.getMode

  def this() = this(new edu.gemini.model.p1.mutable.ZorroBlueprint())

  override def getDisplay = name

  override def getDisplayAdaptiveOptics = BlueprintBase.DISPLAY_NOT_APPLICABLE

  override def getDisplayCamera = BlueprintBase.DISPLAY_NOT_APPLICABLE

  override def getDisplayFocalPlaneUnit = BlueprintBase.DISPLAY_NOT_APPLICABLE

  override def getDisplayDisperser = BlueprintBase.DISPLAY_NOT_APPLICABLE

  override def getDisplayFilter = BlueprintBase.DISPLAY_NOT_APPLICABLE

  override def getDisplayOther = BlueprintBase.DISPLAY_NOT_APPLICABLE

  override def toMutable:BlueprintPair = {
    val choice = new edu.gemini.model.p1.mutable.ZorroBlueprintChoice
    val mBlueprint = new edu.gemini.model.p1.mutable.ZorroBlueprint
    choice.setZorro(mBlueprint)

    mBlueprint.setId(getBlueprintId)
    mBlueprint.setName(getName)
    mBlueprint.setMode(mode)

    new BlueprintPair(choice, mBlueprint)
  }

  override def getComplementaryInstrumentBlueprint = throw new RuntimeException("Switching sites has no meaning for Zorro blueprints.")

  override def getResourcesByCategory = Map[String, java.util.Set[String]]("mode" -> Set[String](mode.value()).asJava).asJava

  override def isMOS: Boolean = false
}
