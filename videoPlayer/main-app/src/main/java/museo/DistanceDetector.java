package museo;

import java.io.File;

import com.pi4j.Pi4J;
import com.pi4j.boardinfo.util.BoardInfoHelper;
import com.pi4j.io.gpio.digital.*;
import com.pi4j.util.Console;
import com.pi4j.context.*;
import java.util.concurrent.*;

public class DistanceDetector extends Thread {

	private static final int TRIG_PIN = 23;
	private static final int ECHO_PIN = 25;

	private Console console;
	private Context pi4j;
	private Runnable task;
	private double minDist;

	private DigitalInput echoPin;
	private DigitalOutput trigPin;

	public DistanceDetector(Context pi4j, double minDist, Runnable task) throws Exception {
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
			int validCount = 0; // contatore delle letture valide consecutive

			while (true) {
				// log("Get distance..");
				double d = getDistance();
				// log("DISTANCE: " + d);

				if (d < minDist && d > 6) {
					validCount++;
					if (validCount >= 2) {
						task.run();
						validCount = 0;
						Thread.sleep(5000);
					} else {
						Thread.sleep(150);
					}
				} else {
					validCount = 0;
					Thread.sleep(150);
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
		// log("tri out..");
		// trigPin.pulse(10, TimeUnit.MICROSECONDS);
		// log("trig done");
		trigPin.low();
		busyWait(2000);
		trigPin.high();
		busyWait(10000);
		trigPin.low();

		long startTime = -1;
		long endTime = -1;

		long timeout = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(100);
		while (echoPin.isLow() && System.nanoTime() < timeout)
			;
		startTime = System.nanoTime();

		timeout = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(100);
		while (echoPin.isHigh() && System.nanoTime() < timeout)
			;
		endTime = System.nanoTime();

		if (endTime <= startTime)
			return Double.MAX_VALUE;

		long duration = endTime - startTime;
		double distanceInCM = duration / 58_000.0;
		distanceInCM = duration / 1_000.0 / 58.0;

		return distanceInCM;

	}

	void busyWait(long ns) {
		long t0 = System.nanoTime();
		while (System.nanoTime() - t0 < ns) {
		}
	}

}