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

import onight.act.ordbgens.act.entity.TActTransLogsDebt;
import onight.act.ordbgens.act.entity.TActTransLogsDebtExample;
import onight.act.ordbgens.act.entity.TActTransLogsDebtExample.Criteria;
import onight.act.ordbgens.act.entity.TActTransLogsDebtKey;
import onight.act.ordbgens.act.mapper.TActTransLogsDebtMapper;
import onight.tfw.ojpa.api.annotations.Tab;
import onight.tfw.ojpa.ordb.ExtendDaoSupper;


@Data
@Tab(name="T_ACT_TRANS_LOGS_DEBT")
public class TActTransLogsDebtDao extends ExtendDaoSupper<TActTransLogsDebt, TActTransLogsDebtExample, TActTransLogsDebtKey>{

	private TActTransLogsDebtMapper mapper;

	private SqlSessionFactory sqlSessionFactory;
	
	
	@Override
	public int countByExample(TActTransLogsDebtExample example) {
		return mapper.countByExample(example);
	}

	@Override
	public int deleteByExample(TActTransLogsDebtExample example) {
		return mapper.deleteByExample(example);
	}

	@Override
	public int deleteByPrimaryKey(TActTransLogsDebtKey key) {
		return mapper.deleteByPrimaryKey(key);
	}

	@Override
	public int insert(TActTransLogsDebt record)  {
		return mapper.insert(record);
	}

	@Override
	public int insertSelective(TActTransLogsDebt record)  {
		return mapper.insertSelective(record);
	}

	@Override
	//@Transactional
	public int batchUpdate(List<TActTransLogsDebt> records)
			 {
		for(TActTransLogsDebt record : records){
			mapper.updateByPrimaryKeySelective(record);
		}
		return records.size();
	}

	@Override
	//@Transactional
	public int batchDelete(List<TActTransLogsDebt> records)
			 {
		for(TActTransLogsDebt record : records){
			mapper.deleteByPrimaryKey(record);
		}
		return records.size();
	}

	@Override
	public List<TActTransLogsDebt> selectByExample(TActTransLogsDebtExample example)
			 {
		return mapper.selectByExample(example);
	}

	@Override
	public TActTransLogsDebt selectByPrimaryKey(TActTransLogsDebtKey key)
			 {
		return mapper.selectByPrimaryKey(key);
	}

	@Override
	public List<TActTransLogsDebt> findAll(List<TActTransLogsDebt> records) {
		if(records==null||records.size()<=0){
			return mapper.selectByExample(new TActTransLogsDebtExample());
		}
		List<TActTransLogsDebt> list = new ArrayList();
		for(TActTransLogsDebt record : records){
			TActTransLogsDebt result = mapper.selectByPrimaryKey(record);
			if(result!=null){
				list.add(result);
			}
		}
		return list;
	}

	@Override
	public int updateByExampleSelective(TActTransLogsDebt record, TActTransLogsDebtExample example)  {
		return mapper.updateByExampleSelective(record, example);
	}

	@Override
	public int updateByExample(TActTransLogsDebt record, TActTransLogsDebtExample example) {
		return mapper.updateByExample(record, example);
	}

	@Override
	public int updateByPrimaryKeySelective(TActTransLogsDebt record) {
		return mapper.updateByPrimaryKeySelective(record);
	}

	@Override
	public int updateByPrimaryKey(TActTransLogsDebt record) {
		return mapper.updateByPrimaryKey(record);
	}

	@Override
	public int sumByExample(TActTransLogsDebtExample example) {
		return 0;
	}

	@Override
	public void deleteAll()  {
		mapper.deleteByExample(new TActTransLogsDebtExample());
	}

	@Override
	public TActTransLogsDebtExample getExample(TActTransLogsDebt record) {
		TActTransLogsDebtExample example = new TActTransLogsDebtExample();
		if(record!=null){
			Criteria criteria = example.createCriteria();
							if(record.getLogUuid()!=null){
				criteria.andLogUuidEqualTo(record.getLogUuid());
				}
				if(record.getFromFundNo()!=null){
				criteria.andFromFundNoEqualTo(record.getFromFundNo());
				}
				if(record.getToFundNo()!=null){
				criteria.andToFundNoEqualTo(record.getToFundNo());
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
				if(record.getCreateTime()!=null){
				criteria.andCreateTimeEqualTo(record.getCreateTime());
				}
				if(record.getUpdateTime()!=null){
				criteria.andUpdateTimeEqualTo(record.getUpdateTime());
				}

		}
		return example;
	}
	
	public TActTransLogsDebt selectOneByExample(TActTransLogsDebtExample example)
			 {
		example.setLimit(1);
		List<TActTransLogsDebt> list=mapper.selectByExample(example);
		if(list!=null&&list.size()>0){
			return list.get(0);
		}
		return null;
	}
	
	@Override
	//@Transactional
	public int batchInsert(List<TActTransLogsDebt> records) {
		SqlSession session=sqlSessionFactory.openSession();
		Connection conn = session.getConnection();
		Statement st = null;
		int result=0;
		try {
			conn.setAutoCommit(false);
			
			st = conn.createStatement();
			StringBuffer sb=new StringBuffer();
			sb.append("INSERT INTO T_ACT_TRANS_LOGS_DEBT() values");
			int i=0;
			for (TActTransLogsDebt record : records) {
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
			
				if(record.getCreateTime()==null){
						sb.append("'"+"CURRENT_TIMESTAMP"+"'");						
				}else{
					sb.append("'"+record.getCreateTime()+"'");
				}
			
				sb.append(",");
			
				if(record.getUpdateTime()==null){
						sb.append("'"+"0000-00-00 00:00:00"+"'");						
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
