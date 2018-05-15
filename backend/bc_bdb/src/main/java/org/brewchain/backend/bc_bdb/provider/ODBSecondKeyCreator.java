package org.brewchain.backend.bc_bdb.provider;

import org.apache.commons.lang3.StringUtils;
import org.brewchain.bcapi.gens.Oentity.OValue;

import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.SecondaryKeyCreator;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ODBSecondKeyCreator implements SecondaryKeyCreator {
	private TupleBinding<OValue> binding;

	@Override
	public boolean createSecondaryKey(SecondaryDatabase secondary, DatabaseEntry key, DatabaseEntry data,
			DatabaseEntry result) {
		try {
			OValue v = binding.entryToObject(data);
			if(!StringUtils.isBlank(v.getSecondKey()))
			{
				result.setData(v.getSecondKey().getBytes("UTF-8"));
				return true;
			}
		} catch (Exception e) {
		}
		return false;
	}

}
