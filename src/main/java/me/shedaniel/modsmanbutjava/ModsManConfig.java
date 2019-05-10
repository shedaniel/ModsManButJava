package me.shedaniel.modsmanbutjava;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Optional;

public class ModsManConfig {
    public List<ModObject> mods;
    
    public Optional<ModObject> get(String projectId) {
        return mods.stream().filter(modObject -> projectId.equalsIgnoreCase(modObject.projectId + "")).findFirst();
    }
    
    public static class ModObject {
        @SerializedName("project_id") public int projectId;
        @SerializedName("file_id") public int fileId;
        @SerializedName("file_name") public String fileName;
        @SerializedName("project_name") public String projectName;
    }
    
}
