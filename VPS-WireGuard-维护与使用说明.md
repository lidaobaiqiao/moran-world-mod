# Vultr VPS + WireGuard 维护与使用说明

## 1. 用途

这台 Vultr VPS 用作两个人日常上网的自建 VPN 出口。客户端使用 WireGuard，连接后设备的网络流量通过 VPS 访问互联网。

适用场景：网页浏览、观看视频、查阅文献等日常使用。

## 2. 当前服务器信息

| 项目 | 当前值 |
|---|---|
| 服务商 | Vultr |
| 地区 | Tokyo，日本 |
| 公网 IPv4 | `202.182.112.19` |
| 操作系统 | Ubuntu 26.04.1 LTS x64 |
| CPU | 1 vCPU |
| 内存 | 1 GB |
| 磁盘 | 25 GB SSD |
| SSH 用户 | `admin` |
| SSH 端口 | `22/tcp` |
| WireGuard 接口 | `wg0` |
| WireGuard 端口 | `51820/udp` |
| VPN 网段 | `10.8.0.0/24` |
| 外网网卡 | `enp1s0` |

不要把密码、服务端私钥、客户端私钥或完整 `.conf` 文件写入文档或公开发送。

## 3. 客户端分配

| 配置文件 | VPN 地址 | 用途 |
|---|---|---|
| `client1.conf` | `10.8.0.2/32` | 主设备 |
| `client2.conf` | `10.8.0.3/32` | 第二台设备或朋友的设备 |

每个配置文件只能给一台设备使用。不要把同一个配置同时导入两台设备，否则两个设备会争用同一个 WireGuard 地址。

当前客户端为全局模式：

```ini
AllowedIPs = 0.0.0.0/0
DNS = 1.1.1.1
```

## 4. 日常使用

### Windows

1. 打开 WireGuard。
2. 导入对应的 `.conf` 文件。
3. 关闭其他会接管全局流量的 VPN/TUN 软件，避免路由冲突。
4. 点击 WireGuard 隧道的“连接”。
5. 用下面的命令确认出口 IP：

```powershell
curl.exe -4 https://ifconfig.me
```

返回 `202.182.112.19`，说明流量已经通过 VPS。

不用时可以在 WireGuard 中断开隧道。断开客户端不会关闭 VPS。

### Android / iPhone

1. 安装官方 WireGuard 应用。
2. 将对应的 `.conf` 文件安全地传到手机，或使用二维码导入。
3. 在 WireGuard 中选择“从文件导入”或“扫描二维码”。
4. 允许系统添加 VPN 配置。
5. 打开隧道开关。

手机和电脑不要同时使用同一个客户端配置；手机应使用独立的客户端配置。

## 5. 登录 VPS

在 Windows PowerShell 中：

```powershell
ssh admin@202.182.112.19
```

需要 root 权限时：

```bash
sudo -i
```

不要在普通 Windows PowerShell 中执行 Linux 的 `sudo`、`ufw` 或 `wg` 命令；这些命令应在 SSH 登录后的 VPS 终端中执行。

## 6. 日常检查

查看 WireGuard 状态：

```bash
sudo wg show
```

正常连接的客户端会显示 `latest handshake`、`endpoint` 和流量统计。

查看服务状态：

```bash
sudo systemctl status wg-quick@wg0 --no-pager
```

查看防火墙：

```bash
sudo ufw status verbose
```

查看资源：

```bash
free -h
swapon --show
df -h /
```

查看 SSH、Fail2ban：

```bash
sudo systemctl is-active ssh
sudo systemctl is-active fail2ban
```

## 7. 更新与重启

建议偶尔登录服务器更新系统：

```bash
sudo apt update
sudo apt full-upgrade -y
```

如果系统提示需要重启：

```bash
sudo reboot
```

重启后等待一两分钟，再重新连接 WireGuard。维护服务器时，最好先关闭客户端 VPN 隧道，并保留一条直连 SSH 会话，避免路由变化导致失联。

## 8. WireGuard 服务操作

重启 WireGuard：

```bash
sudo systemctl restart wg-quick@wg0
```

停止 WireGuard：

```bash
sudo systemctl stop wg-quick@wg0
```

启动 WireGuard：

```bash
sudo systemctl start wg-quick@wg0
```

设置开机自动启动：

```bash
sudo systemctl enable wg-quick@wg0
```

配置文件位置：

```text
/etc/wireguard/wg0.conf
```

不要直接把该文件内容发到聊天或公开平台，因为它包含服务端私钥和客户端公钥信息。

## 9. 常见故障排查

### 客户端显示已连接，但没有网络

先确认：

```powershell
curl.exe -4 https://ifconfig.me
```

如果失败：

1. 关闭其他 VPN/TUN 软件。
2. 确认 WireGuard 使用的是最新的全局配置，包含 `AllowedIPs = 0.0.0.0/0`。
3. 断开后重新连接隧道。
4. 在 VPS 检查：

```bash
sudo wg show
sudo iptables -L FORWARD -n -v
sudo iptables -t nat -L POSTROUTING -n -v
```

### 隧道本身是否连通

客户端连接后测试：

```powershell
ping 10.8.0.1
```

能收到回复说明 WireGuard 隧道本身正常；不能收到回复时，重点检查客户端是否真的处于“已连接”状态以及服务端 `sudo wg show` 是否有 `latest handshake`。

### SSH 进不去

先关闭客户端 WireGuard 隧道，使用直连 SSH：

```powershell
ssh admin@202.182.112.19
```

如果服务器确实无法通过网络连接，可使用 Vultr 控制台中的 Web Console 进入服务器检查服务。

## 10. Vultr 计费与关机

不用 VPN 时，只需关闭各设备上的 WireGuard 隧道，VPS 可以保持运行。

Vultr 的服务器即使处于停止状态，通常仍会继续按小时计费；只有销毁服务器才会停止服务器费用，但销毁会永久删除服务器及其数据。因此不要为了“省一点不用时的费用”随意点击“停止”或“销毁”。

## 11. 安全注意事项

- SSH 只开放 `22/tcp`，WireGuard 只开放 `51820/udp`。
- 不要公开分享密码、私钥、完整 `.conf` 文件或二维码。
- `client2.conf` 发给朋友时应通过私聊或安全文件传输发送。
- 如果某个配置文件泄露、丢失或发错人，应立即删除对应服务端 peer 并生成新配置。
- 本次配置过程中 `client1` 私钥曾被粘贴到聊天中；该密钥应视为已泄露，后续应重新生成 `client1` 配置并替换服务端 peer。
- VPS 是单节点出口，服务器故障、IP 被网站限制或流量耗尽时，所有客户端都会受影响。
