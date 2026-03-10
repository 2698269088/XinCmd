package top.mcocet.xincmd.command;

import xin.bbtt.mcbot.command.Command;
import xin.bbtt.mcbot.command.TabExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import top.mcocet.xincmd.XinCmd;
import top.mcocet.xincmd.service.CommandProcessor;

public class RemoteCommandExecutor extends TabExecutor {
    private static final Logger log = LoggerFactory.getLogger("XinCmdRemoteCommandExecutor");

    @Override
    public void onCommand(Command cmd, String label, String[] args) {
        if (args.length == 0) {
            log.info("用法：" + cmd.getUsage());
            return;
        }

        RemoteCommandHandler handler = new RemoteCommandHandler();
        handler.handleCommand(cmd, label, args);
    }

    @Override
    public List<String> onTabComplete(Command cmd, String label, String[] args) {
        RemoteTabCompleter completer = new RemoteTabCompleter();
        return completer.getCompletions(cmd, label, args);
    }

    public List<String> onCommandWithOutput(Command cmd, String label, String[] args) {
        List<String> output = new ArrayList<>();

        if (args.length == 0) {
            output.add("用法：" + cmd.getUsage());
            return output;
        }

        try {
            switch (args[0].toLowerCase()) {
                case "reload" -> {
                    XinCmd.INSTANCE.cmdReload();
                    output.add("信息：配置文件已重载");
                }
                case "admin" -> handleAdminCommandWithOutput(args, output);
                case "help" -> output.addAll(showHelpOutput());
                case "exec" -> handleExecCommandWithOutput(args, output);
               default -> output.add("未知子命令：" + args[0] + "！请使用 /xrcmd help 查看帮助");
            }
        } catch (Exception e) {
            output.add("错误：执行命令时发生异常：" + e.getMessage());
            log.error("执行命令时发生错误", e);
        }

        return output;
    }

    // 处理 admin 命令
    private void handleAdminCommandWithOutput(String[] args, List<String> output) {

        if (args.length < 2) {
            output.add("用法：/xrcmd admin add <玩家名> | remove <玩家名> | list");
            return;
        }

        // 检查是否是添加管理员的命令
        if (args[1].equalsIgnoreCase("add")) {
            // 检查远程命令的 admin 功能是否启用
            if (!XinCmd.INSTANCE.getConfig().isRemoteCommandAdminEnabled()) {
                output.add("错误：远程命令的 admin 功能已被禁用，无法添加管理员");
                return;
            }
        }

        switch (args[1].toLowerCase()) {
            case "add" -> {
                if (args.length < 3) {
                    output.add("用法：/xrcmd admin add <玩家名>");
                } else {
                    XinCmd.INSTANCE.cmdAddAdministrator(args[2]);
                    output.add("信息：已将玩家 " + args[2] + " 添加到管理员列表");
                }
            }
            case "remove" -> {
                if (args.length < 3) {
                    output.add("用法：/xrcmd admin remove <玩家名>");
                } else {
                    XinCmd.INSTANCE.cmdRemoveAdministrator(args[2]);
                    output.add("信息：已将玩家 " + args[2] + " 从管理员列表中移除");
                }
            }
            case "list" -> {
                List<String> admins = XinCmd.INSTANCE.getConfig().getAdministrators();
                if (admins.isEmpty()) {
                    output.add("信息：管理员列表为空");
                } else {
                    output.add("信息：管理员列表 (" + admins.size() + " 个管理员):");
                    output.add(String.join(", ", admins));
                }
            }
           default -> output.add("错误：未知的管理员子命令！用法：/xrcmd admin add <玩家名> | remove <玩家名> | list");
        }
    }

    // 处理 exec 命令 - 通过 xinbot 执行其他命令
    private void handleExecCommandWithOutput(String[] args, List<String> output) {
        if (args.length < 2) {
            output.add("用法：/xrcmd exec <命令> [参数...]");
            output.add("示例：/xrcmd exec say hello world");
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
        output.add("准备以控制台模式执行命令：" + command);
        
        // 创建 CommandProcessor 并以控制台模式执行命令
        CommandProcessor processor = new CommandProcessor();
        List<String> result = processor.executeCommand(command, true);
        output.addAll(result);
    }

    // 显示帮助信息
    private List<String> showHelpOutput() {
        List<String> output = new ArrayList<>();
        output.add("=== XinCmd 插件远程命令帮助 ===");
        output.add("#command xrcmd reload - 重载配置文件");
        output.add("#command xrcmd admin add <玩家名> - 添加玩家到管理员列表");
        output.add("#command xrcmd admin remove <玩家名> - 从管理员列表移除玩家");
        output.add("#command xrcmd admin list - 列出管理员");
        output.add("#command xrcmd exec <命令> [参数...] - 通过 xinbot 执行其他命令");
        output.add("#command xrcmd help - 显示此帮助信息");
        output.add("==============================");
        return output;
    }
}
