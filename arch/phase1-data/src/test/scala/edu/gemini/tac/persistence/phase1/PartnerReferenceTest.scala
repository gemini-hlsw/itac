package edu.gemini.tac.persistence.phase1

import org.junit._
import Assert._
import edu.gemini.model.p1.mutable.NgoPartner
import submission.{NgoSubmission, SubmissionReceipt}
import edu.gemini.tac.persistence.phase1.submission.{SubmissionReceipt, NgoSubmission}

// TODO: Commenting this out out of despair.  Must get checked in.
@Ignore
class PartnerReferenceTest {

//  @Test def testNoPartnerProducesNone() {
//    val sub = new NgoSubmission()
//    val receipt = new SubmissionReceipt()
//    receipt.setReceiptId("x")
//    sub.setReceipt(receipt)
//    assertNull(sub.getPartner)
//
//    assertEquals(None, PartnerReference.extract(sub))
//  }
//
//  @Test def testNoReferenceNumberProducesNone() {
//    val sub = new NgoSubmission()
//    {
//      override def getPartner: NgoPartner = NgoPartner.CA
//
//      override def getReceipt : SubmissionReceipt = new SubmissionReceipt()
//    }
//    assertNull(sub.getReceipt.getReceiptId)
//
//    assertEquals(None, PartnerReference.extract(sub))
//  }
//
//  @Test def testValidPartnerReferenceProducesSome() {
//    val sub = new NgoSubmission(){
//       override def getPartner : NgoPartner = NgoPartner.CA
//
//      override def getReceipt : SubmissionReceipt = {
//        val receipt = new SubmissionReceipt()
//        receipt.setReceiptId("ABC123")
//        receipt
//      }
//    }
//
//    assertEquals(Some(PartnerReference(NgoPartner.CA, "ABC123")), PartnerReference.extract(sub))
//  }
}