// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.operation

import itac._
import edu.gemini.tac.qengine.api.QueueEngine
import java.nio.file.Path
import cats.effect.{Blocker, ExitCode}
import io.chrisdavenport.log4cats.Logger
import itac.config.PerSite
import org.apache.poi.hssf.usermodel.HSSFWorkbookFactory
import cats.effect.Sync
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
// import edu.gemini.tac.qengine.p1.QueueBand
// import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.Cell
// import org.apache.poi.ss.usermodel.BorderStyle
// import edu.gemini.tac.qengine.ctx.Partner
import cats.implicits._
import java.nio.file.Paths
import scala.math.BigDecimal.RoundingMode
import edu.gemini.tac.qengine.p1.Proposal
// import _root_.operation.ProgramPartnerTime
import edu.gemini.model.p1.mutable.{ Site => _, Proposal => MProposal, _ }
import scala.jdk.CollectionConverters._
// import org.apache.poi.ss.usermodel.Sheet
import edu.gemini.tac.qengine.p1.QueueBand
import edu.gemini.spModel.core.ProgramId
import scala.language.reflectiveCalls
import org.apache.poi.ss.usermodel.Workbook
import edu.gemini.tac.qengine.util.Time
import edu.gemini.tac.qengine.p1.QueueBand.QBand3
object InstrumentScientistSpreadsheet {

  // ffs
  trait Excel[A] {
    def setValue(c: Cell, a: A): Unit
  }
  object Excel {
    implicit val ExcelInt: Excel[Int] = (c, a) => c.setCellValue(a.toDouble)
    implicit val ExcelDouble: Excel[Double] = (c, a) => c.setCellValue((a: BigDecimal).setScale(1, RoundingMode.HALF_UP).toDouble)
    implicit val ExcelString: Excel[String] = (c, a) => c.setCellValue(a)
    implicit val ExcelBoolean: Excel[Boolean] = (c, a) => c.setCellValue(if (a) "TRUE" else "")
    implicit val ExcelJavaBoolean: Excel[java.lang.Boolean] = (c, a) => if (a != null) c.setCellValue(if (a) "TRUE" else "")
    implicit val ExcelInstrument: Excel[Instrument] = (c, a) => if (a != null) c.setCellValue(a.toString)
    implicit def dxcelEnum[E <: Enum[E]]: Excel[E] = (c, a) => if (a != null) c.setCellValue(a.name)
    implicit def list[A: Excel]: Excel[java.util.List[A]] = (c, a) => if (a != null) c.setCellValue(a.asScala.mkString(", "))
  }

  trait Value {
    type T
    def value: T
    def excel: Excel[T]
    override def toString = value.toString
  }
  object Value {
    implicit def lift[A: Excel](a: A): Value =
      new Value {
        type T    = A
        val value = a
        val excel = implicitly
      }
  }

  // Instrument
  sealed trait Instrument
  object Instrument {

    case object Alopeke    extends Instrument
    case object Dssi       extends Instrument
    case object Flamingos2 extends Instrument
    case object GmosN      extends Instrument
    case object GmosS      extends Instrument
    case object Gnirs      extends Instrument
    case object Gpi        extends Instrument
    case object Graces     extends Instrument
    case object Gsaoi      extends Instrument
    case object Igrins     extends Instrument
    case object Keck       extends Instrument
    case object Michelle   extends Instrument
    case object Nici       extends Instrument
    case object Nifs       extends Instrument
    case object Niri       extends Instrument
    case object Phoenix    extends Instrument
    case object Subaru     extends Instrument
    case object Texes      extends Instrument
    case object Trecs      extends Instrument
    case object Visitor    extends Instrument
    case object Zorro      extends Instrument
    case object MaroonX    extends Instrument

    val all: List[Instrument] =
      List(Alopeke, Dssi, Flamingos2, GmosN, GmosS, Gnirs, Gpi, Graces, Gsaoi, Igrins, Keck, Michelle, Nici, Nifs, Niri, Phoenix, Subaru, Texes, Trecs, Visitor, Zorro, MaroonX)

