package robot;

import java.io.IOException;
import java.util.Scanner;

public class StartRobot {
    public static void main(String[] args) {
        System.out.println("Insert the id of the robot");
        Scanner scanner = new Scanner(System.in);
        int id = scanner.nextInt();
//        int id = 1;
        Robot robot = new Robot(id, 10000 + id, "http://localhost:1337");

        while (true) {
            System.out.println("[ROBOT] Insert a number to execute an operation: ");
            System.out.println("0 - Shutdown the robot");
            System.out.println("1 - leave Greenfield");
            System.out.println("2 - send the robot to the mechanic");

            String input = scanner.next();
            switch (input) {
                case "0":
                    System.out.println("Shutdown client");
                    System.exit(0);
                case "1":
                    System.out.println("Robot leaves Greenfield");
                    robot.leaveCity();
                    break;
                case "2":
                    System.out.println("Robot goes to the mechanic");
                    robot.fix();
                    break;
                default:
                    System.out.println("Operation not available.");
                    break;
            }
        }
    }
}
