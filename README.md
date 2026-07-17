# 轻帆集 / Quiet Fleet Collection

轻帆集是一个项目资料展示与管理系统，包含一个 Spring Boot 后端、一个主站前端和一个后台前端。

## 项目结构

- `server/`：Java 8 + Spring Boot 2.7 后端服务。
- `web/`：Vue 3 + Vite 主站前端。
- `admin-web/`：Vue 3 + Vite + Element Plus 后台前端。
- `config/`：本机数据库和运行配置。
- `scripts/`：Windows 本地初始化、重启、停止和日志整理脚本。
- `docs/`：设计、开发和进度文档。

## 环境要求

- JDK 8
- Maven 3.x
- Node.js 和 npm
- MySQL 5.7 或兼容版本

默认端口：

```text
后端: 8081
主站前端: 5173
后台前端: 5174
```

## 快速开始

请先进入项目根目录，再执行以下命令。

### 1. 初始化开发环境

首次拉取项目后，推荐双击运行：

```text
scripts\setup-dev.bat
```

也可以在终端执行：

```cmd
powershell -NoProfile -ExecutionPolicy Bypass -File ".\scripts\setup-dev.ps1"
```

初始化脚本会：

- 检查 Java、Maven、Node.js、npm 和 MySQL。
- 安装 `web/` 和 `admin-web/` 的 npm 依赖。
- 创建本地 `logs/` 和 `storage/` 目录。
- 缺少数据库配置时，生成 `config/application.yml`，并交互创建唯一的 MySQL 配置 `config/mysql.env`。
- 检查项目所需的 4 个数据库和关键表。
- 发现数据库或表缺失时，询问是否执行建表和初始数据脚本。

MySQL 密码使用隐藏输入。`config/application.yml` 和 `config/mysql.env` 已被 `.gitignore` 忽略，不要提交。`mysql.env` 可以配置本机或远程 MySQL。

### 2. 重启项目

日常开发推荐双击：

```text
scripts\start-dev.bat
```

虽然文件名保留为 `start-dev`，实际行为是重启全部开发服务：

1. 检查后端、主站和后台是否正在运行。
2. 停止当前项目占用 `8081`、`5173`、`5174` 的旧进程。
3. 重新启动三个服务。
4. 等待端口开始监听，并输出成功或失败结果。

启动后，同一局域网内的用户应通过运行服务这台电脑的内网 IPv4 地址访问。当前 WLAN 内网地址示例为 `192.168.31.189`：

```text
主站: http://192.168.31.189:5173/
后台: http://192.168.31.189:5174/
后端: http://192.168.31.189:8081
```

内网 IP 可能因 DHCP 重新分配而变化，可以执行以下命令查看当前地址：

```powershell
ipconfig
```

找到当前使用的以太网或 WLAN 网卡对应的“IPv4 地址”，然后使用：

```text
主站: http://<本机内网IP>:5173/
后台: http://<本机内网IP>:5174/
后端: http://<本机内网IP>:8081
```

`127.0.0.1` 只能由运行服务的电脑自己访问。其他内网用户无法访问时，需要确认双方处于同一局域网，并允许 Windows 防火墙放行 TCP `5173`、`5174`、`8081` 端口。管理后台和后端接口仍需要依赖项目自身的登录与权限校验，不能把内网环境视为安全边界。

### 3. 停止项目

双击运行：

```text
scripts\stop-dev.bat
```

停止脚本只会结束当前仓库的后端、主站和后台进程。如果端口被其他程序占用，脚本会报错并拒绝误杀。

### 4. 手工初始化数据库

如果不使用 `setup-dev.bat`，可以按下面的步骤手工初始化。

复制配置模板：

```cmd
copy "config\application.yml.example" "config\application.yml"
copy "config\mysql.env.example" "config\mysql.env"
```

只需要修改 `config/mysql.env` 中的 MySQL 连接信息：

```dotenv
QFC_MYSQL_HOST=127.0.0.1
QFC_MYSQL_PORT=3306
QFC_MYSQL_USERNAME=root
QFC_MYSQL_PASSWORD=<your-password>
```

加载环境变量并执行建表和初始化数据：

```powershell
. .\scripts\mysql-env.ps1
$mysql = Import-QfcMysqlEnvironment -Path ".\config\mysql.env"
$env:MYSQL_PWD = $mysql.Password
Get-Content ".\server\src\main\resources\db\schema.sql" -Raw |
  mysql --no-defaults --host=$($mysql.Host) --port=$($mysql.Port) --user=$($mysql.Username)
Get-Content ".\server\src\main\resources\db\data.sql" -Raw |
  mysql --no-defaults --host=$($mysql.Host) --port=$($mysql.Port) --user=$($mysql.Username)
Remove-Item Env:MYSQL_PWD
```