    def forBlueprint(b: BlueprintBase): Instrument =
      b match {
        case _: AlopekeBlueprint              => Alopeke
        case _: DssiBlueprint                 => Dssi
        case _: Flamingos2BlueprintImaging    => Flamingos2
        case _: Flamingos2BlueprintLongslit   => Flamingos2
        case _: Flamingos2BlueprintMos        => Flamingos2
        case _: GmosNBlueprintIfu             => GmosN
        case _: GmosNBlueprintImaging         => GmosN
        case _: GmosNBlueprintLongslit        => GmosN
        case _: GmosNBlueprintLongslitNs      => GmosN
        case _: GmosNBlueprintMos             => GmosN
        case _: GmosSBlueprintIfu             => GmosS
        case _: GmosSBlueprintIfuNs           => GmosS
        case _: GmosSBlueprintImaging         => GmosS
        case _: GmosSBlueprintLongslit        => GmosS
        case _: GmosSBlueprintLongslitNs      => GmosS
        case _: GmosSBlueprintMos             => GmosS
        case _: GnirsBlueprintImaging         => Gnirs
        case _: GnirsBlueprintSpectroscopy    => Gnirs
        case _: GpiBlueprint                  => Gpi
        case _: GracesBlueprint               => Graces
        case _: GsaoiBlueprint                => Gsaoi
        case _: IgrinsBlueprint               => Igrins
        case _: KeckBlueprint                 => Keck
        case _: MichelleBlueprintImaging      => Michelle
        case _: MichelleBlueprintSpectroscopy => Michelle
        case _: NiciBlueprintCoronagraphic    => Nici
        case _: NiciBlueprintStandard         => Nici
        case _: NifsBlueprint                 => Nifs
        case _: NifsBlueprintAo               => Nifs
        case _: NiriBlueprint                 => Niri
        case _: PhoenixBlueprint              => Phoenix
        case _: SubaruBlueprint               => Subaru
        case _: TexesBlueprint                => Texes
        case _: TrecsBlueprintImaging         => Trecs
        case _: TrecsBlueprintSpectroscopy    => Trecs
        case _: VisitorBlueprint              => Visitor
        case _: ZorroBlueprint                => Zorro
        case _: MaroonXBlueprint              => MaroonX
      }

  }

  type Props = Map[Key, Value]
  object Props {
    def apply(kv: (Key, Value)*): Props = kv.toMap
  }

  def props(a: AltairChoice): Props =
    Option(a).flatMap { a =>
      Option(a.getLgs).map(a => Props(Key.Altair -> "LGS", Key.AOWFS -> a.isAowfs, Key.OIWFS -> a.isOiwfs, Key.PWFS1 -> a.isPwfs1)) <+>
      Option(a.getNgs).map(a => Props(Key.Altair -> "NGS", Key.FieldLens -> a.isFieldLens))                                         <+>
      Option(a.getNone).as(Props(Key.Altair -> false))                                                                              <+>
      None
    }.getOrElse(Map.empty)


