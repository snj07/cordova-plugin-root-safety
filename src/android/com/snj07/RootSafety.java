package com.snj07;

import android.app.Activity;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.safetynet.HarmfulAppsData;
import com.google.android.gms.safetynet.SafetyNet;
import com.google.android.gms.safetynet.SafetyNetApi;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RootSafety extends CordovaPlugin {

    private final String TAG = "RootSafety";

    // actions
    private final String CHECK_GOOGLE_PLAY_SERVICES_AVAILABILITY = "checkGooglePlayServicesAvailability";
    private final String ATTEST_ACTION = "attest";
    private final String OFFLINE_VERIFY_ACTION = "offlineVerify";
    private final String ONLINE_VERIFY_ACTION = "onlineVerify";
    private final String EXTRACT_PAYLOAD_ACTION = "extractPayload";
    private final String CHECK_APP_VERIFICATION_ACTION = "checkAppVerification";
    private final String LIST_HARMFUL_ACTION = "listHarmfulApps";
    private final String ENABLE_VERIFY_APPS_ACTION = "enableVerifyApps";

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
    }

    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if (action.equals(CHECK_GOOGLE_PLAY_SERVICES_AVAILABILITY)) {
            checkGooglePlayServicesAvailability(callbackContext);
        }

        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                this.cordova.getActivity().getApplicationContext()) == ConnectionResult.SUCCESS) {
            switch (action) {
                case ATTEST_ACTION: {
                    String nonce = args.getString(0);
                    String key = args.getString(1);
                    this.handleAttestRequest(nonce, key, RootSafety.this.cordova.getActivity(), callbackContext);
                    break;
                }
                case EXTRACT_PAYLOAD_ACTION: {
                    String jwsString = args.getString(0);
                    this.cordova.getThreadPool().execute(new Runnable() {
                        @Override
                        public void run() {
                            Map<String, String> data = new SafetyNetHandler().extractAttestationResponsePayload(jwsString);
                            RootSafety.this.sentCallbackResponse(data, callbackContext);
                        }
                    });
                    break;
                }
                case OFFLINE_VERIFY_ACTION: {
                    String jwsString = args.getString(0);
                    this.cordova.getThreadPool().execute(new Runnable() {
                        @Override
                        public void run() {
                            Map<String, String> data = new SafetyNetHandler().handleOfflineVerification(jwsString);
                            RootSafety.this.sentCallbackResponse(data, callbackContext);
                        }
                    });
                    break;
                }
                case ONLINE_VERIFY_ACTION: {
                    String key = args.getString(0);
                    String jwsString = args.getString(1);
                    this.cordova.getThreadPool().execute(new Runnable() {
                        @Override
                        public void run() {
                            Map<String, String> data = new SafetyNetHandler().handleOnlineVerification(key, jwsString);
                            RootSafety.this.sentCallbackResponse(data, callbackContext);
                        }
                    });
                    break;
                }
                case CHECK_APP_VERIFICATION_ACTION: {
                    SafetyNet.getClient(this.cordova.getActivity()).isVerifyAppsEnabled()
                            .addOnCompleteListener(new OnCompleteListener<SafetyNetApi.VerifyAppsUserResponse>() {
                                @Override
                                public void onComplete(Task<SafetyNetApi.VerifyAppsUserResponse> task) {
                                    if (task.isSuccessful()) {
                                        SafetyNetApi.VerifyAppsUserResponse result = task.getResult();
                                        if (result.isVerifyAppsEnabled()) {
                                            callbackContext.success(createJsonReponse(SafetyNetHandler.STATUS_SUCCESS, "enabled"));
                                        } else {
                                            callbackContext.success(createJsonReponse(SafetyNetHandler.STATUS_FAILURE, "disabled"));
                                        }
                                    } else {
                                        callbackContext.error(createJsonReponse(SafetyNetHandler.STATUS_ERROR, "Error occurred while checking status."));
                                    }
                                }
                            });
                    break;
                }
                case LIST_HARMFUL_ACTION: {
                    SafetyNet.getClient(this.cordova.getActivity()).listHarmfulApps()
                            .addOnCompleteListener(new OnCompleteListener<SafetyNetApi.HarmfulAppsResponse>() {
                                @Override
                                public void onComplete(Task<SafetyNetApi.HarmfulAppsResponse> task) {
                                    if (task.isSuccessful()) {
                                        SafetyNetApi.HarmfulAppsResponse result = task.getResult();
                                        List<HarmfulAppsData> appList = result.getHarmfulAppsList();
                                        Log.d(TAG, "Harmful appList size: " + appList.size());
                                        JSONArray appsJson = new JSONArray(appList);
                                        callbackContext.success(appsJson);
                                    } else {
                                        callbackContext.error(createJsonReponse(SafetyNetHandler.STATUS_ERROR, "Error occurred while finding harmful Apps"));
                                    }
                                }
                            });
                    break;

                }
                case ENABLE_VERIFY_APPS_ACTION: {
                    SafetyNet.getClient(this.cordova.getActivity()).enableVerifyApps()
                            .addOnCompleteListener(new OnCompleteListener<SafetyNetApi.VerifyAppsUserResponse>() {
                                @Override
                                public void onComplete(Task<SafetyNetApi.VerifyAppsUserResponse> task) {
                                    if (task.isSuccessful()) {
                                        SafetyNetApi.VerifyAppsUserResponse result = task.getResult();
                                        if (result.isVerifyAppsEnabled()) {
                                            callbackContext.success(createJsonReponse(SafetyNetHandler.STATUS_SUCCESS, "enabled"));
                                        } else {
                                            callbackContext.success(createJsonReponse(SafetyNetHandler.STATUS_FAILURE, "disabled"));
                                        }
                                    } else {
                                        callbackContext.error(createJsonReponse(SafetyNetHandler.STATUS_ERROR, "error in checking status"));
                                    }
                                }
                            });
                }
            }

        } else {
            // play service not supported
            callbackContext.error("Play Services not supported");
        }
        return true;
    }

    private void sentCallbackResponse(Map<String, String> data, CallbackContext callbackContext) {
        Log.e(TAG, Arrays.toString(data.entrySet().toArray()));
        if (data.containsKey(SafetyNetHandler.STATUS_ERROR)) {
            callbackContext.error(new JSONObject(data));
        } else {
            callbackContext.success(new JSONObject(data));
        }
    }

    private void handleAttestRequest(String nonce, String key, Activity activity, CallbackContext callbackContext) {
        try {
            SafetyNet.getClient(activity).attest(nonce.getBytes(), key)
                    .addOnSuccessListener(activity,
                            new OnSuccessListener<SafetyNetApi.AttestationResponse>() {
                                @Override
                                public void onSuccess(SafetyNetApi.AttestationResponse response) {
                                    callbackContext.success(createJsonReponse(SafetyNetHandler.STATUS_SUCCESS, response.getJwsResult()));
                                }
                            })
                    .addOnFailureListener(activity, new OnFailureListener() {
                        @Override
                        public void onFailure(Exception et) {
                            callbackContext.success(createJsonReponse(SafetyNetHandler.STATUS_FAILURE, et.getLocalizedMessage()));
                        }
                    });
        } catch (Exception e) {
            callbackContext.error(createJsonReponse(SafetyNetHandler.STATUS_FAILURE, e.getLocalizedMessage()));
        }
    }

    private JSONObject createJsonReponse(String status, String reponse) {
        Map<String, String> data = new HashMap<>();
        data.put(SafetyNetHandler.STATUS_FIELD, status);
        data.put(SafetyNetHandler.RESPONSE_FIELD, reponse);
        return new JSONObject(data);
    }

    private void checkGooglePlayServicesAvailability(CallbackContext callbackContext) {
        String response = new SafetyNetHandler().checkGooglePlayServicesAvailability(this.cordova.getActivity());
        if (response.equals(SafetyNetHandler.STATUS_ERROR)) {
            callbackContext.error(SafetyNetHandler.STATUS_ERROR);
        }
        callbackContext.success(response);
    }
}