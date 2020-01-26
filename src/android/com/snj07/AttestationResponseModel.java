package com.snj07;

import com.google.api.client.json.webtoken.JsonWebSignature;
import com.google.api.client.util.Key;

import java.util.Arrays;


/**
 * Unencoded from JSON Web token SafetyNet API payload Response
 * {
 * "nonce": "pCaf1AI4KCA6plS7yDxzUVJYxBYUIQG396Wuio23A/Z=",
 * "timestampMs": 1444648019200,
 * "apkPackageName": "com.snj07.demoapp",
 * "apkDigestSha256": "AL2BLq4LVmDsp0FBCLkGR19cn5mKLIppCmnqsrJzUJg=",
 * "ctsProfileMatch": false,
 * "basicIntegrity": false,
 * "extension": "CY+oATrcJ6Cr",
 * "apkCertificateDigestSha256": ["Yao6w7Yy7/ab2bNEygMbXqN9+16j8mLKKTCsUcU3Mzw="]
 * "advice": "LOCK_BOOTLOADER,RESTORE_TO_FACTORY_ROM"
 * }
 */

public class AttestationResponseModel extends JsonWebSignature.Payload {

    /**
     * Embedded nonce sent as part of the request.
     */
    @Key
    private String nonce;

    /**
     * Timestamp of the request.
     */
    @Key
    private long timestampMs;

    /**
     * Package name of the APK that submitted this request.
     */
    @Key
    private String apkPackageName;

    /**
     * Digest of certificate of the APK that submitted this request.
     */
    @Key
    private String[] apkCertificateDigestSha256;

    /**
     * Digest of the APK that submitted this request.
     */
    @Key
    private String apkDigestSha256;
    /**
     * The device passed CTS and matches a known profile.
     * If the value of ctsProfileMatch is true, then the profile of the device running your
     * app matches the profile of a device that has passed Android compatibility testing
     */
    @Key
    private boolean ctsProfileMatch;

    /**
     * The device has passed a basic integrity test, but the CTS profile could not be verified.
     * A more lenient verdict of device integrity. If only the value of basicIntegrity is true,
     * then the device running your app likely wasn't tampered with
     */
    @Key
    private boolean basicIntegrity;

    /**
     * advice parameter provides information to help explain why the SafetyNet Attestation API set
     * either ctsProfileMatch or basicIntegrity to false in a particular result.
     */
    @Key
    private String advice;

    /**
     * Constructor
     */
    public AttestationResponseModel() {
    }

    public String getNonce() {
        return nonce;
    }

    public long getTimestampMs() {
        return timestampMs;
    }

    public String getApkPackageName() {
        return apkPackageName;
    }

    public String[] getApkCertificateDigestSha256() {
        return apkCertificateDigestSha256;
    }

    @Deprecated
    public String getApkDigestSha256() {
        return apkDigestSha256;
    }

    public boolean isCtsProfileMatch() {
        return ctsProfileMatch;
    }

    public boolean isBasicIntegrity() {
        return basicIntegrity;
    }

    public String getAdvice() {
        return advice;
    }


    @Override
    public String toString() {
        return "{" +
                "nonce='" + nonce + '\'' +
                ", timestampMs=" + timestampMs +
                ", apkPackageName='" + apkPackageName + '\'' +

                ", apkCertificateDigestSha256=" + (apkCertificateDigestSha256 == null ? null : Arrays.toString(apkCertificateDigestSha256)) +
                ", apkDigestSha256='" + apkDigestSha256 + '\'' +
                ", ctsProfileMatch=" + ctsProfileMatch +
                ", basicIntegrity=" + basicIntegrity +
                ", advice='" + advice + '\'' +
                '}';
    }

}