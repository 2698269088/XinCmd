package top.mcocet.xincmd.config;

import com.google.gson.*;
import java.nio.file.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class XinCmdConfig {
    private final Path configPath; // 配置文件路径

    private List<String> administrators = new ArrayList<>(); // 管理员列表
    private boolean remoteCommandEnabled = true; // 远程命令是否启用
    private boolean remoteCommandAdminEnabled = false; // 远程命令的 admin 功能是否启用

    public XinCmdConfig(Path configPath) {
        this.configPath = configPath;
    }

    public void loadConfig() throws IOException {
        if (Files.notExists(configPath)) {
            // 确保目录存在
            Files.createDirectories(configPath.getParent());
            createDefaultConfig();
            return;
        }

        try {
            String content = Files.readString(configPath);
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();

            if (root.has("administrators") && root.get("administrators").isJsonArray()) {
                administrators = new ArrayList<>();
                for (JsonElement element : root.getAsJsonArray("administrators")) {
                    administrators.add(element.getAsString());
                }
            }

            // 加载远程命令配置
            if (root.has("remoteCommandEnabled")) {
                remoteCommandEnabled = root.get("remoteCommandEnabled").getAsBoolean();
            }

            if (root.has("remoteCommandAdminEnabled")) {
                remoteCommandAdminEnabled = root.get("remoteCommandAdminEnabled").getAsBoolean();
            }
        } catch (Exception e) {
            throw new IOException("配置文件解析失败：" + configPath.toAbsolutePath() + " - " + e.getMessage(), e);
        }
    }

    public void saveConfig() throws IOException {
        // 确保目录存在
        Files.createDirectories(configPath.getParent());

        JsonObject root = new JsonObject();

        JsonArray adminArray = new JsonArray();
        administrators.forEach(a -> adminArray.add(a));
        root.add("administrators", adminArray);

        // 保存远程命令配置
        root.addProperty("remoteCommandEnabled", remoteCommandEnabled);
        root.addProperty("remoteCommandAdminEnabled", remoteCommandAdminEnabled);

        Files.writeString(configPath, new GsonBuilder().setPrettyPrinting().create().toJson(root));
    }

    private void createDefaultConfig() throws IOException {
        // 确保目录存在
        Files.createDirectories(configPath.getParent());

        // 基本配置
        JsonObject def = new JsonObject();

        // 管理员列表
        JsonArray defaultAdmins = new JsonArray();
       def.add("administrators", defaultAdmins);

        // 远程命令配置项
       def.addProperty("remoteCommandEnabled", true);
       def.addProperty("remoteCommandAdminEnabled", false);

        Files.writeString(configPath, new GsonBuilder().setPrettyPrinting().create().toJson(def));
    }

    // Getters & Setters
    public List<String> getAdministrators() {
        return administrators;
    }

    public void setAdministrators(List<String> administrators) {
        this.administrators = new ArrayList<>(administrators);
    }

    public void addAdministrator(String playerName) {
        if (!administrators.contains(playerName)) {
            administrators.add(playerName);
        }
    }

    public void removeAdministrator(String playerName) {
        administrators.remove(playerName);
    }

    public boolean isAdministrator(String playerName) {
        return administrators.contains(playerName);
    }

    public boolean isRemoteCommandEnabled() {
        return remoteCommandEnabled;
    }

    public void setRemoteCommandEnabled(boolean remoteCommandEnabled) {
        this.remoteCommandEnabled = remoteCommandEnabled;
    }

    public boolean isRemoteCommandAdminEnabled() {
        return remoteCommandAdminEnabled;
    }

    public void setRemoteCommandAdminEnabled(boolean remoteCommandAdminEnabled) {
        this.remoteCommandAdminEnabled = remoteCommandAdminEnabled;
    }
}
