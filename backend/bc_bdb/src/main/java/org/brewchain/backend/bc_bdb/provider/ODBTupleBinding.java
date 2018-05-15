package org.brewchain.backend.bc_bdb.provider;

import org.brewchain.bcapi.backend.ODBHelper;
import org.brewchain.bcapi.gens.Oentity.OValue;

import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;

public class ODBTupleBinding extends TupleBinding<OValue> {

	@Override
	public OValue entryToObject(TupleInput input) {
		byte bb[] =new byte[input.available()];
		input.read(bb);
		return ODBHelper.b2Value(bb);
	}

	@Override
	public void objectToEntry(OValue object, TupleOutput output) {
		output.write(object.toByteArray());
	}

}
