package com.cit.vericash.api.gateway.security.cryptography;

import com.cit.vericash.api.gateway.model.*;
import com.cit.vericash.portal.backend.model.message.Message;
import com.cit.vericash.portal.backend.model.request.NonSslRequest;
import com.cit.vericash.portal.backend.model.request.Request;
import com.cit.vericash.portal.backend.model.request.SslRequest;
import com.google.gson.Gson;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.PrivateKey;


@Component
public class CryptoComponent {

	private static PrivateKey privateKey = null;
	public Message decrypt(Request request, boolean isSSLEnabled) throws IOException {
		Gson gson = new Gson();
		if(!isSSLEnabled){
			StringBuffer buffer = new StringBuffer();
			NonSslRequest nonSslRequest = (NonSslRequest) request;
			if(privateKey==null){
				privateKey = RSAUtils.readPrivateKeyFromFile("/Private.key");
			}
			for (String element : nonSslRequest.getEncryptedMessage()) {
				buffer.append(RSAUtils.decrypt(element, privateKey));
			}
			return gson.fromJson(buffer.toString(),Message.class);
		} else {
			SslRequest sslRequest = (SslRequest) request;
			return sslRequest.getMessage();
		}
	}

}
