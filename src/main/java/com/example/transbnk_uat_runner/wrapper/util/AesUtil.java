package com.example.transbnk_uat_runner.wrapper.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class AesUtil {

	private AesUtil() {
	}

	public static String encrypt(String plainText, String base64Key, String base64IV) throws Exception {
		byte[] keyBytes = Base64.getDecoder().decode(base64Key);
		byte[] ivBytes = Base64.getDecoder().decode(base64IV);

		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
		IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

		cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
		byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
		return Base64.getEncoder().encodeToString(encrypted);
	}

	public static String decrypt(String encryptedText, String base64Key, String base64IV) throws Exception {
		byte[] keyBytes = Base64.getDecoder().decode(base64Key);
		byte[] ivBytes = Base64.getDecoder().decode(base64IV);

		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
		IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

		cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

		byte[] decoded = Base64.getDecoder().decode(encryptedText);
		byte[] decrypted = cipher.doFinal(decoded);
		return new String(decrypted, StandardCharsets.UTF_8);
	}
}

