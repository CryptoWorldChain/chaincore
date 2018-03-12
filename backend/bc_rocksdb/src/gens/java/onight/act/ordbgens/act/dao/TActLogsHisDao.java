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

import onight.act.ordbgens.act.entity.TActLogsHis;
import onight.act.ordbgens.act.entity.TActLogsHisExample;
import onight.act.ordbgens.act.entity.TActLogsHisExample.Criteria;
import onight.act.ordbgens.act.entity.TActLogsHisKey;
import onight.act.ordbgens.act.mapper.TActLogsHisMapper;
import onight.tfw.ojpa.api.annotations.Tab;
import onight.tfw.ojpa.ordb.ExtendDaoSupper;


@Data
@Tab(name="T_ACT_LOGS_HIS")
public class TActLogsHisDao extends ExtendDaoSupper<TActLogsHis, TActLogsHisExample, TActLogsHisKey>{

	private TActLogsHisMapper mapper;

	private SqlSessionFactory sqlSessionFactory;
	
	
	@Override
	public int countByExample(TActLogsHisExample example) {
		return mapper.countByExample(example);
	}

	@Override
	public int deleteByExample(TActLogsHisExample example) {
		return mapper.deleteByExample(example);
	}

	@Override
	public int deleteByPrimaryKey(TActLogsHisKey key) {
		return mapper.deleteByPrimaryKey(key);
	}

	@Override
	public int insert(TActLogsHis record)  {
		return mapper.insert(record);
	}

	@Override
	public int insertSelective(TActLogsHis record)  {
		return mapper.insertSelective(record);
	}

	@Override
	//@Transactional
	public int batchUpdate(List<TActLogsHis> records)
			 {
		for(TActLogsHis record : records){
			mapper.updateByPrimaryKeySelective(record);
		}
		return records.size();
	}

	@Override
	//@Transactional
	public int batchDelete(List<TActLogsHis> records)
			 {
		for(TActLogsHis record : records){
			mapper.deleteByPrimaryKey(record);
		}
		return records.size();
	}

	@Override
	public List<TActLogsHis> selectByExample(TActLogsHisExample example)
			 {
		return mapper.selectByExample(example);
	}

	@Override
	public TActLogsHis selectByPrimaryKey(TActLogsHisKey key)
			 {
		return mapper.selectByPrimaryKey(key);
	}

	@Override
	public List<TActLogsHis> findAll(List<TActLogsHis> records) {
		if(records==null||records.size()<=0){
			return mapper.selectByExample(new TActLogsHisExample());
		}
		List<TActLogsHis> list = new ArrayList();
		for(TActLogsHis record : records){
			TActLogsHis result = mapper.selectByPrimaryKey(record);
			if(result!=null){
				list.add(result);
			}
		}
		return list;
	}

	@Override
	public int updateByExampleSelective(TActLogsHis record, TActLogsHisExample example)  {
		return mapper.updateByExampleSelective(record, example);
	}

	@Override
	public int updateByExample(TActLogsHis record, TActLogsHisExample example) {
		return mapper.updateByExample(record, example);
	}

	@Override
	public int updateByPrimaryKeySelective(TActLogsHis record) {
		return mapper.updateByPrimaryKeySelective(record);
	}

	@Override
	public int updateByPrimaryKey(TActLogsHis record) {
		return mapper.updateByPrimaryKey(record);
	}

	@Override
	public int sumByExample(TActLogsHisExample example) {
		return 0;
	}

	@Override
	public void deleteAll()  {
		mapper.deleteByExample(new TActLogsHisExample());
	}

	@Override
	public TActLogsHisExample getExample(TActLogsHis record) {
		TActLogsHisExample example = new TActLogsHisExample();
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
				if(record.getSendMchntId()!=null){
				criteria.andSendMchntIdEqualTo(record.getSendMchntId());
				}
				if(record.getTransCode()!=null){
				criteria.andTransCodeEqualTo(record.getTransCode());
				}
				if(record.getSubTransCode()!=null){
				criteria.andSubTransCodeEqualTo(record.getSubTransCode());
				}
				if(record.getFundNo()!=null){
				criteria.andFundNoEqualTo(record.getFundNo());
				}
				if(record.getActNo()!=null){
				criteria.andActNoEqualTo(record.getActNo());
				}
				if(record.getBizType()!=null){
				criteria.andBizTypeEqualTo(record.getBizType());
				}
				if(record.getBizDtlType()!=null){
				criteria.andBizDtlTypeEqualTo(record.getBizDtlType());
				}
				if(record.getFromCustId()!=null){
				criteria.andFromCustIdEqualTo(record.getFromCustId());
				}
				if(record.getToCustId()!=null){
				criteria.andToCustIdEqualTo(record.getToCustId());
				}
				if(record.getDcType()!=null){
				criteria.andDcTypeEqualTo(record.getDcType());
				}
				if(record.getAmt()!=null){
				criteria.andAmtEqualTo(record.getAmt());
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
				if(record.getCreateTime()!=null){
				criteria.andCreateTimeEqualTo(record.getCreateTime());
				}
				if(record.getUpdateTime()!=null){
				criteria.andUpdateTimeEqualTo(record.getUpdateTime());
				}

		}
		return example;
	}
	
	public TActLogsHis selectOneByExample(TActLogsHisExample example)
			 {
		example.setLimit(1);
		List<TActLogsHis> list=mapper.selectByExample(example);
		if(list!=null&&list.size()>0){
			return list.get(0);
		}
		return null;
	}
	
	@Override
	//@Transactional
	public int batchInsert(List<TActLogsHis> records) {
		SqlSession session=sqlSessionFactory.openSession();
		Connection conn = session.getConnection();
		Statement st = null;
		int result=0;
		try {
			conn.setAutoCommit(false);
			
			st = conn.createStatement();
			StringBuffer sb=new StringBuffer();
			sb.append("INSERT INTO T_ACT_LOGS_HIS() values");
			int i=0;
			for (TActLogsHis record : records) {
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
			
				if(record.getSendMchntId()==null){
						sb.append("null");
				}else{
					sb.append("'"+record.getSendMchntId()+"'");
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
			
				if(record.getFundNo()==null){
						sb.append("null");
				}else{
					sb.append("'"+record.getFundNo()+"'");
				}
			
				sb.append(",");
			
				if(record.getActNo()==null){
						sb.append("null");
				}else{
					sb.append("'"+record.getActNo()+"'");
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
			
				if(record.getFromCustId()==null){
						sb.append("null");
				}else{
					sb.append("'"+record.getFromCustId()+"'");
				}
			
				sb.append(",");
			
				if(record.getToCustId()==null){
						sb.append("null");
				}else{
					sb.append("'"+record.getToCustId()+"'");
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
