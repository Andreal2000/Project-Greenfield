package robot;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.FIELD)
public abstract class AbstractRobot {

    protected int id;
    protected int listenPort;
    protected String robotAddress = "localhost";
    protected int district;
    protected int x;
    protected int y;

    public int getId() {
        return id;
    }

    public int getListenPort() {
        return listenPort;
    }

    public String getRobotAddress() {
        return robotAddress;
    }

    public int getDistrict() {
        return district;
    }

    public void setDistrict(int district) {
        this.district = district;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    @Override
    public String toString() {
        return "AbstractRobot{" +
                "id=" + id +
                ", listenPort=" + listenPort +
                ", robotAddress='" + robotAddress + '\'' +
                ", district=" + district +
                ", x=" + x +
                ", y=" + y +
                '}';
    }
}
