// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac

import java.io.File
import org.apache.poi.ss.usermodel._
import org.apache.poi.hssf.usermodel.HSSFWorkbookFactory
import edu.gemini.tac.qengine.p1.Proposal
import cats.implicits._
import cats.kernel.Comparison.EqualTo
import cats.kernel.Comparison.GreaterThan
import cats.kernel.Comparison.LessThan
import org.apache.poi.poifs.filesystem.POIFSFileSystem
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import scala.collection.JavaConverters._
import cats.effect.Sync
import edu.gemini.model.p1.immutable.VisitorBlueprint

object BulkEditFile {

  object Column {
    val Reference     = 0
    val Site          = 1
    val Instrument    = 2
    val Pi            = 3
    val NgoEmail      = 4
    val StaffEmail    = 5
    val NtacComment   = 6
    val ItacComment   = 7
  }

  def createOrUpdate[F[_]: Sync](file: File, ps: List[Proposal]): F[Unit] =
    Sync[F].delay(unsafe.createWorkbook(file, ps)).void

  def read[F[_]: Sync](file: File): F[Map[String, BulkEdit]] =
    Sync[F].delay(unsafe.read(file))

  private object unsafe {
    import Column._

    private implicit class CellOps(cell: Cell) {
      def safeGetStringCellValue: Option[String] =
        Option(cell).map(_.getStringCellValue).filterNot(_.isEmpty)
    }

    def read(file: File): Map[String, BulkEdit] = {
      val wb = HSSFWorkbookFactory.createWorkbook(new POIFSFileSystem(file, false).getRoot)
      val sh = wb.getSheet("Proposals")
      val it = sh.rowIterator.asScala.takeWhile(_.getCell(Reference) != null)
      it.drop(1).map { r =>
        val ref         = r.getCell(Reference).getStringCellValue()
        val ngoEmail    = r.getCell(NgoEmail).safeGetStringCellValue
        val staffEmail  = r.getCell(StaffEmail).safeGetStringCellValue
        val ntacComment = r.getCell(NtacComment).safeGetStringCellValue
        val itacComment = r.getCell(ItacComment).safeGetStringCellValue
        ref -> BulkEdit(ngoEmail, staffEmail, ntacComment, itacComment)
      } .toMap
    }

    def createWorkbook(file: File, ps: List[Proposal]): Workbook = {

      val wb: HSSFWorkbook =
        if (file.exists) HSSFWorkbookFactory.createWorkbook(new POIFSFileSystem(file, false).getRoot())
        else             HSSFWorkbookFactory.createWorkbook()

      val sh: Sheet =
        if (file.exists) wb.getSheet("Proposals")
        else             wb.createSheet("Proposals")

      if (!file.exists()) {
        unsafe.createHeader(sh)
      }

      unsafe.updateSheet(sh, ps)

      if (file.exists()) {
        wb.write()
      } else {
        // only do this when we initially write the file
        sh.setColumnWidth(Reference,     256 * 20)
        sh.setColumnWidth(Site,          256 * 5)
        sh.setColumnWidth(Instrument,    256 * 20)
        sh.setColumnWidth(Pi,            256 * 20)
        sh.setColumnWidth(NgoEmail,      256 * 20)
        sh.setColumnWidth(StaffEmail,    256 * 20)
        sh.setColumnWidth(NtacComment,   256 * 64)
        sh.setColumnWidth(ItacComment,   256 * 64)
        sh.createFreezePane(3, 1, 3, 1);
        sh.setZoom(120)
        wb.write(file)
      }
      wb
    }

    def createHeader(sh: Sheet): Unit = {

      // Row zero
      val r = sh.createRow(0)

      // A bold font!
      val font = sh.getWorkbook.createFont
      font.setBold(true)

      // A bold style!
      val style = sh.getWorkbook.createCellStyle
      style.setFont(font)

      // Helper to create a header cell
      def create(col: Int, value: String): Unit = {
        val c = r.createCell(col)
        c.setCellStyle(style)
        c.setCellValue(value)
      }

      // Create them all!
      create(Reference,     "Reference")
      create(Site,          "Site")
      create(Instrument,    "Instrument")
      create(Pi,            "PI")
      create(NgoEmail,      "NGO Email")
      create(StaffEmail,    "Staff Email")
      create(NtacComment,   "NTAC Comment")
      create(ItacComment,   "ITAC Comment")

    }

    def instruments(p: Proposal): String =
      (p.obsList ++ p.band3Observations)
        .map { o =>
          o.p1Observation.blueprint.foldMap {
            case VisitorBlueprint(_, name) => name.trim
            case b                         => b.name.takeWhile(_ != ' ')
          }
        }
        .distinct
        .sorted
        .mkString(", ")

    def addNewRowAt(sh: Sheet, row: Int, p: Proposal): Unit = {
      val newR = sh.createRow(row)
      newR.createCell(Reference    ).setCellValue(p.ntac.reference)
      newR.createCell(Site         ).setCellValue(p.site.abbreviation)
      newR.createCell(Pi           ).setCellValue(p.piName.orEmpty)
      newR.createCell(Instrument   ).setCellValue(instruments(p))
      newR.createCell(NgoEmail     ).setCellValue(p.ntac.ngoEmail.orEmpty)
      newR.createCell(StaffEmail   ).setBlank()
      newR.createCell(NtacComment  ).setCellValue(p.ntac.comment.orEmpty)
      newR.createCell(ItacComment  ).setBlank()
    }

    def updateSheet(sh: Sheet, ps: List[Proposal]): Unit = {

      // If the sheet only has 7 columns it's from an earlier version and we need to add and
      // populate the instrument column. N.B. cell number is 1-based
      if (sh.getRow(0).getLastCellNum == 7) {

        // Move everything over
        sh.shiftColumns(Instrument, Instrument, 1)

        // A bold font!
        val font = sh.getWorkbook.createFont
        font.setBold(true)

        // A bold style!
        val style = sh.getWorkbook.createCellStyle
        style.setFont(font)

        // Add the header
        val c = sh.getRow(0).createCell(Instrument)
        c.setCellStyle(style)
        c.setCellValue("Instrument")

        // Add data
        sh.rowIterator().asScala.drop(1).foreach { r =>
          val c = r.createCell(Instrument)
          val ref = r.getCell(Reference).getStringCellValue()
          ps.find(_.ntac.reference == ref).foreach { p =>
            c.setCellValue(instruments(p))
          }
        }

      }

      ps.sortBy(_.ntac.reference).foldLeft(1) { (row, p) =>

        // Try to add `p` at `row` and yield the new value for `row`
        def go(row: Int): Int =
          Option(sh.getRow(row)) match {

            // No such row! Make one.
            case None =>
              addNewRowAt(sh, row, p)
              row + 1

            // Row exists, is it ours?
            case Some(r) =>
              val ref = r.getCell(0).getStringCellValue
              ref.comparison(p.ntac.reference) match {
                case LessThan    => go(row + 1) // skip this one, it's not in our list anymore
                case EqualTo     => row + 1     // it's here already, nothing to do
                case GreaterThan =>
                  // shift everything down to make room, then add a row
                  sh.shiftRows(row, sh.getLastRowNum, 1)
                  addNewRowAt(sh, row, p)
                  row + 1
              }

          }

        go(row)
      }
      () // done
    }

  }

}

