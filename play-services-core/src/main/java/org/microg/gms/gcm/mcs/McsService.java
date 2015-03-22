/*
 * Copyright 2013-2015 µg Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.gms.gcm.mcs;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.squareup.wire.Message;

import org.microg.gms.checkin.LastCheckinInfo;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLContext;

import static android.os.Build.VERSION.SDK_INT;

public class McsService extends IntentService {
    private static final String TAG = "GmsGcmMcsSvc";
    public static final String PREFERENCES_NAME = "mcs";

    public static final String SERVICE_HOST = "mtalk.google.com";
    public static final int SERVICE_PORT = 5228;
    public static final String PREF_LAST_PERSISTENT_ID = "last_persistent_id";
    public static final String SELF_CATEGORY = "com.google.android.gsf.gtalkservice";
    public static final String IDLE_NOTIFICATION = "IdleNotification";
    public static final String FROM_FIELD = "gcm@android.com";
    private static AtomicBoolean connected = new AtomicBoolean(false);

    private Socket socket;
    private Socket sslSocket;
    private McsInputStream inputStream;
    private McsOutputStream outputStream;

    public McsService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (connected.compareAndSet(false, true)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    connect();
                }
            }).start();
        } else {
            Log.d(TAG, "MCS connection already started");
        }
    }

    private void connect() {
        try {
            Log.d(TAG, "Starting MCS connection...");
            LastCheckinInfo info = LastCheckinInfo.read(this);
            socket = new Socket(SERVICE_HOST, SERVICE_PORT);
            Log.d(TAG, "Connected to " + SERVICE_HOST + ":" + SERVICE_PORT);
            sslSocket = SSLContext.getDefault().getSocketFactory().createSocket(socket, "mtalk.google.com", 5228, true);
            Log.d(TAG, "Activated SSL with " + SERVICE_HOST + ":" + SERVICE_PORT);
            inputStream = new McsInputStream(sslSocket.getInputStream());
            outputStream = new McsOutputStream(sslSocket.getOutputStream());
            LoginRequest loginRequest = buildLoginRequest(info);
            Log.d(TAG, "Sending login request...");
            outputStream.write(loginRequest);
            boolean close = false;
            while (!close) {
                Message o = inputStream.read();
                if (o instanceof DataMessageStanza) {
                    handleMessage((DataMessageStanza) o);
                } else if (o instanceof HeartbeatPing) {
                    handleHearbeatPing((HeartbeatPing) o);
                } else if (o instanceof Close) {
                    handleClose((Close) o);
                } else if (o instanceof LoginResponse) {
                    handleLoginresponse((LoginResponse) o);
                }
            }
            socket.close();
        } catch (Exception e) {
            Log.w(TAG, e);
            try {
                sslSocket.close();
            } catch (Exception ignored) {
            }
        }
        connected.set(false);
    }

    private void handleClose(Close close) throws IOException {
        throw new IOException("Server requested close!");
    }

    private void handleLoginresponse(LoginResponse loginResponse) throws IOException {
        getSharedPreferences().edit().putString(PREF_LAST_PERSISTENT_ID, null);
        if (loginResponse.error == null) {
            Log.d(TAG, "Logged in");
        } else {
            throw new IOException("Could not login: " + loginResponse.error);
        }
    }

    private void handleMessage(DataMessageStanza message) throws IOException {
        if (message.persistent_id != null) {
            String old = getSharedPreferences().getString(PREF_LAST_PERSISTENT_ID, null);
            if (old == null) {
                old = "";
            } else {
                old += "|";
            }
            getSharedPreferences().edit()
                    .putString(PREF_LAST_PERSISTENT_ID, old + message.persistent_id).apply();
        }
        if (SELF_CATEGORY.equals(message.category)) {
            handleSelfMessage(message);
        } else {
            handleAppMessage(message);
        }
    }

    private void handleHearbeatPing(HeartbeatPing ping) throws IOException {
        HeartbeatAck.Builder ack = new HeartbeatAck.Builder().status(ping.status);
        if (inputStream.newStreamIdAvailable()) {
            ack.last_stream_id_received(inputStream.getStreamId());
        }
        outputStream.write(ack.build());
    }

    private LoginRequest buildLoginRequest(LastCheckinInfo info) {
        return new LoginRequest.Builder()
                .adaptive_heartbeat(false)
                .auth_service(LoginRequest.AuthService.ANDROID_ID)
                .auth_token(Long.toString(info.securityToken))
                .id("android-" + SDK_INT)
                .domain("mcs.android.com")
                .device_id("android-" + Long.toHexString(info.androidId))
                .network_type(1)
                .resource(Long.toString(info.androidId))
                .user(Long.toString(info.androidId))
                .use_rmq2(true)
                .setting(Arrays.asList(new Setting("new_vc", "1")))
                .received_persistent_id(Arrays.asList(getSharedPreferences().getString(PREF_LAST_PERSISTENT_ID, "").split("\\|")))
                .build();
    }

    private void handleAppMessage(DataMessageStanza msg) {
        Intent intent = new Intent();
        intent.setAction("com.google.android.c2dm.intent.RECEIVE");
        intent.addCategory(msg.category);
        for (AppData appData : msg.app_data) {
            intent.putExtra(appData.key, appData.value);
        }
        sendOrderedBroadcast(intent, msg.category + ".permission.C2D_MESSAGE");
    }

    private void handleSelfMessage(DataMessageStanza msg) throws IOException {
        for (AppData appData : msg.app_data) {
            if (IDLE_NOTIFICATION.equals(appData.key)) {
                DataMessageStanza.Builder msgResponse = new DataMessageStanza.Builder()
                        .from(FROM_FIELD)
                        .sent(System.currentTimeMillis() / 1000)
                        .ttl(0)
                        .category(SELF_CATEGORY)
                        .app_data(Arrays.asList(new AppData(IDLE_NOTIFICATION, "false")));
                if (inputStream.newStreamIdAvailable()) {
                    msgResponse.last_stream_id_received(inputStream.getStreamId());
                }
                outputStream.write(msgResponse.build());
            }
        }
    }

    private SharedPreferences getSharedPreferences() {
        return getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }
}
