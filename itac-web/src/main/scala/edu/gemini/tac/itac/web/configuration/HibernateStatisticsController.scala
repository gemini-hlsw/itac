package edu.gemini.tac.itac.web.configuration

import org.springframework.stereotype.Controller
import javax.annotation.Resource
import edu.gemini.tac.service.HibernateStatistics
import org.springframework.web.bind.annotation.RequestMapping._
import org.springframework.web.bind.annotation.{RequestMethod, RequestMapping}
import org.springframework.web.servlet.ModelAndView
import edu.gemini.tac.persistence.Partner
import edu.gemini.tac.itac.web.{UrlFor, AbstractApplicationController}
import org.hibernate.stat.{QueryStatistics, Statistics}
import org.apache.log4j.{Logger, Level}
import java.util.HashMap

/**
 * TODO: Why does this file exist? 
 *
 */

@Controller
class HibernateStatisticsController extends AbstractApplicationController() {
  private val LOGGER: Logger = Logger.getLogger(classOf[HibernateStatisticsController])

  @Resource(name = "HibernateStatistics")
  private var statService: HibernateStatistics = _ //Injected

  @RequestMapping(value = Array("/statistics"), method = Array(RequestMethod.GET))
  def statistics: ModelAndView = {
    var modelAndView: ModelAndView = boilerPlateModelAndView
    val stats = statService.getStatistics()
    breakEmDown(stats, modelAndView)
    modelAndView.addObject("stats", stats)
    modelAndView.setViewName("session/statistics")

    return modelAndView
  }

  private def breakEmDown(stats: Statistics, mav: ModelAndView) = {
    var map = new java.util.HashMap[String, QueryStatistics]()

    mav.addObject("map", map)

    //Slowest query
    val queryExecutionMaxTime: Long = stats.getQueryExecutionMaxTime
    var maxQueryCount: Long = 0
    var maxQueryQuery: String = ""
    var maxQueryTime : Long = 0
    var queries: Array[String] = stats.getQueries
    for (query <- queries) {
      var queryStats: QueryStatistics = stats.getQueryStatistics(query)
      var totalDur: Long = queryStats.getExecutionCount * queryStats.getExecutionAvgTime
      if (totalDur > 1000) {
        //          map += Map(query, Map("Execution Count", queryStats.getExecutionCount.toString))
        map.put(query, queryStats)

        LOGGER.log(Level.INFO, "Query " + query + " CacheHitCount = " + queryStats.getCacheHitCount)
        LOGGER.log(Level.INFO, "Query " + query + " CacheMissCount = " + queryStats.getCacheMissCount)
        LOGGER.log(Level.INFO, "Query " + query + " CachePutCount = " + queryStats.getCachePutCount)
        LOGGER.log(Level.INFO, "Query " + query + " ExecutionCount = " + queryStats.getExecutionCount)
        LOGGER.log(Level.INFO, "Query " + query + " ExecutionAvgTime = " + queryStats.getExecutionAvgTime)
        if (queryStats.getExecutionMaxTime > maxQueryTime) {
          LOGGER.log(Level.INFO, "SLOW QUERY!")
          mav.addObject("slowestQuery", query)
          mav.addObject("slowestQueryTime", queryStats.getExecutionMaxTime)
          maxQueryTime = queryStats.getExecutionMaxTime

        }
      }
      if (queryStats.getExecutionCount > maxQueryCount) {
        maxQueryQuery = query
        maxQueryCount = queryStats.getExecutionCount
        mav.addObject("maxQueryQuery", maxQueryQuery)
        mav.addObject("maxQueryCount", maxQueryCount)
      }
    }


    LOGGER.log(Level.INFO, "Most commonly executed query " + maxQueryCount + " times called " + maxQueryQuery)
  }

  private def boilerPlateModelAndView: ModelAndView = {
    val modelAndView: ModelAndView = new ModelAndView
    addResourceInformation(modelAndView.getModelMap)
    addSubHeaders(modelAndView.getModelMap, subheaders)
    modelAndView
  }

  override def getController: UrlFor.Controller = UrlFor.Controller.QUEUE_CONFIGURATION

  override def getTopLevelController: UrlFor.Controller = UrlFor.Controller.CONFIGURATION

}