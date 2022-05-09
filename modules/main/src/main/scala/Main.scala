// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac

import cats.data.NonEmptyList
import cats.data.Validated
import cats.data.ValidatedNel
import cats.effect._
import cats.implicits._
import com.monovore.decline.Argument
import com.monovore.decline.Command
import com.monovore.decline.effect.CommandIOApp
import com.monovore.decline.Opts
import edu.gemini.spModel.core.Semester
import edu.gemini.spModel.core.Site
import edu.gemini.tac.qengine.impl.{ QueueEngine2 => QueueEngine }
import gsp.math.Angle
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import itac.operation._
import java.nio.file.Path
import java.nio.file.Paths
import java.text.ParseException
import org.slf4j.impl.ColoredSimpleLogger
import scala.util.control.NonFatal
import itac.config.PerSite
import edu.gemini.spModel.core.ProgramId

// object Stub {
//   def main(args: Array[String]): Unit =
//     Main.main(Array("-d", "test-ws", "queue", "-n"))
// }

object Main extends CommandIOApp(
  name    = "itac",
  header  =
    s"""|For more information pass `--help` to a subcommand, for instance `itac init --help`.
        |
        |ITAC Command Line Interface ${BuildInfo.version}
        |
        |This is a simple program that generates queues by reading proposal XML files and a set of
        |configuration files that you (the user) edit to fine-tune the queue to your liking. There
        |is no database. The workspace contains all the information about queue generation. There
        |are no external dependencies.
        |
        |The general workflow is:
        |
        | - use `itac init` to create a new workspace
        | - copy proposal XML files into the proposals/ directory in your workspace
        | - edit the common.yaml file as necessary (it contains instructions)
        | - edit the site-specific configuration files as necessary (they contain instructions)
        | - use `itac queue` to generate a queue
        | - refine your configurations (and/or create new ones) until you're happy with the queue
        | - export program skeletons to the observing database with `itac skeleton`
        | - edit email templates as necessary
        | - notify PIs with `itac email`
        |
        |Note that itac will never change proposal XML files, nor will it override any existing file
        |unless you specify the `--force` option. It is ok to experiment, you're not going to cause
        |any damage by doing so.
        |
        |The options below are valid for all sub-commands. For information on specific sub-commands
        |try `itac <sub-command> --help`, for example `itac rollover --help`.
        |""".stripMargin.trim
) with MainOpts {

  def main: Opts[IO[ExitCode]] =
    (cwd, commonConfig, logger[IO], force, ops).mapN { (cwd, commonConfig, log, force, cmd) =>
      Blocker[IO].use { b =>
        for {
          _  <- IO(System.setProperty("edu.gemini.model.p1.schemaVersion", "2020.2.1")) // how do we figure out what to do here?
          _  <- log.debug(s"main: workspace directory is ${cwd.toAbsolutePath}")
          c  <- Workspace[IO](cwd, commonConfig, log, force).flatMap(cmd.run(_, log, b)).handleErrorWith {
                  case ItacException(msg) => log.error(msg).as(ExitCode.Error)
                  case NonFatal(e)        => log.error(e)(e.getMessage).as(ExitCode.Error)
                }
          _  <- log.trace(s"main: exiting with ${c.code}")
        } yield c
      }
    }

}

// decline opts used by main, sliced off because it seems more tidy
trait MainOpts { this: CommandIOApp =>

  lazy val cwd: Opts[Path] =
    Opts.option[Path](
      short = "d",
      long  = "dir",
      help  = "Workspace directory. Default is current directory."
    ) .withDefault(Paths.get("."))

  lazy val commonConfig: Opts[Path] =
    Opts.option[Path](
      short = "c",
      long  = "common",
      help  = s"Common configuation file, relative to workspace (or absolute). Default is ${Workspace.Default.CommonConfigFile}"
    ).withDefault(Workspace.Default.CommonConfigFile)

  lazy val siteConfig: Opts[Path] =
    site.map(Workspace.Default.queueConfigFile) <+> Opts.option[Path](
      short = "-c",
      long  = "config",
      help  = s"Site configuration file, relative to workspace (or absolute). Default is ${Site.values.toList.map(Workspace.Default.queueConfigFile).mkString(" or ")}"
    )

