// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac

package object codec {

  object all
    extends PercentCodec
       with SiteCodec
       with SemesterCodec
       with TokensCodecs
       with ConditionsBinCodec
       with ConditionsCategoryCodec
       with RaBinSizeCodec
       with DecBinSizeCodec
       with RolloverObservationCodec
       with RolloverReportCodec
       with QueueBandCodec
       with PartnerCodec

}
