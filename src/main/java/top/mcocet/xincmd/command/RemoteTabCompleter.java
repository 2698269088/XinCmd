package top.mcocet.xincmd.command;

import xin.bbtt.mcbot.command.Command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RemoteTabCompleter {
    public List<String> getCompletions(Command cmd, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("reload");
            completions.add("admin");
            completions.add("help");
            return completions;
        }
        
        if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            completions.add("add");
            completions.add("remove");
            completions.add("list");
            return completions;
        }
        
        return completions;
    }
}
