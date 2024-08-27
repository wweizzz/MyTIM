package com.tencent.qcloud.tim.demo.signature;

import android.os.Build;
import android.text.TextUtils;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.zip.Deflater;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Module: GenerateTestUserSig
 * <p>
 * Function: Used to generate UserSig for testing. UserSig is a security signature designed by Tencent Cloud for its cloud services.
 * It is calculated based on SDKAppID, UserID, and EXPIRETIME using the HMAC-SHA256 encryption algorithm.
 * <p>
 * Attention: Do not use the code below in your commercial application. This is because:
 * <p>
 * The code may be able to calculate UserSig correctly, but it is only for quick testing of the SDK’s basic features, not for commercial applications.
 * SECRETKEY in client code can be easily decompiled and reversed, especially on web.
 * Once your key is disclosed, attackers will be able to steal your Tencent Cloud traffic.
 * <p>
 * The correct method is to deploy the UserSig calculation code and encryption key on your project server so that your application can request from your server a UserSig that is calculated whenever one is needed.
 * Given that it is more difficult to hack a server than a client application, server-end calculation can better protect your key.
 * <p>
 * Reference: https://intl.cloud.tencent.com/document/product/1047/34385
 */
public class GenerateTestUserSig {

    /**
     * Tencent Cloud SDKAppID. Set it to the SDKAppID of your account.
     * <p>
     * You can view your SDKAppID after creating an application in the [Tencent Cloud IM console](https://console.intl.cloud.tencent.com/im).
     * SDKAppID uniquely identifies a Tencent Cloud account.
     */
    public static final int SDKAPPID = 1600043655;

    public static final String userId1 = "750340";
    public static final String userSig1 = "eJyrVgrxCdZLrSjILEpVsjI1tDAxMDDQAQuWpRYpWSkZ6RkoQfjFKdmJBQWZKUpWhmZARSbGZqamEJnMlNS8ksy0TLAGc1MDYxO4nsx0oFBhqFNBlLOrZbmlsUFqsYlxmm9FlHaKqZN3rk*pX6SJfoq*aUZuUVhxVFikLVRjSWYu0D2G5kYmZqaG5hYGtQDIki-X";

    public static final String userId2 = "458705";
    public static final String userSig2 = "eJw1jkEOgjAURO-SrYa02A8tiQsWrtQQrbonaYGvoA2UYjTeXQK6nDfzknmT004F5mmxNSQBJjildDlBb1qSkDCgZM6dvuXWoiYJi8YRX0UAc4Pa3B0WOAkcREzh72A5ItvtVZo3dT-IOh0emS-p1vWXTPnX5qiq6qCFiBcg3fW8-okOm-EPi0MeAZOMfb4aHjEZ";

    private static Boolean isMoNiQi() {
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.R;
    }

    public static String getUserId() {
        return isMoNiQi() ? userId1 : userId2;
    }

    public static String getUserSig() {
        return isMoNiQi() ? userSig1 : userSig2;
    }

    /**
     * Signature validity period, which should not be set too short
     * <p>
     * Time unit: second
     * Default value: 604800 (7 days)
     */
    private static final int EXPIRETIME = 604800;

    /**
     * Follow the steps below to obtain the key required for UserSig calculation.
     * <p>
     * Step 1. Log in to the [Tencent Cloud IM console](https://console.intl.cloud.tencent.com/im), and create an application if you don’t have one.
     * Step 2. Click Application Configuration to go to the basic configuration page and locate Account System Integration.
     * Step 3. Click View Key to view the encrypted key used for UserSig calculation. Then copy and paste the key to the variable below.
     * <p>
     * Note: this method is for testing only. Before commercial launch, please migrate the UserSig calculation code and key to your backend server to prevent key disclosure and traffic stealing.
     * Reference: https://intl.cloud.tencent.com/document/product/1047/34385
     */
    private static final String SECRETKEY = "";

