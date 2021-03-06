package io.dcloud.feature.unipush;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.igexin.sdk.PushManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.dcloud.common.DHInterface.IApp;
import io.dcloud.common.DHInterface.ISysEventListener;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.util.AndroidResources;
import io.dcloud.feature.aps.APSFeatureImpl;
import io.dcloud.feature.aps.AbsPushService;
import io.dcloud.feature.aps.PushMessage;

/**
 * unipush模块 个推多厂商推送
 */
public class GTPushService extends AbsPushService implements ISysEventListener{

	public static final String ID = "unipush";
	IApp mApp;

	@Override
	public void onStart(Context pContext, Bundle pSaveBundle,String[] pArgs) {
		id=ID;
		PushManager.getInstance().initialize(pContext.getApplicationContext(),null);
        // GTNormalIntentService 为第三方自定义的推送服务事件接收类
        PushManager.getInstance().registerPushIntentService(pContext.getApplicationContext(),GTNormalIntentService.class);
        SharedPreferences _sp = pContext.getSharedPreferences(AbsPushService.CLIENTID + ID, Context.MODE_PRIVATE);
		clientid = _sp.getString(AbsPushService.PUSH_CLIENT_ID_NAME, clientid);
		appid = AndroidResources.getMetaValue("PUSH_APPID");
		appkey = AndroidResources.getMetaValue("PUSH_APPKEY");
		appsecret = AndroidResources.getMetaValue("PUSH_APPSECRET");
	}

	@Override
	public String getClientInfo(Context context) {
		if(clientid == null){
			clientid = PushManager.getInstance().getClientid(context);
            saveClientId(context);
		}
		return super.getClientInfo(context);
	}

	@Override
	public void onStop() {
		super.onStop();
		if(isRegisterNewIntent) {
			isRegisterNewIntent =false;
			mApp.unregisterSysEventListener(this, ISysEventListener.SysEventType.onNewIntent);
			mApp = null;
		}
	}

	@Override
	public void addEventListener(Context context, IWebview pWebViewImpl, JSONArray pJsArgs) throws JSONException {
		super.addEventListener(context, pWebViewImpl, pJsArgs);
		Intent intent = pWebViewImpl.getActivity().getIntent();
		registerOnNewIntent(pWebViewImpl.obtainApp());
		// 处理离线厂商push启动参数触发click
		fireClickEvent(intent);

	}
	boolean isRegisterNewIntent = false;
	private void registerOnNewIntent(IApp app) {
		if(!isRegisterNewIntent) {
			mApp = app;
			isRegisterNewIntent = true;
			app.registerSysEventListener(this, ISysEventListener.SysEventType.onNewIntent);
		}
	}

	@Override
	public boolean onExecute(SysEventType pEventType, Object pArgs) {
		if(pEventType.equals(ISysEventListener.SysEventType.onNewIntent) && mApp != null) {
			Intent intent = mApp.getActivity().getIntent();
			fireClickEvent(intent);
		}
		return false;
	}

	private void fireClickEvent(Intent intent) {
		if(intent.hasExtra("UP-OL-SU") && mApp != null) {
			JSONObject params = new JSONObject();
			try {
				params.put("title", intent.getStringExtra("title"));
				params.put("content", intent.getStringExtra("content"));
				params.put("payload", intent.getStringExtra("payload"));
				intent.removeExtra("UP-OL-SU");
				intent.removeExtra("title");
				intent.removeExtra("content");
				intent.removeExtra("payload");
				PushMessage _pushMessage = new PushMessage(params.toString(), mApp.obtainAppId(), "");
				if(!APSFeatureImpl.execScript(mApp.getActivity(),"click",_pushMessage.toJSON())){
					APSFeatureImpl.addNeedExecMessage(mApp.getActivity(), _pushMessage);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}
}
