// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.operation

import cats._
import cats.effect.Blocker
import cats.effect.ExitCode
import cats.implicits._
import edu.gemini.spModel.core.Semester
import io.chrisdavenport.log4cats.Logger
import io.circe.syntax._
import io.circe.yaml.syntax._
import itac.config.LocalDateRange
import itac.Operation
import itac.Workspace
import java.time.LocalDate
import edu.gemini.spModel.core.Site
import cats.effect.ContextShift
import cats.effect.Sync
import itac.EmailTemplateRef

object Init {

  def apply[F[_]: Sync: ContextShift: Parallel](semester: Semester): Operation[F] =
    new Operation[F] {

      def run(ws: Workspace[F], log: Logger[F], b: Blocker): F[ExitCode] = {

        val initLog = log.withModifiedString("init: " + _)

        def init: F[ExitCode] =
          for {
            _ <- Workspace.WorkspaceDirs.traverse(ws.mkdirs(_))
            _ <- EmailTemplateRef.all.traverse(ws.extractEmailTemplate)
            _ <- ws.writeText(Workspace.Default.CommonConfigFile, initialCommonConfig(semester))
            _ <- Site.values.toList.traverse(s => ws.writeText(Workspace.Default.queueConfigFile(s), initialSiteConfig(s)))
            _ <- Site.values.toList.traverse(s => Rollover(s, None).run(ws, log, b))
          } yield ExitCode.Success

        for {
          _   <- initLog.trace(s"semester is $semester")
          cwd <- ws.cwd
          ok  <- ws.isEmpty
          ec  <- if (ok) init <* initLog.info(s"initialized ITAC workspace in $cwd")
                 else initLog.error(s"working directory is not empty: $cwd").as(ExitCode.Error)
        } yield ec

      }

  }

