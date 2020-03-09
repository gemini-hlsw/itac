package edu.gemini.tac.qengine.app

import java.io.File

/**
 * Produces a flat listing of XML files.
 */
private object XmlLs {
  private def flatLs(fa: Array[File]): List[File] =
    fa.toList.flatMap {
      f => if (f.isDirectory) flatLs(f.listFiles) else List(f)
    }

  /**
   * Given an array of files, produces a flat listing of the XML files
   * contained in the listing recursing through directory hierarchies if
   * necessary.
   */
  def apply(fa: Array[File]): List[File] =
    flatLs(fa).filter(_.getName.endsWith(".xml"))
}