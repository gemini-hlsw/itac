//package edu.gemini.tac.qservice.impl.shutdown
//
//import util.parsing.combinator.JavaTokenParsers
//import edu.gemini.tac.qengine.ctx.Site
//import java.util.{Date, Calendar, GregorianCalendar}
//import java.text.SimpleDateFormat
//import java.io.InputStream
//
//object ShutdownConfig {
//  def PARSE_ERROR_PREFIX = "Problem parsing shutdown configuration: "
//  def PARSE_ERROR(msg: String) = "%s: %s".format(PARSE_ERROR_PREFIX, msg)
//  def UNPARSEABLE_DATE(s: String) = "Could not parse date '%s".format(s)
//  def REVERSED_DATES = "Shutdown start date must always preceed the end date"
//  def OVERLAPPING_DATES = "Shutdown dates for the same site may not overlap"
//
//  private[impl] case class Line(site: Site, startDate: String, endDate: Option[String])
//
//  private[impl] class ConfigParser extends JavaTokenParsers {
//    def comment: Parser[String] = """#[^\n]*""".r
//
//    def site: Parser[Site] = """G?[NS]""".r ^^ {
//      _ match {
//        case "GN" | "N" => Site.north
//        case _          => Site.south
//      }
//    }
//
//    def date: Parser[String] = """\d\d\d\d/\d\d?/\d\d?""".r
//
//    def line: Parser[Line] =
//      site~date~opt(date) ^^ { case (s~start~end) => Line(s, start, end) }
//
//    def commentOrLine: Parser[Option[Line]] =
//      (comment | line) ^^ {
//        _ match {
//          case l: Line => Some(l)
//          case _       => None
//        }
//      }
//
//    def config: Parser[List[Line]] =
//      rep(commentOrLine) ^^ {
//        optLines => for {
//          Some(l) <- optLines
//        } yield l
//      }
//  }
//
//  private[impl] object ConfigParser extends ConfigParser {
//    def parse(configStr: String): Either[String, List[Line]] = {
//      parseAll(config, configStr) match {
//        case Success(lines, _) => Right(lines)
//        case NoSuccess(msg, _) => Left(PARSE_ERROR(msg))
//      }
//    }
//  }
//
//  private def parseDate(site: Site, dateStr: String): Either[String, Date] = {
//    val sdf = new SimpleDateFormat("yy/MM/dd h:mm a")
//    sdf.setTimeZone(site.timeZone)
//    sdf.setLenient(false)
//    try {
//      Right(sdf.parse("%s 2:00 PM".format(dateStr)))
//    } catch {
//      case ex: Exception => Left(UNPARSEABLE_DATE(dateStr))
//    }
//  }
//
//  private def nextDay(site: Site, date: Date): Date = {
//    val cal = new GregorianCalendar(site.timeZone)
//    cal.setTime(date)
//    cal.add(Calendar.DAY_OF_MONTH, 1)
//    cal.getTime
//  }
//
//  private def toEndDate(line: Line, startDate: Date): Either[String, Date] =
//    line.endDate map {
//      dateStr => parseDate(line.site, dateStr)
//    } getOrElse Right(nextDay(line.site, startDate))
//
//  private def toShutdown(site: Site, start: Date, end: Date): Either[String, Shutdown] =
//    if (start.getTime < end.getTime)
//      Right(Shutdown(site, start, end))
//    else
//      Left(REVERSED_DATES)
//
//  private def toShutdown(line: Line): Either[String, Shutdown] =
//    for {
//      start    <- parseDate(line.site, line.startDate).right
//      end      <- toEndDate(line, start).right
//      shutdown <- toShutdown(line.site, start, end).right
//    } yield shutdown
//
//  private def toShutdown(lst: List[Line]): Either[String, List[Shutdown]] = {
//    val parsedList = lst.map(toShutdown)
//    val errors    = parsedList collect { case Left(msg) => msg }
//    val shutdowns = parsedList collect { case Right(shutdown) => shutdown }
//
//    errors match {
//      case Nil =>
//        if (Shutdown.validate(shutdowns)) Right(shutdowns) else Left(OVERLAPPING_DATES)
//      case _   =>
//        Left(errors.mkString("\n"))
//    }
//  }
//
//  def parse(configStr: String): Either[String, List[Shutdown]] =
//    ConfigParser.parse(configStr).right flatMap {
//      lst => toShutdown(lst)
//    }
//
//  private def configStream: Option[InputStream] =
//    Option(ShutdownConfig.getClass.getResourceAsStream("/shutdown-dates.txt"))
//
//  private def configContents(is: InputStream): String = {
//    val src = io.Source.fromInputStream(is)
//    try {
//      src.mkString
//    } finally {
//      src.close()
//    }
//  }
//
//  def parsedConfigFile: Either[String, List[Shutdown]] = {
//    configStream map { is => parse(configContents(is)) } getOrElse Right(Nil)
//  }
//}
//
