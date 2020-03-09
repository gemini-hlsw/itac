package edu.gemini.tac.persistence

import phase1._

/**
 * Provides implicit conversions to rich wrapper types to make working with the
 * bare Java types more bearable.
 */
object Implicits {
  implicit val timeWrapper = (x: TimeAmount) => new RichTime(x)
}