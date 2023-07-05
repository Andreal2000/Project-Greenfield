package robot;

import java.util.List;

public class HeartbeatThread extends Thread{
    private volatile boolean stopCondition = false;
    private Robot robot;

    public HeartbeatThread(Robot robot) {
        this.robot = robot;
    }

    public void stopMeGently() {
        stopCondition = true;
    }

    @Override
    public void run() {
        while (!stopCondition){
            try {
                Thread.sleep(1000);
                robot.heartbeatAll();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
