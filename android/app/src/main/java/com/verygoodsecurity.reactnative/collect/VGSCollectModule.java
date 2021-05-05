package com.verygoodsecurity.reactnative.collect;

import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import android.app.Activity;
import android.util.Log;
import android.content.Intent;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import android.app.Activity;
import com.verygoodsecurity.vgscollect.core.Environment;
import com.verygoodsecurity.vgscollect.core.VGSCollect;
import com.verygoodsecurity.vgscollect.core.HTTPMethod;
import com.verygoodsecurity.reactnative.collect.VGSCollectOnCreateViewInstanceListener;
import com.verygoodsecurity.vgscollect.view.InputFieldView;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.verygoodsecurity.vgscollect.core.VgsCollectResponseListener;
import com.verygoodsecurity.vgscollect.core.model.network.VGSResponse;
import com.verygoodsecurity.vgscollect.core.model.state.FieldState;
import com.verygoodsecurity.vgscollect.core.storage.OnFieldStateChangeListener;
import com.verygoodsecurity.vgscollect.VGSCollectLogger;
import com.verygoodsecurity.reactnative.util.JSONObjectUtil;

public class VGSCollectModule extends ReactContextBaseJavaModule {

    private static final String RESPONSE_EVENT_NAME = "VGSCollectOnVGSResponse";
    private static final String VAULT_ID = "tntpszqgikn";

    private static ReactApplicationContext reactContext;

    private VGSCollect collect;

    VGSCollectModule(ReactApplicationContext c) {
        super(c);
        reactContext = c;
    }

    @Override
    public void initialize() {
        super.initialize();
        VGSCollectLogger.INSTANCE.setLogLevel(VGSCollectLogger.Level.DEBUG);

        Activity activity = reactContext.getCurrentActivity();
        collect = new VGSCollect.Builder(activity, VAULT_ID)
                .setEnvironment(Environment.SANDBOX)
                .create();

        initListeners();
    }

    private void initListeners() {
        collect.addOnResponseListeners(new VgsCollectResponseListener() {
            @Override
            public void onResponse( VGSResponse response) {
                sendResponse(response);
            }
        });
        collect.addOnFieldStateChangeListener(new OnFieldStateChangeListener() {
            @Override
            public void onStateChange(FieldState state) {
                updateUserStates();
            }
        });
    }

    @Override
    public String getName() {
        return "VGSCollect";
    }

    @ReactMethod
    public void submitAsync() {
        collect.asyncSubmit("/post", HTTPMethod.POST);
    }

    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        collect.onActivityResult(requestCode, resultCode, data);
    }

    public void bindView(InputFieldView inputFieldView) {
        Log.e("test", "collect bindView "+inputFieldView.getFieldName());
        collect.bindView(inputFieldView);
    }

    private void sendResponse(VGSResponse response) {
        String responseStr = " ";

        if(response instanceof VGSResponse.SuccessResponse ) {
            parseNumberAlias(response);
            parseDateAlias(response);

            responseStr = "Code: " + ((VGSResponse.SuccessResponse) response).getSuccessCode();
        } else if(response instanceof VGSResponse.ErrorResponse ) {
            responseStr = new StringBuilder("Code: ")
                    .append(response.getCode())
                    .append(" \n ")
                    .append(((VGSResponse.ErrorResponse) response).getLocalizeMessage())
                    .toString();
        }

        this.getReactApplicationContext()
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(RESPONSE_EVENT_NAME, responseStr);

    }

    private void parseNumberAlias(VGSResponse response) {
        String token = JSONObjectUtil.getValue(response.getBody(), "cardNumber");
        this.getReactApplicationContext()
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("cardNumberToken", token);
    }

    private void parseDateAlias(VGSResponse response) {
        String token = JSONObjectUtil.getValue(response.getBody(), "expDate");
        this.getReactApplicationContext()
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("expirationDateToken", token);
    }

    private void updateUserStates() {
        List<FieldState> states = collect.getAllStates();

        for(int i = 0; i < states.size(); i++) {
            FieldState state = states.get(i);

            if(!state.isValid()) {
                String message = new StringBuilder("Field ")
                        .append(state.getFieldName())
                        .append(" is not valid.")
                        .toString();

                this.getReactApplicationContext()
                        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                        .emit(RESPONSE_EVENT_NAME, message);
                break;
            }
        }
    }

}