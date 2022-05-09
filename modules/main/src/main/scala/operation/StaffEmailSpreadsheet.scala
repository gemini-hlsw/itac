// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.operation

import itac._
import edu.gemini.tac.qengine.api.QueueEngine
import java.nio.file.Path
import cats.effect.{Blocker, ExitCode}
import io.chrisdavenport.log4cats.Logger
import itac.config.PerSite
import org.apache.poi.ss.usermodel.Workbook
import edu.gemini.spModel.core.Site
import cats.implicits._
import org.apache.poi.hssf.usermodel.HSSFWorkbookFactory
import cats.effect.Sync
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import edu.gemini.tac.qengine.p1.QueueBand
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.Cell
import edu.gemini.model.p1.immutable.VisitorBlueprint
import edu.gemini.tac.qengine.p1.Too
import edu.gemini.model.p1.immutable.GeminiBlueprintBase
import org.apache.poi.ss.usermodel.BorderStyle
import java.nio.file.Paths

object StaffEmailSpreadsheet {

  // ffs
  trait Excel[A] {
    def setValue(c: Cell, a: A): Unit
  }
  object Excel {
    implicit val ExcelInt: Excel[Int] = (c, a) => c.setCellValue(a.toDouble)
    implicit val ExcelDouble: Excel[Double] = (c, a) => c.setCellValue(a)
    implicit val ExcelString: Excel[String] = (c, a) => c.setCellValue(a)
    implicit val ExcelBoolean: Excel[Boolean] = (c, a) => c.setCellValue(if (a) "TRUE" else "")
  }

  def apply[F[_]: Sync](
    qe:             QueueEngine,
    siteConfig:     PerSite[Path],
    rolloverReport: PerSite[Path]
  ): Operation[F] =
    new Operation[F] {
      def run(ws: Workspace[F], log: Logger[F], b: Blocker): F[ExitCode] =
        for {
          wb  <- Sync[F].delay(HSSFWorkbookFactory.createWorkbook())
          _   <- Site.values.toList.traverse_(s => addSheet(wb, qe, siteConfig.forSite(s), Some(rolloverReport.forSite(s))).run(ws, log, b))
          cwd <- ws.cwd
          _   <- Sync[F].delay(wb.write(cwd.resolve(Paths.get("staff-email-spreadsheet.xls")).toFile))
        } yield ExitCode.Success
    }

  val Reference        = 0
  val Band             = 1
  val ProgId           = 2
  val PIName           = 3
  val Time             = 4
  val Instrument       = 5
  val ToO              = 6
  val LGS              = 7
  val NGO              = 8
  val NgoEmail         = 9
  val GeminiEmail      = 10
  val Title            = 11
  val ObsConstraints   = 12
  val RaMin            = 13
  val RaMax            = 14
  val SchedConstraints = 15

