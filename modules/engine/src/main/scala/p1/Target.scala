package edu.gemini.tac.qengine.p1

import edu.gemini.tac.qengine.util.Angle

object Target {
  def apply(raDeg: Double, decDeg: Double, name: String): Target =
    new Target(new Angle(raDeg, Angle.Deg), new Angle(decDeg, Angle.Deg), Option(name))

  def apply(raDeg: Double, decDeg: Double): Target =
    new Target(new Angle(raDeg, Angle.Deg), new Angle(decDeg, Angle.Deg))
}

case class Target(ra: Angle, dec: Angle, name: Option[String] = None)