`config/application.yml` 使用 `${QFC_MYSQL_*}` 占位符，Spring Boot 启动前由脚本加载 `config/mysql.env`。数据库地址、端口、账号和密码只维护一份。

安装前端依赖：

```cmd
npm --prefix ".\web" install
npm --prefix ".\admin-web" install
```

## 脚本说明

### `scripts/setup-dev.ps1`

用途：首次初始化或补全本机开发环境。

- 检查必需的开发工具。
- 安装两个前端的 npm 依赖。
- 创建本地运行目录和数据库配置。
- 连接失败时允许重新输入并覆盖错误的 `mysql.env`。
- 只在数据库或关键表缺失时询问执行初始化 SQL。

```cmd
powershell -NoProfile -ExecutionPolicy Bypass -File ".\scripts\setup-dev.ps1"
```

可选参数：

```powershell
# 跳过 npm install
.\scripts\setup-dev.ps1 -SkipNpmInstall

# 跳过数据库检查和初始化
.\scripts\setup-dev.ps1 -SkipDatabaseInit
```

也可以双击运行：

```text
scripts\setup-dev.bat
```

### `scripts/start-dev.ps1`

用途：

- 检查并停止当前项目已有的开发服务。
- 重新启动后端 Spring Boot 服务。
- 重新启动主站和后台 Vite 服务。
- 等待 `8081`、`5173`、`5174` 端口开始监听。
- 调用项目列表接口验证后端和数据库连接。
- 进程提前退出或启动超时时，输出对应日志路径并返回失败。
- 启动前整理旧日志。
- 后端自动加载 `config/mysql.env` 和 `config/application.yml`。
- 后端日志写到项目根目录 `logs/`。

在 `cmd` 中执行：

```cmd
powershell -NoProfile -ExecutionPolicy Bypass -File ".\scripts\start-dev.ps1"
```

在 PowerShell 中执行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File ".\scripts\start-dev.ps1"
```

如果公司机器不允许使用 `ExecutionPolicy Bypass`，可以右键 PowerShell 以管理员身份打开后执行：

```powershell
Set-ExecutionPolicy -Scope CurrentUser RemoteSigned
```

然后再执行：

```powershell
.\scripts\start-dev.ps1
```

### `scripts/stop-dev.ps1`

用途：停止当前仓库的后端、主站和后台开发服务，并清理残留的 Maven、Java、npm 和 Vite 进程。

```cmd
powershell -NoProfile -ExecutionPolicy Bypass -File ".\scripts\stop-dev.ps1"
```

也可以双击运行：

```text
scripts\stop-dev.bat
```

### `scripts/restart-backend.ps1`

用途：只重启后端，不影响主站和后台前端开发服务。适合后端代码、配置或数据库脚本调整后快速刷新 `8081` 服务。

```cmd
powershell -NoProfile -ExecutionPolicy Bypass -File ".\scripts\restart-backend.ps1"
```

也可以双击运行：

```text
scripts\restart-backend.bat
```

### `scripts/organize-logs.ps1`

用途：只整理旧日志，不启动服务。

```cmd
powershell -NoProfile -ExecutionPolicy Bypass -File ".\scripts\organize-logs.ps1"
```

也可以双击运行：

```text
scripts\organize-logs.bat
```

## 日志规则

后端日志由 `server/src/main/resources/logback-spring.xml` 控制：

```text
logs/qfc-server.log
logs/qfc-server-2026-07-03-0.log.gz
logs/qfc-server-2026-07-03-1.log.gz
```

规则：

- 当前日志写入 `logs/qfc-server.log`。
- 每天自动归档一次。
- 单个日志文件超过 `100MB` 也会归档。
- 归档文件压缩为 `.log.gz`。
- 默认保留最近 30 天。

前端开发服务由启动脚本输出带日期的日志：

```text
logs/web-dev-5173-2026-07-03-1.out.log
logs/web-dev-5173-2026-07-03-1.err.log
logs/admin-web-dev-5174-2026-07-03-1.out.log
logs/admin-web-dev-5174-2026-07-03-1.err.log
```

## 常用命令

后端测试：

```cmd
mvn -f ".\server\pom.xml" test -B -ntp
```

主站构建：

```cmd
npm --prefix ".\web" run build
```

后台构建：

```cmd
npm --prefix ".\admin-web" run build
```
