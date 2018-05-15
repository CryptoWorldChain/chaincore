package org.brewchain.backend.bc_bdb.provider;

import java.util.concurrent.Future;

import org.apache.commons.lang3.StringUtils;
import org.brewchain.bcapi.gens.Oentity.OKey;
import org.brewchain.bcapi.gens.Oentity.OValue;

import com.google.protobuf.ByteString;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VersionChecker {

	public static final String BIG_VERSION = "v1.";
	public static final String SUB_VERSION = "0.";
	public static final String MIN_VERSION = "0";

	public static final String FULL_VERSION = BIG_VERSION + SUB_VERSION + MIN_VERSION;


	public static boolean check(OBDBImpl db) {
		try {
			OKey key = OKey.newBuilder().setData(ByteString.copyFrom("BC_VERSION", "UTF-8")).build();
			Future<OValue> ver = db.get(key);
			if (ver == null || ver.get() == null) {
				db.put(key, OValue.newBuilder().setInfo(FULL_VERSION).build());
				log.info("DBVersion Check SUCCESS:");
			} else if (!StringUtils.startsWith(ver.get().getInfo(), BIG_VERSION)) {
				//
				log.error("DBVersion Check ERROR!Current=" + FULL_VERSION + ",db version=" + ver.get().getInfo()
						+ ". It will Cause Unknowm Problem!!");
				System.exit(-1);
			} else if (!StringUtils.startsWith(ver.get().getInfo(), BIG_VERSION + SUB_VERSION)) {
				//
				log.warn("DBVersion Check Warning!Current=" + FULL_VERSION + ",db version=" + ver.get().getInfo()
						+ ". It will Cause Unknowm Problem!!");
				// this.put("BC_VERSION", FULL_VERSION);
			} else {
				log.info("DBVersion Check SUCCESS:");
			}
		} catch (Exception e) {
			log.error("DBVersion Check Failed", e);
			System.exit(-1);
		}
		return true;
	}
}
