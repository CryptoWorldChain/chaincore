package org.fc.brewchain.bcapi.crypto

import java.math.BigInteger
import org.apache.commons.lang3.StringUtils
import java.security.SecureRandom
import org.brewchain.core.crypto.ECKey
import org.spongycastle.util.encoders.Hex
import org.brewchain.core.crypto.HashUtil
import org.brewchain.core.crypto.cryptohash.Keccak256
import org.brewchain.core.crypto.ECIESCoder
import org.spongycastle.util.encoders.Base64Encoder
import org.apache.commons.codec.binary.Base64
import org.brewchain.core.crypto.ECKey.ECDSASignature
import com.googlecode.protobuf.format.util.HexUtils
import org.brewchain.core.crypto.jce.ECSignatureFactory
import org.brewchain.core.crypto.jce.SpongyCastleProvider
import org.brewchain.core.crypto.jce.ECKeyFactory
import org.spongycastle.jce.spec.ECPrivateKeySpec
import java.security.spec.ECParameterSpec
import java.security.SignatureException

@Deprecated
case class KeyPair(
  val pubkey: String,
  val prikey: String,
  val address: String,
  val bcuid: String)

@Deprecated
object EncHelper {

  def newKeyPair(): KeyPair = {

    val ran = new SecureRandom();
    //ran.generateSeed(System.currentTimeMillis().asInstanceOf[Int])
    val eckey = new ECKey(ran);
    val pubstr = Hex.toHexString(eckey.getPubKey);
    return new KeyPair(
      pubstr,
      Hex.toHexString(eckey.getPrivKeyBytes),
      Hex.toHexString(eckey.getAddress),
      BCNodeHelper.nextUID(pubstr));

  }

  def ecEncode(pubKey: String, content: Array[Byte]): String = {
    val eckey = ECKey.fromPublicOnly(Hex.decode(pubKey));
    val encBytes = ECIESCoder.encrypt(eckey.getPubKeyPoint, content);
    Base64.encodeBase64String(encBytes);
  }

  def ecEncode2Bytes(pubKey: String, content: Array[Byte]): Array[Byte] = {
    val eckey = ECKey.fromPublicOnly(Hex.decode(pubKey));
    val encBytes = ECIESCoder.encrypt(eckey.getPubKeyPoint, content);
    encBytes;
  }

  def ecSign(priKey: String, content: Array[Byte]): String = {
    val eckey = ECKey.fromPrivate(Hex.decode(priKey));

    //    println("hash:" + Hex.toHexString(HashUtil.sha256(content)));
    //    println("hashsigbase64:" + eckey.doSign(HashUtil.sha256(content)).toBase64());
    //    eckey.doSign( HashUtil.sha256(content)).toBase64();
    //    //    eckey.doSign(HashUtil.sha256(content))
    val ecSig = ECSignatureFactory.getRawInstance(SpongyCastleProvider.getInstance());
    val prikey = ECKeyFactory
      .getInstance(SpongyCastleProvider.getInstance())
      .generatePrivate(new ECPrivateKeySpec(eckey.getPrivKey, ECKey.CURVE_SPEC));

    ecSig.initSign(prikey);
    ecSig.update(HashUtil.sha256(content));
    val derSignature = ecSig.sign();
    return Base64.encodeBase64String(derSignature);
  }

  def ecVerify(pubKey: String, data: Array[Byte], sign: Array[Byte]): Boolean = {
    val eckey = ECKey.fromPublicOnly(Hex.decode(pubKey));
    eckey.verify(HashUtil.sha256(data), sign)
  }
  def ecDecode(priKey: String, encbase64str: String): Array[Byte] = {
    val eckey = ECKey.fromPrivate(Hex.decode(priKey));
    val orgBytes = ECIESCoder.decrypt(eckey.getPrivKey, Base64.decodeBase64(encbase64str));
    orgBytes;
  }

