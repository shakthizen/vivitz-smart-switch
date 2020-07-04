package com.vivitz.vivitzsmartswitch;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.eclipse.paho.client.mqttv3.*;

public class MainActivity extends AppCompatActivity {
    private boolean switchState = false;
    private ImageView ivSwitch;
    private TextView tvLoading;
    private ConstraintLayout clLoadingCircle;


    private final String TOPIC = "/vivitz/status";

    SwitchConnection connection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ivSwitch = findViewById(R.id.ivSwitch);
        tvLoading = findViewById(R.id.tvLoading);
        clLoadingCircle = findViewById(R.id.loading_circle);
        setLoadingState(true);

        connection = new SwitchConnection(getApplication().getApplicationContext(), new SwitchConnection.Callback() {
            @Override
            public void onMessageReceived(String topic, MqttMessage message) {
                if (topic.equals(TOPIC)) {
                    boolean newState = message.toString().equals("1");
                    setSwitchState(newState);
                }
            }

            @Override
            public void onDeliveryComplete(IMqttDeliveryToken token) {

            }

            @Override
            public void onConnectionSuccess(IMqttToken asyncActionToken) {
                setLoadingState(false);
                connection.subscribe(TOPIC);
            }

            @Override
            public void onConnectionFailure(IMqttToken asyncActionToken, Throwable exception) {
                setLoadingState(true);
            }

            @Override
            public void onConnectionLost(Throwable cause) {
                setLoadingState(true);
                tvLoading.setText("Please Reconnect & Restart App");
            }

            @Override
            public void onConnectComplete(boolean reconnect, String serverURI) {
                setLoadingState(false);
//                connection.subscribe(TOPIC);
            }
        });

        ivSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean newState = !switchState;
                connection.publish(TOPIC, newState ? "1".getBytes() : "0".getBytes());
                setSwitchState(newState);
            }
        });

    }

    private void vibrate() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            //deprecated in API 26
            v.vibrate(40);
        }
    }

    private void setSwitchState(boolean state) {
        switchState = state;
        if (state) {
            ivSwitch.setImageResource(R.drawable.btn_on);
        } else {
            ivSwitch.setImageResource(R.drawable.btn_off);
        }
        vibrate();
    }

    private void setLoadingState(boolean visible) {
        if (visible) {
            clLoadingCircle.setVisibility(View.VISIBLE);
            clLoadingCircle.setClickable(true);
        } else {
            clLoadingCircle.setVisibility(View.GONE);
            clLoadingCircle.setClickable(false);
        }
    }

}
