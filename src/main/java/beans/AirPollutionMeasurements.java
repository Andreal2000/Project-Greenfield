package beans;

import java.util.List;

public class AirPollutionMeasurements {
    public int id;
    public int district;
    public long timestamp;
    public List<Double> averagePollutionList;

    public AirPollutionMeasurements(int id, int district, long timestamp, List<Double> averagePollutionList) {
        this.id = id;
        this.district = district;
        this.timestamp = timestamp;
        this.averagePollutionList = averagePollutionList;
    }

    public int getId() {
        return id;
    }

    public int getDistrict() {
        return district;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public List<Double> getAveragePollutionList() {
        return averagePollutionList;
    }

    @Override
    public String toString() {
        return "AirPollutionMeasurements{" +
                "id=" + id +
                ", district=" + district +
                ", timestamp=" + timestamp +
                ", averagePollutionList=" + averagePollutionList +
                '}';
    }
}