  lazy val rolloverReport: Opts[Option[Path]] =
    Opts.option[Path](
      short = "-r",
      long  = "rollover",
      help  = s"Rollover report, relative to workspace (or absolute). Default is <site>-rollovers.yaml"
    ).orNone

  def out(default: Path): Opts[Path] =
    Opts.option[Path](
      short = "o",
      long  = "out",
      help  = s"Output file. Default is $default"
    ).withDefault(default)

  lazy val semester: Opts[Semester] =
    Opts.argument[String]("semester")
      .mapValidated { s =>
        Validated
          .catchOnly[ParseException](Semester.parse(s))
          .leftMap(_ => NonEmptyList.of(s"Not a valid semester: $s"))
      }

  def logger[F[_]: Sync]: Opts[Logger[F]] =
    Opts.option[String](
      long    = "verbose",
      short   = "v",
      metavar = "level",
      help    = "Log verbosity. One of: trace debug info warn error off. Default is info."
    ) .withDefault("info")
      .mapValidated {
        case s @ ("trace" | "debug" | "info" | "warn" | "error" | "off") =>

          // Construct a logger that we will use in the application. Note that this gets hooked up
          // to the Velocity engine used by the Email operation, so look over there if Velocity
          // logging isn't doing what you expect.

          // http://www.slf4j.org/api/org/slf4j/impl/SimpleLogger.html
          System.setProperty("org.slf4j.simpleLogger.log.edu", s)
          System.setProperty("org.slf4j.simpleLogger.log.org", s)
          ColoredSimpleLogger.init() // slf4j forces us to be dumb
          val log = new ColoredSimpleLogger("edu.gemini.itac")
          Slf4jLogger.getLoggerFromSlf4j(log).validNel[String]

        case s => s"Invalid log level: $s".invalidNel
      }

  lazy val init: Command[Operation[IO]] =
    Command(
      name   = "init",
      header =
        s"""|Initialize an ITAC workspace in the current directory (override with --dir). Requires
            |an internal network connection.
            |""".stripMargin.trim
    )(semester.map(Init[IO]))

  lazy val ls: Command[Operation[IO]] =
    Command(
      name   = "ls",
      header = "List proposals in the workspace."
    )((lsFields, lsPartners).mapN(Ls[IO](_, _)))

  lazy val lsPartners: Opts[List[String]] =
      Opts.arguments[String](
        metavar = "partner"
      ).orEmpty

  lazy val queue: Command[Operation[IO]] =
    Command(
      name   = "queue",
      header = "Generate a queue."
    )((siteConfig, rolloverReport).mapN((sc, rr) => Queue[IO](QueueEngine, sc, rr)))

  lazy val staffEmailSpreadsheet: Command[Operation[IO]] =
    Command(
      name   = "staff-email-spreadsheet",
      header = "Generate the staff email spreadsheet."
    )(StaffEmailSpreadsheet[IO](QueueEngine, PerSite.unfold(Workspace.Default.queueConfigFile), PerSite.unfold(Workspace.Default.rolloverReport)).pure[Opts])

  lazy val ngoSpreadsheet: Command[Operation[IO]] =
    Command(
      name   = "ngo-spreadsheets",
      header = "Generate NGO spreadsheets."
    )(NgoSpreadsheet[IO](QueueEngine, PerSite.unfold(Workspace.Default.queueConfigFile), PerSite.unfold(Workspace.Default.rolloverReport)).pure[Opts])

  lazy val instrumentSpreadsheet: Command[Operation[IO]] =
    Command(
      name   = "instrument-spreadsheets",
      header = "Generate instrument scientist spreadsheets."
    )(InstrumentScientistSpreadsheet[IO](QueueEngine, PerSite.unfold(Workspace.Default.queueConfigFile), PerSite.unfold(Workspace.Default.rolloverReport)).pure[Opts])

  lazy val directorSpreadsheet: Command[Operation[IO]] =
    Command(
      name   = "director-spreadsheet",
      header = "Generate Director spreadsheet."
    )(DirectorSpreadsheet[IO](QueueEngine, PerSite.unfold(Workspace.Default.queueConfigFile), PerSite.unfold(Workspace.Default.rolloverReport)).pure[Opts])
  lazy val host: Opts[String] =
    Opts.option[String](
      long = "host",
      short = "h",
      help = "ODB hostname, like gnodb.hi.gemini.edu"
    )

