package administrator.client;

import beans.Robot;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import javax.ws.rs.core.MultivaluedMap;
import java.util.List;

class Client {
    private final String serverAddress = "http://localhost:1337";
    private final com.sun.jersey.api.client.Client client;

    public Client() {
        client = com.sun.jersey.api.client.Client.create();
    }

    private ClientResponse getRequest(String url) {
        return getRequest(url, new MultivaluedMapImpl());
    }

    private ClientResponse getRequest(String url, MultivaluedMap<String, String> queryParam) {
        WebResource webResource = client.resource(url).queryParams(queryParam);
        try {
            return webResource.type("application/json").get(ClientResponse.class);
        } catch (ClientHandlerException e) {
            System.out.println("Server non disponibile");
            return null;
        }
    }

    // The list of the cleaning robots currently located in Greenfield
    public void getRobots() {
        String getPath = "/server/getRobots";
        ClientResponse clientResponse = getRequest(serverAddress + getPath);
        String body = clientResponse.getEntity(String.class).toString();
        List<Robot> robots = new Gson().fromJson(body, new TypeToken<List<Robot>>() {
        }.getType());
        System.out.println(robots);
    }

    // The average of the last n air pollution levels sent to the server by a given robot
    public void getAverageLastNPollution(int n, int robot) {
        MultivaluedMapImpl queryParam = new MultivaluedMapImpl();
        queryParam.add("n", n);
        queryParam.add("robot", robot);
        String getPath = "/server/getAverageLastNPollution";
        ClientResponse clientResponse = getRequest(serverAddress + getPath, queryParam);
        String body = clientResponse.getEntity(String.class).toString();
        if (!body.isEmpty()) {
            System.out.println("The average of the last " + n + " air pollution levels sent to the server by the robot " + robot + " is " + body);
        } else {
            System.out.println("The request cannot be fulfilled due to some error in the input");
        }
    }

    // The average of the air pollution levels sent by all the robots to the server and occurred from timestamps t1 and t2
    public void getAverageRangePollution(long t1, long t2) {
        MultivaluedMapImpl queryParam = new MultivaluedMapImpl();
        queryParam.add("t1", String.valueOf(t1));
        queryParam.add("t2", String.valueOf(t2));
        String getPath = "/server/getAverageRangePollution";
        ClientResponse clientResponse = getRequest(serverAddress + getPath, queryParam);
        String body = clientResponse.getEntity(String.class).toString();
        if (!body.isEmpty()) {
            System.out.println("The average of the air pollution levels sent by all the robots to the server and occurred from timestamps " + t1 + " and " + t2 + " is " + body);
        } else {
            System.out.println("The request cannot be fulfilled due to some error in the input");
        }
    }

}
