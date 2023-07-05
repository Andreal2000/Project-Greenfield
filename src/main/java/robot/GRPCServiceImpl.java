package robot;

import com.example.grpc.GRPCServiceGrpc.*;
import com.example.grpc.GRPCServiceOuterClass.*;
import io.grpc.stub.StreamObserver;


public class GRPCServiceImpl extends GRPCServiceImplBase {
    private Robot robot;

    public GRPCServiceImpl(Robot robot) {
        this.robot = robot;
    }

    @Override
    public void addRobot(RobotRequest request, StreamObserver<Response> responseObserver) {
        System.out.println("Received GRPC [addRobot] form robot " + request.getId());
        beans.Robot newRobot = new beans.Robot(request);
        robot.addFriend(newRobot);
        responseObserver.onNext(Response.newBuilder().setStringResponse("Completed GRPC [addRobot] to robot " + robot.getId()).build());
        responseObserver.onCompleted();
        System.out.println("Completed GRPC [addRobot] form robot " + request.getId());
    }

    @Override
    public void removeRobot(RobotRequest request, StreamObserver<Response> responseObserver) {
        System.out.println("Received GRPC [removeRobot] form robot " + request.getId());
        beans.Robot newRobot = new beans.Robot(request);
        robot.removeFriend(newRobot);
        responseObserver.onNext(Response.newBuilder().setStringResponse("Completed GRPC [removeRobot] to robot " + robot.getId()).build());
        responseObserver.onCompleted();
        System.out.println("Completed GRPC [removeRobot] form robot " + request.getId());
    }

    @Override
    public void heartbeat(message request, StreamObserver<message> responseObserver) {
//        System.out.println("Received GRPC [heartbeat]");
        responseObserver.onCompleted();
//        System.out.println("Completed GRPC [heartbeat]");
    }

    @Override
    public void mechanic(BrokenRequest request, StreamObserver<Response> responseObserver) {
        if (robot.isMechanic() || (robot.isBroken() && robot.getTimestamp() < request.getTimestamp())){
            robot.mechanicThread.addWaitList(new beans.Robot(request.getRobot()));
            responseObserver.onNext(Response.newBuilder().setStringResponse("NO").build());
        } else {
            responseObserver.onNext(Response.newBuilder().setStringResponse("OK").build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void fixed(RobotRequest request, StreamObserver<Response> responseObserver) {
        robot.mechanicThread.setMapValue(new beans.Robot(request), true);
        robot.mechanicThread.Notify();
        responseObserver.onNext(Response.newBuilder().setStringResponse("Completed GRPC [fixed] to robot " + robot.getId()).build());
        responseObserver.onCompleted();
    }
}
