// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.tac.qengine.p1

import xml.Elem

abstract sealed class QueueBand(val number: Int) extends Ordered[QueueBand] {
  def compare(that: QueueBand): Int = number - that.number
  def categories: Set[QueueBand.Category]
  def isIn(category: QueueBand.Category): Boolean = categories.contains(category)
  def logCategory: QueueBand.Category

  def toXML: Elem
}

object QueueBand {

  abstract sealed class Category(val name: String, val order: Int) extends Ordered[Category] {
    // Ordering is a bit arbitrary
    def compare(that: Category): Int = order - that.order
    override def toString: String    = name
  }

  object Category {
    // Bands 1 and 2 use the normal observing conditions specified per
    // observation.
    case object B1_2 extends Category("Bands 1,2", 0)

    // Band 3 uses the special "Band 3" conditions.
    case object B3 extends Category("Band 3", 1)

    // Time is "guaranteed" if a proposal falls in bands 1, 2, or 3.
    case object Guaranteed extends Category("Guaranteed", 2)

    // Poor weather is not guaranteed and is used to fill up time when no
    // better proposal is available.
    case object PoorWeather extends Category("Poor Weather", 3)

    val values = List(B1_2, B3, Guaranteed, PoorWeather)
  }

  case object QBand1 extends QueueBand(1) {
    def categories  = Set(Category.B1_2, Category.Guaranteed)
    def logCategory = Category.B1_2

    override def toXML: Elem = <QBand1/>
  }

  case object QBand2 extends QueueBand(2) {
    def categories  = Set(Category.B1_2, Category.Guaranteed)
    def logCategory = Category.B1_2

    override def toXML: Elem = <QBand2/>
  }

  case object QBand3 extends QueueBand(3) {
    def categories  = Set(Category.B3, Category.Guaranteed)
    def logCategory = Category.B3

    override def toXML: Elem = <QBand3/>
  }

  case object QBand4 extends QueueBand(4) {
    def categories  = Set(Category.PoorWeather)
    def logCategory = Category.PoorWeather

    override def toXML: Elem = <QBand4/>
  }

  val values = List(QBand1, QBand2, QBand3, QBand4)
}
