// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
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
import org.apache.poi.hssf.usermodel.HSSFWorkbookFactory
import cats.effect.Sync
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import edu.gemini.tac.qengine.p1.QueueBand
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.BorderStyle
import edu.gemini.tac.qengine.ctx.Partner
import cats.implicits._
import java.nio.file.Paths
import scala.math.BigDecimal.RoundingMode
import edu.gemini.tac.qengine.p1.Proposal
import _root_.operation.ProgramPartnerTime


object NgoSpreadsheet {

  // ffs
  trait Excel[A] {
    def setValue(c: Cell, a: A): Unit
  }
  object Excel {
    implicit val ExcelInt: Excel[Int] = (c, a) => c.setCellValue(a.toDouble)
    implicit val ExcelDouble: Excel[Double] = (c, a) => c.setCellValue((a: BigDecimal).setScale(1, RoundingMode.HALF_UP).toDouble)
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
          wbs <- Partner.all.traverse(p => Sync[F].delay((p, HSSFWorkbookFactory.createWorkbook))) // List[(Partner, Workbook)]
          _   <- Site.values.toList.traverse_(s => addSheetsForSite(wbs, qe, siteConfig.forSite(s), Some(rolloverReport.forSite(s))).run(ws, log, b))
          cwd <- ws.cwd
          _   <- wbs.traverse { case (p, wb) => Sync[F].delay(wb.write(cwd.resolve(Paths.get(s"itac-results-${p.id}.xls")).toFile)) }
        } yield ExitCode.Success
    }

  def addSheetsForSite[F[_]: Sync](
    wbs:            List[(Partner, Workbook)],
    qe:             QueueEngine,
    siteConfig:     Path,
    rolloverReport: Option[Path],
  ): Operation[F] =
    new AbstractQueueOperation[F](qe, siteConfig, rolloverReport) {
      def run(ws: Workspace[F], log: Logger[F], b: Blocker): F[ExitCode] =
        for {
          // TODO: include removed proposals
          p   <- computeQueue(ws)
          (ps, qc) = p
          _   <- wbs.traverse { case (p, wb) => Sync[F].delay(writeSheet(wb, p, ps, QueueResult(qc))) }
        } yield ExitCode.Success
    }

  val ProgId      =  0 // Gemini Program ID: @PROG_ID@
  val Band        =  1 // Scientific Ranking Band: @QUEUE_BAND@
  val Reference   =  2 // Program xml file number : @NTAC_REF_NUMBER@
  val Rank        =  3 // proposal rank : @rank@
  val PIName      =  4 // Principal Investigator: @PI_NAME@
  val PiEmail     =  5 // Principal Investigator e-mail: @PI_MAIL@
  val Time        =  6 // Time awarded: @TIME_AWARDED@
  val ProgTime    =  7 // Program Time Awarded: @PROG_TIME@
  val PartTime    =  8 // Partner Calibration Time Awarded: @PARTNER_TIME@
  val ItacComment =  9 // ITAC Feedback: @ITAC_COMMENTS@
  val Title       = 10 // Program Title: @PROG_TITLE@

  def writeSheet(wb: Workbook, p: Partner, ps: List[Proposal], qr: QueueResult): Unit = {
    val sh = wb.createSheet(qr.context.site.displayName)

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
    create(ProgId, "Gemini Id", 18)
    create(Band, "Band", 5)
    create(Reference, "NGO Reference", 17)
    create(Rank, "Rank", 5)
    create(PIName, "PI Name", 25)
    create(Time, "Time Awarded", 15)
    create(ProgTime, "Program Time", 15)
    create(PartTime, "Partner Cal Time", 15)
    create(PiEmail, "PI Email", 25)
    create(ItacComment, "ITAC Comment", 100)
    create(Title, "Title", 100)

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
    QueueBand.values.foreach { b =>

      val es: List[QueueResult.Entry] =
        b match {
          case QueueBand.QBand1 => qr.classical(ps, p) ++ qr.entries(b, p)
          case _                => qr.entries(b, p)
        }

      es.foreach { e =>

        val p = e.proposals.head // there will never be more than one because this is per-partner (hm, should change the entry type)
        val r = sh.createRow(n)

        def addCell[A](col: Int, value: A)(implicit ev: Excel[A]): Unit = {
          val c = r.createCell(col)
          c.setCellStyle(bandStyles(b))
          ev.setValue(c, value)
        }

        val pi = p.p1proposal.investigators.pi
        val (progTime, partTime) = ProgramPartnerTime.programAndPartnerTime(p, b)

        addCell(Reference, p.ntac.reference)
        addCell(Rank, p.ntac.ranking.num.orEmpty)
        addCell(Band, b.number)
        addCell(ProgId, e.programId.toString)
        addCell(PIName, s"${pi.lastName}, ${pi.firstName}")
        addCell(PiEmail, p.p1proposal.investigators.pi.email)
        addCell(Time, (e.proposals.foldMap(_.time)).toHours.value)
        addCell(ProgTime, progTime.toHours.value)
        addCell(PartTime, partTime.toHours.value)
        addCell(ItacComment, p.itacComment.orEmpty)
        addCell(Title, p.p1proposal.title)
        n += 1

      }
    }

    // A style for unsuccessful
    val unsuccessfulStyle:CellStyle = {
      val style = sh.getWorkbook.createCellStyle
      style.setBorderBottom(BorderStyle.THIN)
      style.setBorderTop(BorderStyle.THIN)
      style.setBorderLeft(BorderStyle.THIN)
      style.setBorderRight(BorderStyle.THIN)
      style.setBottomBorderColor(IndexedColors.GREY_50_PERCENT.getIndex)
      style.setTopBorderColor(IndexedColors.GREY_50_PERCENT.getIndex)
      style.setLeftBorderColor(IndexedColors.GREY_50_PERCENT.getIndex)
      style.setRightBorderColor(IndexedColors.GREY_50_PERCENT.getIndex)
      style
    }

    // Unsuccessful
    qr.unsuccessful(ps, p).foreach { p =>

      val r = sh.createRow(n)

      def addCell[A](col: Int, value: A)(implicit ev: Excel[A]): Unit = {
        val c = r.createCell(col)
        c.setCellStyle(unsuccessfulStyle)
        ev.setValue(c, value)
      }

      val pi = p.p1proposal.investigators.pi

      addCell(Reference, p.ntac.reference)
      addCell(Rank, p.ntac.ranking.num.orEmpty)
      addCell(Band, "")
      addCell(ProgId, "")
      addCell(PIName, s"${pi.firstName} ${pi.lastName}")
      addCell(PiEmail, p.p1proposal.investigators.pi.email)
      addCell(Time, 0.0)
      addCell(ProgTime, 0.0)
      addCell(PartTime, 0.0)
      addCell(ItacComment, p.itacComment.orEmpty)
      addCell(Title, p.p1proposal.title)
      n += 1

    }

  }



}