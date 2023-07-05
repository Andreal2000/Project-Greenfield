package beans;

import com.example.grpc.GRPCServiceOuterClass;
import robot.AbstractRobot;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Robot extends AbstractRobot {

    public Robot() {
    }

    public Robot(AbstractRobot robot) {
        this.id = robot.getId();
        this.listenPort = robot.getListenPort();
        this.robotAddress = robot.getRobotAddress();
        this.district = robot.getDistrict();
        this.x = robot.getX();
        this.y = robot.getY();
    }

    public Robot(GRPCServiceOuterClass.RobotRequest robot) {
        this.id = robot.getId();
        this.listenPort = robot.getListenPort();
        this.robotAddress = robot.getRobotAddress();
        this.district = robot.getDistrict();
        this.x = robot.getX();
        this.y = robot.getY();
    }

    @Override
    public String toString() {
        return "Robot{" +
                "id=" + id +
                ", listenPort=" + listenPort +
                ", robotAddress='" + robotAddress + '\'' +
                ", district=" + district +
                ", x=" + x +
                ", y=" + y +
                '}';
    }
}

