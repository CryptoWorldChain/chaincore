package onight.act.persist.mysql.dao;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import onight.act.persist.entity.UserCount;
import onight.act.persist.mysql.mapper.UserCountMapper;
import onight.tfw.ojpa.api.annotations.Tab;
import onight.tfw.ojpa.ordb.ExtendDaoSupper;

//import org.springframework.transaction.annotation.Transactional;

@Data
@Tab(name = "custdao_usercount")
public class UserCountDao extends ExtendDaoSupper<UserCount, UserCount, UserCount> {

	private UserCountMapper mapper;

	public List<UserCount> selectByExample(UserCount example) {
//		return mapper.selectUserCount(example);
		UserCount uc=mapper.selectUserCount(example);
		List<UserCount> ret = new ArrayList<UserCount>();
		if (uc != null) {
			ret.add(uc);
		}
		return ret;
	}

}
