// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.util

import cats._
import cats.implicits._

sealed trait OneOrTwo[A] extends Product with Serializable {

  def fold[B](f: A => B, g: (A, A) => B): B =
    this match {
      case OneOrTwo.One(a)    => f(a)
      case OneOrTwo.Two(a, b) => g(a, b)
    }

  def fst: A = fold(identity, (a, _) => a)
  def snd: Option[A] = fold(_ => none, (_, a) => a.some)

}

object OneOrTwo {

  final case class One[A](a: A)       extends OneOrTwo[A]
  final case class Two[A](a: A, b: A) extends OneOrTwo[A]

  def apply[A](a: A):       OneOrTwo[A] = One(a)
  def apply[A](a: A, b: A): OneOrTwo[A] = Two(a, b)

  def fromFoldable[F[_]: Foldable, A](fa: F[A]): Option[OneOrTwo[A]] =
    fa.toList match {
      case List(a)    => One(a).some
      case List(a, b) => Two(a, b).some
      case _          => none
    }

  implicit val TraverseOneOrTwo: Traverse[OneOrTwo] =
    new Traverse[OneOrTwo] {

      override def map[A, B](fa: OneOrTwo[A])(f: A => B): OneOrTwo[B] =
        fa.fold(a => OneOrTwo(f(a)), (a, b) => OneOrTwo(f(a), f(b)))

      def foldLeft[A, B](fa: OneOrTwo[A], b: B)(f: (B, A) => B): B =
        fa.fold(a => f(b, a), (a, a1) => f(f(b, a), a1))

      def foldRight[A, B](fa: OneOrTwo[A], lb: Eval[B])(f: (A, Eval[B]) => Eval[B]): Eval[B] =
        fa.fold(a => f(a, lb), (a, a1) => f(a, f(a1, lb)))

      def traverse[G[_]: Applicative, A, B](fa: OneOrTwo[A])(f: A => G[B]): G[OneOrTwo[B]] =
        fa.fold(a => f(a).map(OneOrTwo(_)), (a, a1) => (f(a), f(a1)).mapN(OneOrTwo(_, _)))

    }

}


