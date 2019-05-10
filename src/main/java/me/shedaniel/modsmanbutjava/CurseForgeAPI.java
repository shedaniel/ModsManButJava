package me.shedaniel.modsmanbutjava;

import com.google.gson.JsonObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class CurseForgeAPI {
    
    public static final String USER_AGENT = "Mozilla/5.0";
    
    public static FileInformation getFile(int project, int file) {
        try {
            return Launch.GSON.fromJson(getString(get(new URL(String.format("https://staging_cursemeta.dries007.net/api/v3/direct/addon/%d/file/%d", project, file)))), FileInformation.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static JsonObject getFiles(int[] projects, int[] files) {
        try {
            if (projects.length != files.length)
                throw new NullPointerException();
            String args = projects.length > 0 ? "?" : "";
            for(int i = 0; i < projects.length; i++) {
                if (args.charAt(args.length() - 1) != '?')
                    args += '&';
                args += "addon=" + projects[i] + "&file=" + files[i];
            }
            return Launch.GSON.fromJson(getString(get(new URL(String.format("https://staging_cursemeta.dries007.net/api/v3/direct/addon/files" + args)))), JsonObject.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static void download(URL url, File file) throws IOException {
        ReadableByteChannel rbc = Channels.newChannel(url.openStream());
        FileOutputStream fos = new FileOutputStream(file);
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
    }
    
    public static String getString(InputStream stream) throws IOException {
        InputStreamReader inputStreamReader = new InputStreamReader(stream);
        BufferedReader in = new BufferedReader(inputStreamReader);
        String inputLine;
        StringBuffer response = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        stream.close();
        return response.toString();
    }
    
    public static InputStream get(URL url) throws IOException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", USER_AGENT);
        return con.getInputStream();
    }
    
    public static class FileInformation {
        public int id;
        public String fileName;
        public String fileNameOnDisk;
        public String downloadUrl;
        
        @Override
        public String toString() {
            return String.format("File[id = %d, fileName = %s, fileNameOnDisk = %s, downloadUrl = %s]", id, fileName, fileNameOnDisk, downloadUrl);
        }
    }
    
}
