package me.shedaniel.modsmanbutjava;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.shedaniel.cursemetaapi.CurseMetaAPI;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Launch {
    
    public static final Gson GSON = new GsonBuilder().create();
    public static ExecutorService service = Executors.newFixedThreadPool(16);
    private static File currentDir;
    private static File modsmanConfig;
    private static ModsManConfig config;
    
    public static void main(String[] args) {
        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });
        currentDir = new File(System.getProperty("user.dir"));
        modsmanConfig = new File(currentDir, ".modlist.json");
        if (!modsmanConfig.exists() || !modsmanConfig.isFile() || !modsmanConfig.canRead()) {
            System.out.printf("%s doesn't exist or can't be read!%n", modsmanConfig.getAbsolutePath());
            return;
        }
        try {
            config = GSON.fromJson(new FileReader(modsmanConfig), ModsManConfig.class);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        List<ModsManConfig.ModObject> mods = config.mods.stream().filter(modObject -> modObject.fileName == null || !new File(currentDir, modObject.fileName).exists()).collect(Collectors.toList());
        System.out.println("Downloading cf data (0/" + mods.size() + ").");
        Map<CurseMetaAPI.AddonFile, String> addonFiles = new LinkedHashMap<>();
        final int[] doneDownloaded = {0};
        for (int i = 0; i < mods.size(); i++) {
            int finalI = i;
            service.submit(() -> {
                ModsManConfig.ModObject object = mods.get(finalI);
                try {
                    CurseMetaAPI.AddonFile file = CurseMetaAPI.getAddonFile(object.projectId, object.fileId);
                    addonFiles.put(file, object.fileName == null ? file.fileName : object.fileName);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(0);
                    return;
                }
                doneDownloaded[0]++;
                System.out.println("Downloading cf data (" + (doneDownloaded[0]) + "/" + mods.size() + ").");
            });
        }
        do {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while (doneDownloaded[0] < mods.size());
        System.out.println("Downloaded cf data.");
        System.out.println("\nInitialising Downloads! (" + mods.size() + " entries with " + addonFiles.size() + " loaded and " + (config.mods.size() - mods.size()) + " skipped)\n");
        AtomicInteger downloaded = new AtomicInteger(0), done = new AtomicInteger(0);
        for (Map.Entry<CurseMetaAPI.AddonFile, String> entry : addonFiles.entrySet()) {
            service.submit(() -> {
                CurseMetaAPI.AddonFile addonFile = entry.getKey();
                String fileName = entry.getValue();
                File end = new File(currentDir, fileName);
                if (end.exists()) {
                    System.out.println(fileName + " already exists! Skipping!");
                    done.incrementAndGet();
                } else {
                    System.out.println("Downloading: " + fileName + "");
                    try {
                        download(new URL(addonFile.downloadUrl), end);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println("Downloaded: " + fileName + "");
                    downloaded.incrementAndGet();
                    done.incrementAndGet();
                }
            });
        }
        while (true) {
            if (done.get() >= addonFiles.size()) {
                System.out.println("\nDownloaded " + downloaded.get() + "/" + addonFiles.size() + " files (" + (config.mods.size() - mods.size()) + " skipped)!");
                System.exit(0);
            }
        }
    }
    
    public static void download(URL url, File file) throws IOException {
        ReadableByteChannel rbc = Channels.newChannel(url.openStream());
        FileOutputStream fos = new FileOutputStream(file);
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
    }
    
}
