package top.mcocet.xincmd.command;

import xin.bbtt.mcbot.command.Command;

public class RemoteCommand extends Command {
    @Override
    public String getName() {
        return "xrcmd";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"xrcmd", "xrc",  "xinremote"};
    }

    @Override
    public String getDescription() {
        return "XinCmd 远程命令管理";
    }

    @Override
    public String getUsage() {
        return "/xrcmd reload|admin add <玩家名>|admin remove <玩家名>|admin list|help";
    }
}
