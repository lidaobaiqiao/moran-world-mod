# 墨世界 · 常用命令速查

> 在项目目录 `moran-world-mod-master/moran-world-mod-master` 下执行。
> 使用 JDK 17；项目的 `gradle.properties` 没有固定本机 JDK 路径。

## 一、启动 / 构建（终端命令）

| 你想干什么 | 命令 |
|---|---|
| **启动游戏（最常用）** | gradlew runClient |
| 启动专用服务器（测试刷怪/维度） | `./gradlew runServer` |
| 生成数据包资源（掉落表等，新方块后跑一次） | `./gradlew runDatagen` |
| 打包 jar（发布/装进正式客户端） | `./gradlew build` |
| 清理构建产物（构建玄学问题时先来这个） | `./gradlew clean` |

**终端差异**：
- Git Bash（我平时用的）→ `./gradlew xxx`
- PowerShell / CMD → `.\gradlew.bat xxx`

**jar 产物位置**：`build/libs/` 下的 `moran-mod-x.x.x.jar`（不带 `-sources` 的那个）。

**日志位置**（游戏崩溃先看这个）：`run/logs/latest.log`

## 二、游戏内命令

| 你想干什么 | 命令 |
|---|---|
| 传送到桃花源维度 | `/peach` |
| 离开桃花源 | `/exit` |
| 召唤墨灵 | `/summon moran_mod:moling` |
| 给自己墨灵生成蛋 | `/give @s moran_mod:moling_spawn_egg` |
| 查看当前群系 | 按 F3 看左上角 "Biome" |

## 三、Git 推送（需要 VPN）

这台电脑直连 GitHub 不通，推送前先开 VPN，然后：

```bash
git add -A
git commit -m "写点改动说明"
git push
```

如果远端有新提交（比如你在别的机器推过），先同步再推：

```bash
git fetch origin && git reset origin/master
```

> 注意：`reset origin/master` 会用远端覆盖本地提交记录，仅在确认远端是最新时用。

## 四、日常开发节奏（备忘）

1. 改完代码 → `./gradlew build` 确认能编译
2. `./gradlew runClient` 进游戏验证
3. 提交推送（见上）
4. 新增方块后记得 `./gradlew runDatagen` 补掉落表，生成的文件要一起提交