  lazy val DefaultOdbPort = 8442

  lazy val port: Opts[Int] =
    Opts.option[Int](
      long = "port",
      short = "p",
      help = s"ODB port number, default = $DefaultOdbPort"
    ).withDefault(DefaultOdbPort)

  lazy val progids: Opts[List[ProgramId]] =
    Opts.arguments[String](
      metavar = "program-id"
    ).map(_.map(ProgramId.parse)).orEmpty

  lazy val export: Command[Operation[IO]] =
    Command(
      name   = "export",
      header = "Export proposals to the specified ODB. You can re-run this if necessary (programs will be replaced)."
    )((siteConfig, rolloverReport, host, port, progids).mapN(Export[IO](QueueEngine, _, _, _, _, _)))

  lazy val testExport: Command[Operation[IO]] =
    Command(
      name   = "test-export",
      header = "Export proposals to the console."
    )((siteConfig, rolloverReport, progids).mapN(TestExport[IO](QueueEngine, _, _, _)))

  lazy val emailGen: Command[Operation[IO]] =
    Command(
      name   = "gen-emails",
      header = "Create PI emails for successful proposals."
    )((siteConfig, rolloverReport, progids).mapN(EmailGen[IO](QueueEngine, _, _, _)))

  lazy val bulkEdits: Command[Operation[IO]] =
    Command(
      name = "bulk-edits",
      header = "Create bulk-edits.xml"
    )(BulkEdits[IO].pure[Opts])

  lazy val chartData: Command[Operation[IO]] =
    Command(
      name   = "chart-data",
      header = "Create chart data for the specified queue."
    )((siteConfig, rolloverReport).mapN(ChartData[IO](QueueEngine, _, _)))

  lazy val scheduling: Command[Operation[IO]] =
    Command(
      name   = "scheduling",
      header = "Scheduling report."
    )((siteConfig, rolloverReport).mapN(Scheduling[IO](QueueEngine, _, _)))

  lazy val blueprints: Command[Operation[IO]] =
    Command(
      name   = "blueprints",
      header = "Blueprints report."
    )((siteConfig, rolloverReport).mapN((sc, rr) => Blueprints[IO](QueueEngine, sc, rr)))

  lazy val gn: Opts[Site.GN.type] = Opts.flag(
    short = "n",
    long  = "north",
    help  = Site.GN.displayName,
  ).as(Site.GN)

  lazy val gs: Opts[Site.GS.type] = Opts.flag(
    short = "s",
    long  = "south",
    help  = Site.GS.displayName,
  ).as(Site.GS)

  lazy val force: Opts[Boolean] =
    Opts.flag(
      long  = "force",
      short = "f",
      help  = "Overwrite existing files (normally itac will not do this)."
    ).orFalse

  lazy val site = gn <+> gs

  lazy val rollover: Command[Operation[IO]] =
    Command(
      name   = "rollover",
      header =
        s"""|Generate a rollover report by fetching information from the specified observing
            |database. Requires an internal network connection.
            |""".stripMargin.trim
    )((site, Opts.option[Path](
      short = "o",
      long  = "out",
      help  = s"Output file. Default is GN-rollover.yaml or GS-rollover.yaml"
    ).orNone).mapN(Rollover(_, _)))

  object Placeholder extends Operation[IO] {
    def run(ws: Workspace[IO], log: Logger[IO], b: Blocker): IO[ExitCode] =
      log.error("This feature is not yet implemented").as(ExitCode.Error)
  }

  def placeholder(name: String): Command[Operation[IO]] =
    Command(
      name = name,
      header = s"Placeholder for $name, which is not yet implemented."
    )(Placeholder.pure[Opts])

  lazy val reference: Opts[String] =
    Opts.argument[String]("semester")

  implicit lazy val ArgumentAngle: Argument[Angle] =
    new Argument[Angle] {
      def defaultMetavar: String = "dd:mm:ss.xxx"
      def read(string: String): ValidatedNel[String,Angle] =
        Angle.fromStringDMS.getOption(string).toValidNel(s"Expected dd:mm:ss.xxx, found $string")
    }

