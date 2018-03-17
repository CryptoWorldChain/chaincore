package onight.act.ordbgens.act.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
//import org.springframework.transaction.annotation.Transactional;

import onight.act.ordbgens.act.entity.TActTransLogs;
import onight.act.ordbgens.act.entity.TActTransLogsExample;
import onight.act.ordbgens.act.entity.TActTransLogsExample.Criteria;
import onight.act.ordbgens.act.entity.TActTransLogsKey;
import onight.act.ordbgens.act.mapper.TActTransLogsMapper;
import onight.tfw.ojpa.api.annotations.Tab;
import onight.tfw.ojpa.ordb.ExtendDaoSupper;


@Data
@Tab(name="T_ACT_TRANS_LOGS")
public class TActTransLogsDao extends ExtendDaoSupper<TActTransLogs, TActTransLogsExample, TActTransLogsKey>{

	private TActTransLogsMapper mapper;

	private SqlSessionFactory sqlSessionFactory;
	
	
	@Override
	public int countByExample(TActTransLogsExample example) {
		return mapper.countByExample(example);
	}

	@Override
	public int deleteByExample(TActTransLogsExample example) {
		return mapper.deleteByExample(example);
	}

	@Override
	public int deleteByPrimaryKey(TActTransLogsKey key) {
		return mapper.deleteByPrimaryKey(key);
	}

	@Override
	public int insert(TActTransLogs record)  {
		return mapper.insert(record);
	}

	@Override
	public int insertSelective(TActTransLogs record)  {
		return mapper.insertSelective(record);
	}

	@Override
	//@Transactional
	public int batchUpdate(List<TActTransLogs> records)
			 {
		for(TActTransLogs record : records){
			mapper.updateByPrimaryKeySelective(record);
		}
		return records.size();
	}

	@Override
	//@Transactional
	public int batchDelete(List<TActTransLogs> records)
			 {
		for(TActTransLogs record : records){
			mapper.deleteByPrimaryKey(record);
		}
		return records.size();
	}

	@Override
	public List<TActTransLogs> selectByExample(TActTransLogsExample example)
			 {
		return mapper.selectByExample(example);
	}

	@Override
	public TActTransLogs selectByPrimaryKey(TActTransLogsKey key)
			 {
		return mapper.selectByPrimaryKey(key);
	}

	@Override
	public List<TActTransLogs> findAll(List<TActTransLogs> records) {
		if(records==null||records.size()<=0){
			return mapper.selectByExample(new TActTransLogsExample());
		}
		List<TActTransLogs> list = new ArrayList();
		for(TActTransLogs record : records){
			TActTransLogs result = mapper.selectByPrimaryKey(record);
			if(result!=null){
				list.add(result);
			}
		}
		return list;
	}

	@Override
	public int updateByExampleSelective(TActTransLogs record, TActTransLogsExample example)  {
		return mapper.updateByExampleSelective(record, example);
	}

	@Override
	public int updateByExample(TActTransLogs record, TActTransLogsExample example) {
		return mapper.updateByExample(record, example);
	}

	@Override
	public int updateByPrimaryKeySelective(TActTransLogs record) {
		return mapper.updateByPrimaryKeySelective(record);
	}

	@Override
	public int updateByPrimaryKey(TActTransLogs record) {
		return mapper.updateByPrimaryKey(record);
	}

	@Override
	public int sumByExample(TActTransLogsExample example) {
		return 0;
	}

	@Override
	public void deleteAll()  {
		mapper.deleteByExample(new TActTransLogsExample());
	}

