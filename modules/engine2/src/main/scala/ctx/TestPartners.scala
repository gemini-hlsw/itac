package edu.gemini.tac.qengine.ctx

import edu.gemini.spModel.core.Site

// TODO: need a better place for what is essentially a test fixture

object TestPartners {
  private val AllSites = Site.values().toSet

  val AR = Partner("AR", "Argentina", 2.1, AllSites, "mail@whatever.ar")
  val AU = Partner("AU", "Australia", 5.8, AllSites, "mail@whatever.au")
  val BR = Partner("BR", "Brazil", 4.4, AllSites, "mail@whatever.br")
  val CA = Partner("CA", "Canada", 17.3, AllSites, "mail@whatever.ca")
  val CL = Partner("CL", "Chile", 8.9, Set(Site.GS), "mail@whatever.cl")
  val GS = Partner("GS", "Gemini Staff", 3.8, AllSites, "mail@whatever.gs")
  val UH = Partner("UH", "University of Hawaii", 8.9, Set(Site.GN), "mail@whatever.uh")
  val US = Partner("US", "United States", 57.7, AllSites, "mail@whatever.us")

  val All = List(AR, AU, BR, CA, CL, GS, UH, US)
  val AllMap = All.map(p => p.id -> p).toMap
}
