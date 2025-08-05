package museo;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ProcessManager {

    private Process videoPlayerProcess;

    // Metodo usato per avviare l'applicazione video e mandare il Raspberry Pi in
    // modalità risparmio energetico (solo se non è il primo avvio)

    public void startPlayVideoApp(Boolean first) {

        if (videoPlayerProcess != null && videoPlayerProcess.isAlive()) {
            System.out.println("videoPlayer è già in esecuzione");
            return;
        }

        if (!first) {
            // non essendo il primo avvio è stato "addormentato" in precedenza, sveglia il
            // Raspberri Pi dal risparmio energetico
            executeScript("/home/villasilvia/Desktop/condivisa/videoPlayer/MqttVideoClient/wake.sh");
        }

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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

    // Metodo usato per terminare l'applicazione video e mandare il Raspberry Pi in
    // modalità risparmio energetico
    public void stopPlayVideoApp() {
        if (videoPlayerProcess != null && videoPlayerProcess.isAlive()) {
            System.out.println("Tentativo di chiusura playvideo-app...");
            killAllRelatedProcesses();
            // Manda il Raspberry Pi in risparmio energetico
            executeScript("/home/villasilvia/Desktop/condivisa/videoPlayer/MqttVideoClient/sleep.sh");

        } else {
            System.out.println("Nessun processo attivo da fermare");
        }

        videoPlayerProcess = null;
    }

    public void killAllRelatedProcesses() {
        String[] commands = {
                "pkill -f PlayVideo",
                "pkill -f mpv",
                "pkill -f feh",
                "pkill -f unclutter"
        };

        for (String cmd : commands) {
            try {
                Process p = Runtime.getRuntime().exec(cmd);
                int exitCode = p.waitFor();
                if (exitCode == 0) {
                    System.out.println("Processo terminato con successo: " + cmd);
                } else if (exitCode == 1) {
                    System.out.println("Nessun processo trovato per: " + cmd);
                } else {
                    System.err.println("Errore eseguendo: " + cmd + " (codice " + exitCode + ")");
                }
            } catch (IOException | InterruptedException e) {
                System.err.println("Eccezione durante: " + cmd + " -> " + e.getMessage());
            }
        }
    }

    public void executeScript(String path) {
        try {
            ProcessBuilder pb = new ProcessBuilder("/bin/bash", path);
            pb.inheritIO();
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
