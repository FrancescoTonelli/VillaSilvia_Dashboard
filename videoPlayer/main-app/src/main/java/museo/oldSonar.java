package museo;

import java.io.File;

import com.pi4j.Pi4J;
import com.pi4j.boardinfo.util.BoardInfoHelper;
import com.pi4j.io.gpio.digital.*;
import com.pi4j.util.Console;
import com.pi4j.context.*;
import java.util.concurrent.*;

public class oldSonar extends Thread {

    private static final int TRIG_PIN = 23; // pin 16 marrone ottavo pin
    private static final int ECHO_PIN = 25; // pin 22 verde undicesimo pin

    private Console console;
    private Context pi4j;
    private Runnable task;
    private double minDist;

    private DigitalInput echoPin;
    private DigitalOutput trigPin;

    public oldSonar(Context pi4j, double minDist, Runnable task) throws Exception {
        this.pi4j = pi4j;
        this.task = task;
        this.minDist = minDist;

        var echoConfig = DigitalInput.newConfigBuilder(pi4j)
                .id("echo")
                .address(ECHO_PIN);
        echoPin = pi4j.create(echoConfig);

        var trigConfig = DigitalOutput.newConfigBuilder(pi4j)
                .id("trig")
                .address(TRIG_PIN);
        trigPin = pi4j.create(trigConfig);
        log("Detector initialed");

    }

    public void run() {
        try {
            log("started");
            while (true) {
                // log("Get distance..");
                double d = getDistance();
                log("DISTANCE: " + d);
                if (d < minDist) {
                    task.run();
                    Thread.sleep(5000);
                } else {
                    Thread.sleep(100);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void log(String msg) {
        System.out.println(msg);
    }

    private double getDistance() {
        trigPin.low();
        busyWait(2000);
        trigPin.high();
        busyWait(15000); // trigger di almeno 10 µs
        trigPin.low();

        long timeout = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(60);

        while (echoPin.isLow() && System.nanoTime() < timeout) {
            // aspetta inizio eco
        }
        long startTime = System.nanoTime();

        while (echoPin.isHigh() && System.nanoTime() < timeout) {
            // aspetta fine eco
        }
        long endTime = System.nanoTime();

        if (startTime == 0 || endTime <= startTime) {
            log("Timeout: nessun eco ricevuto.");
            return Double.MAX_VALUE;
        }

        long duration = endTime - startTime;
        double distanceInCM = duration * 0.00001715;

        return distanceInCM;
    }

    void busyWait(long ns) {
        long t0 = System.nanoTime();
        while (System.nanoTime() - t0 < ns) {
        }
    }

}