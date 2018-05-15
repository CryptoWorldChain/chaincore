package org.brewchain.account.core;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public abstract class AbstractLocalCache {
	public static LoadingCache<String, Long> dayTotalAmount = CacheBuilder.newBuilder()
			.maximumSize(10000)
			.expireAfterAccess(86400, TimeUnit.SECONDS)
			.build(new CacheLoader<String, Long>(){
	            @Override
	            public Long load(String key) throws Exception {        
	                return Long.valueOf("0");
	            }
	        });
}