	@Override
	public TActTransLogsExample getExample(TActTransLogs record) {
		TActTransLogsExample example = new TActTransLogsExample();
		if(record!=null){
			Criteria criteria = example.createCriteria();
							if(record.getLogUuid()!=null){
				criteria.andLogUuidEqualTo(record.getLogUuid());
				}
				if(record.getSettDate()!=null){
				criteria.andSettDateEqualTo(record.getSettDate());
				}
				if(record.getConsDate()!=null){
				criteria.andConsDateEqualTo(record.getConsDate());
				}
				if(record.getTxSno()!=null){
				criteria.andTxSnoEqualTo(record.getTxSno());
				}
				if(record.getTransCode()!=null){
				criteria.andTransCodeEqualTo(record.getTransCode());
				}
				if(record.getSubTransCode()!=null){
				criteria.andSubTransCodeEqualTo(record.getSubTransCode());
				}
				if(record.getBizType()!=null){
				criteria.andBizTypeEqualTo(record.getBizType());
				}
				if(record.getBizDtlType()!=null){
				criteria.andBizDtlTypeEqualTo(record.getBizDtlType());
				}
				if(record.getFromFundNo()!=null){
				criteria.andFromFundNoEqualTo(record.getFromFundNo());
				}
				if(record.getToFundNo()!=null){
				criteria.andToFundNoEqualTo(record.getToFundNo());
				}
				if(record.getDcType()!=null){
				criteria.andDcTypeEqualTo(record.getDcType());
				}
				if(record.getAmt()!=null){
				criteria.andAmtEqualTo(record.getAmt());
				}
				if(record.getCnt()!=null){
				criteria.andCntEqualTo(record.getCnt());
				}
				if(record.getFlagCancel()!=null){
				criteria.andFlagCancelEqualTo(record.getFlagCancel());
				}
				if(record.getRelatedTransId()!=null){
				criteria.andRelatedTransIdEqualTo(record.getRelatedTransId());
				}
				if(record.getStatus()!=null){
				criteria.andStatusEqualTo(record.getStatus());
				}
				if(record.getActBalAfter()!=null){
				criteria.andActBalAfterEqualTo(record.getActBalAfter());
				}
				if(record.getActBalBefore()!=null){
				criteria.andActBalBeforeEqualTo(record.getActBalBefore());
				}
				if(record.getExtId1()!=null){
				criteria.andExtId1EqualTo(record.getExtId1());
				}
				if(record.getExtId2()!=null){
				criteria.andExtId2EqualTo(record.getExtId2());
				}
				if(record.getExtCommets()!=null){
				criteria.andExtCommetsEqualTo(record.getExtCommets());
				}
				if(record.getCreateTime()!=null){
				criteria.andCreateTimeEqualTo(record.getCreateTime());
				}
				if(record.getUpdateTime()!=null){
				criteria.andUpdateTimeEqualTo(record.getUpdateTime());
				}

		}
		return example;
	}
	
	public TActTransLogs selectOneByExample(TActTransLogsExample example)
			 {
		example.setLimit(1);
		List<TActTransLogs> list=mapper.selectByExample(example);
		if(list!=null&&list.size()>0){
			return list.get(0);
		}
		return null;
	}
	
