package administrator.client;

import java.util.Scanner;

public class StartClient {
    public static void main(String[] args) {
        Client client = new Client();
        while (true) {
            System.out.println("[CLIENT] Insert a number to execute an operation: ");
            System.out.println("0 - Shutdown the client");
            System.out.println("1 - list of the cleaning robots currently located in Greenfield");
            System.out.println("2 - average of the last n air pollution levels sent to the server by a given robot");
            System.out.println("3 - average of the air pollution levels sent by all the robots to the server and occurred from timestamps t1 and t2");

            Scanner scanner = new Scanner(System.in);
            String input = scanner.next();
            switch (input) {
                case "0":
                    System.out.println("Shutdown client");
                    System.exit(0);
                case "1":
                    client.getRobots();
                    break;
                case "2":
                    System.out.print("Insert the number of the last n air pollution levels: ");
                    int n = Integer.parseInt(scanner.next());
                    System.out.print("Insert the robot id: ");
                    int robot = Integer.parseInt(scanner.next());
                    client.getAverageLastNPollution(n, robot);
                    break;
                case "3":
                    System.out.print("Insert the timestamp t1 (long): ");
                    long t1 = Long.parseLong(scanner.next());
                    System.out.print("Insert the timestamp t2 (long): ");
                    long t2 = Long.parseLong(scanner.next());
                    client.getAverageRangePollution(t1, t2);
                    break;
                default:
                    System.out.println("Operation not available.");
                    break;
            }
        }
    }
}
