package com.cit.vericash.api.gateway.security.cryptography;

import org.apache.commons.codec.binary.Base64;
import org.springframework.core.io.ClassPathResource;

import javax.crypto.Cipher;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class RSAUtils {
	public static final String RSA_ECB_PKCS1_PADDING = "RSA/ECB/PKCS1Padding";

	public static final int KEY_SIZE_2048 = 2048;
	public static final int KEY_SIZE_1024 = 1024;

	private RSAUtils() {
	}

	private static final String ALGORITHM = "RSA";

	public static KeyPair generateKeyPair() {
		return generateKeyPair(KEY_SIZE_2048);
	}

	public static KeyPair generateKeyPair(int keySize) {
		try {
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM);
			keyPairGenerator.initialize(keySize);
			return keyPairGenerator.generateKeyPair();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException("Failed to generate key pair!", e);
		}
	}

	public static PublicKey getPublicKey(String base64PublicKey) {
		try {
			X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.decodeBase64(base64PublicKey));
			KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
			PublicKey publicKey = keyFactory.generatePublic(keySpec);
			return publicKey;
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to get public key!", e);
		}
	}

	public static PublicKey getPublicKey(BigInteger modulus, BigInteger exponent) {
		try {
			KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
			RSAPublicKeySpec keySpec = new RSAPublicKeySpec(modulus, exponent);
			return keyFactory.generatePublic(keySpec);
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to get public key!", e);
		}
	}

	public static String getBase64PublicKey(PublicKey publicKey) {
		return Base64.encodeBase64String(publicKey.getEncoded());
	}

	public static PrivateKey getPrivateKey(String base64PrivateKey) {
		try {
			PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.decodeBase64(base64PrivateKey));
			KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
			PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
			return privateKey;
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to get private key!", e);
		}
	}

	public static PrivateKey getPrivateKey(BigInteger modulus, BigInteger exponent) {
		try {
			KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
			RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(modulus, exponent);
			return keyFactory.generatePrivate(keySpec);
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to get private key!", e);
		}
	}

	public static String getBase64PrivateKey(PrivateKey privateKey) {
		return Base64.encodeBase64String(privateKey.getEncoded());
	}

	public static byte[] encryptAsByteArray(String data, PublicKey publicKey) {
		throwNullPointException(data);
		throwNullPointException(publicKey);
		try {
			Cipher cipher = Cipher.getInstance(RSA_ECB_PKCS1_PADDING);
			cipher.init(Cipher.ENCRYPT_MODE, publicKey);
			return cipher.doFinal(data.getBytes());
		} catch (Exception e) {
			throw new IllegalArgumentException("Encrypt failed!", e);
		}
	}

	public static byte[] encryptAsByteArray(String data, String base64PublicKey) {
		return encryptAsByteArray(data, getPublicKey(base64PublicKey));
	}

	public static String encryptAsString(String data, PublicKey publicKey) {
		return Base64.encodeBase64String(encryptAsByteArray(data, publicKey));
	}

	public static String encryptAsString(String data, String base64PublicKey) {
		return Base64.encodeBase64String(encryptAsByteArray(data, getPublicKey(base64PublicKey)));
	}
	public static Cipher cipher=null;
	public static String decrypt(byte[] data, PrivateKey privateKey) {
		throwNullPointException(data);
		throwNullPointException(privateKey);
		try {
			if(cipher==null) {
				cipher = Cipher.getInstance(RSA_ECB_PKCS1_PADDING);
				cipher.init(Cipher.DECRYPT_MODE, privateKey);
			}
			return new String(cipher.doFinal(data));
		} catch (Exception e) {
			throw new IllegalArgumentException("Decrypt failed!", e);
		}
	}

	public static String decrypt(byte[] data, String base64PrivateKey) {
		return decrypt(data, getPrivateKey(base64PrivateKey));
	}

	public static String decrypt(String data, PrivateKey privateKey) {
		return decrypt(Base64.decodeBase64(data), privateKey);
	}

	public static String decrypt(String data, String base64PrivateKey) {
		return decrypt(Base64.decodeBase64(data), getPrivateKey(base64PrivateKey));
	}

	private static void throwNullPointException(Object obj) {
		if (null == obj) {
			throw new NullPointerException();
		}
	}

	/*Reads private key on the classpath*/
	public static PrivateKey readPrivateKeyFromFile(String fileName) {
		FileInputStream fis = null;
		ObjectInputStream ois = null;
		try {
			InputStream in = RSAUtils.class.getResourceAsStream(fileName);
			ois = new ObjectInputStream(in);
			BigInteger modulus = (BigInteger) ois.readObject();
			BigInteger exponent = (BigInteger) ois.readObject();

			RSAPrivateKeySpec rsaPrivateKeySpec = new RSAPrivateKeySpec(modulus, exponent);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			PrivateKey privateKey = keyFactory.generatePrivate(rsaPrivateKeySpec);
			return privateKey;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static PublicKey readPublicKeyFromFile(String fileName) {
		// TODO Auto-generated method stub
		FileInputStream fis = null;
		ObjectInputStream ois = null;
		try {
			fis = new FileInputStream(new File(fileName));
			ois = new ObjectInputStream(fis);
			BigInteger modulus = (BigInteger) ois.readObject();
			BigInteger exponent = (BigInteger) ois.readObject();

			RSAPublicKeySpec rsaPublicKeySpec = new RSAPublicKeySpec(modulus, exponent);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			PublicKey publicKey = keyFactory.generatePublic(rsaPublicKeySpec);
			return publicKey;

		} catch (Exception e) {
			// TODO: handle exception
		}
		return null;
	}

}
