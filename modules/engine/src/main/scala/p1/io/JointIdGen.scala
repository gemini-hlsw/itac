// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.tac.qengine.p1.io

case class JointIdGen(count: Int) {
  override def toString: String = s"j$count"
  def next: JointIdGen          = copy(count = count + 1)
}
