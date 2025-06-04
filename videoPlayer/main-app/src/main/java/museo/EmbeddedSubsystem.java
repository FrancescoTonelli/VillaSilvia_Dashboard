package museo;

import java.io.File;

import com.pi4j.Pi4J;
import com.pi4j.boardinfo.util.BoardInfoHelper;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.io.gpio.digital.PullResistance;
import com.pi4j.util.Console;
import com.pi4j.context.*;

public class EmbeddedSubsystem {

    private static final int PIN_BUTTON = 24; // PIN 18 = BCM 24

    private int pressCount = 0;
    private Console console;
    private Context pi4j;
    private DistanceDetector detector;

    public EmbeddedSubsystem() throws Exception {

        pi4j = Pi4J.newAutoContext();

    }

    public void spawnSonarDetector(Runnable task) throws Exception {
        detector = new DistanceDetector(pi4j, 15, task);
        detector.start();
    }

    public void onTriggered(Runnable task) {

        var buttonConfig = DigitalInput.newConfigBuilder(pi4j)
                .id("button")
                .name("Press button")
                .address(PIN_BUTTON)
                .pull(PullResistance.PULL_DOWN)
                .debounce(3000L);
        var button = pi4j.create(buttonConfig);
        button.addListener(e -> {
            if (e.state() == DigitalState.LOW) {
                // pressCount++;
                // console.println("Button was pressed for the " + pressCount + "th time");
                task.run();
            }
        });

    }

    public void shutdown() {
        pi4j.shutdown(); // Ferma GPIO e thread Pi4J
    }

}
