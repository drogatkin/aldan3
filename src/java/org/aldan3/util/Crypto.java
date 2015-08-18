/* aldan3 - Crypto.java
 * Copyright (C) 1999-2009 Dmitriy Rogatkin.  All rights reserved.
 *  $Id: Crypto.java,v 1.6 2012/05/16 04:14:28 dmitriy Exp $                
 *  Created on Jun 23, 2009
 *  @author Dmitriy
 */
package org.aldan3.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

public class Crypto {

	public static final String DEFAULT_METHOD = "PBEWithMD5AndDES";

	private static final char ALG_SEP = '$';

	public static String METHOD = DEFAULT_METHOD;

	// TODO using SealedObject, it protects implementation details
	// @see
	// http://java.sun.com/j2se/1.5.0/docs/api/javax/crypto/SealedObject.html

	// Salt
	protected static byte[] salt = { (byte) 0xc7, (byte) 0x73, (byte) 0x21,
			(byte) 0x8c, (byte) 0x7e, (byte) 0xc9, (byte) 0xee, (byte) 0x99 };

	protected static int count = 20;

	protected static PBEParameterSpec pbeParamSpec = new PBEParameterSpec(salt,
			count);

	private Key key;

	public Crypto(String password) {
		try {
			password = new String(password.getBytes("ISO-8859-1"));
		} catch (UnsupportedEncodingException e1) {
		}
		
		KeySpec keySpec = null;
		
		SecretKeyFactory keyFac;
		try {
			if (METHOD.startsWith("PBE"))
				keySpec = new PBEKeySpec(password.toCharArray());
			else if (METHOD.startsWith("DES"))
				keySpec = new DESKeySpec(password.getBytes());
			if (keySpec == null)
				throw new NoSuchAlgorithmException("No key found matching algorithm: "+METHOD);
			keyFac = SecretKeyFactory.getInstance(METHOD);
			// read key from file if available
			key = keyFac.generateSecret(keySpec);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		/*
		 * KeyGenerator keygen; try { keygen =
		 * KeyGenerator.getInstance(CertificateOperations.DES); keygen.init( 56,
		 * new SecureRandom(new SecureRandom().generateSeed(8))); key =
		 * keygen.generateKey(); } catch (NoSuchAlgorithmException e) {}
		 */catch (InvalidKeySpecException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		}
	}

	public String encrypt(String src) {
		if (src == null)
			return null;
		StringBuffer result = new StringBuffer();
		try {
			Cipher c = Cipher.getInstance(METHOD);
			c.init(Cipher.ENCRYPT_MODE, key, pbeParamSpec);
			byte[] cipherText = c.doFinal(src.getBytes("utf-8"));
			result.append(DataConv.bytesToHex(cipherText)).append(ALG_SEP);
			AlgorithmParameters params = c.getParameters();
			if (params != null) {
				byte[] encodedAlgParams = params.getEncoded();
				result.append(DataConv.bytesToHex(encodedAlgParams));
			}
		} catch (NoSuchAlgorithmException e) {
			result.append(e);
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			result.append(e);
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			result.append(e);
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			result.append(e);
			e.printStackTrace();
		} catch (BadPaddingException e) {
			result.append(e);
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			result.append(e);
			e.printStackTrace();
		} catch (IOException e) {
			result.append(e);
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		}
		return result.toString();
	}

	public String decrypt(String src) {
		if (src == null)
			return null;
		int p = src.indexOf(ALG_SEP);
		if (p < 0)
			return null;
		AlgorithmParameters algParams = null;
		try {
			if (src.length() > p + 1) {
				algParams = AlgorithmParameters.getInstance(METHOD);
				algParams.init(DataConv.hexToBytes(src.substring(p + 1)));
			}
			Cipher c = Cipher.getInstance(METHOD);
			if (algParams == null)
				c.init(Cipher.DECRYPT_MODE, key, pbeParamSpec);
			else
				c.init(Cipher.DECRYPT_MODE, key, algParams);
			return new String(
					c.doFinal(DataConv.hexToBytes(src.substring(0, p))),
					"utf-8");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return e.getMessage();
		} catch (IOException e) {
			e.printStackTrace();
			return e.getMessage();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
			return e.getMessage();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
			return e.getMessage();
		} catch (InvalidAlgorithmParameterException e) {
			e.printStackTrace();
			return e.getMessage();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
			return e.getMessage();
		} catch (BadPaddingException e) {
			e.printStackTrace();
			return e.getMessage();
		}
	}

	public static void main(String... strings) {
		if (strings.length == 2)
			System.out.println(new Crypto(strings[0]).encrypt(strings[1]));
		else if (strings.length == 3)
			System.out.println(new Crypto(strings[0]).decrypt(strings[1]));
	}
}
