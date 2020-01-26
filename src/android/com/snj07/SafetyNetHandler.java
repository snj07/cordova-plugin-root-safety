package com.snj07;


import android.app.Activity;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.webtoken.JsonWebSignature;
import com.google.api.client.util.Key;

import org.apache.http.conn.ssl.DefaultHostnameVerifier;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLException;

public class SafetyNetHandler {

    public static final String STATUS_FIELD = "status";
    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_FAILURE = "failure";
    public static final String STATUS_ERROR = "error";
    public static final String RESPONSE_FIELD = "response";
    // API to verify SafetyNet Response - fixed quota of 10,000 requests per day
    private static final String GOOGLE_VERIFICATION_URL = "https://www.googleapis.com/androidcheck/v1/attestations/verify?key=";
    private final String TAG = "SafetyNetHandler";

    public SafetyNetHandler() {

    }

    private boolean verifyHostname(String hostname, X509Certificate leafCert) {
        try {
            DefaultHostnameVerifier hostnameVerifier = new DefaultHostnameVerifier();
            hostnameVerifier.verify(hostname, leafCert);
            return true;
        } catch (SSLException e) {
        }
        return false;
    }

    protected Map<String, String> handleOnlineVerification(String apiKey, String jwsString) {
        Map<String, String> result = new HashMap<>();
        try {
            HttpTransport httpTransport = new NetHttpTransport();
            JsonFactory jsonFactory = new JacksonFactory();
            VerificationRequest request = new VerificationRequest(jwsString);
            // Request to google device verification API and set a parser for JSON
            HttpRequestFactory requestFactory = httpTransport.createRequestFactory(new HttpRequestInitializer() {
                @Override
                public void initialize(HttpRequest request) {
                    request.setParser(new JsonObjectParser(jsonFactory));
                }
            });
            GenericUrl url = new GenericUrl(GOOGLE_VERIFICATION_URL + apiKey);
            // Post the request with the verification statement to the API.
            HttpRequest httpRequest = requestFactory.buildPostRequest(url, new JsonHttpContent(jsonFactory, request));
            // Parse the returned data as a verification response.
            result.put(RESPONSE_FIELD, httpRequest.execute().parseAsString());
            result.put(STATUS_FIELD, STATUS_SUCCESS);
            /*
             *  {
             *    "isValidSignature": true
             *    }
             * */


        } catch (IOException e) {
            result.put(STATUS_FIELD, STATUS_ERROR);
            result.put(RESPONSE_FIELD, e.getLocalizedMessage());
            Log.e(TAG, "Failure: Network error while connecting to the Google Service " + GOOGLE_VERIFICATION_URL + ".");
        }
        return result;
    }

    @Nullable
    protected Map<String, String> extractAttestationResponsePayload(String jwsResult) {
        Map<String, String> result = new HashMap<>();
        try {
            if (jwsResult == null) {
                result.put(STATUS_FIELD, STATUS_FAILURE);
                result.put(RESPONSE_FIELD, "jwsResult string is null!");
            }
            // the JWT (JSON WEB TOKEN) is just a 3 base64 encoded parts concatenated by a . (dot)
            // character
            final String[] jwtParts = jwsResult.split("\\.");
            if (jwtParts.length == 3) {
                //extract body/payload
                result.put(STATUS_FIELD, STATUS_SUCCESS);
                result.put(RESPONSE_FIELD, new String(Base64.decode(jwtParts[1], Base64.DEFAULT)));
            } else {
                result.put(STATUS_FIELD, STATUS_FAILURE);
                result.put(RESPONSE_FIELD, "Error during cryptographic verification of the JWS signature");
            }
        } catch (Exception e) {
            result.put(STATUS_FIELD, STATUS_ERROR);
            result.put(RESPONSE_FIELD, e.getLocalizedMessage());
        }
        return result;
    }

    private byte[] generateOneTimeRequestNonce() {
        byte[] nonce = new byte[32];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(nonce);
        return nonce;
    }

    @Nullable
    protected Map<String, String> handleOfflineVerification(String jwsString) {
        Map<String, String> result = new HashMap<>();
        JsonWebSignature jws;
        // Parse JSON Web Signature format.
        try {
            try {
                jws = JsonWebSignature.parser(JacksonFactory.getDefaultInstance())
                        .setPayloadClass(AttestationResponseModel.class).parse(jwsString);
            } catch (IOException e) {
                result.put(STATUS_FIELD, STATUS_ERROR);
                result.put(RESPONSE_FIELD, jwsString + " is not valid JWS format");
                return result;
            }
            // Verify the signature of the JWS and retrieve the signature certificate.
            X509Certificate cert;
            try {
                cert = jws.verifySignature();
                if (cert == null) {
                    result.put(STATUS_FIELD, STATUS_FAILURE);
                    result.put(RESPONSE_FIELD, "Signature verification failed.");
                    return result;
                }
            } catch (GeneralSecurityException e) {
                result.put(STATUS_FIELD, STATUS_ERROR);
                result.put(RESPONSE_FIELD, "Error during cryptographic verification of the JWS signature");
                return result;
            }
            // Verify the hostname of the certificate.
            if (!verifyHostname("attest.android.com", cert)) {
                result.put(STATUS_FIELD, STATUS_FAILURE);
                result.put(RESPONSE_FIELD, "Failure: Certificate isn't issued for the hostname attest.android.com.");
                return result;
            }
            result.put(STATUS_FIELD, STATUS_SUCCESS);
            result.put(RESPONSE_FIELD, "Certificate is issued for the hostname attest.android.com.");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "ERROR : " + e.getCause().getLocalizedMessage() + " : " + e.getStackTrace().toString());
            result.put(STATUS_FIELD, STATUS_ERROR);
            result.put(RESPONSE_FIELD, e.getLocalizedMessage());
        }
        return result;
    }

    public String checkGooglePlayServicesAvailability(Activity activity) {
        switch (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(activity)) {
            case ConnectionResult.SUCCESS:
                return STATUS_SUCCESS;
            case ConnectionResult.SERVICE_MISSING:
                return "service_missing";
            case ConnectionResult.SERVICE_UPDATING:
                return "service_updating";
            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                return "service_version_update_required";
            case ConnectionResult.SERVICE_DISABLED:
                return "service_disabled";
            case ConnectionResult.SERVICE_INVALID:
                return "service_invalid";
        }
        return STATUS_ERROR;
    }


    /**
     * Class for parsing JSON data.
     */
    private class VerificationRequest {
        @Key
        public String signedAttestation;

        public VerificationRequest(String signedAttestation) {
            this.signedAttestation = signedAttestation;
        }
    }

    /**
     * Class for parsing JSON data.
     */
    private class VerificationResponse {
        @Key
        public boolean isValidSignature;

        /**
         * Optional field that is only set when the server encountered an error
         * processing the request.
         */
        @Key
        public String error;
    }

}