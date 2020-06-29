package edu.gemini.tac.qengine.util

import scala.collection.JavaConverters._

import annotation.tailrec

/**
 * Utility routines for the Queue Engine.
 */
object QEngineUtil {

  // Tail recursive implementation of promoteEither.
  @tailrec
  private def promoteEither[L,R](rem: List[Either[L, R]], res: List[R]): Either[L, List[R]] =
    rem match {
      case Nil              => Right(res.reverse)
      case Left(l)  :: _    => Left(l)
      case Right(r) :: tail => promoteEither(tail, r :: res)
    }

  /**
   * Converts a List of Either[L,R] to an Either[L, List[R]].  This is useful
   * when the entire list should be treated as a failure if any element of the
   * list represents failure.
   */
  def promoteEither[L,R](rem: List[Either[L, R]]): Either[L, List[R]] =
    promoteEither(rem, Nil)

  /**
   * Converts a possibly null Java collection into a Scala List that is Nil if
   * the Java collection is null.
   */
  def toList[T](javaCol: java.util.Collection[T]): List[T] =
    Option(javaCol).map(_.asScala.toList).getOrElse(Nil)

  /**
   * Trims the given (possibly null) string, returning an Option that is Some
   * only if the string is not null and contains something other than
   * whitespace.
   */
  def trimString(s: String): Option[String] =
    Option(s) map { _.trim } filter { _.length != 0 }
}