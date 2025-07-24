package museo;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;

import museo.UsbMediaScanner.ScanResult;

public class PlayVideo {

    // Lo schermo mostra un immagine (che nelle stazioni di mezzo è una copertina
    // nera) SHOW_THUMBNAIL, poi viene riprodotto il video PLAYING_VIDEO e poi la
    // stazione viene disabilitata mostrando schermata nera DISABLED
    private enum State {
        SHOW_THUMBNAIL,
        PLAYING_VIDEO,
        SECOND_SONAR,
        THIRD_SONAR,
        DISABLED
    }

    private State currentState = State.SHOW_THUMBNAIL;

    private String activeVideoPath = null;
    private String activeThumbnailPath = null;
    private String activeVideoName = null;
    private String rotatedThumbnailPath;

    private String blackPath = "/home/villasilvia/Desktop/condivisa/videoPlayer/main-app/image/black.png";

    private List<LightConfig> lightConfigList = new ArrayList<>();

    private Process pausedMpvProcess;

    public static void main(String[] args) {
        new PlayVideo().start();
    }

    public void start() {
        System.out.println("Avvio sistema...");

        // rimuove il cursore dallo schermo
        try {
            new ProcessBuilder("unclutter", "-display", ":0", "-idle", "0").start();
            System.out.println("Cursore nascosto con unclutter.");
        } catch (IOException e) {
            System.err.println("Errore avvio unclutter: " + e.getMessage());
        }

        ScanResult result = null;

        // non parte fino a quando non trova una USB con i path al .mp4, .jpg e il json
        // di configurazione

        while (result == null || !result.isComplete()) {
            result = UsbMediaScanner.scanForMedia();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return;
            }
        }

        this.activeVideoPath = result.videoPath;
        this.activeVideoName = result.videoName;
        this.activeThumbnailPath = result.thumbnailPath;
        this.lightConfigList = result.lightConfigList;

        try {

            Thread.sleep(5000);
            showThumbnail();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            EmbeddedSubsystem raspi = new EmbeddedSubsystem();
            raspi.spawnSonarDetector(() -> {
                System.out.println("SONAR TRIGGERED");
                triggered();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showThumbnail() throws InterruptedException {
        currentState = State.SHOW_THUMBNAIL;

        try {
            new ProcessBuilder(
                    "bash", "-c", "feh --hide-pointer --no-menus --borderless -F -Z " + blackPath)
                    .start();

        } catch (IOException e) {
            System.err.println("Errore visualizzazione miniatura: " + e.getMessage());
        }

        Thread.sleep(3000);

        loadMpv();

    }

    private void loadMpv() {
        if (activeVideoPath != null) {
            try {
                ProcessBuilder pb = new ProcessBuilder(
                        "mpv",
                        "--fs",
                        "--no-border",
                        "--osd-level=0",
                        "--vo=gpu",
                        "--pause",
                        "--start=1",
                        "--input-ipc-server=/tmp/mpvsocket",
                        "--audio-device=alsa/sysdefault:CARD=vc4hdmi",
                        activeVideoPath);
                System.out.println("mpv avviato in pausa e in ascolto su /tmp/mpvsocket.");

                pb.redirectErrorStream(true);
                pb.redirectOutput(new File("/tmp/mpv.log"));
                pausedMpvProcess = pb.start();
            } catch (IOException e) {
                System.err.println("Errore avvio mpv in pausa: " + e.getMessage());
            }
        }
    }

    private void triggered() {
        if (currentState == State.SHOW_THUMBNAIL) {
            playVideo();
        } else if (currentState == State.SECOND_SONAR) {
            // la seconda volta lo notifica comunque che è stato triggerato ma non
            // fa più partire video
            notifyEvent("triggered");
            currentState = State.THIRD_SONAR;
        } else if (currentState == State.THIRD_SONAR) {
            // notifica la fine del suo uso
            notifyEvent("ended");
            currentState = State.DISABLED;
        }
    }

    private void playVideo() {
        currentState = State.PLAYING_VIDEO;

        try {

            // Invia comando play tramite socket
            sendMpvCommand("{\"command\": [\"set_property\", \"pause\", false]}");

            notifyEvent("triggered");

            // Attendi che il video finisca
            pausedMpvProcess.waitFor();

            currentState = State.SECOND_SONAR;

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void sendMpvCommand(String jsonCommand) {
        try {
            // Escape degli apici singoli nel comando
            String escapedJson = jsonCommand.replace("'", "'\\''");

            // Comando socat: echo '<json>' | socat - /tmp/mpvsocket
            String[] command = {
                    "bash",
                    "-c",
                    "echo '" + escapedJson + "' | socat - /tmp/mpvsocket"
            };

            ProcessBuilder pb = new ProcessBuilder(command);
            Process p = pb.start();
            int exitCode = p.waitFor();

            if (exitCode == 0) {
                System.out.println("Comando inviato con socat: " + jsonCommand);
            } else {
                System.err.println("Errore socat (codice " + exitCode + ")");
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("Errore invio comando a mpv con socat: " + e.getMessage());
        }
    }

    private void notifyEvent(String event) {
        try {
            URL url = new URL("http://localhost:8080/event/" + event);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String json = "[]";
            if (event.equals("triggered")) {
                Gson gson = new Gson();
                json = gson.toJson(lightConfigList);
            }

            conn.getOutputStream().write(json.getBytes());
            conn.getResponseCode();
            conn.disconnect();
            System.out.println("Evento '" + event + "' notificato con payload: " + json);
        } catch (IOException e) {
            System.err.println("Errore invio evento '" + event + "': " + e.getMessage());
        }
    }
}
