package administrator.server;

import beans.AddRobotResponse;
import beans.Robot;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Path("server")
public class ServerService {

    @Path("add")
    @POST
    @Consumes({"application/json", "application/xml"})
    @Produces({"application/json", "application/xml"})
    public Response addRobot(Robot robot) {
        AddRobotResponse addRobotResponse = Server.getInstance().addRobot(robot);
        System.out.println(Server.getInstance().getRobot()); // TODO
        if (addRobotResponse != null) {
            return Response.ok(addRobotResponse).build();
        } else {
            return Response.status(Response.Status.CONFLICT).build();
        }
    }

    @Path("remove")
    @POST
    @Consumes({"application/json", "application/xml"})
    public Response removeRobot(int id) {
        if (Server.getInstance().removeRobot(id)) {
            System.out.println(Server.getInstance().getRobot()); // TODO
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.CONFLICT).build();
        }
    }

    @Path("getRobots")
    @GET
    @Produces({"application/json", "application/xml"})
    public Response getRobots() {
        return Response.ok(Server.getInstance().getRobot()).build();
    }

    @Path("getAverageLastNPollution")
    @GET
    @Produces({"application/json", "application/xml"})
    public Response getAverageLastNPollution(@QueryParam("n") int n, @QueryParam("robot") int robot) {
        return Response.ok(Server.getInstance().getAverageLastNPollution(n, robot)).build();
    }

    @Path("getAverageRangePollution")
    @GET
    @Produces({"application/json", "application/xml"})
    public Response getAverageRangePollution(@QueryParam("t1") long t1, @QueryParam("t2") long t2) {
        return Response.ok(Server.getInstance().getAverageRangePollution(t1, t2)).build();
    }
}
