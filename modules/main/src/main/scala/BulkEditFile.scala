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

object BulkEditFile {

  object Column {
    val Reference     = 0
    val Site          = 1
    val Pi            = 2
    val NgoEmail      = 3
    val StaffEmail    = 4
    val NtacComment   = 5
    val ItacComment   = 6
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
      val it = sh.rowIterator()
      it.asScala.drop(1).map { r =>
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
        sh.setColumnWidth(Pi,            256 * 20)
        sh.setColumnWidth(NgoEmail,      256 * 20)
        sh.setColumnWidth(StaffEmail,    256 * 20)
        sh.setColumnWidth(NtacComment,   256 * 64)
        sh.setColumnWidth(ItacComment,   256 * 64)
        sh.createFreezePane(0, 1, 0, 1);
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
      create(Pi,            "PI")
      create(NgoEmail,      "NGO Email")
      create(StaffEmail,    "Staff Email")
      create(NtacComment,   "NTAC Comment")
      create(ItacComment,   "ITAC Comment")

    }

    def addNewRowAt(sh: Sheet, row: Int, p: Proposal): Unit = {
      val newR = sh.createRow(row)
      newR.createCell(Reference    ).setCellValue(p.ntac.reference)
      newR.createCell(Site         ).setCellValue(p.site.abbreviation)
      newR.createCell(Pi           ).setCellValue(p.piName.orEmpty)
      newR.createCell(NgoEmail     ).setBlank()
      newR.createCell(StaffEmail   ).setBlank()
      newR.createCell(NtacComment  ).setCellValue(p.ntac.comment.orEmpty)
      newR.createCell(ItacComment  ).setBlank()
    }

    def updateSheet(sh: Sheet, ps: List[Proposal]): Unit = {
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

