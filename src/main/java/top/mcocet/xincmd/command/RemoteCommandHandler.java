package top.mcocet.xincmd.command;

import xin.bbtt.mcbot.command.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import top.mcocet.xincmd.XinCmd;
import top.mcocet.xincmd.service.CommandProcessor;

public class RemoteCommandHandler {
    private static final Logger log = LoggerFactory.getLogger("XinCmdRemoteCommandHandler");
    private final CommandProcessor commandProcessor;

    public RemoteCommandHandler() {
        this.commandProcessor = new CommandProcessor();
    }

    public void handleCommand(Command cmd, String label, String[] args) {
        switch (args[0].toLowerCase()) {
            case "reload" -> XinCmd.INSTANCE.cmdReload();
            case "admin" -> handleAdminCommand(args);
            case "help" -> showHelp(cmd);
            case "exec" -> handleExecCommand(args);
          default -> log.warn("未知子命令：" + args[0] + "！用法：" + cmd.getUsage());
        }
    }

    private void handleAdminCommand(String[] args) {
        // 注意：这个方法只被控制台命令调用，不应该检查 remoteCommandAdminEnabled
        // remoteCommandAdminEnabled 只应该限制远程命令中的 admin add 操作，不限制控制台命令

        if (args.length < 2) {
            log.info("用法：/xrcmd admin add <玩家名> | remove <玩家名> | list");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "add" -> {
                if (args.length < 3) {
                    log.info("用法：/xrcmd admin add <玩家名>");
                    return;
                }
                XinCmd.INSTANCE.cmdAddAdministrator(args[2]);
            }
            case "remove" -> {
                if (args.length < 3) {
                    log.info("用法：/xrcmd admin remove <玩家名>");
                    return;
                }
                XinCmd.INSTANCE.cmdRemoveAdministrator(args[2]);
            }
            case "list" -> XinCmd.INSTANCE.cmdListAdministrators();
          default -> log.warn("未知的管理员子命令！用法：/xrcmd admin add <玩家名> | remove <玩家名> | list");
        }
    }

    // 处理 exec 命令 - 以控制台模式执行其他命令
    private void handleExecCommand(String[] args) {
        if (args.length < 2) {
            log.info("用法：/xrcmd exec <命令> [参数...]");
            log.info("示例：/xrcmd exec say hello world");
            return;
        }
        
        // 提取要执行的命令（从第二个参数开始）
        StringBuilder commandBuilder= new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 0) {
                commandBuilder.append(" ");
            }
            commandBuilder.append(args[i]);
        }
        
        String command = commandBuilder.toString();
        log.info("准备以控制台模式执行命令：" + command);
        
        // 使用 CommandProcessor 以控制台模式执行命令
        commandProcessor.executeCommandAsync(command, true, results -> {
            for (String result : results) {
                log.info(result);
            }
        });
    }

    // 显示帮助信息
    private void showHelp(Command cmd) {
        log.info("=== XinCmd 插件远程命令帮助 ===");
        log.info("#command xrcmd reload - 重载配置文件");
        log.info("#command xrcmd admin add <玩家名> - 添加玩家到管理员列表");
        log.info("#command xrcmd admin remove <玩家名> - 从管理员列表移除玩家");
        log.info("#command xrcmd admin list - 列出管理员");
        log.info("#command xrcmd exec <命令> [参数...] - 通过 xinbot 执行其他命令");
        log.info("#command xrcmd help - 显示此帮助信息");
        log.info("==============================");
    }
}
