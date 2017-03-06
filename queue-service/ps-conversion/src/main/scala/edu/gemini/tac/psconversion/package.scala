package edu.gemini.tac

import scalaz._
import Scalaz._

package object psconversion {

  /**
   * A trait for the myriad problems that can happen while working with data
   * from the persistence layer.
   */
  sealed trait PsError

  /** Persistence model incomplete or bad somehow. */
  case class BadData(message: String) extends PsError

  /** Hibernate vomit. */
  case class DatabaseError(ex: Throwable) extends PsError

  /**
   * Wrap an operation involving hibernate to handle the inevitable failures.
   */
  def safePersistence[T](op: => T): DatabaseError \/ T =
    \/.fromTryCatchNonFatal(op).leftMap(DatabaseError)

  /**
   * Compute a value that cannot be null.
   */
  def nullSafe[T](nullMessage: => String)(op: => T): PsError \/ T =
    Option(op) \/> BadData(nullMessage)

  /**
   * Compute a value (which cannot be null) using persistence objects.
   * So many things can and will go wrong that we must do this carefully.
   */
  def nullSafePersistence[T](nullMessage: => String)(op: => T): PsError \/ T =
    for {
      t0 <- safePersistence(op)
      t  <- nullSafe(nullMessage)(t0)
    } yield t

  def safePersistenceInt(nullMessage: String)(op: => java.lang.Integer): PsError \/ Int =
    for {
      x <- safePersistence(op).map(Option.apply(_).map(_.intValue))
      y <- x \/> BadData(nullMessage)
    } yield y

  def safePersistenceInt(default: Int)(op: => java.lang.Integer): PsError \/ Int =
    for {
      x <- safePersistence(op).map(Option.apply(_).map(_.intValue))
    } yield x | default
}
