package edu.gemini.tac.qengine.api.config

import edu.gemini.tac.qengine.util.Percent
import edu.gemini.tac.qengine.p1.QueueBand

/**
 * Queue band percentages, the percent of total queue time to assign to each
 * band.  Defaults to 30/30/40.
 */
final case class QueueBandPercentages(band1: Percent, band2: Percent, band3: Percent) {
  /**
   * Determines what percent of the total queue is designated for the
   * specified band.
   */
  val bandPercent: Map[QueueBand, Percent] =
    (QueueBand.values zip List(band1, band2, band3, Percent.Hundred - (band1 + band2 + band3))).toMap

  require(bandPercent.values forall { perc => (perc >= Percent.Zero) && (perc <= Percent.Hundred) })

  /**
   * The percentage of the queue associated with the given QueueBand Category.
   */
  def categoryPercent(cat: QueueBand.Category): Percent =
    (Percent.Zero/:QueueBand.values.filter(_.categories.contains(cat))) {
      (perc, band) => perc + bandPercent(band)
    }

  override def toString: String =
    s"(B1=${band1.value.toInt}%, B2=${band2.value.toInt}%, B3=${band3.value.toInt}%)"

  def apply(band: QueueBand): Percent         = bandPercent(band)
  def apply(cat: QueueBand.Category): Percent = categoryPercent(cat)
}

object QueueBandPercentages {
  import edu.gemini.tac.qengine.api.config.Default.{Band1Percent, Band2Percent, Band3Percent}

  val Default: QueueBandPercentages =
    apply(Band1Percent, Band2Percent, Band3Percent)

  def apply(band1: Int, band2: Int, band3: Int) =
    new QueueBandPercentages(Percent(band1), Percent(band2), Percent(band3))
}
