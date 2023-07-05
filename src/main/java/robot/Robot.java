package robot;

import beans.AddRobotResponse;
import com.example.grpc.GRPCServiceGrpc;
import com.example.grpc.GRPCServiceOuterClass;
import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import robot.simulator.PM10Simulator;
import robot.simulator.SlidingWindowBuffer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

class Robot extends AbstractRobot {

    private final String adminAddress;
    private boolean broken;
    private boolean mechanic;
    private volatile long timestamp;
    private final List<beans.Robot> friends;
    private PM10Simulator pm10Simulator;
    private SlidingWindowBuffer slidingWindowBuffer;
    private SlidingWindowConsumer slidingWindowConsumer;
    private HeartbeatThread heartbeatThread;
    private List<Double> averagePollutionList;
    private MeasurementPublisher measurementPublisher;
    private io.grpc.Server GRPCServer;
    public MechanicThread mechanicThread;

    public Robot(int id, int listenPort, String adminAddress) {
        this.id = id;
        this.listenPort = listenPort;
        this.adminAddress = adminAddress;
        this.broken = false;
        this.mechanic = false;
        this.friends = new ArrayList<>();
        initialization();
    }

    public List<beans.Robot> getFriends() {
        synchronized (friends) {
            return new ArrayList<>(friends);
        }
    }

    private final Object brokenLock = new Object();

    public boolean isBroken() {
        synchronized (brokenLock) {
            return broken;
        }
    }

    public void setBroken(boolean broken) {
        synchronized (brokenLock) {
            this.broken = broken;
        }
    }

    private final Object MechanicLock = new Object();

    public boolean isMechanic() {
        synchronized (MechanicLock) {
            return mechanic;
        }
    }

