package museo;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ProcessManager {

    private Process playVideoProcess;

    public void startPlayVideoApp(Boolean first) {
        if (playVideoProcess != null && playVideoProcess.isAlive()) {
            System.out.println("playvideo-app è già in esecuzione");
            return;
        }

        if (!first) {
            executeScript("/home/aricci/Desktop/condivisa/MqttVideoClient/wake.sh");
        }

        try {
            // ⚠️ Usa path assoluto allo script .sh
            playVideoProcess = new ProcessBuilder(
                    "/bin/bash", "/home/aricci/Desktop/condivisa/main-app/target/distribution/run.sh")
                    .inheritIO()
                    .start();
            System.out.println("Avviato playvideo-app via script");
        } catch (IOException e) {
            System.err.println("Errore avvio playvideo-app: " + e.getMessage());
        }

    }

    public void stopPlayVideoApp() {
        if (playVideoProcess != null && playVideoProcess.isAlive()) {
            System.out.println("Tentativo di chiusura playvideo-app...");
            playVideoProcess.destroyForcibly();

            try {
                boolean exited = playVideoProcess.waitFor(2, TimeUnit.SECONDS);
                if (!exited) {
                    System.out.println("Ancora vivo, uso pkill");
                    Runtime.getRuntime().exec("pkill -f PlayVideo");
                } else {
                    System.out.println("playvideo-app terminato correttamente");
                }
            } catch (Exception e) {
                System.err.println("Errore in stop: " + e.getMessage());
            }
            executeScript("/home/aricci/Desktop/condivisa/MqttVideoClient/sleep.sh");

        } else {
            System.out.println("Nessun processo attivo da fermare");
        }

        playVideoProcess = null;
    }

    public void executeScript(String path) {
        try {
            ProcessBuilder pb = new ProcessBuilder("/bin/bash", path);
            pb.inheritIO(); // facoltativo: mostra output su console Java
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("Errore eseguendo lo script: " + path);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

}
