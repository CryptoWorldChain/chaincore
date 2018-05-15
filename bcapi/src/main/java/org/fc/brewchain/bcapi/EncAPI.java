package org.fc.brewchain.bcapi;

public interface EncAPI {
	public KeyPairs genKeys();
	
	/**
	 * 创建地址
	 * @param seed 助记词字符串
	 * @return
	 */
	public KeyPairs genKeys(String seed);

	public byte[] ecEncode(String pubKey, byte[] content);

	public byte[] ecDecode(String priKey, byte[] content);

	public byte[] ecSign(String priKey, byte[] contentHash);

	public String ecSignHex(String priKey, byte[] contentHash);

	public boolean ecVerify(String pubKey, byte[] contentHash, byte[] sign);

	public String ecSignHex(String priKey, String hexHash);

	public boolean ecVerifyHex(String pubKey, String hexHash, String signhex);

	public boolean ecVerifyHex(String pubKey, byte[] hexHash, String signhex);

	public String base64Enc(byte[] data);

	public byte[] base64Dec(String data);

	public String hexEnc(byte[] data);

	public byte[] hexDec(String data);
	
	public byte[] ecToAddress(byte[] contentHash, String signBase64);
	
	public byte[] ecToKeyBytes(byte[] contentHash, String signBase64);
	
	public byte[] sha3Encode(byte[] input);
	
	public byte[] sha256Encode(byte[] input);
}
