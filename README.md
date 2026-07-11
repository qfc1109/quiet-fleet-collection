# 轻帆集 / Quiet Fleet Collection

轻帆集是一个项目资料展示与管理系统，包含一个 Spring Boot 后端、一个主站前端和一个后台前端。

## 项目结构

- `server/`：Java 8 + Spring Boot 2.7 后端服务。
- `web/`：Vue 3 + Vite 主站前端。
- `admin-web/`：Vue 3 + Vite + Element Plus 后台前端。
- `config/`：本机数据库和运行配置。
- `scripts/`：Windows 本地启动和日志整理脚本。
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

### 1. 初始化数据库

复制 MySQL 客户端配置：

```cmd
copy "config\mysql-client.cnf.example" "config\mysql-client.cnf"
```

修改 `config/mysql-client.cnf`：

```ini
[client]
host=::1
port=3306
user=root
password=<your-password>
```

执行建表和初始化数据：

```cmd
mysql --defaults-extra-file=config/mysql-client.cnf < server\src\main\resources\db\schema.sql
mysql --defaults-extra-file=config/mysql-client.cnf < server\src\main\resources\db\data.sql
```

### 2. 配置后端数据库连接

复制本机配置：

```cmd
copy "config\application-local.yml.example" "config\application-local.yml"
```

修改 `config/application-local.yml` 里的数据库账号密码：

```yaml
qfc:
  local:
    db:
      username: &qfc_db_username root
      password: &qfc_db_password "<your-password>"
```

`config/application-local.yml` 和 `config/mysql-client.cnf` 都是本机配置文件，已被 `.gitignore` 忽略，不要提交。

### 3. 安装前端依赖

```cmd
npm --prefix ".\web" install
npm --prefix ".\admin-web" install
```

### 4. 启动项目

推荐用脚本启动三端：

```cmd
powershell -NoProfile -ExecutionPolicy Bypass -File ".\scripts\start-dev.ps1"
```

也可以双击运行：

```text
scripts\start-dev.bat
```

只重启后端：

```text
scripts\restart-backend.bat
```

只整理旧日志：

```text
scripts\organize-logs.bat
```

启动后访问：

```text
主站: http://127.0.0.1:5173/
后台: http://127.0.0.1:5174/
后端: http://127.0.0.1:8081
```

## 脚本说明

### `scripts/start-dev.ps1`

用途：

- 启动后端 Spring Boot 服务。
- 启动主站 Vite 服务。
- 启动后台 Vite 服务。
- 启动前整理旧日志。
- 后端自动加载 `config/application-local.yml`。
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