  def addSheet[F[_]: Sync](
    wb:             Workbook,
    qe:             QueueEngine,
    siteConfig:     Path,
    rolloverReport: Option[Path],
  ): Operation[F] =
    new AbstractQueueOperation[F](qe, siteConfig, rolloverReport) {
      def run(ws: Workspace[F], log: Logger[F], b: Blocker): F[ExitCode] =
        computeQueue(ws).flatMap { case (_, qc) =>
          Sync[F].delay {
            val sh = wb.createSheet(qc.context.site.displayName)

            // A bold font!
            val font = sh.getWorkbook.createFont
            font.setBold(true)

            // A header style!
            val headerStyle = sh.getWorkbook.createCellStyle
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND)
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex())
            headerStyle.setFont(font)

            // Header
            val h = sh.createRow(0)

            // Helper to create a header cell
            def create(col: Int, value: String, charWidth: Int): Unit = {
              val c = h.createCell(col)
              c.setCellStyle(headerStyle)
              c.setCellValue(value)
              sh.setColumnWidth(col, 256 * charWidth)
            }

            // Create header cells
            create(Reference, "Reference", 17)
            create(Band, "Band", 5)
            create(ProgId, "Gemini Id", 18)
            create(PIName, "PI Name", 25)
            create(Time, "Time", 5)
            create(Instrument, "Instrument", 20)
            create(ToO, "ToO", 5)
            create(LGS, "LGS", 6)
            create(NGO, "NGO", 5)
            create(NgoEmail, "NGO/principal support Email", 35)
            create(GeminiEmail, "Gemini/additional support Email", 35)
            create(Title, "Title", 100)
            create(ObsConstraints, "Obs Constraints", 50)
            create(RaMin, "RA Min", 6)
            create(RaMax, "RA Max", 6)
            create(SchedConstraints, "Scheduling", 100)

            // A style for each band
            val bandStyles: QueueBand => CellStyle =
              QueueBand.values.map { b =>
                val color = b match {
                  case QueueBand.QBand1 => IndexedColors.LIGHT_YELLOW.getIndex
                  case QueueBand.QBand2 => IndexedColors.LIGHT_GREEN.getIndex
                  case QueueBand.QBand3 => IndexedColors.PALE_BLUE.getIndex
                  case QueueBand.QBand4 => IndexedColors.GREY_25_PERCENT.getIndex
                }
              val style = sh.getWorkbook.createCellStyle
              style.setFillPattern(FillPatternType.SOLID_FOREGROUND)
              style.setFillForegroundColor(color)
              style.setBorderBottom(BorderStyle.THIN)
              style.setBorderTop(BorderStyle.THIN)
              style.setBorderLeft(BorderStyle.THIN)
              style.setBorderRight(BorderStyle.THIN)
              style.setBottomBorderColor(IndexedColors.GREY_50_PERCENT.getIndex)
              style.setTopBorderColor(IndexedColors.GREY_50_PERCENT.getIndex)
              style.setLeftBorderColor(IndexedColors.GREY_50_PERCENT.getIndex)
              style.setRightBorderColor(IndexedColors.GREY_50_PERCENT.getIndex)
              (b -> style)
            } .toMap

            var n = 1
            val qr = QueueResult(qc)
            QueueBand.values.foreach { b =>
              qr.entries(b).foreach { e =>

                val m = Merge.merge(e.proposals.map(_.p1mutableProposal))
                val ngoInfo = PrimaryNgo.find(m)

                // for now just take the head
                val p = e.proposals.head
                val r = sh.createRow(n)

                def addCell[A](col: Int, value: A)(implicit ev: Excel[A]): Unit = {
                  val c = r.createCell(col)
                  c.setCellStyle(bandStyles(b))
                  ev.setValue(c, value)
                }

                val mpi = m.getInvestigators().getPi()

                addCell(Reference, p.ntac.reference)
                addCell(Band, b.number)
                addCell(ProgId, e.programId.toString)
                addCell(PIName, s"${mpi.getLastName}, ${mpi.getFirstName}")
                addCell(Time, (e.proposals.foldMap(_.time)).toHours.value)

                val insts = e.proposals.toList.flatMap { p =>
                  p.obsListFor(b).map { o =>
                    o.p1Observation.blueprint.foldMap {
                      case VisitorBlueprint(_, name) => name.trim
                      case b                         => b.name.takeWhile(_ != ' ')
                    }
                  }
                } .distinct.sorted

                addCell(Instrument, insts.mkString(", "))

                addCell(ToO, p.too match {
                  case Too.none     => ""
                  case Too.rapid    => "RT"
                  case Too.standard => "ST"
                })

                val lgs = e.proposals.toList.exists { p =>
                  p.obsListFor(b).exists { o =>
                    o.p1Observation.blueprint match {
                      case Some(b: GeminiBlueprintBase) => b.ao.toBoolean
                      case _ => false
                    }
                  }
                }

                addCell(LGS, lgs)
                addCell(NGO, ngoInfo.foldMap(_.partner.value.toUpperCase()))
                addCell(NgoEmail, ngoInfo.flatMap(_.ngoEmail).orEmpty)
                addCell(GeminiEmail, "")
                addCell(Title, p.p1proposal.title)
                addCell(ObsConstraints, p.obsListFor(b).map(_.conditions.toString).distinct.sorted.mkString(" / "))

                val ras = p.obsList.map(_.target.ra.toHr.mag)
                ras.minimumOption.fold(addCell(RaMin, ""))(addCell(RaMin, _))
                ras.maximumOption.fold(addCell(RaMax, ""))(addCell(RaMax, _))

                addCell(SchedConstraints, p.p1proposal.scheduling)

                n += 1
              }
            }

            // Done
            ExitCode.Success

          }
        }
    }

}