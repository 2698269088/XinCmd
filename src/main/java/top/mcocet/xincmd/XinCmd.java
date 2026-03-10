package top.mcocet.xincmd;

import xin.bbtt.mcbot.Bot;
import xin.bbtt.mcbot.event.EventHandler;
import xin.bbtt.mcbot.event.Listener;
import xin.bbtt.mcbot.plugin.Plugin;
import xin.bbtt.mcbot.events.PrivateChatEvent;

import top.mcocet.xincmd.config.XinCmdConfig;
import top.mcocet.xincmd.command.RemoteCommand;
import top.mcocet.xincmd.command.RemoteCommandExecutor;
import top.mcocet.xincmd.service.CommandProcessor;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class XinCmd implements Plugin, Listener {
    public static XinCmd INSTANCE;

    private XinCmdConfig config;
    private final Path configPath = Paths.get("plugin", "XinCmd", "config.json");
    private final Path logPath = Paths.get("plugin", "XinCmd", "remote_command.log");
    private CommandProcessor commandProcessor;

    public XinCmd() {
        INSTANCE = this;
        commandProcessor = new CommandProcessor();
    }

    @Override
    public String getName() {
        return ("XinCmd");
    }

    @Override
    public String getVersion() {
        return "1.2-SNAPSHOT";
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
            String commandType = null; // 用于区分 xrcmd 和普通命令
                        
            if (message.startsWith("#command xrcmd ")) {
                commandPrefix = "#command xrcmd ";
                commandType = "xrcmd";
            } else if (message.startsWith("#cmd xrcmd ")) {
                commandPrefix = "#cmd xrcmd ";
                commandType = "xrcmd";
            } else if (message.startsWith("#command ")) {
                commandPrefix = "#command ";
                commandType = "normal";
            } else if (message.startsWith("#cmd ")) {
                commandPrefix = "#cmd ";
                commandType = "normal";
            }

            if (commandPrefix != null) {
                // 提取命令部分
                String command = message.substring(commandPrefix.length());
                getLogger().info("收到来自管理员 " + playerName + " 的远程命令：" + command);
            
                // 记录日志
                logRemoteCommand(playerName, command);
            
                // 安全检查：检测是否包含 admin 命令关键字
               if (containsAdminCommand(command)) {
                    // 如果启用了 admin 功能，需要验证玩家是否在管理员列表中
                   if (!config.isRemoteCommandAdminEnabled()) {
                        getLogger().warn("检测到尝试执行 admin 命令但未启用 admin 功能：" + command);
                        getLogger().warn("已阻止该命令的执行。如需使用 admin 功能，请在配置文件中设置 remoteCommandAdminEnabled: true");
                       return;
                    }
                }
            
                // 异步执行命令，避免阻塞事件线程
                final String finalCommandType = commandType;
                CompletableFuture.runAsync(() -> {
                   if ("xrcmd".equals(finalCommandType)) {
                        executeRemoteCommand(playerName, command);
                    } else {
                        executeConsoleCommand(command);
                    }
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

    // 执行普通命令（以控制台身份执行，用于 #command 和 #cmd 前缀）
    private void executeConsoleCommand(String command) {
        try {
            getLogger().info("通过控制台执行命令：" + command);
            
            // 使用 CommandProcessor 以控制台模式执行命令
            // 这会调用 Bot.Instance.executeCommand() 在本地控制台执行
            List<String> result = commandProcessor.executeCommand(command, true);
            
            for (String line : result) {
                getLogger().info(line);
            }
        } catch (Exception e) {
            getLogger().error("执行命令时发生错误：" + e.getMessage());
        }
    }
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
                        // 使用 xinbot 的 msg 命令发送私聊消息
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
    
    /**
     * 检查命令是否包含 admin 关键字
     * @param command 要检查的命令
     * @return 如果包含 admin 相关关键字返回 true，否则返回 false
     */
    private boolean containsAdminCommand(String command) {
       if (command == null || command.trim().isEmpty()) {
           return false;
        }
        
        String lowerCaseCommand = command.toLowerCase().trim();
        
        // 将命令按空格分割成单词数组
        String[] parts = lowerCaseCommand.split("\\s+");
        
        // 如果只有一个词，检查是否是 "admin"
       if (parts.length == 1) {
           return parts[0].equals("admin");
        }
        
        // 如果有多个词，检查前缀 + admin 的组合
       if (parts.length >= 2) {
            String firstWord = parts[0];
            String secondWord = parts[1];
            
            // 检查是否是以下格式：
            // - xrc admin ...
            // - xrcmd admin ...
            // - xinremote admin ...
            // - admin ... (单独的 admin 命令)
           if ((firstWord.equals("xrc") || firstWord.equals("xrcmd") || 
                 firstWord.equals("xinremote")) && secondWord.equals("admin")) {
               return true;
            }
            
            // 检查是否以 admin 开头
           if (firstWord.equals("admin")) {
               return true;
            }
        }
        
       return false;
    }
    
    /**
     * 记录远程命令到日志文件
     * @param playerName 玩家名称
     * @param command 执行的命令
     */
    private void logRemoteCommand(String playerName, String command) {
        CompletableFuture.runAsync(() -> {
            try {
                // 确保日志目录存在
               if (Files.notExists(logPath.getParent())) {
                    Files.createDirectories(logPath.getParent());
                }
                
                // 格式化时间戳
                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter formatter= DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String timestamp = now.format(formatter);
                
                // 构建日志条目
                String logEntry = String.format("[%s] 玩家：%s | 命令：%s%n", timestamp, playerName, command);
                
                // 追加到日志文件
                Files.writeString(logPath, logEntry, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                
            } catch (Exception e) {
                getLogger().error("记录远程命令日志失败：" + e.getMessage(), e);
            }
        });
    }
}