    public void setMechanic(boolean mechanic) {
        synchronized (MechanicLock) {
            this.mechanic = mechanic;
        }
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    private void initialization() {
        startGRPC();
        serverAdd();
        synchronized (friends) {
            for (beans.Robot r : friends) {
                addRobot(r);
            }
        }
        startSimulator();
        startHeartbeat();
        startMechanic();
    }

    private void startSimulator() {
        slidingWindowBuffer = new SlidingWindowBuffer();
        averagePollutionList = new ArrayList<>();

        pm10Simulator = new PM10Simulator(slidingWindowBuffer);
        slidingWindowConsumer = new SlidingWindowConsumer(slidingWindowBuffer, averagePollutionList);
        measurementPublisher = new MeasurementPublisher(id, district, averagePollutionList);

        pm10Simulator.start();
        slidingWindowConsumer.start();
        measurementPublisher.start();
        System.out.println("Simulator started!");
    }

    private void stopSimulator() {
        pm10Simulator.stopMeGently();
        slidingWindowConsumer.stopMeGently();
        measurementPublisher.stopMeGently();
        System.out.println("Simulator stopped!");
    }

    private void startGRPC() {
        try {
            GRPCServer = ServerBuilder.forPort(listenPort).addService(new GRPCServiceImpl(this)).build();
            GRPCServer.start();
            System.out.println("GRPC server started!");
//            GRPCServer.awaitTermination();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopGRPC() {
        GRPCServer.shutdown();
//        try {
//            GRPCServer.awaitTermination();
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
        System.out.println("GRPC server stopped!");
    }

    public void addFriend(beans.Robot robot) {
        synchronized (friends) {
            if (friends.stream().filter(r -> r.getId() == robot.getId()).findAny().orElse(null) == null) {
                friends.add(robot);
            }
        }
    }

    public void removeFriend(beans.Robot robot) {

        synchronized (friends) {
            beans.Robot remove = friends.stream().filter(r -> r.getId() == robot.getId()).findAny().orElse(null);
            friends.remove(remove);
        }
    }

    private void addRobot(beans.Robot robot) {
        //opening a connection with server
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(robot.robotAddress + ":" + robot.listenPort).usePlaintext().build();

        //creating the asynchronous stub
        GRPCServiceGrpc.GRPCServiceStub stub = GRPCServiceGrpc.newStub(channel);

        GRPCServiceOuterClass.RobotRequest robotRequest = GRPCServiceOuterClass.RobotRequest.newBuilder().setId(this.getId())
                .setListenPort(this.getListenPort())
                .setRobotAddress(this.getRobotAddress())
                .setDistrict(this.getDistrict())
                .setX(this.getX())
                .setY(this.getY())
                .build();

        System.out.println("Sending GRPC [addRobot] to robot " + robot.getId());
        stub.addRobot(robotRequest, new StreamObserver<GRPCServiceOuterClass.Response>() {
            //remember: all the methods here are CALLBACKS which are handled in an asynchronous manner.

            //we define what to do when a message from the server arrives (just print the message)
            @Override
            public void onNext(GRPCServiceOuterClass.Response value) {
                System.out.println(value.getStringResponse());
            }

            public void onError(Throwable throwable) {
                System.out.println(throwable.getMessage());
            }

            public void onCompleted() {
                System.out.println("Closed connection GRPC [addRobot] to robot " + robot.getId());
                channel.shutdown();
            }
        });

        try {
            //you need this. otherwise the method will terminate before that answers from the server are received
            channel.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void removeRobot(beans.Robot fromRobot, beans.Robot toRobot) {
        //opening a connection with server
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(toRobot.robotAddress + ":" + toRobot.listenPort).usePlaintext().build();

        //creating the asynchronous stub
        GRPCServiceGrpc.GRPCServiceStub stub = GRPCServiceGrpc.newStub(channel);

        GRPCServiceOuterClass.RobotRequest robotRequest = GRPCServiceOuterClass.RobotRequest.newBuilder().setId(fromRobot.getId())
                .setListenPort(fromRobot.getListenPort())
                .setRobotAddress(fromRobot.getRobotAddress())
                .setDistrict(fromRobot.getDistrict())
                .setX(fromRobot.getX())
                .setY(fromRobot.getY())
                .build();

        System.out.println("Sending GRPC [removeRobot] to robot " + toRobot.getId());
        stub.removeRobot(robotRequest, new StreamObserver<GRPCServiceOuterClass.Response>() {
            //remember: all the methods here are CALLBACKS which are handled in an asynchronous manner.

            //we define what to do when a message from the server arrives (just print the message)
            @Override
            public void onNext(GRPCServiceOuterClass.Response value) {
                System.out.println(value.getStringResponse());
            }

            public void onError(Throwable throwable) {
                System.out.println(throwable.getMessage());
            }

            public void onCompleted() {
                System.out.println("Closed connection GRPC [removeRobot] to robot " + toRobot.getId());
                channel.shutdown();
            }
        });

        try {
            //you need this. otherwise the method will terminate before that answers from the server are received
            channel.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void serverAdd() {
        Client client = Client.create();
        String postPath = "/server/add";
        WebResource webResource = client.resource(adminAddress + postPath);
        String input = new Gson().toJson(new beans.Robot(this));

        try {
            ClientResponse clientResponse = webResource.type("application/json").post(ClientResponse.class, input);
            System.out.println(clientResponse);

            if (clientResponse.getStatus() == 200) {
                AddRobotResponse robotResponse = clientResponse.getEntity(AddRobotResponse.class);
                System.out.println(robotResponse.toString());
                setDistrict(robotResponse.getDistrict());
                setX(robotResponse.getX());
                setY(robotResponse.getY());
                if (robotResponse.getRobots() != null) {
                    synchronized (friends) {
                        friends.addAll(robotResponse.getRobots());
                    }
                }
            } else {
                System.out.println(clientResponse);
                System.exit(1);
            }
        } catch (ClientHandlerException e) {
            System.out.println("Server non disponibile");
            System.exit(1);
        }
    }

    private void serverRemove(int id) {
        Client client = Client.create();
        String postPath = "/server/remove";
        WebResource webResource = client.resource(this.adminAddress + postPath);
        String input = new Gson().toJson(id);

        try {
            ClientResponse clientResponse = webResource.type("application/json").post(ClientResponse.class, input);
            System.out.println(clientResponse);

            if (clientResponse.getStatus() == 200) {
                System.out.println("Robot " + id + " removed from the server");
            } else {
                System.out.println("Errore rimozione robot");
            }
        } catch (ClientHandlerException e) {
            System.out.println("Server non disponibile");
            System.exit(1);
        }
    }

    public void leaveCity() {
        stopMechanic();

        synchronized (friends) {
            for (beans.Robot r : friends) {
                removeRobot(new beans.Robot(this), r);
            }
        }
        serverRemove(this.getId());

        stopSimulator();
        stopHeartbeat();
        stopGRPC();
        System.exit(0);
    }

    private void startHeartbeat() {
        heartbeatThread = new HeartbeatThread(this);
        heartbeatThread.start();
        System.out.println("Heartbeat started!");
    }

    private void stopHeartbeat() {
        heartbeatThread.stopMeGently();
        try {
            heartbeatThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Heartbeat stopped!");
    }

    private void heartbeat(beans.Robot robot) {
        //opening a connection with server
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(robot.robotAddress + ":" + robot.listenPort).usePlaintext().build();

        //creating the asynchronous stub
        GRPCServiceGrpc.GRPCServiceStub stub = GRPCServiceGrpc.newStub(channel);

//        System.out.println("Sending GRPC [heartbeat] to robot " + robot.getId());
        stub.heartbeat(GRPCServiceOuterClass.message.newBuilder().build(), new StreamObserver<GRPCServiceOuterClass.message>() {
            @Override
            public void onNext(GRPCServiceOuterClass.message value) {

            }

            public void onError(Throwable throwable) {
                System.out.println("No heartbeat from robot " + robot.getId());
                removeFriend(robot);
                serverRemove(robot.getId());
                synchronized (friends) {
                    for (beans.Robot r : friends) {
                        removeRobot(robot, r);
                    }
                }
                if (isBroken()) {
                    mechanicThread.setMapValue(robot, true);
                    mechanicThread.removeWaitList(robot);
                    mechanicThread.Notify();
                }
            }

            public void onCompleted() {
//                System.out.println("Closed connection GRPC [heartbeat] to robot " + robot.getId());
                channel.shutdown();
            }
        });

        try {
            //you need this. otherwise the method will terminate before that answers from the server are received
            channel.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void heartbeatAll() {
        synchronized (friends) {
            for (beans.Robot r : friends) {
                heartbeat(r);
            }
        }
    }

    private void startMechanic() {
        mechanicThread = new MechanicThread(this);
        mechanicThread.start();
        System.out.println("Mechanic started!");
    }

    private void stopMechanic() {
        mechanicThread.stopMeGently();
        try {
            mechanicThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Mechanic stopped!");
    }

    public void wantMechanic(beans.Robot robot) {
        //opening a connection with server
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(robot.robotAddress + ":" + robot.listenPort).usePlaintext().build();

        //creating the asynchronous stub
        GRPCServiceGrpc.GRPCServiceStub stub = GRPCServiceGrpc.newStub(channel);

        GRPCServiceOuterClass.RobotRequest robotRequest = GRPCServiceOuterClass.RobotRequest.newBuilder().setId(this.getId())
                .setListenPort(this.getListenPort())
                .setRobotAddress(this.getRobotAddress())
                .setDistrict(this.getDistrict())
                .setX(this.getX())
                .setY(this.getY())
                .build();

        GRPCServiceOuterClass.BrokenRequest brokenRequest = GRPCServiceOuterClass.BrokenRequest.newBuilder().setRobot(robotRequest).setTimestamp(getTimestamp()).build();

        stub.mechanic(brokenRequest, new StreamObserver<GRPCServiceOuterClass.Response>() {
            @Override
            public void onNext(GRPCServiceOuterClass.Response value) {
                if (value.getStringResponse().equals("OK")) {
                    mechanicThread.setMapValue(robot, true);
                    mechanicThread.Notify();
                }
            }

            public void onError(Throwable throwable) {
                mechanicThread.setMapValue(robot, true);
                mechanicThread.Notify();
            }

            public void onCompleted() {
                channel.shutdown();
            }
        });

        try {
            //you need this. otherwise the method will terminate before that answers from the server are received
            channel.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void fixed(beans.Robot robot) {
        //opening a connection with server
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(robot.robotAddress + ":" + robot.listenPort).usePlaintext().build();

        //creating the asynchronous stub
        GRPCServiceGrpc.GRPCServiceStub stub = GRPCServiceGrpc.newStub(channel);

        GRPCServiceOuterClass.RobotRequest robotRequest = GRPCServiceOuterClass.RobotRequest.newBuilder().setId(this.getId())
                .setListenPort(this.getListenPort())
                .setRobotAddress(this.getRobotAddress())
                .setDistrict(this.getDistrict())
                .setX(this.getX())
                .setY(this.getY())
                .build();

        stub.fixed(robotRequest, new StreamObserver<GRPCServiceOuterClass.Response>() {
            @Override
            public void onNext(GRPCServiceOuterClass.Response value) {
                System.out.println(value.getStringResponse());
            }

            public void onError(Throwable throwable) {
                System.out.println(throwable.getMessage());
            }

            public void onCompleted() {
                channel.shutdown();
            }
        });

        try {
            //you need this. otherwise the method will terminate before that answers from the server are received
            channel.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void fix() {
        if (!isBroken()) {
            setBroken(true);
            mechanicThread.interrupt();
        }
    }

}
