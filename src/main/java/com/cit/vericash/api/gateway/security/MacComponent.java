package com.cit.vericash.api.gateway.security;


import com.cit.vericash.api.gateway.model.*;
import com.cit.vericash.portal.backend.data.QueryService;
import com.cit.vericash.portal.backend.model.dao.QueryMessage;
import com.cit.vericash.portal.backend.model.dao.Record;
import com.cit.vericash.portal.backend.model.dao.ResultSet;
import com.cit.vericash.portal.backend.util.PropertyLoader;
import org.apache.commons.codec.binary.Base64;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class MacComponent {

    @Autowired
    PropertyLoader propLoader;

    @Autowired
    QueryService queryService;

    /*
    *Checks if the signature of the Message is the same signature sent from the front-end
    */
    public boolean isDataAltered(String serviceCode, String body, String requestMac, Long userId) throws ParseException {
        String isEnabledPerServiceCode = propLoader.getPropertyAsString(serviceCode+".mac.enabled", Constants.GATEWAY_FOLDER_NAME,Constants.GATEWAY_CONFIG_FILE_NAME);
        Boolean isEnabled = propLoader.getPropertyAsBoolean("mac.enabled", Constants.GATEWAY_FOLDER_NAME,Constants.GATEWAY_CONFIG_FILE_NAME);
        if(!isEnabled){
            return false;
        }
        if(isEnabled && (isEnabledPerServiceCode!=null && isEnabledPerServiceCode.equalsIgnoreCase("false")))
            return false;

        if(requestMac==null || requestMac.trim().isEmpty())
        {
            return true;
        }
        body=body.replaceAll("\\{\"message\":","");
        body=body.substring(0,body.indexOf(",\"mac\""));
        String data=body;
        String  macKey = getMacKeyByUserId(userId);
        try {
            String signture=generateSignature(data,macKey);
//            System.out.println("requestMac: "+requestMac);
//            System.out.println("signture: "+signture);
            if(!requestMac.equals(signture)){
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
    private static MessageDigest messageDigest=null;
    static
    {
        try {
            messageDigest=MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
    private String getMacKeyByUserId(Long userId)
    {
        String macKey=null;
        QueryMessage queryMessage=new QueryMessage();
        queryMessage.setQuery("select mac_key from PORTAL_USER_AUTHENTICATION where business_user_id="+userId);
        ResultSet resultSet=queryService.executeQuery(queryMessage);
        if(resultSet.getRecords()!=null && resultSet.getRecords().size()>0)
        {
            Record commonRecord =resultSet.getRecords().get(0);
            macKey= commonRecord.getFieldAsString("MAC_KEY");
        }
        return macKey;
    }
    private String generateSignature(String data,String key){
        String signature = "";
        try {
            synchronized (this) {
                messageDigest.update((data).getBytes());
                Mac mac = Mac.getInstance("HmacSHA512");
                mac.init(new SecretKeySpec(Base64.decodeBase64(key.getBytes()), "HmacSHA512"));
                byte[] digest = messageDigest.digest();
                signature = Base64.encodeBase64String(mac.doFinal(digest));
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return signature;
    }
}
