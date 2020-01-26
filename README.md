# Cordova RootSafety plugin for root detection and certificate verification using Google SafetyNet API

It's a cordova plugin to assess android device using SafetyNet APIs. It won't work for iOS devices as SafetyNet APIs are not available for iOS devices. You can read more about SafetyNet [here](https://developer.android.com/training/safetynet)
> Android developers can also use code snippets from this plugin in their project.
## Features
- Google Play Service availability check
- Attestation 
- Offline verification of the SafetyNet attestation response
- Online verification of the SafetyNet attestation response
- Decryption of payload from attestation signature 
- List harmful apps
- Check/enable app verification service state

## Getting Started

For using this plugin you need an API key for SafetyNet attestation. You can use [these](https://developer.android.com/training/safetynet/attestation.html#obtain-api-key) steps to generate the API key.

### Check the Google Play services
Google Play services is required for SafetyNet APIs to work. 
```typescript
declare var rootSafety: any;
```
```typescript
rootSafety.checkGooglePlayServicesAvailability((state)=>{
	if(state == "success"){
		// other stuff
	}
}, (error)=>{
})
```

### Attestation 
You need a nonce and an API key while calling the SafetyNet Attestation API. The resulting attestation contains this nonce, allowing you to determine that the attestation belongs to your API call and isn't replayed by an attacker.
Attest API returns JWS token which is just a 3 base64 encoded parts concatenated by a . (dot) character.
```typescript
rootSafety.attest(nonce, apikey,successCallback,errorCallback)
```

### Offline Verification 
You can use this service to verify if SafetyNet attestation response actually came from the SafetyNet service and includes data matching your request. It verifies hostname of the attestation response.
```typescript
rootSafety.offlineVerification(jwsResponse,successCallback,errorCallback)
```
This service returns a JSON with `status` parameter with value `success` if it successfully verifies the attestation signature certificate.

### Online Verification 
>You can verify attestation signature by sending the entire JWS response to your own server, using a secure connection, for verification. It's not recommend that you perform the verification directly in your app.

This plugin also includes a service for verification of the JWS response which sends JWS token through a POST request to the following API, `https://www.googleapis.com/androidcheck/v1/attestations/verify?key=ATTESTATION_API_KEY` 

```typescript
rootSafety.onlineVerification(apiKey,jwsResponse,successCallback,errorCallback)
```
The above API return a JSON with `isValidSignature` parameter with value `true` if it successfully validates the signature.

### Decryption of payload from attestation signature 
It extracts the payload JSON string from the attestation signature.
```typescript
rootSafety.extractPayload(jwsResponse,successCallback,errorCallback)
```
The payload response JSON contains following parameter:
```json
{  
	"timestampMs": 9860437986543,  
	"nonce": "R2Rra24fVm5xa2Mg",  
	"apkPackageName": "com.package.name.of.requesting.app", 
	"apkCertificateDigestSha256": ["base64 encoded, SHA-256 hash of the certificate used to sign requesting app"],  
	"ctsProfileMatch": true,  
	"basicIntegrity": true,
}
```
**nonce** -  nonce sent as part of the request.<br/>
**timestampMs** - timestamp of the request.<br/>
**apkPackageName** - package name of the APK that submitted this request.<br/>
**apkCertificateDigestSha256** - base-64 encoded representation of the SHA-256 hash of the calling app's signing certificate.<br/>
**ctsProfileMatch** -  if the value of ctsProfileMatch is true, then the profile of the device running your app matches the profile of a device that has passed android compatibility testing.<br/>
**basicIntegrity** - true if the device has passed a basic integrity test, but the CTS profile could not be verified. A more lenient verdict of device integrity. If only the value of basicIntegrity is true, then the device running your app likely wasn't tampered with.<br/>
**advice** -  advice parameter provides information to help explain why the SafetyNet Attestation API set either ctsProfileMatch or basicIntegrity to false in a particular result.<br/>
