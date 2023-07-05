package administrator.server;

import java.io.IOException;

public class StartServer {

    public static void main(String[] args) throws IOException {
        Server server = Server.getInstance();
        System.out.println("[SERVER] press any key to shutdown the server");
        System.in.read();
        server.shutdown();
        System.exit(0);
    }
}
