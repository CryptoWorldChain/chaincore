package onight.act.persist.mysql.mapper;

import java.util.List;

import onight.act.persist.entity.UserCount;

public interface UserCountMapper {

//	@Select(value = "select count(1) as user_count , CUST_ID as cust_id from T_ACT_INFO where CUST_ID = #{cust_id,jdbcType=VARCHAR}")
//	@Results({ @Result(column = "user_count", property = "user_count", jdbcType = JdbcType.INTEGER),
//			@Result(column = "cust_id", property = "cust_id", jdbcType = JdbcType.INTEGER) })
	UserCount selectUserCount(UserCount counter);

	// @SelectProvider(type=EXFrontUserProdAcctSqlProvider.class,method="getAllCountAndAmt")
	// @Results({
	// @Result(column = "allCount", property = "allCount", jdbcType =
	// JdbcType.VARCHAR),
	// @Result(column = "allAmt", property = "allAmt", jdbcType =
	// JdbcType.VARCHAR)
	// })
	// List<AllCountAndAmt> getAllCountAndAmt(String sql);
}
