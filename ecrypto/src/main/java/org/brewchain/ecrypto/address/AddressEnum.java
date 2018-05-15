package org.brewchain.ecrypto.address;

public enum AddressEnum {
	
	BTC("",(byte)0x10)
	,IOTA("",(byte)0x20)
	,ETH("",(byte)0x30);
	
	public String name;
	public byte addrPefix;
	
	private AddressEnum(String name,byte addrPefix) {
		this.name=name;
		this.addrPefix=addrPefix;
	}
	
	
}
