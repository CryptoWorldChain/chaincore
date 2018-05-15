package org.brewchain.ecrypto.sm;

import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.util.encoders.Base64Encoder;
import org.brewchain.core.crypto.ECKey;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * Email: king.camulos@gmail.com
 * Date: 2018/4/3
 * DESC:
 */
public class SM4Utils {
    private String secretKey = "";
    private String iv = "";
    private boolean hexString = false;

    public SM4Utils()
    {
    }

    public String encryptData_ECB(String plainText)
    {
        try
        {
            SM4_Context ctx = new SM4_Context();
            ctx.isPadding = true;
            ctx.mode = SM4.SM4_ENCRYPT;

            byte[] keyBytes;
            if (hexString)
            {
                keyBytes = Util.hexStringToBytes(secretKey);
            }
            else
            {
                keyBytes = secretKey.getBytes();
            }

            SM4 sm4 = new SM4();
            sm4.sm4_setkey_enc(ctx, keyBytes);
            byte[] encrypted = sm4.sm4_crypt_ecb(ctx, plainText.getBytes("GBK"));
            String cipherText = Base64.encodeBase64String(encrypted);
//            		new Base64Encoder()(encrypted);
            if (cipherText != null && cipherText.trim().length() > 0)
            {
                Pattern p = Pattern.compile("\\s*|\t|\r|\n");
                Matcher m = p.matcher(cipherText);
                cipherText = m.replaceAll("");
            }
            return cipherText;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public String decryptData_ECB(String cipherText)
    {
        try
        {
            SM4_Context ctx = new SM4_Context();
            ctx.isPadding = true;
            ctx.mode = SM4.SM4_DECRYPT;

            byte[] keyBytes;
            if (hexString)
            {
                keyBytes = Util.hexStringToBytes(secretKey);
            }
            else
            {
                keyBytes = secretKey.getBytes();
            }

            SM4 sm4 = new SM4();
            sm4.sm4_setkey_dec(ctx, keyBytes);
            byte[] decrypted = sm4.sm4_crypt_ecb(ctx, Base64.decodeBase64(cipherText));
            return new String(decrypted, "GBK");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public String encryptData_CBC(String plainText)
    {
        try
        {
            SM4_Context ctx = new SM4_Context();
            ctx.isPadding = true;
            ctx.mode = SM4.SM4_ENCRYPT;

            byte[] keyBytes;
            byte[] ivBytes;
            if (hexString)
            {
                keyBytes = Util.hexStringToBytes(secretKey);
                ivBytes = Util.hexStringToBytes(iv);
            }
            else
            {
                keyBytes = secretKey.getBytes();
                ivBytes = iv.getBytes();
            }

            SM4 sm4 = new SM4();
            sm4.sm4_setkey_enc(ctx, keyBytes);
            byte[] encrypted = sm4.sm4_crypt_cbc(ctx, ivBytes, plainText.getBytes("GBK"));
            String cipherText = Base64.encodeBase64String(encrypted);
            if (cipherText != null && cipherText.trim().length() > 0)
            {
                Pattern p = Pattern.compile("\\s*|\t|\r|\n");
                Matcher m = p.matcher(cipherText);
                cipherText = m.replaceAll("");
            }
            return cipherText;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public String decryptData_CBC(String cipherText)
    {
        try
        {
            SM4_Context ctx = new SM4_Context();
            ctx.isPadding = true;
            ctx.mode = SM4.SM4_DECRYPT;

            byte[] keyBytes;
            byte[] ivBytes;
            if (hexString)
            {
                keyBytes = Util.hexStringToBytes(secretKey);
                ivBytes = Util.hexStringToBytes(iv);
            }
            else
            {
                keyBytes = secretKey.getBytes();
                ivBytes = iv.getBytes();
            }

            SM4 sm4 = new SM4();
            sm4.sm4_setkey_dec(ctx, keyBytes);
            byte[] decrypted = sm4.sm4_crypt_cbc(ctx, ivBytes, Base64.decodeBase64(cipherText));
            return new String(decrypted, "GBK");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

	public static void main(String[] args) 
	{
		String plainText = "beijinG";

		SM4Utils sm4 = new SM4Utils();
		//meF8U9wHFOMfs2Y9
		sm4.secretKey = "meF8U9wHFOMfs2Y9";  //meF8U9wHFOMfs2Y9
		sm4.hexString = false;

		System.out.println("ECB模式");
		String cipherText = sm4.encryptData_ECB(plainText);
		System.out.println("密文: " + cipherText);
		System.out.println("");

		plainText = sm4.decryptData_ECB(cipherText);
		System.out.println("明文: " + plainText);
		System.out.println("");

		System.out.println("CBC模式");
		sm4.iv = "UISwD9fW6cFh9SNS";
		cipherText = sm4.encryptData_CBC(plainText);
		System.out.println("密文: " + cipherText);
		System.out.println("");

		plainText = sm4.decryptData_CBC(cipherText);
		System.out.println("明文: " + plainText);
        ECKey ecKey=new ECKey();
        //签名
        //org.fc.brewchain.bcapi.crypto.EncHelper.ecSign()
        //验签
        //org.fc.brewchain.bcapi.crypto.EncHelper.ecSign()
	}

    /**加密*/
    public String getEncStr(String inputStr,String secretKey){
        SM4Utils sm4 = new SM4Utils();
        sm4.secretKey = secretKey;  //meF8U9wHFOMfs2Y9
        sm4.hexString = false;

        System.out.println("ECB模式");
        String cipherText = sm4.encryptData_ECB(inputStr);
        System.out.println("ECB模式");
        return cipherText;
    }

    /**解密*/
    public String getDecStr(String inputStr,String secretKey){
        SM4Utils sm4Util = new SM4Utils();
        sm4Util.secretKey = secretKey; // meF8U9wHFOMfs2Y9
        sm4Util.hexString = false;
        String plainText = sm4Util.decryptData_ECB(inputStr);
        return plainText;
    }
}
