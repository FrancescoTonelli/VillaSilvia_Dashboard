package museo;

import com.google.gson.Gson;
import museo.LightConfig;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

//Classe utilizzata all'inizio per trovare l'USB inserita nel Raspebrry Pi (e i suoi contenuti)
public class UsbMediaScanner {

    public static class ScanResult {
        public String videoPath;
        public String thumbnailPath;
        public String videoName;
        public List<LightConfig> lightConfigList = new ArrayList<>();

        public boolean isComplete() {
            return videoPath != null && thumbnailPath != null && !lightConfigList.isEmpty();
        }
    }

    public static ScanResult scanForMedia(String username) {
        List<File> searchDirs = getSearchDirs(username);
        ScanResult result = new ScanResult();

        for (File dir : searchDirs) {
            File[] videoFiles = dir.listFiles(f -> f.isFile() && f.getName().toLowerCase().endsWith(".mp4"));
            if (videoFiles != null && videoFiles.length > 0) {
                File videoFile = videoFiles[0];
                result.videoPath = videoFile.getAbsolutePath();
                result.videoName = videoFile.getName();
                System.out.println("Trovato video: " + result.videoPath);
            }

            File[] imageFiles = dir.listFiles(f -> f.isFile() && f.getName().toLowerCase().endsWith(".jpg"));
            if (imageFiles != null && imageFiles.length > 0) {
                result.thumbnailPath = imageFiles[0].getAbsolutePath();
                System.out.println("Trovata immagine: " + result.thumbnailPath);
            }

            File configFile = new File(dir, "light.json");
            if (configFile.exists()) {
                try {
                    String json = new String(Files.readAllBytes(configFile.toPath()));
                    Gson gson = new Gson();
                    LightConfig[] configArray = gson.fromJson(json, LightConfig[].class);
                    result.lightConfigList = Arrays.asList(configArray);
                    System.out.println("Caricate " + result.lightConfigList.size() + " configurazioni luci.");
                } catch (Exception e) {
                    System.err.println("Errore nella lettura di light.json: " + e.getMessage());
                }
            }

            if (result.isComplete())
                break;
        }

        return result;
    }

    public static List<File> getSearchDirs(String username) {
        List<File> searchDirs = new ArrayList<>();
        File mediaRoot = new File("/media/" + username);
        File mntRoot = new File("/mnt/");

        if (mediaRoot.exists()) {
            File[] mediaDevices = mediaRoot.listFiles(File::isDirectory);
            if (mediaDevices != null)
                searchDirs.addAll(Arrays.asList(mediaDevices));
        }

        if (mntRoot.exists()) {
            File[] mntDevices = mntRoot.listFiles(File::isDirectory);
            if (mntDevices != null)
                searchDirs.addAll(Arrays.asList(mntDevices));
        }

        return searchDirs;
    }
}
