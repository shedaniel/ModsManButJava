package me.shedaniel.modsmanbutjava;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        List<Pair<CurseForgeAPI.FileInformation, ModsManConfig.ModObject>> files = new ArrayList<>();
        for(int j = 0; j < config.mods.size(); j += 50) {
            int size = Math.min(config.mods.size() - j, 50);
            int[] a = new int[size];
            int[] b = new int[size];
            for(int i = 0; i < size; i++) {
                a[i] = config.mods.get(i + j).projectId;
                b[i] = config.mods.get(i + j).fileId;
            }
            JsonObject object = CurseForgeAPI.getFiles(a, b);
            for(Map.Entry<String, JsonElement> entry : object.entrySet()) {
                Optional<ModsManConfig.ModObject> modObject = config.get(entry.getKey());
                try {
                    CurseForgeAPI.FileInformation[] fileInformations = GSON.fromJson(entry.getValue(), CurseForgeAPI.FileInformation[].class);
                    CurseForgeAPI.FileInformation file = null;
                    if (fileInformations.length == 1)
                        file = fileInformations[0];
                    else
                        throw new NullPointerException();
                    System.out.println(modObject.map(mod -> mod.projectName).orElse("NULL") + ": Loaded from " + file.downloadUrl + " [" + file.fileNameOnDisk + "]");
                    files.add(new Pair<>(file, modObject.get()));
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Failed to load " + modObject.map(mod -> mod.projectName).orElse("NULL"));
                }
            }
        }
        System.out.println("\nInitialising Downloads! (" + config.mods.size() + " entries with " + files.size() + " loaded)\n");
        AtomicInteger downloaded = new AtomicInteger(0), done = new AtomicInteger(0);
        for(Pair<CurseForgeAPI.FileInformation, ModsManConfig.ModObject> file : files) {
            service.submit(() -> {
                CurseForgeAPI.FileInformation fileInformation = file.getLeft();
                ModsManConfig.ModObject modObject = file.getRight();
                File end = new File(currentDir, fileInformation.fileNameOnDisk);
                if (end.exists()) {
                    System.out.println(fileInformation.fileNameOnDisk + " already exists! Skipping!");
                    done.incrementAndGet();
                } else {
                    System.out.println("Downloading: " + fileInformation.fileNameOnDisk + "");
                    try {
                        CurseForgeAPI.download(new URL(fileInformation.downloadUrl), end);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println("Downloaded: " + fileInformation.fileNameOnDisk + "");
                    downloaded.incrementAndGet();
                    done.incrementAndGet();
                }
            });
        }
        while (true) {
            if (done.get() >= files.size()) {
                System.out.println("\nDownloaded " + downloaded.get() + "/" + files.size() + " files!");
                System.exit(0);
            }
        }
    }
    
}
