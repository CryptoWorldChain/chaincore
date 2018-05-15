package org.brewchain.account.core.collector;

import lombok.Data;

@Data
public class AccountDailyCollector {
	private String address;
	private long totalDailyAmount;
	private int totalDailyCount;
	
	public AccountDailyCollector(){}
	
	public void put(String address, long amount) {}
	
	
}
