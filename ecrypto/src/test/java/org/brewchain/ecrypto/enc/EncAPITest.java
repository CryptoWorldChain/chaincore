package org.brewchain.ecrypto.enc;

import org.brewchain.ecrypto.impl.EncInstance;
import org.fc.brewchain.bcapi.EncAPI;
import org.fc.brewchain.bcapi.KeyPairs;

public class EncAPITest {

	public static void main(String[] args) {
		
		
//		EncAPI enc = new EncInstance();
//		KeyPairs key = enc.genKeys();
//		
//		System.out.println("bcuid:" + key.getBcuid());
//		System.out.println("pri:  " + key.getPrikey());
//		System.out.println("pub:  " + key.getPubkey());
//		System.out.println("addr: " + key.getAddress());
//	    String content = "测试";
//	    byte[] hash = HashUtil.sha3(content.getBytes());
//	    System.out.println("hash: " + hash);
//	    val sign = enc.ecSignHex(key.getPrikey(), hash);
//	    System.out.println("sign: " + sign);
//	    
//	    System.out.println(enc.ecToAddress(hash, enc.hexEnc(Base64.encode(sign.getBytes()))));
//
//	    System.out.println(enc.ecToKeyBytes(hash, enc.hexEnc(Base64.encode(sign.getBytes()))));
	    
		
		EncAPI encAPI = new EncInstance();
		
		KeyPairs k1 = encAPI.genKeys("ailen");
		System.out.println("pri1 ="+k1.getPrikey());
		System.out.println("pub1 ="+k1.getPubkey());
		System.out.println("addr1="+k1.getAddress());
		
		KeyPairs k2 = encAPI.genKeys("ailen");
		System.out.println("pri2 ="+k2.getPrikey());
		System.out.println("pub2 ="+k2.getPubkey());
		System.out.println("addr2="+k2.getAddress());
		
		KeyPairs k3 = encAPI.genKeys("ailen");
		System.out.println("pri3 ="+k2.getPrikey());
		System.out.println("pub3 ="+k2.getPubkey());
		System.out.println("addr3="+k2.getAddress());
		
		
		KeyPairs k = encAPI.genKeys();
		System.out.println("pri ="+k.getPrikey());
		System.out.println("pub ="+k.getPubkey());
		System.out.println("addr="+k.getAddress());
		byte[] s = encAPI.ecSign(k.getPrikey(), "abc".getBytes());
		System.out.println("sign="+encAPI.hexEnc(s));
		String sb64 = encAPI.base64Enc(s);
		System.out.println("sb64="+sb64);
		
		System.out.println("verify="+encAPI.ecVerify(k.getPubkey(), "abc".getBytes(), s));

		byte[] pkb641 = encAPI.ecToKeyBytes(
				encAPI.sha256Encode("abc".getBytes())
				, sb64);
		System.out.println("pub ="+encAPI.hexEnc(pkb641));
		
		byte[] ab641 = encAPI.ecToAddress(
				encAPI.sha256Encode("abc".getBytes())
				, sb64);
		System.out.println("addr="+encAPI.hexEnc(ab641));
		
		byte[] pkb64 = encAPI.ecToKeyBytes(encAPI.sha3Encode("abc".getBytes()), sb64);
		System.out.println("pub ="+encAPI.hexEnc(pkb64));
		
		byte[] ab64 = encAPI.ecToAddress(encAPI.sha3Encode("abc".getBytes()), sb64);
		System.out.println("addr="+encAPI.hexEnc(ab64));
		
		
	}
}
