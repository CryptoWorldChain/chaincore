package org.brewchain.ecrypto.address;

import java.security.SecureRandom;
import java.util.List;

import org.brewchain.core.crypto.ECKey;
import org.spongycastle.util.encoders.Hex;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestNewAddr {

	public static String randomPK(int length) {
	    //随机字符串的随机字符库
	    String keyString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ9";
	    StringBuffer sb = new StringBuffer();
	    int len = keyString.length();
	    for (int i = 0; i < length; i++) {
	       sb.append(keyString.charAt((int) Math.round(Math.random() * (len - 1))));
	    }
	    return sb.toString();
	}
	
	public static void main(String[] args) throws Exception {

		SecureRandom ran = new SecureRandom();
		ECKey eckey = new ECKey(ran);
		System.out.println(Hex.toHexString(eckey.getAddress()));
		System.out.println(Hex.toHexString(eckey.getPrivKeyBytes()));
		System.out.println(Hex.toHexString(eckey.getPubKey()));
		
		System.out.println(Hex.toHexString(ECKey.fromPrivate(eckey.getPrivKeyBytes()).getAddress()));
		
		
		
		System.exit(0);
		
		String private_key = randomPK(81);
		
		org.brewchain.ecrypto.address.NewAddress newAddr = 
				org.brewchain.ecrypto.address.AddressFactory.create(org.brewchain.ecrypto.address.AddressEnum.IOTA);
		
		log.info("pri="+private_key);
		List<String> addr = newAddr.newAddress(private_key, 2, 0, false, 1, true);
		for(int i=0;i<addr.size();i++) {
			log.info("addr.get("+i+")="+addr.get(i));
		}
		
		//  TODO seed相同，相同顺序的地址也相同
		log.info("");
		log.info("pri="+private_key);
		private_key = "NDIICQCKUMCBXUGTIUZCCTYRMYVISHFGVEJDLVDODS9TELXS9YGSNSXDJTNTZUZJOJNBTYLPGIFUXVHKH";
		addr = newAddr.newAddress(private_key, 2, 0, false, 5, true);
		for(int i=0;i<addr.size();i++) {
			log.info("addr.get("+i+")="+addr.get(i));
		}
		

		log.info("");
		log.info("pri="+private_key);
		private_key = "NDIICQCKUMCBXUGTIUZCCTYRMYVISHFGVEJDLVDODS9TELXS9YGSNSXDJTNTZUZJOJNBTYLPGIFUXVHKH";
		addr = newAddr.newAddress(private_key, 2, 4, false, 1, true);
		for(int i=0;i<addr.size();i++) {
			log.info("addr.get("+i+")="+addr.get(i));
		}
	}
}
