package edu.gemini.tac.persistence.phase1.blueprint.phoenix

import javax.persistence.{Column, Enumerated, _}

import edu.gemini.model.p1.mutable.{PhoenixFilter, PhoenixFocalPlaneUnit, Site}
import edu.gemini.tac.persistence.phase1.Instrument
import edu.gemini.tac.persistence.phase1.blueprint.{BlueprintBase, BlueprintPair}

import scala.collection.JavaConverters._

@Entity
@DiscriminatorValue("PhoenixBlueprint")
class PhoenixBlueprint(b: edu.gemini.model.p1.mutable.PhoenixBlueprint) extends BlueprintBase(b.getId, b.getName, if (b.getSite == Site.GEMINI_SOUTH) Instrument.PHOENIX_GS else Instrument.PHOENIX_GN) {

  @Enumerated(EnumType.STRING)
  @Column(name = "visitor_site")
  val site: Site = b.getSite

  @Enumerated(EnumType.STRING)
  @Column(name = "fpu")
  val fpu: PhoenixFocalPlaneUnit = b.getFpu

  @Enumerated(EnumType.STRING)
  @Column(name = "filter")
  val filter: PhoenixFilter = b.getFilter

  def this() = this(new edu.gemini.model.p1.mutable.PhoenixBlueprint())

  override def getDisplayAdaptiveOptics = BlueprintBase.DISPLAY_NOT_APPLICABLE

  override def getDisplayCamera = BlueprintBase.DISPLAY_NOT_APPLICABLE

  override def getDisplayFocalPlaneUnit = fpu.value

  override def getDisplayDisperser = BlueprintBase.DISPLAY_NOT_APPLICABLE

  override def getDisplayFilter = filter.value

  override def getDisplayOther = BlueprintBase.DISPLAY_NOT_APPLICABLE

  override def toMutable:BlueprintPair = {
    val choice = new edu.gemini.model.p1.mutable.PhoenixBlueprintChoice
    val mBlueprint = new edu.gemini.model.p1.mutable.PhoenixBlueprint
    choice.setPhoenix(mBlueprint)

    mBlueprint.setId(getBlueprintId)
    mBlueprint.setName(getName)
    mBlueprint.setFpu(fpu)
    mBlueprint.setFilter(filter)
    mBlueprint.setSite(site)

    new BlueprintPair(choice, mBlueprint)
  }

  override def getComplementaryInstrumentBlueprint = throw new RuntimeException("Switching sites has no meaning for Phoenix blueprints.")

  override def getResourcesByCategory = if (fpu != null) {
    Map[String, java.util.Set[String]]("fpu" -> Set[String](fpu.value()).asJava, "filter" -> Set[String](filter.value()).asJava, "site" -> Set[String](site.value()).asJava).asJava
  } else {
    Map.empty[String, java.util.Set[String]].asJava
  }

  override def isMOS: Boolean = false
}
