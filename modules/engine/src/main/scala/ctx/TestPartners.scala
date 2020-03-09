// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.tac.qengine.ctx

import edu.gemini.spModel.core.Site

// TODO: need a better place for what is essentially a test fixture

object TestPartners {
  private val AllSites = Site.values().toSet

  val AR = Partner("AR", "Argentina", 2.1, AllSites)
  val AU = Partner("AU", "Australia", 5.8, AllSites)
  val BR = Partner("BR", "Brazil", 4.4, AllSites)
  val CA = Partner("CA", "Canada", 17.3, AllSites)
  val CL = Partner("CL", "Chile", 8.9, Set(Site.GS))
  val GS = Partner("GS", "Gemini Staff", 3.8, AllSites)
  val UH = Partner("UH", "University of Hawaii", 8.9, Set(Site.GN))
  val US = Partner("US", "United States", 57.7, AllSites)

  val All    = List(AR, AU, BR, CA, CL, GS, UH, US)
  val AllMap = All.map(p => p.id -> p).toMap
}
