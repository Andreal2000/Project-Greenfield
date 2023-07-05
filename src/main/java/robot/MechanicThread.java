package robot;

import java.util.*;

public class MechanicThread extends Thread {
    private volatile boolean stopCondition = false;
    private final Robot robot;
    private final Map<beans.Robot, Boolean> copyFriends;
    private final List<beans.Robot> waitFriends;

    public MechanicThread(Robot robot) {
        this.robot = robot;
        this.copyFriends = new HashMap<>();
        this.waitFriends = new ArrayList<>();
    }

    public void stopMeGently() {
        stopCondition = true;
    }

    public void setMapValue(beans.Robot robot, Boolean value) {
        synchronized (copyFriends) {
            beans.Robot key = copyFriends.keySet().stream().filter(r -> r.getId() == robot.getId()).findFirst().orElse(null);
            if (key != null) {
                copyFriends.put(key, value);
            }
        }
    }

    public void addWaitList(beans.Robot robot) {
        synchronized (waitFriends) {
            waitFriends.add(robot);
        }
    }

    public void removeWaitList(beans.Robot robot) {
        synchronized (waitFriends) {
            beans.Robot remove = waitFriends.stream().filter(r -> r.getId() == robot.getId()).findAny().orElse(null);
            waitFriends.remove(remove);
        }
    }

    @Override
    public void run() {
        while (!stopCondition) {
            try {
                if (new Random().nextInt(10) == 7 || robot.isBroken()) {
                    System.out.println("NEED THE MECHANIC");
                    robot.setBroken(true);
                    robot.setTimestamp(System.currentTimeMillis());
                    copyFriends.clear();
                    for (beans.Robot r : robot.getFriends()) {
                        copyFriends.put(r, false);
                    }
                    // mechanic all
                    for (beans.Robot r : copyFriends.keySet()) {
                        robot.wantMechanic(r);
                    }
                    while (true) {
                        if (copyFriends.values().stream().allMatch(a -> a)) {
                            // GO TO THE MECHANIC
                            System.out.println("GO TO THE MECHANIC");
                            robot.setMechanic(true);
                            Thread.sleep(10000);
                            robot.setBroken(false);
                            robot.setMechanic(false);
                            for (beans.Robot r : waitFriends) {
                                robot.fixed(r);
                            }
                            waitFriends.clear();
                            System.out.println("FIXED");
                            break;
                        } else {
                            System.out.println("WAIT");
                            synchronized (this) {
                                wait();
                            }
                        }
                    }
                }
                System.out.println("TRY BRAKE");
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                System.out.println("SKIPPED SLEEP TO BRAKE THE ROBOT");
//                e.printStackTrace();
            }
        }
    }

    public synchronized void Notify() {
        notify();
    }
}
