package edu.gemini.tac.persistence.phase1

/**
 * Dress up the TimeAmount class a bit.  It's a royal shame, but I can't implement
 * Ordered[TimeAmount] because TimeAmount.equals wouldn't be consistent.  I don't dare
 * update TimeAmount.equals and TimeAmount.hashCode though.
 */
final class RichTime(val time: TimeAmount) extends Proxy {
  // Proxy
  def self: Any = time

  def compareAmounts(that: TimeAmount): Int = {
    val (t1, t2) = RichTime.toCompatibleUnits(time, that)
    t1.getValue.compareTo(t2.getValue)
  }

  //  def <(that: TimeAmount): Boolean = compareAmounts(that) < 0
  // ... etc.

  // You could do this, and it would make the code more readable, but
  // unfortunately it opens up the expectation that t1 == t2 will work even
  // if t1 and t2 have different units.

  def ltTime(that: TimeAmount): Boolean = compareAmounts(that) <  0
  def leTime(that: TimeAmount): Boolean = compareAmounts(that) <= 0
  def eqTime(that: TimeAmount): Boolean = compareAmounts(that) == 0
  def gtTime(that: TimeAmount): Boolean = compareAmounts(that) >  0
  def geTime(that: TimeAmount): Boolean = compareAmounts(that) >= 0

  def min(that: TimeAmount): TimeAmount = if (compareAmounts(that) < 0) time else that
  def max(that: TimeAmount): TimeAmount = if (compareAmounts(that) > 0) time else that

  def +(that: TimeAmount): TimeAmount = {
    val (t1, t2) = RichTime.toCompatibleUnits(time, that)
    new TimeAmount(t1.getValue.add(t2.getValue), t1.getUnits)
  }

  def -(that: TimeAmount): TimeAmount = {
    val (t1, t2) = RichTime.toCompatibleUnits(time, that)
    new TimeAmount(t1.getValue.subtract(t2.getValue), t1.getUnits)
  }

}

object RichTime {
  implicit val toRichTime = (x: TimeAmount) => new RichTime(x)

  def minUnits(u1: TimeUnit, u2: TimeUnit): TimeUnit =
    if (u1.ordinal < u2.ordinal) u1 else u2

  def toCompatibleUnits(t1: TimeAmount, t2: TimeAmount): (TimeAmount, TimeAmount) = {
    val u1    = t1.getUnits
    val u2    = t2.getUnits
    val units = RichTime.minUnits(u1, u2)

    val res1  = if (u1 == units) t1 else t1.convertTo(units)
    val res2  = if (u2 == units) t2 else t2.convertTo(units)
    (res1, res2)
  }

  def sum(lst: List[TimeAmount]): Option[TimeAmount] =
    lst match {
      case Nil => None
      case head :: tail => Some((head/:tail) { _ + _ })
    }

}