  // So, we're doing it this way because we want to write comments. There is a test that ensures
  // this is valid YAML that decodes correctly into a CommonConfig.
  def initialCommonConfig(semester: Semester): String = {

    val start: LocalDate = LocalDate.of(semester.getYear, semester.getHalf.getStartMonth, 1)

    def shutdown(offsetDays: Int, lengthDays: Int): LocalDateRange =
      LocalDateRange(start.plusDays(offsetDays.toLong), start.plusDays((offsetDays + lengthDays).toLong)).right.get

    val (sd1, sd2, sd3) = (shutdown(20, 3), shutdown(40, 4), shutdown(70, 2))

    s"""|
        |# This is the common configuration file, shared by all queues. You can change any values here, as
        |# long as the format remains the same. You can also add comment lines.
        |
        |# The queue semester.
        |semester: $semester
        |
        |# Shutdown periods, given as date ranges of the form YYYYMMDD-YYYYMMDD during which observing is not
        |# possible. Use this to black out dates or simply reduce time during periods when weather loss is
        |# common. This isn't the ideal way to do this but we will continue living with it for a bit longer.
        |shutdown:
        |  gn:
        |  - ${sd1.asJson.asYaml.spaces2.trim}
        |  - ${sd2.asJson.asYaml.spaces2.trim}
        |  - ${sd3.asJson.asYaml.spaces2.trim}
        |  gs:
        |  - ${sd1.asJson.asYaml.spaces2.trim}
        |  - ${sd2.asJson.asYaml.spaces2.trim}
        |  - ${sd3.asJson.asYaml.spaces2.trim}
        |
        |# Partner shares and sites for this semester, along with partner contact information.
        |partners:
        |  KR:
        |    email: dummy@example.org
        |    percent: 4.5
        |    sites: GN GS
        |  SUBARU:
        |    email: dummy@example.org
        |    percent: 0.0
        |    sites: GN GS
        |  CFH:
        |    email: dummy@example.org
        |    percent: 0.0
        |    sites: GN GS
        |  CL:
        |    email: dummy@example.org
        |    percent: 10.6
        |    sites: GS
        |  KECK:
        |    email: dummy@example.org
        |    percent: 0.0
        |    sites: GN GS
        |  BR:
        |    email: dummy@example.org
        |    percent: 5.85
        |    sites: GN GS
        |  AU:
        |    email: dummy@example.org
        |    percent: 0.0
        |    sites: GN GS
        |  CA:
        |    email: dummy@example.org
        |    percent: 13.0
        |    sites: GN GS
        |  UH:
        |    email: dummy@example.org
        |    percent: 10.6
        |    sites: GN
        |  AR:
        |    email: dummy@example.org
        |    percent: 2.8
        |    sites: GN GS
        |  US:
        |    email: dummy@example.org
        |    percent: 48.05
        |    sites: GN GS
        |  LP:
        |    email: dummy@example.org
        |    percent: 15.22
        |    sites: GN GS
        |
        |# Partner sequences for both sites.
        |sequence:
        |  gn: US LP LP US BR US AR US UH US KR CA US US US US CA LP LP KR UH UH US US US BR CA CA US US US US LP LP KR CA CA US US US BR BR US US US UH UH US US US LP BR LP US US US CA CA CA US US US US AR US US US US KR LP US US US UH UH UH CA CA US US KR BR AR US US US CA UH LP US US CA CA UH US US LP LP LP US
        |  gs: US LP LP US BR US AR US CL US KR CA US US US US CA LP LP KR CL CL US US US BR CA CA US US US US LP LP KR CA CA US US US BR BR US US US CL CL US US US LP BR LP US US US CA CA CA US US US US AR US US US US KR LP US US US CL CL CL CA CA US US KR BR AR US US US CA CL LP US US CA CA CL US US LP LP LP US
        |
        |# Conditions bins and percentages for each, which must add up to 100.
        |conditionsBins:
        |  - name: "super-seeing, dark, photometric"
        |    conditions: "CC50 IQ20 <=SB20"
        |    available: 5
        |  - name: "super-seeing, grey/bright, photometric"
        |    conditions: "CC50 IQ20 <=SB80"
        |    available: 5
        |  - name: "super-seeing, cloudy"
        |    conditions: "CC80 IQ20 SBAny"
        |    available: 10
        |  - name: "good-seeing, dark, photometric"
        |    conditions: "CC50 IQ70 <=SB20"
        |    available: 15
        |  - name: "good-seeing, grey/bright, photometric"
        |    conditions: "CC50 IQ70 <=SB80"
        |    available: 15
        |  - name: "poor-seeing, photometric"
        |    conditions: "CC50 IQ85 SBAny"
        |    available: 15
        |  - name: "poor-seeing cloudy"
        |    conditions: "CC80 IQ85 SBAny"
        |    available: 20
        |  - name: "good-seeing, cloudy"
        |    conditions: "CC80 IQ70 SBAny"
        |    available: 25
        |  - name: "terrible seeing"
        |    conditions: "IQAny SBAny"
        |    available: 30
        |
        |""".stripMargin
  }

  def initialSiteConfig(site: Site): String =
    s"""|
        |# This is a queue configuration file. You can change any of the values here as long as the format
        |# remains the same. You can also add comment lines.
        |
        |# Site
        |site: ${site.abbreviation}
        |
        |# Observing hours per partner in each band.
        |hours:
        |  US: 226.5	247.8	165.2
        |  CA:  60.5	 66.2	 44.2
        |  AR:  11.3  11.3   7.5
        |  BR:  23.8  23.8  15.9
        |  KR:  18.2  18.2  12.2
        |  CL:  43.2  43.2  28.8
        |  LP:  27.0   0.0   0.0
        |
        |# RA bin size, in minutes. Must evenly divide 24 * 60 = 1440
        |raBinSize: 180
        |
        |# Dec bin size, in degrees. Must evenly divide 180.
        |decBinSize: 20
        |
        |# NOT IMPLEMENTED -- how does this work?
        |# Which partner gets the initial pick?
        |initialPick: US
        |
        |# Queue filling limit (percentage over 80).
        |overfill: 5
        |""".stripMargin

}