package edu.gemini.tac.persistence.phase1.blueprint.dssi

import javax.persistence._

import edu.gemini.model.p1.mutable.Site

import scala.collection.JavaConverters._
import edu.gemini.tac.persistence.phase1.blueprint.{BlueprintBase, BlueprintPair}
import edu.gemini.tac.persistence.phase1.Instrument

@Entity
@DiscriminatorValue("DssiBlueprint")
class DssiBlueprint(b: edu.gemini.model.p1.mutable.DssiBlueprint) extends BlueprintBase(b.getId, b.getName, if (b.getSite == Site.GEMINI_NORTH) Instrument.DSSI_GN else Instrument.DSSI_GS) {
  @Enumerated(EnumType.STRING)
  @Column(name = "visitor_site")
  val site: Site = b.getSite

  def this() = this(new edu.gemini.model.p1.mutable.DssiBlueprint())

  override def getDisplayAdaptiveOptics = BlueprintBase.DISPLAY_NOT_APPLICABLE

  override def getDisplayCamera = BlueprintBase.DISPLAY_NOT_APPLICABLE

  override def getDisplayFocalPlaneUnit = BlueprintBase.DISPLAY_NOT_APPLICABLE

  override def getDisplayDisperser = BlueprintBase.DISPLAY_NOT_APPLICABLE

  override def getDisplayFilter = BlueprintBase.DISPLAY_NOT_APPLICABLE

  override def getDisplayOther = BlueprintBase.DISPLAY_NOT_APPLICABLE

  override def toMutable:BlueprintPair = {
      val choice = new edu.gemini.model.p1.mutable.DssiBlueprintChoice
      val mBlueprint = new edu.gemini.model.p1.mutable.DssiBlueprint
      choice.setDssi(mBlueprint)

      mBlueprint.setId(getBlueprintId)
      mBlueprint.setName(getName)
      mBlueprint.setSite(site)

      new BlueprintPair(choice, mBlueprint)
    }

  override def getComplementaryInstrumentBlueprint = throw new RuntimeException("Switching sites has no meaning for Dssi blueprints.")

  override def getResourcesByCategory = Map[String, java.util.Set[String]]("site" -> Set[String](site.value()).asJava).asJava

  override def isMOS: Boolean = false
}
