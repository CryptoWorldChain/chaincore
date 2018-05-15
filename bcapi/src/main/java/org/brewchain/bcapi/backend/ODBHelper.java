package org.brewchain.bcapi.backend;

import org.brewchain.bcapi.gens.Oentity.OValue;

import com.google.protobuf.InvalidProtocolBufferException;


public class ODBHelper {

	public static OValue b2Value(byte[] data) throws ODBException {
		try {
			return OValue.newBuilder().mergeFrom(data).build();
		} catch (InvalidProtocolBufferException e) {
			throw new ODBException("read error", e);
		}
	}

	public static byte[] v2Bytes(OValue value) throws ODBException {
		return value.toByteArray();
	}
}
