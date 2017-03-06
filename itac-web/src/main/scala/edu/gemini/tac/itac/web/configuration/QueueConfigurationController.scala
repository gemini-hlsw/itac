package edu.gemini.tac.itac.web.configuration

import edu.gemini.tac.itac.web.{UrlFor, AbstractApplicationController}
import edu.gemini.tac.persistence.Partner
import org.springframework.stereotype.Controller
import org.springframework.web.servlet.ModelAndView
import scala.collection.JavaConverters._
import org.apache.commons.lang.Validate
import org.springframework.web.bind.annotation.{RequestParam, RequestMapping, RequestMethod}
import javax.servlet.http.HttpServletResponse
import org.apache.log4j.{Level, Logger}

@Controller
class QueueConfigurationController extends AbstractApplicationController() {
  subheaders = ConfigurationController.getSubheaderLinks

  private val LOGGER = Logger.getLogger(this.getClass.toString);
  private def boilerPlateModelAndView: ModelAndView = {
    val modelAndView: ModelAndView = new ModelAndView
    addResourceInformation(modelAndView.getModelMap)
    addSubHeaders(modelAndView.getModelMap, subheaders)
    modelAndView
  }

  @RequestMapping(value = Array("/configuration/partner-percentages"), method = Array(RequestMethod.GET))
  def percentagesGet: ModelAndView = {
    LOGGER.log(Level.DEBUG, "percentages (GET)")
    var modelAndView: ModelAndView = boilerPlateModelAndView
    val partners: java.util.List[Partner] = partnerService.findAllPartners
    modelAndView.addObject(partners)
    val ps = partners.asScala
    val (someNorths, justSouths) = ps.partition(_.isNorth)
    val (both, justNorths) = someNorths.partition(_.isSouth)
    val ss = justSouths ++ both
    val ns = justNorths ++ both
    val northTotalVal = ns.foldLeft(0.0)((accum, value) => accum + value.getPercentageShare)
    val southTotalVal = ss.foldLeft(0.0)((accum, value) => accum + value.getPercentageShare)
    modelAndView.addObject("sumOfNorthPercents", northTotalVal)
    modelAndView.addObject("sumOfSouthPercents", southTotalVal)
    modelAndView.setViewName("configuration/queue/partner-percentages")
    return modelAndView
  }

  @RequestMapping(value = Array("/configuration/partner-percentages"), method = Array(RequestMethod.POST))
  def percentagesPost(@RequestParam partnerName: String, @RequestParam newPercentage: Double, response: HttpServletResponse): Unit = {
    LOGGER.log(Level.DEBUG, "percentages (POST)")
    try {
      Validate.notEmpty(partnerName, "Partner name not passed")
      Validate.notNull(newPercentage, "New percentage not passed")
      Validate.isTrue(!partnerName.equals("undefined"))
      Validate.isTrue(newPercentage >= 0)
      Validate.isTrue(newPercentage <= 100)
      partnerService.setPartnerPercentage(partnerName, newPercentage)
      response.setStatus(205)
    }
    catch {
      case x: Exception => {
        LOGGER.error(x)
        response.setStatus(500)
      }
    }
  }


  override def getController: UrlFor.Controller = UrlFor.Controller.QUEUE_CONFIGURATION

  override def getTopLevelController: UrlFor.Controller = UrlFor.Controller.CONFIGURATION
}