  lazy val tolerance: Opts[Angle] =
    Opts.option[Angle](
      short = "t",
      long = "tolerance",
      help = "Angular separation of targets that might be the same. Default 00:00:10.00"
    ).withDefault(Angle.fromDoubleArcseconds(10))

  implicit lazy val ArgumentNelLsField: Argument[NonEmptyList[Ls.Field]] =
    new Argument[NonEmptyList[Ls.Field]] {
      def defaultMetavar: String = "sort"
      def read(string: String): ValidatedNel[String,NonEmptyList[Ls.Field]] =
        Ls.Field.parse(string).toValidatedNel
    }

  lazy val lsFields: Opts[NonEmptyList[Ls.Field]] =
    Opts.option[NonEmptyList[Ls.Field]](
      short = "s",
      long  = "sort",
      help  = s"Fields to sort by, comma-delimited. One or more of ${Ls.Field.all.map(_.name).mkString(",")}. Default is band,ra"
    ) .withDefault(NonEmptyList.of(Ls.Field.partner, Ls.Field.rank))

  implicit lazy val ArgumentNelSummarizeField: Argument[NonEmptyList[Summarize.Field]] =
    new Argument[NonEmptyList[Summarize.Field]] {
      def defaultMetavar: String = "sort"
      def read(string: String): ValidatedNel[String,NonEmptyList[Summarize.Field]] =
        Summarize.Field.parse(string).toValidatedNel
    }

  lazy val summarizeFields: Opts[NonEmptyList[Summarize.Field]] =
    Opts.option[NonEmptyList[Summarize.Field]](
      short = "s",
      long  = "sort",
      help  = s"Fields to sort by, comma-delimited. One or more of ${Summarize.Field.all.map(_.name).mkString(",")}. Default is band,ra"
    ) .withDefault(NonEmptyList.of(Summarize.Field.band, Summarize.Field.ra))

  lazy val edit: Opts[Boolean] =
    Opts.flag(
      long  = "edit",
      short = "e",
      help  = "Write the summary to the edits/ folder."
    ).orFalse

  implicit lazy val ArgumentSite: Argument[Site] =
    new Argument[Site] {
      def read(string: String): ValidatedNel[String, Site] =
        try Site.parse(string).validNel
        catch {
          case _: ParseException => "Invalid site. Try GN or GS.".invalidNel
        }
      def defaultMetavar: String = "site"
    }

  lazy val disable: Opts[Option[Site]] =
    Opts.option[Site](
      long  = "disable",
      short = "d",
      help  = "Use with -e to disable all observations at the specified site."
    ) .orNone

  lazy val summarize: Command[Operation[IO]] =
    Command(
      name   = "summarize",
      header = "Summarize a proposal."
    )((Opts.argument[String]("reference"), summarizeFields, edit, disable).mapN(Summarize(_, _, _, _)))

  lazy val splits: Command[Operation[IO]] =
    Command(
      name   = "splits",
      header = "List split/joint proposals."
    )(Splits[IO].pure[Opts])

  lazy val emailSend: Command[Operation[IO]] =
    Command(
      name   = "send-emails",
      header = "Sends all emails in the emails/ folder."
    )(Opts.flag("dry-run", "Test, but don't actually send emails.", "n").orFalse.map(EmailSend[IO](_)))

  lazy val duplicates: Command[Operation[IO]] =
    Command(
      name   = "duplicates",
      header = "Search for duplicate targets."
    )((siteConfig, rolloverReport, tolerance).mapN((sc, rr, t) => Duplicates[IO](QueueEngine, sc, rr, t)))

  lazy val ops: Opts[Operation[IO]] =
    List(
      emailGen,
      placeholder("skeleton"),
      init,
      ls,
      rollover,
      queue,
      summarize,
      duplicates,
      export,
      splits,
      scheduling,
      blueprints,
      chartData,
      staffEmailSpreadsheet,
      ngoSpreadsheet,
      instrumentSpreadsheet,
      directorSpreadsheet,
      bulkEdits,
      emailSend,
      testExport,
    ).sortBy(_.name).map(Opts.subcommand(_)).foldK

}

