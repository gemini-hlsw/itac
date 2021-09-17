package itac
import itac.ReferenceTime.Default
import itac.ReferenceTime.At

/**
 * A reference time to use when determining target coordinates. Nonsidereal target edits can specify
 * a non-default reference time via an edit.
 */
sealed trait ReferenceTime {
  def getOrElse(default: Long): Long =
    this match {
      case Default  => default
      case At(when) => when
    }
}
object ReferenceTime {
  final case object Default        extends ReferenceTime
  final case class  At(when: Long) extends ReferenceTime
}