  def props(b: BlueprintBase): Props = {
    import Key._
    b match {
      case b: AlopekeBlueprint              => Props(Mode -> b.getMode)
      case b: DssiBlueprint                 => Props(Site -> b.getSite)
      case b: Flamingos2BlueprintImaging    => Props(Mode -> "Imaging", Filter -> b.getFilter)
      case b: Flamingos2BlueprintLongslit   => Props(Mode -> "Longslit", Filter -> b.getFilter, Disperser -> b.getDisperser, FPU -> b.getFpu)
      case b: Flamingos2BlueprintMos        => Props(Mode -> "MOS", Filter -> b.getFilter, Disperser -> b.getDisperser)
      case b: GmosNBlueprintIfu             => Props(Mode -> "IFU", Filter -> b.getFilter, Disperser -> b.getDisperser, FPU -> b.getFpu) ++ props(b.getAltair)
      case b: GmosNBlueprintImaging         => Props(Mode -> "Imaging", Filter -> b.getFilter) ++ props(b.getAltair)
      case b: GmosNBlueprintLongslit        => Props(Mode -> "Longslit", Filter -> b.getFilter, Disperser -> b.getDisperser, FPU -> b.getFpu) ++ props(b.getAltair)
      case b: GmosNBlueprintLongslitNs      => Props(Mode -> "Lonigslit-NS", Filter -> b.getFilter, Disperser -> b.getDisperser, FPU -> b.getFpu) ++ props(b.getAltair)
      case b: GmosNBlueprintMos             => Props(Mode -> "MOS", Filter -> b.getFilter, Disperser -> b.getDisperser, FPU -> b.getFpu) ++ props(b.getAltair)
      case b: GmosSBlueprintIfu             => Props(Mode -> "IFU", Filter -> b.getFilter, Disperser -> b.getDisperser, FPU -> b.getFpu)
      case b: GmosSBlueprintIfuNs           => Props(Mode -> "IFU-NS", Filter -> b.getFilter, Disperser -> b.getDisperser, FPU -> b.getFpu)
      case b: GmosSBlueprintImaging         => Props(Mode -> "Imaging", Filter -> b.getFilter)
      case b: GmosSBlueprintLongslit        => Props(Mode -> "Longslit", Filter -> b.getFilter, Disperser -> b.getDisperser, FPU -> b.getFpu)
      case b: GmosSBlueprintLongslitNs      => Props(Mode -> "Longslit-NS", Filter -> b.getFilter, Disperser -> b.getDisperser, FPU -> b.getFpu)
      case b: GmosSBlueprintMos             => Props(Mode -> "MOS", Filter -> b.getFilter, Disperser -> b.getDisperser, FPU -> b.getFpu)
      case b: GnirsBlueprintImaging         => Props(Mode -> "Imaging",Filter -> b.getFilter, PixelScale -> b.getPixelScale)  ++ props(b.getAltair)
      case b: GnirsBlueprintSpectroscopy    => Props(Mode -> "Spectroscopy",FPU -> b.getFpu, CentralWavelength -> b.getCentralWavelength, CrossDisperser -> b.getCrossDisperser, Disperser -> b.getDisperser, PixelScale -> b.getPixelScale)  ++ props(b.getAltair)
      case b: GpiBlueprint                  => Props(Disperser -> b.getDisperser, Mode -> b.getObservingMode)
      case b: GracesBlueprint               => Props(FiberMode -> b.getFiberMode, ReadMode -> b.getReadMode)
      case b: GsaoiBlueprint                => Props(Filter -> b.getFilter)
      case _: IgrinsBlueprint               => Props()
      case b: KeckBlueprint                 => Props(InstrumentSelection -> b.getInstrument)
      case b: MichelleBlueprintImaging      => Props(Mode -> "Imaging", Polarimetry -> b.getPolarimetry)
      case b: MichelleBlueprintSpectroscopy => Props(Mode -> "Spectroscopy", Disperser -> b.getDisperser, FPU -> b.getFpu)
      case b: NiciBlueprintCoronagraphic    => Props(Mode -> "Coronographic", FPM -> b.getFpm, BlueFilter -> b.getBlueFilter, RedFilter -> b.getRedFilter, Dichroic -> b.getDichroic)
      case b: NiciBlueprintStandard         => Props(Mode -> "Standard", BlueFilter -> b.getBlueFilter, RedFilter -> b.getRedFilter, Dichroic -> b.getDichroic)
      case b: NifsBlueprint                 => Props(Mode -> "Non-AO", Disperser -> b.getDisperser)
      case b: NifsBlueprintAo               => Props(Mode -> "AO", Disperser -> b.getDisperser, OccultingDisk -> b.getOccultingDisk) ++ props(b.getAltair)
      case b: NiriBlueprint                 => Props(Filter -> b.getFilter, Camera -> b.getCamera) ++ props(b.getAltair)
      case b: PhoenixBlueprint              => Props(Site -> b.getSite, FPU -> b.getFpu, Filter -> b.getFilter)
      case b: SubaruBlueprint               => Props(InstrumentSelection -> b.getInstrument)
      case b: TexesBlueprint                => Props(Site -> b.getSite, Disperser -> b.getDisperser)
      case b: TrecsBlueprintImaging         => Props(Mode -> "Imaging", Filter -> b.getFilter)
      case b: TrecsBlueprintSpectroscopy    => Props(Mode -> "Spectroscopy", FPU -> b.getFpu, Disperser -> b.getDisperser)
      case b: VisitorBlueprint              => Props(Site -> b.getSite, InstrumentSelection -> b.getCustomName)
      case b: ZorroBlueprint                => Props(Mode -> b.getMode)
      case _: MaroonXBlueprint              => Props()
    }
  }

  sealed abstract class Key(val name: String, val charWidth: Int)
  object Key {
    case object Altair extends Key("Altair", 20)
    case object OccultingDisk extends Key("OccultingDisk", 20)
    case object FPM extends Key("FPM", 20)
    case object BlueFilter extends Key("BlueFilter", 20)
    case object RedFilter extends Key("RedFilter", 20)
    case object Dichroic extends Key("Dichroic", 20)
    case object FieldLens extends Key("FieldLens", 20)
    case object AOWFS extends Key("AOWFS", 20)
    case object OIWFS extends Key("OIWFS", 20)
    case object PWFS1 extends Key("PWFS1", 20)
    case object Mode extends Key("Mode", 20)
    case object Site extends Key("Site", 20)
    case object FPU extends Key("FPU", 20)
    case object Disperser extends Key("Disperser", 20)
    case object Filter extends Key("Filter", 20)
    case object NS extends Key("NS", 20)
    case object PixelScale extends Key("PixelScale", 20)
    case object CentralWavelength extends Key("CentralWavelength", 20)
    case object CrossDisperser extends Key("CrossDisperser", 20)
    case object FiberMode extends Key("FiberMode", 20)
    case object ReadMode extends Key("ReadMode", 20)
    case object InstrumentSelection extends Key("Instrument", 20)
    case object Polarimetry extends Key("Polarimetry", 20)
    case object Camera extends Key("Camera", 20)
    case object Band extends Key("Band", 5)
    case object Time extends Key("Time", 5)
  }

