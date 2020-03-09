package edu.gemini.tac.itac.web.configuration

import edu.gemini.tac.itac.web.committees.CommitteesController
import org.springframework.stereotype.Controller
import org.springframework.web.servlet.ModelAndView
import org.apache.log4j.{Level, Logger}
import org.springframework.web.bind.annotation.{RequestParam, PathVariable, RequestMethod, RequestMapping}
import org.joda.time.DateTime
import edu.gemini.tac.persistence.daterange.{Shutdown, DateRangePersister}
import edu.gemini.tac.itac.web.{Link, UrlFor, AbstractApplicationController}
import java.util.ArrayList
import edu.gemini.shared.util.DateRange
import edu.gemini.tac.persistence.Site
import javax.annotation.Resource
import scala.collection.JavaConverters._
import edu.gemini.tac.service.{ISiteService, IShutdownService}
import org.apache.commons.lang.Validate

@Controller("Shutdowns")
class ShutdownsController extends AbstractApplicationController {
  private final val LOGGER: Logger = Logger.getLogger(classOf[ShutdownsController].toString)

  @Resource(name = "shutdownService")
  protected var shutdownService: IShutdownService = null //Will be injected

  @Resource(name = "siteService")
  protected var siteService : ISiteService = null //Injected

  protected def getController: UrlFor.Controller = {
    return UrlFor.Controller.SHUTDOWN_CONFIGURATION
  }

  protected def getTopLevelController: UrlFor.Controller = {
    return UrlFor.Controller.COMMITTEES
  }

  @RequestMapping(value = Array("/committees/{committeeId}/shutdowns"), method = Array(RequestMethod.GET))
  def index(@PathVariable committeeId: Long): ModelAndView = {
    LOGGER.log(Level.DEBUG, "ShtudownsController.index()")
    val modelAndView: ModelAndView = boilerPlateModelAndView(committeeId)

    modelAndView.addObject("shutdownsBySite", shutdownsBySite(committeeId))
    modelAndView.setViewName("committees/shutdowns/index")
    return modelAndView
  }

  protected def siteForName(siteName: String): Site = siteService.findByDisplayName(siteName)

  @RequestMapping(method = Array(RequestMethod.POST))
  def createShutdown(@PathVariable committeeId: Long,
                     @RequestParam siteName: String,
                     @RequestParam startDate: String,
                     @RequestParam endDate: String): ModelAndView = {
    LOGGER.log(Level.DEBUG, "ShutdownsController.createBlackout()")
    val sDate = DateTime.parse(startDate)
    val eDate = DateTime.parse(endDate)
    val dr = new DateRange(sDate.toDate, eDate.toDate)
    val drp = new DateRangePersister(dr)
    val site = siteForName(siteName)
    val committee = committeeService.getCommittee(committeeId)
    Validate.notNull(committee, "Could not find committee id " + committeeId);
    val s = new Shutdown(drp, site, committee);

    shutdownService.save(s)
    val shutdownId = s.getId
    val mav = new ModelAndView
    mav.addObject("shutdownId", shutdownId)
    mav.addObject("startDate", sDate)
    mav.addObject("endDate", eDate)
    mav.setViewName("committees/shutdowns/shutdownJson")
    return mav
  }

  @RequestMapping(value = Array("committees/{committeeId}/shutdowns/shutdown/{shutdownId}"), method = Array(RequestMethod.DELETE))
  def deleteShutdown(@PathVariable committeeId: Long, @PathVariable shutdownId: Long): ModelAndView = {
    LOGGER.log(Level.DEBUG, "ShutdownController.deleteShutdown()")
    shutdownService.delete(shutdownId)
    val mav: ModelAndView = new ModelAndView
    mav.setViewName("committees/shutdowns/shutdownJson")
    return mav
  }

  def shutdownsBySite(committeeId : Long) : java.util.Map[String,java.util.List[Shutdown]] = {
    val ss = shutdownService.forCommittee(committeeId).asScala.toList
    val partition = ss.partition(_.getSite.getDisplayName == "North")
    Map("North"-> partition._1.toList.asJava, "South" -> partition._2.toList.asJava).asJava
  }

  def getCollectionLinks(topLevelResourceId: String): Array[Link] = {
    val links = new ArrayList[Link]()
    val objects = links.toArray[Link](Array[Link]())
    return objects
  }

  private def boilerPlateModelAndView(committeeId: Long): ModelAndView = {
    val modelAndView: ModelAndView = new ModelAndView
    modelAndView.addObject("user", getUser)
    addResourceInformation(modelAndView.getModelMap)
    addSubHeaders(modelAndView.getModelMap, CommitteesController.getSubheaderLinks(committeeId))
    addSubSubHeaders(modelAndView.getModelMap, getCollectionLinks(String.valueOf(committeeId)))
    addTopLevelResource(modelAndView.getModelMap, String.valueOf(committeeId))
    return modelAndView
  }
}