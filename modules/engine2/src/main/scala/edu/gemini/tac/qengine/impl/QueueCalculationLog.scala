package edu.gemini.tac.qengine.impl


import java.io.StringWriter
import org.apache.log4j._


object QueueCalculationLog{
  private val writer = new StringWriter();
  private val layout = new PatternLayout("<LogMessage time=\"%d{ISO8601}\"><Priority>%p</Priority><Text>%m%n</Text></LogMessage>")
  private val appender = new WriterAppender(layout, writer)
  appender.setName("QueueCalculatorLogAppender")
  appender.setThreshold(Priority.DEBUG)

  private val _logger = Logger.getLogger("QueueCalculationLogger")
  _logger.setLevel(Level.INFO)
  //TODO: Switch to a non-resource-intense appender. StringWriter gobbles heap.
  //_logger.addAppender(appender)

  //TODO: Confirm that logger is only initialized once
  def logger = _logger

  def log : String = writer.getBuffer.toString
}