  def apply[F[_]: Sync](
    qe:             QueueEngine,
    siteConfig:     PerSite[Path],
    rolloverReport: PerSite[Path]
  ): Operation[F] =
    new Operation[F] {
      def run(ws: Workspace[F], log: Logger[F], b: Blocker): F[ExitCode] =
        for {
          rs  <- edu.gemini.spModel.core.Site.values.toList.traverse(s => computer(qe, siteConfig.forSite(s), Some(rolloverReport.forSite(s))).computeForSite(ws))
          rsʹ  = rs.combineAll
          wb  <- Sync[F].delay(HSSFWorkbookFactory.createWorkbook)
          _   <- writeWorkbook(wb, rsʹ)
          cwd <- ws.cwd
          _   <- Sync[F].delay(wb.write(cwd.resolve(Paths.get(s"instruments.xls")).toFile))
        } yield ExitCode.Success

    def writeWorkbook(wb: Workbook, rs: Map[Instrument, Map[ProgramId, List[Props]]]): F[Unit] =
      Sync[F].delay {
        rs.foreach { case (i, map) =>
          val uses = map.filter(_._2.nonEmpty)
          if (uses.nonEmpty) {

            val sh   = wb.createSheet(i.toString)
            val keys = {
              val all = uses.values.toList.foldMap(_.foldMap(_.keySet)).toList
              val sorted = (Key.Band :: Key.Time :: all.sortBy(_.name)).distinct
              sorted
            }

            sh.createFreezePane(0, 1)

            // A bold font!
            val font = sh.getWorkbook.createFont
            font.setBold(true)

            // A header style!
            val headerStyle = sh.getWorkbook.createCellStyle
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND)
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex)
            headerStyle.setFont(font)

            // Header
            val h = sh.createRow(0)

            // Helper to create a header cell
            def create(key: Key, col: Int): Unit = {
              val c = h.createCell(col)
              c.setCellStyle(headerStyle)
              c.setCellValue(key.name)
              sh.setColumnWidth(col, 256 * key.charWidth)
            }

            create(new Key("Program ID", 20) {}, 0)
            keys.zipWithIndex.foreach { case (k, i) => create(k, i + 1) }

            var n = 1
            uses.toList.sortBy(_._1.toString).foreach { case (pid, pss) =>
              pss.foreach { ps =>
                val r = sh.createRow(n)
                r.createCell(0).setCellValue(pid.toString)
                ps.foreach { case (k, v) =>
                  val col = keys.indexOf(k) + 1
                  val cell = r.createCell(col)
                  v.excel.setValue(cell, v.value)
                }
                n += 1
              }
            }

            // println(s"${sh.getSheetName()} - ${keys.mkString(", ")}")
          }
        }
      }

  }

  def computer[F[_]: Sync](
    qe:             QueueEngine,
    siteConfig:     Path,
    rolloverReport: Option[Path],
  ) =
    new AbstractQueueOperation[F](qe, siteConfig, rolloverReport) {

      def run(ws: Workspace[F], log: Logger[F], b: Blocker): F[ExitCode] = ???

      def computeForSite(ws: Workspace[F]): F[Map[Instrument, Map[ProgramId, List[Props]]]] =
        computeQueue(ws).map { case (ps, qc) =>
          Instrument.all.map(i => i -> computeForInstument(i, ps, QueueResult(qc))).toMap
        }

      def computeForInstument(i: Instrument, ps: List[Proposal], qr: QueueResult): Map[ProgramId, List[Props]] = {

        val classical: List[(ProgramId, QueueBand, Time, MProposal)] =
          qr.classical(ps).map { case QueueResult.Entry(ps, pid) =>
            (pid, QueueBand.QBand1, ps.foldMap(_.time), Merge.merge(ps.map(_.p1mutableProposal)) )
          }

        val queue: List[(ProgramId, QueueBand, Time, MProposal)] =
          QueueBand.values.flatMap { b =>
            qr.entries(b).map { case QueueResult.Entry(ps, pid) =>
              (pid, b, ps.foldMap(_.time), Merge.merge(ps.map(_.p1mutableProposal)) )
            }
          }

        val all = classical ++ queue

        all.foldLeft(Map.empty[ProgramId, List[Props]]) { case (acc, (pid, b, t, p)) => acc + (pid -> computeForProgram(i, p, b, t)) }
      }

      def computeForProgram(i: Instrument, p: MProposal, b: QueueBand, t: Time): List[Props] = {
        val p1b = b match {
          case QBand3 => Band.BAND_3
          case _ => Band.BAND_1_2
        }
        val bps = p.getObservations().getObservation().asScala.toList.filter(_.getBand == p1b).map(_.getBlueprint()).distinct.filter(Instrument.forBlueprint(_) == i)
        bps.map(props(_) ++ Props(Key.Band -> b.number, Key.Time -> t.toHours.value))
      }

    }


}