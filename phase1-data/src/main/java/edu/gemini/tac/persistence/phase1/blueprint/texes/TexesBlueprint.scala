package edu.gemini.tac.persistence.phase1.blueprint.texes

import javax.persistence._

import scala.collection.JavaConverters._
import edu.gemini.tac.persistence.phase1.blueprint.{BlueprintBase, BlueprintPair}
import edu.gemini.tac.persistence.phase1.Instrument
import edu.gemini.model.p1.mutable.{Site, TexesDisperser}

@Entity
@DiscriminatorValue("TexesBlueprint")
class TexesBlueprint(b: edu.gemini.model.p1.mutable.TexesBlueprint) extends BlueprintBase(b.getId, b.getName, if (b.getSite == Site.GEMINI_NORTH) Instrument.TEXES_GN else Instrument.TEXES_GS) {
  @Enumerated(EnumType.STRING)
  @Column(name = "visitor_site")
  val site: Site = b.getSite

  @Enumerated(EnumType.STRING)
  @Column(name = "disperser")
  val disperser: TexesDisperser = b.getDisperser

  def this() = this(new edu.gemini.model.p1.mutable.TexesBlueprint())

  override def getDisplayAdaptiveOptics = BlueprintBase.DISPLAY_NOT_APPLICABLE

  override def getDisplayCamera = BlueprintBase.DISPLAY_NOT_APPLICABLE

  override def getDisplayFocalPlaneUnit = BlueprintBase.DISPLAY_NOT_APPLICABLE

  override def getDisplayDisperser = disperser.value

  override def getDisplayFilter = BlueprintBase.DISPLAY_NOT_APPLICABLE

  override def getDisplayOther = BlueprintBase.DISPLAY_NOT_APPLICABLE

  override def toMutable:BlueprintPair = {
    val choice = new edu.gemini.model.p1.mutable.TexesBlueprintChoice
    val mBlueprint = new edu.gemini.model.p1.mutable.TexesBlueprint
    choice.setTexes(mBlueprint)

    mBlueprint.setDisperser(disperser)
    mBlueprint.setId(getBlueprintId)
    mBlueprint.setName(getName)
    mBlueprint.setSite(site)

    new BlueprintPair(choice, mBlueprint)
  }

  override def getComplementaryInstrumentBlueprint = throw new RuntimeException("Switching sites has no meaning for Texes blueprints.")

  override def getResourcesByCategory = if (disperser != null) {
    Map[String, java.util.Set[String]]("disperser" -> Set[String](disperser.value()).asJava, "site" -> Set[String](site.value()).asJava).asJava
  } else {
    Map.empty[String, java.util.Set[String]].asJava
  }

  override def isMOS: Boolean = false
}
