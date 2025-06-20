package museo;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ProcessManager {

    private Process videoPlayerProcess;

    // Metodo usato per avviare l'applicazione JavaFX e mandare il Raspberry Pi in
    // modalità risparmio energetico (solo se non è il primo avvio)

    public void startPlayVideoApp(Boolean first) {
        if (videoPlayerProcess != null && videoPlayerProcess.isAlive()) {
            System.out.println("videoPlayer è già in esecuzione");
            return;
        }

        if (!first) {
            // non essendo il primo avvio è stato "addormentato" in precedenza, stampa in un
            // log il suo stato (CPU,
            // temperatura... durante lo sleep) e poi sveglia il Raspberri Pi dal risparmio
            // energetico
            executeScript("/home/villasilvia/Desktop/condivisa/videoPlayer/MqttVideoClient/log.sh");
            executeScript("/home/villasilvia/Desktop/condivisa/videoPlayer/MqttVideoClient/wake.sh");
        }

        try {
            videoPlayerProcess = new ProcessBuilder(
                    "/bin/bash", "/home/villasilvia/Desktop/condivisa/videoPlayer/main-app/target/distribution/run.sh")
                    .inheritIO()
                    .start();
            System.out.println("Avviato playvideo-app via script");
        } catch (IOException e) {
            System.err.println("Errore avvio playvideo-app: " + e.getMessage());
        }

    }

    // Metodo usato per terminare l'applicazione JavaFX e mandare il Raspberry Pi in
    // modalità risparmio energetico
    public void stopPlayVideoApp() {
        // stampa in un log il suo stato (CPU,temperatura...) durante l'esecuzione
        // normale
        executeScript("/home/villasilvia/Desktop/condivisa/videoPlayer/MqttVideoClient/log.sh");
        if (videoPlayerProcess != null && videoPlayerProcess.isAlive()) {
            System.out.println("Tentativo di chiusura playvideo-app...");
            videoPlayerProcess.destroyForcibly();

            try {
                boolean exited = videoPlayerProcess.waitFor(2, TimeUnit.SECONDS);
                if (!exited) {
                    System.out.println("Ancora vivo, uso pkill");
                    Runtime.getRuntime().exec("pkill -f PlayVideo");
                } else {
                    System.out.println("playvideo-app terminato correttamente");
                }
            } catch (Exception e) {
                System.err.println("Errore in stop: " + e.getMessage());
            }
            // Manda il Raspberry Pi in risparmio energetico
            executeScript("/home/villasilvia/Desktop/condivisa/videoPlayer/MqttVideoClient/sleep.sh");

        } else {
            System.out.println("Nessun processo attivo da fermare");
        }

        videoPlayerProcess = null;
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
