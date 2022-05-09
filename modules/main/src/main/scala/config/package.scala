// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac

import edu.gemini.spModel.core.Site

package object config {

  type Semester = edu.gemini.spModel.core.Semester
  type Site     = edu.gemini.spModel.core.Site

  type Time     = edu.gemini.tac.qengine.util.Time
  val  Time     = edu.gemini.tac.qengine.util.Time

  type Percent  = edu.gemini.tac.qengine.util.Percent
  val  Percent  = edu.gemini.tac.qengine.util.Percent

  // N.B. `Site` is a Java class so it has no companion. So alias these directly.
  final val GN = Site.GN
  final val GS = Site.GS

}
