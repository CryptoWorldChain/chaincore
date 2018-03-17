package onight.act.ordbgens.act.so

import onight.async.mysql.commons.SimpleDAO
import scala.reflect.classTag
import java.math.BigDecimal
import java.sql.Timestamp


object ACTDAOs {


  case class KOTActFund(
 val FUND_NO: String = null
, val ACT_NO: String = null
, val CUST_ID: String = null
, val MCHNT_ID: String = null
, val ACT_TYPE: String = null
, val MNY_SMB: String = null
, val CATALOG: String = null
, val CHANNEL_ID: String = null
, val CUR_BAL: Option[Double] = null
, val FREEZE_TOTAL: Option[Double] = null
, val INCOME_TOTAL: Option[Double] = null
, val PAYOUT_TOTAL: Option[Double] = null
, val FOTBID_AMOUNT: Option[Double] = null
, val FOTBID_FLAG: Option[Char] = null
, val ACT_STAT: String = null
, val ACT_BAL_WARN_FLAG: String = null
, val UPDATE_ACT_LOG_ID: String = null
, val CREATE_TIME: Timestamp = null
, val UPDATE_TIME: Timestamp = null
, val MODIFY_ID: String = null
  	)

  object TActFundDAO extends SimpleDAO[KOTActFund] {
    val ttag = classTag[KOTActFund];
    val tablename = "T_ACT_FUND";
    val keyname = "FUND_NO"
  }


  case class KOTActInfo(
 val ACT_NO: String = null
, val ACT_NAME: String = null
, val CUST_ID: String = null
, val MCHNT_ID: String = null
, val ACT_TYPE: String = null
, val MNY_SMB: String = null
, val CHANNEL_ID: String = null
, val CATALOG: String = null
, val ACT_YINIT_BAL: Option[Double] = null
, val ACT_DINIT_BAL: Option[Double] = null
, val ACT_CUR_BAL: Option[Double] = null
, val ACT_STAT: Option[Double] = null
, val ACT_MAXOD_AMT: Option[Double] = null
, val ACT_CTRL_BAL: Option[Double] = null
, val ACT_BAL_WARN_FLAG: String = null
, val CREATE_TIME: Timestamp = null
, val UPDATE_TIME: Timestamp = null
, val MODIFY_ID: String = null
, val MEMO: String = null
  	)

  object TActInfoDAO extends SimpleDAO[KOTActInfo] {
    val ttag = classTag[KOTActInfo];
    val tablename = "T_ACT_INFO";
    val keyname = "ACT_NO"
  }


  case class KOTActInterest(
 val INST_LOGID: String = null
, val INTEREST_DATE: String = null
, val FUND_NO: String = null
, val ACT_NO: String = null
, val INTEREST_TYPE: String = null
, val INTEREST_AMOUNT: Option[Double] = null
, val GMT_CREATE: Timestamp = null
, val GMT_MODIFY: Timestamp = null
, val TRADE_DATE: String = null
, val RATE_VALUE: Option[Double] = null
, val UNIT_NO: String = null
  	)

  object TActInterestDAO extends SimpleDAO[KOTActInterest] {
    val ttag = classTag[KOTActInterest];
    val tablename = "T_ACT_INTEREST";
    val keyname = "INST_LOGID"
  }


  case class KOTActTransLogs(
 val LOG_UUID: String = null
, val SETT_DATE: String = null
, val CONS_DATE: String = null
, val TX_SNO: String = null
, val TRANS_CODE: String = null
, val SUB_TRANS_CODE: String = null
, val BIZ_TYPE: String = null
, val BIZ_DTL_TYPE: String = null
, val FROM_FUND_NO: String = null
, val TO_FUND_NO: String = null
, val DC_TYPE: String = null
, val AMT: Option[Double] = null
, val CNT: Option[Int] = null
, val FLAG_CANCEL: Option[Char] = null
, val RELATED_TRANS_ID: String = null
, val STATUS: String = null
, val ACT_BAL_AFTER: Option[Double] = null
, val ACT_BAL_BEFORE: Option[Double] = null
, val EXT_ID1: String = null
, val EXT_ID2: String = null
, val EXT_COMMETS: String = null
, val CREATE_TIME: Timestamp = null
, val UPDATE_TIME: Timestamp = null
  	)

  object TActTransLogsDAO extends SimpleDAO[KOTActTransLogs] {
    val ttag = classTag[KOTActTransLogs];
    val tablename = "T_ACT_TRANS_LOGS";
    val keyname = "LOG_UUID"
  }


  case class KOTActTransLogsDebt(
 val LOG_UUID: String = null
, val FROM_FUND_NO: String = null
, val TO_FUND_NO: String = null
, val FLAG_CANCEL: Option[Char] = null
, val RELATED_TRANS_ID: String = null
, val STATUS: String = null
, val CREATE_TIME: Timestamp = null
, val UPDATE_TIME: Timestamp = null
  	)

  object TActTransLogsDebtDAO extends SimpleDAO[KOTActTransLogsDebt] {
    val ttag = classTag[KOTActTransLogsDebt];
    val tablename = "T_ACT_TRANS_LOGS_DEBT";
    val keyname = "LOG_UUID"
  }


  case class KOTActTransLogsHis(
 val LOG_UUID: String = null
, val SETT_DATE: String = null
, val CONS_DATE: String = null
, val TX_SNO: String = null
, val TRANS_CODE: String = null
, val SUB_TRANS_CODE: String = null
, val BIZ_TYPE: String = null
, val BIZ_DTL_TYPE: String = null
, val FROM_FUND_NO: String = null
, val TO_FUND_NO: String = null
, val DC_TYPE: String = null
, val AMT: Option[Double] = null
, val CNT: Option[Int] = null
, val FLAG_CANCEL: Option[Char] = null
, val RELATED_TRANS_ID: String = null
, val STATUS: String = null
, val ACT_BAL_AFTER: Option[Double] = null
, val ACT_BAL_BEFORE: Option[Double] = null
, val EXT_ID1: String = null
, val EXT_ID2: String = null
, val EXT_COMMETS: String = null
, val CREATE_TIME: Timestamp = null
, val UPDATE_TIME: Timestamp = null
  	)

  object TActTransLogsHisDAO extends SimpleDAO[KOTActTransLogsHis] {
    val ttag = classTag[KOTActTransLogsHis];
    val tablename = "T_ACT_TRANS_LOGS_HIS";
    val keyname = "LOG_UUID"
  }


}