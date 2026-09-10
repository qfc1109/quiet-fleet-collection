# QFC 脚本部署说明

本目录用于将当前 Windows 工作区发布到 Debian 内网服务器。发布时在本机构建后端 Jar 和两个前端静态文件；服务器不拉取源码，也不安装 Node、npm、Maven 或 Git。

## 首次部署

首次部署会安装 Docker、OpenJDK 21 JRE 和 Nginx；Docker 中运行 MySQL 8 与 Redis；随后初始化四个 QFC 数据库并启动服务。

~~~powershell
.\deploy\deploy.ps1 -Server 192.168.0.199 -User root -AllowDirtyWorktree
~~~

本工作区当前有未提交的前端改动。AllowDirtyWorktree 是明确确认将这些改动一并构建和发布的开关；没有这个开关时，脚本会拒绝从脏工作区发布。

脚本通过系统 ssh.exe 和 scp.exe 连接服务器。SSH 密码由终端交互输入，绝不会写进脚本、压缩包、日志或仓库。

首次部署仅适用于全新数据库。若服务器已经存在 qfc_site，远端脚本会立即退出，不会导入 SQL 或覆盖数据。

首次脚本还会拒绝修改已存在的 Docker、Nginx、QFC 目录、QFC 用户或 Docker 卷。这是为了避免误改同机已有服务；部署中断后不要直接重复执行首次脚本，应先排查并人工清理本次留下的半成品。

若脚本仅在安装 Docker、JRE 或 Nginx 的阶段失败，且尚未生成 QFC 配置、容器、数据卷或数据库，可在确认该状态后使用 `-ResumeAfterRuntimeInstall` 重试。此恢复开关仅接受空白状态或脚本先前创建的空目录，仍会拒绝已有 QFC 数据，并验证 Docker 中没有容器、卷、镜像、插件或自定义网络；不得用于已有业务服务的机器。

若脚本已完成 QFC 配置、但在拉取 MySQL 或 Redis 镜像时失败，且确认没有 QFC 容器、数据卷、数据库、发布版本、上传文件或日志，可使用 `-ResumeAfterImagePull`。该恢复开关会严格检查上述状态后才继续。Docker Hub 无法访问时，脚本仅对 MySQL 8 和 Redis 两个镜像依次尝试 `docker.m.daocloud.io`、`dockerproxy.net` 代理，并把实际成功的完整镜像名记录在服务器的 `/etc/qfc/qfc.env`；不会更改 Docker 的全局镜像源。

若 MySQL 和 Redis 容器已创建、但四个 QFC 数据库尚未导入，可使用 `-ResumeAfterContainerStart`。该恢复开关仅接受两个命名 QFC 容器和对应的两个数据卷，验证 MySQL root 密码可用且四库均不存在后才继续；它不会清理任何容器、卷或数据。

运行所需的数据库、Redis 与 JWT 密钥只会生成在服务器 `/etc/qfc/` 中权限为 0600 的 root 文件，不会写入仓库、发布包、命令行参数或普通日志。目录本身为 root:qfc、0751，使运行用户只读非秘密配置；种子 SQL 中的固定 admin 密码会在导入后替换为随机密码。首次部署完成后，用 root 在服务器读取 `/root/qfc-initial-admin-credentials.txt` 获取主站和后台的初始管理员密码，并在首次登录后修改密码、删除该文件。

## 更新版本

代码变更后执行：

~~~powershell
.\deploy\update.ps1 -Server 192.168.0.199 -User root -AllowDirtyWorktree
~~~

更新脚本会：

1. 在本机构建后端和两个前端。
2. 打包 Jar、静态文件和数据库 SQL，并校验发布包清单。
3. 上传新版本到服务器。
4. 原子切换 /opt/qfc/current 软链接并重启后端。
5. 调用本机 API 健康检查；检查失败时恢复到上一版本。

更新不会执行 schema.sql 或 data.sql，不会重建 MySQL 或 Redis 容器，不会删除数据库、上传文件和服务器配置。

发布版本保存在服务器 /home/qfc-releases/，Docker 运行时数据位于 /home/qfc-docker/，上传文件和应用日志位于 /home/qfc/；它们都不在根分区的 /opt 下，以适配目标机较小的根分区。

## 内网访问地址

- 主站：http://192.168.0.199/
- 后台：http://192.168.0.199:8082/login

3306、6379 和 8081 仅绑定到服务器本机地址；内网客户端不能直接访问它们。

## 发布后的检查

~~~powershell
ssh root@192.168.0.199 "systemctl is-active docker nginx qfc; docker ps"
~~~

首次部署后，应使用随机生成的初始管理员账号完成主站、后台、文件上传、预览和下载检查，并及时修改密码。
