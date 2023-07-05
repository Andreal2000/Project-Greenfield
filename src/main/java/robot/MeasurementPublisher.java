package robot;

import java.util.List;

import beans.AirPollutionMeasurements;
import com.google.gson.Gson;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

class MeasurementPublisher extends Thread {
    private volatile boolean stopCondition = false;
    private final int id;
    private final int district;
    private final List<Double> averagePollutionList;

    public MeasurementPublisher(int id, int district, List<Double> averagePollutionList) {
        this.averagePollutionList = averagePollutionList;
        this.id = id;
        this.district = district;
    }

    public void stopMeGently() {
        stopCondition = true;
    }

    @Override
    public void run() {
        MqttClient client;
        String broker = "tcp://localhost:1883";
        String clientId = MqttClient.generateClientId();
        String topic = "greenfield/pollution/district" + district;
        int qos = 2;

        try {
            client = new MqttClient(broker, clientId);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);

            // Connect the client
            System.out.println(clientId + " Connecting Broker " + broker);
            client.connect(connOpts);
            System.out.println(clientId + " Connected - Thread PID: " + Thread.currentThread().getId());

            // Callback
            client.setCallback(new MqttCallback() {
                public void messageArrived(String topic, MqttMessage message) {
                    // Not used Here
                }

                public void connectionLost(Throwable cause) {
                    System.out.println(clientId + " Connection lost! cause:" + cause.getMessage());
                }

                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Until the delivery is completed, messages with QoS 1 or 2 are retained from the client
                    // Delivery for a message is completed when all acknowledgments have been received
                    // When the callback returns from deliveryComplete to the main thread, the client removes the retained messages with QoS 1 or 2.
                    if (token.isComplete()) {
                        System.out.println(clientId + " Message delivered - Thread PID: " + Thread.currentThread().getId());
                    }
                }
            });

            while (!stopCondition) {
                Thread.sleep(15000);

                String payload;

                synchronized (averagePollutionList) {
                    payload = new Gson().toJson(new AirPollutionMeasurements(id, district, System.currentTimeMillis(), averagePollutionList));
                    averagePollutionList.clear();
                }

                MqttMessage message = new MqttMessage(payload.getBytes());

                // Set the QoS on the Message
                message.setQos(qos);
                System.out.println(clientId + " Publishing message: " + payload + " ...");
                client.publish(topic, message);
                System.out.println(clientId + " Message published - Thread PID: " + Thread.currentThread().getId());
            }

            if (client.isConnected()) {
                client.disconnect();
            }
            System.out.println("Publisher " + clientId + " disconnected - Thread PID: " + Thread.currentThread().getId());

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (MqttException me) {
            System.out.println("reason " + me.getReasonCode());
            System.out.println("msg " + me.getMessage());
            System.out.println("loc " + me.getLocalizedMessage());
            System.out.println("cause " + me.getCause());
            System.out.println("excep " + me);
            me.printStackTrace();
        }
    }
}
