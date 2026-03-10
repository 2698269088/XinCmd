package top.mcocet.xincmd.service;

import xin.bbtt.mcbot.Bot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 命令处理器 - 处理通过 xinbot 发送的命令
 * 
 * 支持两种执行模式：
 * 1. 控制台模式：使用 CommandManager.callCommand() 在本地控制台执行
 * 2. 服务器模式：使用 Bot.Instance.sendCommand() 发送到游戏服务器执行
 */
public class CommandProcessor {
    private static final Logger log = LoggerFactory.getLogger("XinCmdCommandProcessor");
    
    /**
     * 向游戏服务器发送命令
     * 
     * 注意：此方法会将命令直接发送到游戏服务器控制台。
     * 这是预期行为，因为大部分命令（如 say, op, give 等）都需要在服务器上执行。
     * 
     * @param command 要执行的命令（不包含前缀）
     * @return 命令执行结果（如果有的话）
     */
    /**
     * 向游戏服务器发送命令（旧方法，保持兼容性）
     * 
     * 注意：此方法会将命令直接发送到游戏服务器控制台。
     * 这是预期行为，因为大部分命令（如 say, op, give 等）都需要在服务器上执行。
     * 
     * @param command 要执行的命令（不包含前缀）
     * @return 命令执行结果（如果有的话）
     * @deprecated 请使用 executeCommand(String command, boolean consoleMode)
     */
    @Deprecated
   public List<String> executeCommand(String command) {
       return executeCommand(command, false);
   }
    
    /**
     * 执行命令
     * 
     * @param command 要执行的命令（不包含前缀）
     * @param consoleMode true=控制台模式（使用 CommandManager.callCommand），false=服务器模式（使用 Bot.sendCommand）
     * @return 命令执行结果（如果有的话）
     */
   public List<String> executeCommand(String command, boolean consoleMode) {
       List<String> results = new ArrayList<>();
        
        try {
            if (consoleMode) {
                // 控制台模式：使用 CommandManager.callCommand() 在本地执行
                log.info("通过控制台执行命令：" + command);
                
                // 直接调用 CommandManager 的 callCommand 方法
                // 这会立即执行已注册的命令（包括 xinbot 内置命令和插件命令）
                Bot.Instance.getPluginManager().commands().callCommand(command);
                results.add("命令已在控制台执行：" + command);
                log.info("命令已在控制台执行：{}", command);
            } else {
                // 服务器模式：使用 Bot.Instance.sendCommand() 发送到游戏服务器
                log.info("向服务器发送命令：" + command);
                
                Bot.Instance.sendCommand(command);
                results.add("命令已发送到服务器：" + command);
                log.info("命令已发送：{}", command);
            }
            
        } catch (Exception e) {
            String errorMsg = "执行命令时发生异常：" + e.getMessage();
            results.add(errorMsg);
            log.error("执行命令时发生错误：" + e.getMessage(), e);
        }
        
        return results;
   }
    
    /**
     * 异步执行命令（默认服务器模式）
     * @param command 要执行的命令
     * @param callback 回调函数，接收执行结果
     * @deprecated 请使用 executeCommandAsync(String command, boolean consoleMode, Consumer)
     */
    @Deprecated
    public void executeCommandAsync(String command, java.util.function.Consumer<List<String>> callback) {
        executeCommandAsync(command, false, callback);
    }
    
    /**
     * 异步执行命令
     * @param command 要执行的命令
     * @param consoleMode true=控制台模式，false=服务器模式
     * @param callback 回调函数，接收执行结果
     */
    public void executeCommandAsync(String command, boolean consoleMode, java.util.function.Consumer<List<String>> callback) {
        CompletableFuture.runAsync(() -> {
            List<String> result = executeCommand(command, consoleMode);
            callback.accept(result);
        });
    }
    
    /**
     * 批量执行命令
     * @param commands 命令列表
     * @return 所有命令的执行结果
     */
    public List<String> executeCommands(List<String> commands) {
        List<String> allResults = new ArrayList<>();
        
        for (String command : commands) {
            if (command != null && !command.trim().isEmpty()) {
                List<String> result = executeCommand(command);
                allResults.addAll(result);
                
                // 添加小延迟，避免命令发送过快
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("命令执行被中断");
                    break;
                }
            }
        }
        
        return allResults;
    }
}
