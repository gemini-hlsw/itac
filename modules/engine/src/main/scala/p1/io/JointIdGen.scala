package edu.gemini.tac.qengine.p1.io

case class JointIdGen(count: Int) {
  override def toString: String = s"j$count"
  def next: JointIdGen = copy(count = count + 1)
}
