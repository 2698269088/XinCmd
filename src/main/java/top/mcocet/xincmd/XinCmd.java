package top.mcocet.xincmd;

import xin.bbtt.mcbot.Bot;
import xin.bbtt.mcbot.event.EventHandler;
import xin.bbtt.mcbot.event.Listener;
import xin.bbtt.mcbot.plugin.Plugin;
import xin.bbtt.mcbot.events.PrivateChatEvent;

import top.mcocet.xincmd.config.XinCmdConfig;
import top.mcocet.xincmd.command.RemoteCommand;
import top.mcocet.xincmd.command.RemoteCommandExecutor;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class XinCmd implements Plugin, Listener {
    public static XinCmd INSTANCE;

    private XinCmdConfig config;
    private final Path configPath = Paths.get("plugin", "XinCmd", "config.json");

    public XinCmd() {
        INSTANCE = this;
    }

    @Override
    public String getName() {
        return ("XinCmd");
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public void onLoad() {
        getLogger().info("XinCmd 插件已加载");
    }

    @Override
    public void onEnable() {
        getLogger().info("XinCmd 插件已启用");

        loadConfig();

        // 注册事件监听器
        Bot.Instance.getPluginManager().events().registerEvents(this, this);
        // 注册远程命令
        Bot.Instance.getPluginManager().registerCommand(new RemoteCommand(), new RemoteCommandExecutor(), this);
    }

    @Override
    public void onDisable() {
        getLogger().info("XinCmd 插件已关闭");
    }

    @Override
    public void onUnload() {
        getLogger().info("XinCmd 插件已卸载");
    }

    public void loadConfig() {
        try {
            config = new XinCmdConfig(configPath);
            config.loadConfig();
            getLogger().info("配置文件已成功加载：" + configPath.toAbsolutePath());
        } catch (Exception e) {
            getLogger().error("无法加载配置文件：" + e.getMessage());
            getLogger().error("配置文件路径：" + configPath.toAbsolutePath());
            try {
                config = new XinCmdConfig(configPath);
                config.saveConfig();
                getLogger().info("已创建默认配置文件：" + configPath.toAbsolutePath());
            } catch (Exception ex) {
                getLogger().error("无法创建默认配置文件：" + ex.getMessage());
                throw new RuntimeException("无法创建默认配置文件：" + ex.getMessage(), ex);
            }
        }
    }

    // 处理私聊消息事件 - 监听远程命令
    @EventHandler
    public void onPrivateMessage(PrivateChatEvent event) {
        try {
            // 检查远程命令是否启用
            if (!config.isRemoteCommandEnabled()) {
                return; // 远程命令已禁用，忽略
            }

            String playerName = event.getSender().getName();
            String message = event.getMessage().trim();

            // 检查玩家是否是管理员（只有管理员才能执行远程命令）
            if (!config.isAdministrator(playerName)) {
                return; // 非管理员，忽略
            }

            // 检查消息是否以命令关键字开头
            String commandPrefix = null;
            if (message.startsWith("#command xrcmd ")) {
                commandPrefix = "#command xrcmd ";
            } else if (message.startsWith("#cmd xrcmd ")) {
                commandPrefix = "#cmd xrcmd ";
            }

            if (commandPrefix != null) {
                // 提取命令部分
                String command = message.substring(commandPrefix.length());
                getLogger().info("收到来自管理员 " + playerName + " 的远程命令：" + command);

                // 异步执行命令，避免阻塞事件线程
                CompletableFuture.runAsync(() -> {
                    executeRemoteCommand(playerName, command);
                });
            }
        } catch (Exception e) {
            getLogger().error("处理私聊消息时发生错误：" + e.getMessage());
        }
    }

    // 执行远程命令并返回结果
    private void executeRemoteCommand(String playerName, String command) {
        List<String> output = new ArrayList<>();

        try {
            // 执行命令
            String[] args = command.split("\\s+");
            if (args.length > 0) {
                RemoteCommandExecutor executor = new RemoteCommandExecutor();
                List<String> commandOutput = executor.onCommandWithOutput(new RemoteCommand(), "xrcmd", args);
                output.addAll(commandOutput);
            } else {
                output.add("错误：命令格式不正确");
            }

        } catch (Exception e) {
            output.add("错误：执行命令时发生异常：" + e.getMessage());
            getLogger().error("执行远程命令时发生错误：" + e.getMessage());
        }

        // 通过私聊发送所有输出结果给管理员
        sendCommandResultsToAdmin(playerName, output);
    }

    // 通过私聊发送命令结果给管理员
    private void sendCommandResultsToAdmin(String playerName, List<String> results) {
        if (results.isEmpty()) {
            return;
        }

        new Thread(() -> {
            try {
                for (String line : results) {
                    if (!line.trim().isEmpty()) {
                        // 添加延迟，避免消息发送过快
                        Thread.sleep(200);
                        Bot.Instance.sendCommand("msg " + playerName + " " + line);
                    }
                }
                getLogger().info("已向管理员 " + playerName + " 发送命令执行结果");
            } catch (Exception e) {
                getLogger().error("向管理员发送命令结果失败：" + e.getMessage());
            }
        }).start();
    }

    public XinCmdConfig getConfig() {
        return config;
    }

    // 命令方法
    public void cmdReload() {
        try {
            config.loadConfig();
            getLogger().info("配置文件已重载");
        } catch (Exception e) {
            getLogger().error("重载配置文件失败：" + e.getMessage());
        }
    }

    public void cmdAddAdministrator(String playerName) {
        config.addAdministrator(playerName);
        try {
            config.saveConfig();
            getLogger().info("已将玩家 " + playerName + " 添加到管理员列表");
        } catch (Exception e) {
            getLogger().error("保存配置文件失败：" + e.getMessage());
        }
    }

    public void cmdRemoveAdministrator(String playerName) {
        config.removeAdministrator(playerName);
        try {
            config.saveConfig();
            getLogger().info("已将玩家 " + playerName + " 从管理员列表中移除");
        } catch (Exception e) {
            getLogger().error("保存配置文件失败：" + e.getMessage());
        }
    }

    public void cmdListAdministrators() {
        List<String> admins = config.getAdministrators();
        if (admins.isEmpty()) {
            getLogger().info("管理员列表为空");
        } else {
            getLogger().info("管理员列表：" + String.join(", ", admins));
        }
    }
}
