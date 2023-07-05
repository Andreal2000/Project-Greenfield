package administrator.server;

import beans.AddRobotResponse;
import beans.AirPollutionMeasurements;
import beans.Robot;
import com.google.gson.Gson;
import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.net.httpserver.HttpServer;
import org.eclipse.paho.client.mqttv3.*;
import robot.AbstractRobot;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class Server {
    private final String HOST = "localhost";
    private final int PORT = 1337;
    private final List<Robot> robots;
    private final Map<Integer, List<AirPollutionMeasurements>> measurementsMap;
    private HttpServer httpServer;
    private MqttClient mqttClient;
    private static Server instance;

    private Server() {
        robots = new ArrayList<>();
        measurementsMap = new HashMap<>();
        startRest();
        startMqtt();
    }

    //singleton
    public synchronized static Server getInstance() {
        if (instance == null) {
            instance = new Server();
        }
        return instance;
    }

    public void shutdown() {
        System.out.println("Shutdown server");
        stopMqtt();
        stopRest();
    }

    public AddRobotResponse addRobot(Robot robot) {
        synchronized (robots) {
            if (robots.stream().anyMatch(r -> r.getId() == robot.getId())) {
                return null;
            }

            int district = Stream.concat(IntStream.rangeClosed(1, 4).boxed(), robots.stream().map(AbstractRobot::getDistrict))
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                    .entrySet().stream()
                    .min(Comparator.comparingLong(Map.Entry::getValue))
                    .map(Map.Entry::getKey)
                    .orElse(new Random().nextInt(4) + 1);

            int x = new Random().nextInt(5) + (district == 2 || district == 3 ? 5 : 0);
            int y = new Random().nextInt(5) + (district == 4 || district == 3 ? 5 : 0);

            AddRobotResponse response = new AddRobotResponse(district, x, y, new ArrayList<>(robots));

            robot.setDistrict(district);
            robot.setX(x);
            robot.setY(y);
            robots.add(robot);
            return response;
        }
    }

    public boolean removeRobot(int id) {
        synchronized (robots) {
            Robot robot = robots.stream().filter(r -> r.getId() == id).findAny().orElse(null);
            return robots.remove(robot);
        }
    }

    public List<Robot> getRobot() {
        return robots;
    }

    private void startRest() {
        try {
            httpServer = HttpServerFactory.create("http://" + HOST + ":" + PORT + "/");
            httpServer.start();
            System.out.println("Rest server running!");
            System.out.println("Rest server started on: http://" + HOST + ":" + PORT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void stopRest() {
        System.out.println("Stopping Rest server");
        httpServer.stop(0);
        System.out.println("Rest server stopped");
    }

    private void startMqtt() {
        String broker = "tcp://localhost:1883";
        String clientId = MqttClient.generateClientId();
        String topic = "greenfield/pollution/district";
        int qos = 2;

        try {
            mqttClient = new MqttClient(broker, clientId);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);

            // Connect the client
            System.out.println(clientId + " Connecting Broker " + broker);
            mqttClient.connect(connOpts);
            System.out.println(clientId + " Connected - Thread PID: " + Thread.currentThread().getId());

            // Callback
            mqttClient.setCallback(new MqttCallback() {

                public void messageArrived(String topic, MqttMessage message) {
                    // Called when a message arrives from the server that matches any subscription made by the client
                    String time = String.valueOf(System.currentTimeMillis());
                    AirPollutionMeasurements receivedMessage = new Gson().fromJson(new String(message.getPayload()), AirPollutionMeasurements.class);
                    System.out.println(clientId + " Received a Message! - Callback - Thread PID: " + Thread.currentThread().getId() +
                            "\n\tTime:    " + time +
                            "\n\tTopic:   " + topic +
                            "\n\tMessage: " + receivedMessage +
                            "\n\tQoS:     " + message.getQos() + "\n");

                    synchronized (measurementsMap) {
                        List<AirPollutionMeasurements> measurementsList = measurementsMap.get(receivedMessage.id);
                        if (measurementsList == null) {
                            measurementsMap.put(receivedMessage.id, new ArrayList<>(Collections.singletonList(receivedMessage)));
                        } else {
                            measurementsList.add(receivedMessage);
                        }
                    }
//                    System.out.println(measurementsMap);
                }

                public void connectionLost(Throwable cause) {
                    System.out.println(clientId + " Connection lost! cause:" + cause.getMessage() + "-  Thread PID: " + Thread.currentThread().getId());
                }

                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Not used here
                }

            });

            System.out.println(clientId + " Subscribing ... - Thread PID: " + Thread.currentThread().getId());
            for (int i = 1; i <= 4; i++) {
                mqttClient.subscribe(topic + i, qos);
                System.out.println(clientId + " Subscribed to topics : " + topic + i);
            }

        } catch (MqttException me) {
            System.out.println("reason " + me.getReasonCode());
            System.out.println("msg " + me.getMessage());
            System.out.println("loc " + me.getLocalizedMessage());
            System.out.println("cause " + me.getCause());
            System.out.println("excep " + me);
            me.printStackTrace();
        }
    }

    private void stopMqtt() {
        try {
            if (mqttClient.isConnected()) {
                mqttClient.disconnect();
            }
            System.out.println("Subscriber disconnected");
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public Double getAverageLastNPollution(int n, int robot) {
        synchronized (measurementsMap) {
            List<AirPollutionMeasurements> listMeasurements = measurementsMap.get(robot);
            listMeasurements = listMeasurements.subList(listMeasurements.size() - n, listMeasurements.size());
            return listMeasurements.stream().collect(Collectors.averagingDouble(a -> a.getAveragePollutionList().stream().mapToDouble(b -> b).average().getAsDouble()));
        }
    }

    public Double getAverageRangePollution(long t1, long t2) {
        synchronized (measurementsMap) {
            return measurementsMap.values().stream()
                    .flatMap(List::stream)
                    .filter(a -> a.getTimestamp() >= t1 && a.getTimestamp() <= t2)
                    .mapToDouble(a -> a.getAveragePollutionList().stream().mapToDouble(b -> b).average().getAsDouble())
                    .average().getAsDouble();
        }
    }
}