    /**
     * Calculate UserSig
     * <p>
     * The asymmetric encryption algorithm HMAC-SHA256 is used in the function to calculate UserSig based on SDKAppID, UserID, and EXPIRETIME.
     *
     * @note: Do not use the code below in your commercial application. This is because:
     * <p>
     * The code may be able to calculate UserSig correctly, but it is only for quick testing of the SDK’s basic features, not for commercial applications.
     * SECRETKEY in client code can be easily decompiled and reversed, especially on web.
     * Once your key is disclosed, attackers will be able to steal your Tencent Cloud traffic.
     * <p>
     * The correct method is to deploy the UserSig calculation code and encryption key on your project server so that your application can request from your server a UserSig that is calculated whenever one is needed.
     * Given that it is more difficult to hack a server than a client application, server-end calculation can better protect your key.
     * <p>
     * Reference: https://intl.cloud.tencent.com/document/product/1047/34385
     */

    public static String genTestUserId() {
        return getUserId();
    }

    public static String genTestUserSig() {
        return getUserSig();
    }

    /**
     * Generate a TLS ticket
     *
     * @param sdkappid      AppID of the application
     * @param userId        User ID
     * @param expire        Validity period, in seconds
     * @param userbuf       null by default
     * @param priKeyContent Private key required for generating a TLS ticket
     * @return If an error occurs, an empty string will be returned or exceptions printed. If the operation succeeds, a valid ticket will be returned.
     */
    private static String GenTLSSignature(long sdkappid, String userId, long expire, byte[] userbuf, String priKeyContent) {
        if (TextUtils.isEmpty(priKeyContent)) {
            return "";
        }
        long currTime = System.currentTimeMillis() / 1000;
        JSONObject sigDoc = new JSONObject();
        try {
            sigDoc.put("TLS.ver", "2.0");
            sigDoc.put("TLS.identifier", userId);
            sigDoc.put("TLS.sdkappid", sdkappid);
            sigDoc.put("TLS.expire", expire);
            sigDoc.put("TLS.time", currTime);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String base64UserBuf = null;
        if (null != userbuf) {
            base64UserBuf = Base64.encodeToString(userbuf, Base64.NO_WRAP);
            try {
                sigDoc.put("TLS.userbuf", base64UserBuf);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        String sig = hmacsha256(sdkappid, userId, currTime, expire, priKeyContent, base64UserBuf);
        if (sig.length() == 0) {
            return "";
        }
        try {
            sigDoc.put("TLS.sig", sig);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Deflater compressor = new Deflater();
        compressor.setInput(sigDoc.toString().getBytes(Charset.forName("UTF-8")));
        compressor.finish();
        byte[] compressedBytes = new byte[2048];
        int compressedBytesLength = compressor.deflate(compressedBytes);
        compressor.end();
        return new String(base64EncodeUrl(Arrays.copyOfRange(compressedBytes, 0, compressedBytesLength)));
    }


    private static String hmacsha256(long sdkappid, String userId, long currTime, long expire, String priKeyContent, String base64Userbuf) {
        String contentToBeSigned = "TLS.identifier:" + userId + "\n"
                + "TLS.sdkappid:" + sdkappid + "\n"
                + "TLS.time:" + currTime + "\n"
                + "TLS.expire:" + expire + "\n";
        if (null != base64Userbuf) {
            contentToBeSigned += "TLS.userbuf:" + base64Userbuf + "\n";
        }
        try {
            byte[] byteKey = priKeyContent.getBytes("UTF-8");
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(byteKey, "HmacSHA256");
            hmac.init(keySpec);
            byte[] byteSig = hmac.doFinal(contentToBeSigned.getBytes("UTF-8"));
            return new String(Base64.encode(byteSig, Base64.NO_WRAP));
        } catch (UnsupportedEncodingException e) {
            return "";
        } catch (NoSuchAlgorithmException e) {
            return "";
        } catch (InvalidKeyException e) {
            return "";
        }
    }

    private static byte[] base64EncodeUrl(byte[] input) {
        byte[] base64 = new String(Base64.encode(input, Base64.NO_WRAP)).getBytes();
        for (int i = 0; i < base64.length; ++i)
            switch (base64[i]) {
                case '+':
                    base64[i] = '*';
                    break;
                case '/':
                    base64[i] = '-';
                    break;
                case '=':
                    base64[i] = '_';
                    break;
                default:
                    break;
            }
        return base64;
    }

}
