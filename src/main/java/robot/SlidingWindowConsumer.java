package robot;

import robot.simulator.Measurement;
import robot.simulator.SlidingWindowBuffer;

import java.util.List;

class SlidingWindowConsumer extends Thread {
    private volatile boolean stopCondition = false;
    private final SlidingWindowBuffer slidingWindowBuffer;
    private final List<Double> averagePollutionList;

    public SlidingWindowConsumer(SlidingWindowBuffer slidingWindowBuffer, List<Double> averagePollutionList) {
        this.slidingWindowBuffer = slidingWindowBuffer;
        this.averagePollutionList = averagePollutionList;
    }

    public void stopMeGently() {
        stopCondition = true;
    }

    @Override
    public void run() {
        while (!stopCondition) {
            double average = slidingWindowBuffer.readAllAndClean()
                    .stream().mapToDouble(Measurement::getValue)
                    .average().orElse(0);

            synchronized (averagePollutionList) {
                averagePollutionList.add(average);
            }

        }
    }
}
