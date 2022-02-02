// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.util

import edu.gemini.spModel.core.SPProgramID;
import java.security.MessageDigest;

/**
 * Provides a password based upon the program id. Note that this has to be consistent with
 * edu.gemini.util.security.auth.ProgIdHash in OCS because we need them to interoperate!
 */
final case class ProgIdHash(keyStr: String) {
  import ProgIdHash._

    val md  = MessageDigest.getInstance(HASH)
    val key = keyStr.toCharArray

    def pass(progId: SPProgramID): String =
      pass(progId.toString())

    def pass(progId: String): String = {
      val digest = md.digest(progId.getBytes(ENC));
      val pass = new Array[Char](LENGTH);
      for (i <- 0 until pass.length)
        pass(i) = key(Math.abs(digest(i).toInt) % key.length)
      new String(pass)
    }

}

object ProgIdHash {
  private val HASH = "MD5"
  private val ENC = "UTF-8"
  private val LENGTH = 5
}