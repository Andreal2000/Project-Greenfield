package beans;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class AddRobotResponse {
    private int district;
    private int x;
    private int y;
    private List<Robot> robots;

    AddRobotResponse() {
    }

    public AddRobotResponse(int district, int x, int y, List<Robot> robots) {
        this.district = district;
        this.x = x;
        this.y = y;
        this.robots = robots;
    }

    public int getDistrict() {
        return district;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public List<Robot> getRobots() {
        return robots;
    }

    @Override
    public String toString() {
        return "AddRobotResponse{" +
                "district=" + district +
                ", x=" + x +
                ", y=" + y +
                ", robots=" + robots +
                '}';
    }
}
