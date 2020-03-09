// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package org.slf4j.impl

/** Extend `SimpleLogger` to get some colors. Hacky, sorry. */
class ColoredSimpleLogger(name: String) extends SimpleLogger(name) {
  import ColoredSimpleLogger._

  override protected def renderLevel(level: Int): String =
    level match {
        case SimpleLogger.LOG_LEVEL_TRACE => "TRACE".colored(Console.CYAN)
        case SimpleLogger.LOG_LEVEL_DEBUG => "DEBUG".colored(Console.BLUE)
        case SimpleLogger.LOG_LEVEL_INFO  => "INFO ".colored(Console.GREEN)
        case SimpleLogger.LOG_LEVEL_WARN  => "WARN ".colored(Console.YELLOW) // n.b. ignores the [dumb] fact that [only] this label is configurable in the superclass
        case SimpleLogger.LOG_LEVEL_ERROR => "ERROR".colored(Console.RED)
    }

}

object ColoredSimpleLogger {

  // java is dumb, have to call this before constructing an instance or it will NPE
  def init(): Unit =
    SimpleLogger.lazyInit()

  private implicit class StringOps(val s: String) extends AnyVal {
    def colored(color: String): String =
      s"$color$s${Console.RESET}"
  }

}