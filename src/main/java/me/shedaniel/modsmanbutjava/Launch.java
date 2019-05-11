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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class Launch {
    
    public static final Gson GSON = new GsonBuilder().create();
    public static ExecutorService service = Executors.newFixedThreadPool(8);
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
        List<CurseMetaAPI.AddonFile> addonFiles = new ArrayList<>();
        for(int j = 0; j < config.mods.size(); j += 50) {
            int size = Math.min(config.mods.size() - j, 50);
            int[] a = new int[size];
            int[] b = new int[size];
            for(int i = 0; i < size; i++) {
                a[i] = config.mods.get(i + j).projectId;
                b[i] = config.mods.get(i + j).fileId;
            }
            addonFiles.addAll(CurseMetaAPI.getAddonFilesMap(a, b).values());
        }
        System.out.println("\nInitialising Downloads! (" + config.mods.size() + " entries with " + addonFiles.size() + " loaded)\n");
        AtomicInteger downloaded = new AtomicInteger(0), done = new AtomicInteger(0);
        for(CurseMetaAPI.AddonFile addonFile : addonFiles) {
            service.submit(() -> {
                File end = new File(currentDir, addonFile.fileNameOnDisk);
                if (end.exists()) {
                    System.out.println(addonFile.fileNameOnDisk + " already exists! Skipping!");
                    done.incrementAndGet();
                } else {
                    System.out.println("Downloading: " + addonFile.fileNameOnDisk + "");
                    try {
                        download(new URL(addonFile.downloadUrl), end);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println("Downloaded: " + addonFile.fileNameOnDisk + "");
                    downloaded.incrementAndGet();
                    done.incrementAndGet();
                }
            });
        }
        while (true) {
            if (done.get() >= addonFiles.size()) {
                System.out.println("\nDownloaded " + downloaded.get() + "/" + addonFiles.size() + " files!");
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
