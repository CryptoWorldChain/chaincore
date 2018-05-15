package org.brewchain.account.util;

import org.brewchain.bcapi.gens.Oentity.OKey;
import org.brewchain.bcapi.gens.Oentity.OValue;

import com.google.protobuf.ByteString;

public class OEntityBuilder {
	public static OKey byteKey2OKey(byte[] key) {
		OKey.Builder oOKey = OKey.newBuilder();
		oOKey.setData(ByteString.copyFrom(key));
		return oOKey.build();
	}

	public static OKey byteKey2OKey(ByteString key) {
		OKey.Builder oOKey = OKey.newBuilder();
		oOKey.setData(key);
		return oOKey.build();
	}

	public static byte[] oKey2byteKey(OKey key) {
		return key.getData().toByteArray();
	}

	public static OValue byteValue2OValue(byte[] value) {
		OValue.Builder oOValue = OValue.newBuilder();
		oOValue.setExtdata(ByteString.copyFrom(value));
		return oOValue.build();
	}
	public static OValue byteValue2OValue(byte[] value, String secKey) {
		OValue.Builder oOValue = OValue.newBuilder();
		oOValue.setExtdata(ByteString.copyFrom(value));
		oOValue.setSecondKey(secKey);
		return oOValue.build();
	}
	public static OValue byteValue2OValue(ByteString value) {
		OValue.Builder oOValue = OValue.newBuilder();
		oOValue.setExtdata(value);
		return oOValue.build();
	}

	public static byte[] oValue2byteValue(OValue value) {
		return value.getExtdata().toByteArray();
	}

	public static boolean isNullValue(OValue value) {
		if (value == null)
			return true;
		if (value.getExtdata() == null)
			return true;
		return false;
	}

	public static boolean isNullKey(OKey key) {
		if (key == null)
			return true;
		if (key.getData() == null)
			return true;
		return false;
	}
}
