// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac

import cats._
import cats.data._
import cats.effect.Sync
import cats.implicits._
import edu.gemini.model.p1.{mutable => M, immutable => I}
import java.io.File
import java.lang.reflect.Constructor
import javax.xml.bind.JAXBContext
import edu.gemini.tac.qengine.p1.Proposal
import edu.gemini.tac.qengine.p1.io.ProposalIo
import edu.gemini.tac.qengine.p1.io.JointIdGen
import edu.gemini.tac.qengine.ctx.Partner
import edu.gemini.model.p1.immutable.transform.UpConverter
import scalaz.Failure
import scalaz.Success
import scala.xml.XML
import java.io.StringReader
import io.chrisdavenport.log4cats.Logger
import itac.config.Edit

trait ProposalLoader[F[_]] {

  def load(file: File): F[(File, EitherNel[String, NonEmptyList[Proposal]])]

  def loadMany(dir: File): F[List[(File, EitherNel[String, NonEmptyList[Proposal]])]]

  def loadByReference(dir: File, ref: String): F[(File, EitherNel[String, NonEmptyList[Proposal]])]

}

object ProposalLoader {

  // Private members here are a performance hack until https://github.com/gemini-hlsw/ocs/pull/1722
  // shows up in published library code, at which point most of this goes away and we can deletage
  // to edu.gemini.model.p1.immutable.ProposalIO

  val context: JAXBContext = {
    val factory        = new M.ObjectFactory
    // val contextPackage = factory.getClass.getName.reverse.dropWhile(_ != '.').drop(1).reverse
    JAXBContext.newInstance(factory.createProposal().getClass()) //contextPackage, getClass.getClassLoader)
  }

  private val ctor: Constructor[I.Proposal] =
    classOf[I.Proposal].getConstructor(classOf[M.Proposal])

  def apply[F[_]: Sync: Parallel](
    partners: Map[String, Partner],
    when: Long,
    edits: Map[String, Edit],
    logger: Logger[F]
  ): ProposalLoader[F] =
    new ProposalLoader[F] {

      // Should we do upconversion? Unclear. Delete once we answer this.
      val UpConvert = false

      val editor: Editor[F] =
        new Editor[F](edits, logger)

      // this does upconversion .. is it necessary?
      def loadPhase1(f:File): F[I.Proposal] =
        if (UpConvert) {
          Sync[F].delay(XML.loadFile(f)).map(UpConverter.upConvert).flatMap {
            case Failure(ss) => Sync[F].raiseError(new RuntimeException(ss.list.toList.mkString("\n")))
            case Success(r)  => ctor.newInstance(context.createUnmarshaller.unmarshal(new StringReader(r.root.toString))).pure[F]
          }
        } else {
          Sync[F].delay {
            // This can fail if there is a problem in the XML, so we need to raise a useful error here
            // and catch it in the load methods below; or return Either here.
            ctor.newInstance(context.createUnmarshaller.unmarshal(f))
          }
        } .map { p =>
          // see https://github.com/gemini-hlsw/itac/pull/29 :-(
          p.copy(observations = p.nonEmptyObservations)
        } .flatMap { p =>
          editor.applyEdits(f, p)
        }

      def loadManyPhase1(dir: File): F[List[(File, I.Proposal)]] =
        Sync[F].delay(Option(dir.listFiles)).flatMap {
          case Some(arr) => arr.filter(_.getName().endsWith(".xml")).sortBy(_.getAbsolutePath).toList.traverse(f => loadPhase1(f).tupleLeft(f)) // TODO: parTraverse
          case None      => Sync[F].raiseError(new RuntimeException(s"Not a directory: $dir"))
        }

      val pio: ProposalIo =
        new ProposalIo(partners)

      def read(proposal: I.Proposal): State[JointIdGen, EitherNel[String, NonEmptyList[Proposal]]] =
        State { jig =>
          pio.read(proposal, when, jig) match {
            case scalaz.Failure(ss)         => (jig,  NonEmptyList(ss.head, ss.tail.toList).asLeft)
            case scalaz.Success((ps, jigʹ)) => (jigʹ, NonEmptyList(ps.head, ps.tail.toList).asRight)
          }
        }

      def load(file: File): F[(File, EitherNel[String, NonEmptyList[Proposal]])] =
        loadPhase1(file).map(read(_).tupleLeft(file).runA(JointIdGen(1)).value)

      def loadMany(dir: File): F[List[(File, EitherNel[String, NonEmptyList[Proposal]])]] =
        loadManyPhase1(dir).map(_.traverse(a => read(a._2).tupleLeft(a._1)).runA(JointIdGen(1)).value)

      def loadByReference(dir: File, ref: String): F[(File, EitherNel[String, NonEmptyList[Proposal]])] =
        Sync[F].delay(Option(dir.listFiles)).flatMap {
          case None      => Sync[F].raiseError(new RuntimeException(s"Not a directory: $dir"))
          case Some(arr) =>
            arr
              .filter(_.getName().endsWith(".xml")).toList
              .findM { f =>
                Sync[F].delay {
                  val e = XML.load(f.toURI.toURL)
                  val r = (e \\ "receipt" \ "id").text
                  r == ref
                }
              }
              .flatMap {
                case Some(f) => load(f)
                case None    => Sync[F].raiseError(new ItacException(s"No such proposal: $ref"))
              }
        }


    }

}




