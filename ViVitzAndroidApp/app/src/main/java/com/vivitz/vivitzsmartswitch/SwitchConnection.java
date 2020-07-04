package com.vivitz.vivitzsmartswitch;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class SwitchConnection {
    private MqttAndroidClient client;
//    private String serverURL = "tcp://134.209.154.158:1883";
        private String serverURL = "tcp://192.168.12.1:1883";
    private String clientId = MqttClient.generateClientId();
    private MqttConnectOptions options;
    private Context context;

    public interface Callback {
        void onMessageReceived(String topic, MqttMessage message);

        void onDeliveryComplete(IMqttDeliveryToken token);

        void onConnectionSuccess(IMqttToken asyncActionToken);

        void onConnectionFailure(IMqttToken asyncActionToken, Throwable exception);

        void onConnectionLost(Throwable cause);

        void onConnectComplete(boolean reconnect, String serverURI);
    }

    SwitchConnection(Context context, final Callback callback) {
        this.context = context;
        try {
            client = new MqttAndroidClient(context, serverURL, clientId);

            client.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    callback.onConnectComplete(reconnect,serverURI);
                    Log.d("MQTTConnection", "Complete");
                }

                @Override
                public void connectionLost(Throwable cause) {
                    callback.onConnectionLost(cause);
                    Log.d("MQTTConnection", "Lost");
                    reconnect();
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    callback.onMessageReceived(topic, message);
                    Log.d("MQTTConnection", "Received");
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    callback.onDeliveryComplete(token);
                    Log.d("MQTTConnection", "Delivered");
                }
            });

//            options = new MqttConnectOptions();
//            options.setAutomaticReconnect(true);
//            options.setCleanSession(true);
//            options.setMqttVersion(3);
            IMqttToken token = client.connect();

            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    callback.onConnectionSuccess(asyncActionToken);
                    Log.d("MQTTConnection", "Success");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    callback.onConnectionFailure(asyncActionToken, exception);
                    Log.d("MQTTConnection", "Failed");
                    reconnect();

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void reconnect() {
//        if (!client.isConnected())
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        Log.d("MQTTConnection", "Reconnecting");
//                        client.registerResources(context);
//                        client.close();
//                        client.disconnectForcibly();
                        client.connect();
                        if (!client.isConnected())
                            reconnect();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        Log.d("MQTTConnection", "Reconnection Failed");
                        reconnect();
                    }
                }
            }, 1000);
    }

    public void publish(String topic, byte[] msg) {
        MqttMessage message = new MqttMessage();
        message.setQos(0);
        message.setRetained(true);
        message.setPayload(msg);
        if (client.isConnected()) {
            try {
                client.publish(topic, message);
                Log.d("MQTTPublish", "Success");
            } catch (MqttException e) {
                e.printStackTrace();
                Log.d("MQTTPublish", "Failed");
            }
        } else {
            Log.d("MQTTPublish", "Not Connected");
        }
    }

    public void subscribe(String topic) {
        if (client.isConnected()) {
            try {
                client.subscribe(topic, 0);
                Log.d("MQTTSubscribe", "Success");
            } catch (MqttException e) {
                e.printStackTrace();
                Log.d("MQTTSubscribe", "Failed");
            }
        } else {
            Log.d("MQTTSubscribe", "Not Connected");
        }
    }
}