  def NullStr(v: String): String = {
    if (v == null) ""
    else
      v;
  }
  def NullBytes(v: String): Array[Byte] = {
    if (v == null) "".getBytes
    else
      v.getBytes;
  }

  def MTHash(v1: String, v2: String): String = {
    Hex.toHexString(HashUtil.sha3(
      NullBytes(v1), NullBytes(v2)))
  }

  def MTHash(v1: String, v2: String, v3: String): String = {
    return Hex.toHexString(
      HashUtil.sha3(HashUtil.sha3(NullBytes(v3)), HashUtil.sha3(NullBytes(v1), NullBytes(v2))));
  }

  def MTHash(v1: String, v2: String, v3: String, v4: String): String = {
    return Hex.toHexString(
      HashUtil.sha3(HashUtil.sha3(NullBytes(v3), NullBytes(v4)), HashUtil.sha3(NullBytes(v1), NullBytes(v2))));
  }
  def MTHash(v1: Array[Byte], v2: Array[Byte]): String = {
    return Hex.toHexString(HashUtil.sha3(v1, v2));
  }

  def hashAddr(addr: String): String = {
    Hex.toHexString(HashUtil.ripemd160(NullBytes(addr)))
  }
  def main(args: Array[String]): Unit = {

    val enc = EncHelper.newKeyPair();
    println(enc)
    //    val encContent = EncHelper.ecEncode(enc.pubkey, "blablablamatian".getBytes);
    //    println("encCotent=" + encContent);

    //    val orgCon = EncHelper.ecDecode(enc.prikey, encContent)
    //    println("orgCont:" + new String(orgCon));
    val ecsigncontent = EncHelper.ecSign(enc.prikey, "blablablamatian".getBytes)
    println("signBase64Contnt=" + ecsigncontent);
    println("signHexContnt=" + new String(Hex.encode(Base64.decodeBase64(ecsigncontent))));
    val check = EncHelper.ecVerify(enc.pubkey, "blablablamatian".getBytes, Base64.decodeBase64(ecsigncontent));
    println("checking:" + check);
    val enc1 = EncHelper.newKeyPair();
    //    println("bytse:" + new String(Hex.encode(ecsigncontent)));
    val sign = ECDSASignature.decodeFromDER(Hex.decode("30450220223a7dcd16831528d7878a06de82edc87eedc67fc65cf63e4866382f7ef75e38022100a8bf706741a17d5b2b5d9df4a2e0abe9b72ba10b06b3c0cec3ad81acfc85d4cc"));
    println("pubkey=" + enc.pubkey);
    val check1 = EncHelper.ecVerify("040e5bf73f8ab9cb4299d6ca0f75b12fa3e99605b0fbb7e0b9e653a254cba3dfb4f7b3881086d9d28f2587ba81fdab910450a34423bfeab3abeaed02016fed0e42",
      "blablablamatian".getBytes,
      // Hex.decode("304402202df0ec1409a97eb255580e35141177f9b53f48d0a405e93d7bb2916d75ba5d8c0220675296599dca83890a311b5fbfb0a03cf1b441fbb9ecdcd30c023e54233bb3e1"));
      //      Base64.decodeBase64(ecsigncontent));
      Base64.decodeBase64("MEUCIQChPZh+/jbo3Gu5y9qx/VdOqrc8lhIh577hJyIFZxW0JgIgIIec1ycxhWPOkAm+zsPGIahg0ocgbh9EtOzrXFjfHj0="));
    println("checking.1:" + check1);
    println("checking.2" + StringUtils.equals("304402202df0ec1409a97eb255580e35141177f9b53f48d0a405e93d7bb2916d75ba5d8c0220675296599dca83890a311b5fbfb0a03cf1b441fbb9ecdcd30c023e54233bb3e1",
      new String(Hex.encode(Base64.decodeBase64("MEQCIC3w7BQJqX6yVVgONRQRd/m1P0jQpAXpPXuykW11ul2MAiBnUpZZncqDiQoxG1+/sKA88bRB+7ns3NMMAj5UIzuz4Q==")))))
  }
}