	@Override
	//@Transactional
	public int batchInsert(List<TActTransLogs> records) {
		SqlSession session=sqlSessionFactory.openSession();
		Connection conn = session.getConnection();
		Statement st = null;
		int result=0;
		try {
			conn.setAutoCommit(false);
			
			st = conn.createStatement();
			StringBuffer sb=new StringBuffer();
			sb.append("INSERT INTO T_ACT_TRANS_LOGS() values");
			int i=0;
			for (TActTransLogs record : records) {
				if(i>0){
					sb.append(",");
				}
				i++;
				
			
				sb.append("(");
			
				if(record.getLogUuid()==null){
						sb.append("null");
				}else{
					sb.append("'"+record.getLogUuid()+"'");
				}
			
				sb.append(",");
			
				if(record.getSettDate()==null){
						sb.append("null");
				}else{
					sb.append("'"+record.getSettDate()+"'");
				}
			
				sb.append(",");
			
				if(record.getConsDate()==null){
						sb.append("null");
				}else{
					sb.append("'"+record.getConsDate()+"'");
				}
			
				sb.append(",");
			
				if(record.getTxSno()==null){
						sb.append("null");
				}else{
					sb.append("'"+record.getTxSno()+"'");
				}
			
				sb.append(",");
			
				if(record.getTransCode()==null){
						sb.append("null");
				}else{
					sb.append("'"+record.getTransCode()+"'");
				}
			
				sb.append(",");
			
				if(record.getSubTransCode()==null){
						sb.append("null");
				}else{
					sb.append("'"+record.getSubTransCode()+"'");
				}
			
				sb.append(",");
			
				if(record.getBizType()==null){
						sb.append("null");
				}else{
					sb.append("'"+record.getBizType()+"'");
				}
			
				sb.append(",");
			
				if(record.getBizDtlType()==null){
						sb.append("null");
				}else{
					sb.append("'"+record.getBizDtlType()+"'");
				}
			
				sb.append(",");
			
				if(record.getFromFundNo()==null){
						sb.append("null");
				}else{
					sb.append("'"+record.getFromFundNo()+"'");
				}
			
				sb.append(",");
			
				if(record.getToFundNo()==null){
						sb.append("null");
				}else{
					sb.append("'"+record.getToFundNo()+"'");
				}
			
				sb.append(",");
			
				if(record.getDcType()==null){
						sb.append("null");
				}else{
					sb.append("'"+record.getDcType()+"'");
				}
			
				sb.append(",");
			
				if(record.getAmt()==null){
						sb.append("null");
				}else{
					sb.append("'"+record.getAmt()+"'");
				}
			
				sb.append(",");
			
				if(record.getCnt()==null){
						sb.append("null");
				}else{
					sb.append("'"+record.getCnt()+"'");
				}
			
				sb.append(",");
			
				if(record.getFlagCancel()==null){
						sb.append("null");
				}else{
					sb.append("'"+record.getFlagCancel()+"'");
				}
			
				sb.append(",");
			
				if(record.getRelatedTransId()==null){
						sb.append("null");
				}else{
					sb.append("'"+record.getRelatedTransId()+"'");
				}
			
				sb.append(",");
			
				if(record.getStatus()==null){
						sb.append("'"+"0000"+"'");						
				}else{
					sb.append("'"+record.getStatus()+"'");
				}
			
				sb.append(",");
			
				if(record.getActBalAfter()==null){
						sb.append("null");
				}else{
					sb.append("'"+record.getActBalAfter()+"'");
				}
			
				sb.append(",");
			
				if(record.getActBalBefore()==null){
						sb.append("null");
				}else{
					sb.append("'"+record.getActBalBefore()+"'");
				}
			
				sb.append(",");
			
				if(record.getExtId1()==null){
						sb.append("null");
				}else{
					sb.append("'"+record.getExtId1()+"'");
				}
			
				sb.append(",");
			
				if(record.getExtId2()==null){
						sb.append("null");
				}else{
					sb.append("'"+record.getExtId2()+"'");
				}
			
				sb.append(",");
			
				if(record.getExtCommets()==null){
						sb.append("null");
				}else{
					sb.append("'"+record.getExtCommets()+"'");
				}
			
				sb.append(",");
			
				if(record.getCreateTime()==null){
						sb.append("'"+"CURRENT_TIMESTAMP"+"'");						
				}else{
					sb.append("'"+record.getCreateTime()+"'");
				}
			
				sb.append(",");
			
				if(record.getUpdateTime()==null){
						sb.append("'"+"CURRENT_TIMESTAMP"+"'");						
				}else{
					sb.append("'"+record.getUpdateTime()+"'");
				}
							sb.append(")");
			
			}
			result=st.executeUpdate(sb.toString());
			conn.commit();
		} catch (SQLException e) {
			e.printStackTrace();
			try {
				conn.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		}finally{
			if(st!=null){
				try {
					st.close();
				} catch (Exception est) {
					est.printStackTrace();
				}
			}
			session.close();
		}
		return result;
	}
	
	
}
