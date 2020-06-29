package edu.gemini.tac.qengine.util

import annotation.tailrec

/**
 * Combination of Orderings into a single ranked Ordering.  There must be a
 * more clever way of doing this and obviating the need for this class
 * altogether and some day I know I'll look upon this with shame.
 */
class CompoundOrdering[T](orderings: Ordering[T]*) extends Ordering[T] {
  // recursively applies orderings until it finds one that yields something
  // other than 0
  @tailrec private def recCompare(t1: T, t2: T, rem: Seq[Ordering[T]]): Int =
    if (rem.length == 0) 0
    else rem.head.compare(t1, t2) match {
           case 0 => recCompare(t1, t2, rem.tail)
           case n => n
         }

  def compare(t1: T, t2: T): Int = recCompare(t1, t2, orderings)
}
