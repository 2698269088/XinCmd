# XinCmd - 远程命令管理插件

## 简介

XinCmd 是一个基于 xinbot 框架的 Minecraft 机器人插件，允许管理员通过私聊消息远程控制机器人。

## 功能特性

- ✅ 通过私聊发送远程命令
- ✅ 管理员权限管理
- ✅ 异步命令执行，不阻塞主线程
- ✅ 自动返回命令执行结果
- ✅ 支持多个命令前缀

## 安装方法

1. 将编译好的 jar 文件放入 xinbot 的 `plugins` 目录
2. 启动 xinbot，插件会自动加载并生成配置文件
3. 配置文件位置：`plugin/XinCmd/config.json`

## 配置说明

配置文件 `config.json` 包含以下选项：

```json
{
  "administrators": [],
  // 管理员列表，可以执行远程命令的玩家
  "remoteCommandEnabled": true,
  // 是否启用远程命令功能
  "remoteCommandAdminEnabled": false
  //   // 是否启用远程命令的 admin 功能（启用后允许通过远程命令执行 admin add 操作添加管理员，否则不允许执行 admin 相关命令） 
}
```

### 配置项说明

- **administrators**: 管理员玩家列表，只有在 `remoteCommandAdminEnabled` 为 `true` 时才需要设置
- **remoteCommandEnabled**: 设置为 `false` 可以完全禁用远程命令功能
- **remoteCommandAdminEnabled**: 
  - `false` (默认): 任何私聊机器人都可以执行远程命令
  - `true`: 只有 administrators 列表中的玩家可以执行远程命令

## 使用方法

### 远程命令格式

通过私聊向机器人发送以下格式的消息：

```
#command xrcmd <子命令> [参数]
或
#cmd xrcmd <子命令> [参数]
```

### 可用的远程命令

#### 基本命令

- `/xrcmd reload` - 重载配置文件
- `/xrcmd help` - 显示帮助信息

#### 管理员管理命令

需要先设置 `remoteCommandAdminEnabled: true` 才能使用这些命令：

- `/xrcmd admin add <玩家名>` - 添加玩家到管理员列表
- `/xrcmd admin remove <玩家名>` - 从管理员列表移除玩家
- `/xrcmd admin list` - 列出所有管理员

### 使用示例

1. **重载配置**
   ```
   #command xrcmd reload
   ```

2. **添加管理员** (需要先启用 admin 功能)
   ```
   #command xrcmd admin add Steve
   ```

3. **查看管理员列表**
   ```
   #cmd xrcmd admin list
   ```

4. **移除管理员**
   ```
   #command xrcmd admin remove Alex
   ```

## 控制台命令

你也可以在服务器控制台直接使用命令（不需要前缀）：

```
/xrcmd reload
/xrcmd admin add <玩家名>
/xrcmd admin remove <玩家名>
/xrcmd admin list
```

## 安全建议

1. **生产环境建议启用 admin 检查**
   - 设置 `remoteCommandAdminEnabled: true`
   - 在 `administrators` 列表中只添加信任的管理员

2. **定期备份配置文件**
   - 配置文件位于 `plugin/XinCmd/config.json`

3. **监控日志**
   - 所有远程命令执行都会记录在日志中
   - 可以通过日志追踪谁执行了什么命令

## 命令执行流程

1. 玩家私聊发送 `#command xrcmd <命令>`
2. 插件检查远程命令是否启用
3. 如果启用了 admin 检查，验证玩家是否在管理员列表
4. 异步执行命令
5. 将执行结果通过私聊发送给玩家

## 技术细节

- **异步执行**: 所有远程命令都在独立线程中执行，不会阻塞主线程
- **线程安全**: 使用同步机制确保配置文件操作的安全性
- **错误处理**: 完善的异常处理和日志记录
- **自动恢复**: 命令执行失败不会影响插件的正常运行

## 与 XinPga 的区别

XinCmd 是从 XinPga 插件的远程命令功能独立出来的专门插件，专注于提供简洁的远程命令管理功能：

- ✅ 更轻量：只包含远程命令相关功能
- ✅ 更专注：不涉及定时发送等复杂功能
- ✅ 更易用：简化的配置和命令系统
- ✅ 模块化：可以作为其他插件的参考模板

## 开发信息

- **项目结构**: Maven标准目录结构
- **Java 版本**: Java 17+
- **依赖库**: 
  - xinbot 1.12.1-RELEASE
  - Gson 2.10.1

## 许可证

本项目继承自 XinPga，遵循相同的开源协议。

## 问题反馈

如有问题或建议，请提交 Issue 或 Pull Request。
