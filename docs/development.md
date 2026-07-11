# 轻帆集 / Quiet Fleet Collection 开发文档

## 技术栈

后端：

- Java 8
- Spring Boot 2.7.18
- MySQL 5.7
- MyBatis Plus 3.5.5
- Maven 3.9.16

主站前端：

- Vue 3.5.39
- TypeScript 6.0.2
- Vite 8.1.2
- Vue Router 5.1.0
- Pinia 3.0.4
- Axios 1.18.1
- Marked
- DOMPurify

后台前端：

- Vue 3.5.39
- TypeScript 6.0.2
- Vite 8.1.2
- Vue Router 5.1.0
- Pinia 3.0.4
- Axios 1.18.1
- Element Plus 2.14.2
- @element-plus/icons-vue 2.3.2

文件处理：

- Markdown：后端改写相对图片路径，前端使用 Marked + DOMPurify 渲染；前端从标题生成文档索引并给正文标题加锚点
- PDF：浏览器内嵌预览
- 图片：浏览器展示
- Excel：后端解析为基础表格数据
- Word：后端用 POI 提取文本，前端显示“仅展示文本预览”提示；图片和复杂版式通过下载原文件查看，后续保留接入 `docx-preview`、`mammoth` 等高保真渲染方案的空间
- 头像：后端按图片魔数识别扩展名，保存到本地存储并通过公开头像接口内联读取
- 公开文件和头像 inline 响应添加 `X-Content-Type-Options: nosniff`

## 推荐目录结构

```text
quiet-fleet-collection/
  server/
    src/
      main/
        java/
        resources/
      test/
  web/
    src/
      api/
      components/
      router/
      stores/
      views/
  admin-web/
    src/
      api/
      router/
      stores/
      views/
  docs/
    requirements.md
    design.md
    development.md
    progress.md
```

后台前端模块建议继续按列表页和表单组件拆分，避免单个页面承担过多职责：

```text
admin-web/
  src/
    views/
      site-users/
        SiteUserListView.vue
        components/
          SiteUserFormDialog.vue
      admin-users/
        AdminUserListView.vue
        components/
          AdminUserFormDialog.vue
      roles/
        RoleListView.vue
        components/
          RolePermissionDialog.vue
```

简单模块可以先只保留一个列表页和一个弹窗组件；复杂模块再增加 `CreateView`、`EditView`、`DetailView` 等独立路由页面。

## 数据库

用户已部署 MySQL 5.7，本地端口从截图看为 `3306:3306`。

后端当前使用四个数据库：

```text
qfc_site       网站业务库，保存 site_user、project、project_file
qfc_site_log   网站日志库，保存 site_login_log、site_operation_log
qfc_admin      后台管理库，保存 admin_user、admin_role、permission、admin_user_role、role_permission
qfc_admin_log  后台日志库，保存 admin_login_log、admin_operation_log、admin_console_log
```

后端本地真实配置放在 `config/application-local.yml`，该文件已被 `.gitignore` 忽略，不提交。可提交模板是：

```text
config/application-local.yml.example
```

本地配置中只需要在 `qfc.local.db` 填一次账号密码，四个数据源通过 YAML 锚点复用：

```yaml
qfc:
  local:
    db:
      username: &qfc_db_username root
      password: &qfc_db_password "<your-password>"
  datasource:
    site:
      url: jdbc:mysql://[::1]:3306/qfc_site?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
      username: *qfc_db_username
      password: *qfc_db_password
    site-log:
      url: jdbc:mysql://[::1]:3306/qfc_site_log?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
      username: *qfc_db_username
      password: *qfc_db_password
    admin:
      url: jdbc:mysql://[::1]:3306/qfc_admin?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
      username: *qfc_db_username
      password: *qfc_db_password
    admin-log:
      url: jdbc:mysql://[::1]:3306/qfc_admin_log?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
      username: *qfc_db_username
      password: *qfc_db_password
  storage:
    root: ./storage
```

主站和后台是两个独立网站，不通过前端导航互跳。主站不展示后台入口，后台也不展示打开主站入口；需要进入后台时直接访问后台站点地址。

后端默认配置为：

```text
qfc_site: jdbc:mysql://[::1]:3306/qfc_site?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
qfc_site_log: jdbc:mysql://[::1]:3306/qfc_site_log?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
qfc_admin: jdbc:mysql://[::1]:3306/qfc_admin?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
qfc_admin_log: jdbc:mysql://[::1]:3306/qfc_admin_log?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
username: root
password: 空
storage root: ./storage
```

如果数据库端口、库名、账号或文件存储目录不同，改 `config/application-local.yml` 对应字段即可。不要把真实账号密码写入可提交文件。

建表和初始化账号脚本：

```text
server/src/main/resources/db/schema.sql
server/src/main/resources/db/data.sql
server/src/main/resources/db/migration-2026-07-02-four-database-split.sql
server/src/main/resources/db/migration-2026-07-02-space-project-owner.sql
server/src/main/resources/db/migration-2026-07-11-project-active-slug.sql
```

`data.sql` 初始化网站用户账号和后台管理员账号：

```text
username: admin
password: 123456
```

说明：SQL 中只保存 BCrypt hash，不保存明文密码。

建议字符集：

```text
utf8mb4
```

第一版需要分别初始化网站用户账号和后台管理员账号：

```text
username: admin
password: 123456
```

密码入库时使用 BCrypt hash，不保存明文。

使用命令行导入 SQL 前，先从 `config/mysql-client.cnf.example` 复制一份 `config/mysql-client.cnf`，并在该本机配置里填写 MySQL 连接信息。`config/mysql-client.cnf` 已被 `.gitignore` 忽略，不提交。

全新初始化时先执行 `schema.sql`，再执行 `data.sql`。两个脚本内部会切换到对应数据库：

```powershell
Get-Content -LiteralPath 'server/src/main/resources/db/schema.sql' -Raw |
  mysql --defaults-extra-file=config/mysql-client.cnf

Get-Content -LiteralPath 'server/src/main/resources/db/data.sql' -Raw |
  mysql --defaults-extra-file=config/mysql-client.cnf
```

如果本地数据库已经执行过旧版单库 `schema.sql`，需要在旧库上执行四库拆分迁移脚本。该脚本会从当前连接的旧库复制 `user_account`、`project`、`project_file`、角色和权限数据到四个新库：

```powershell
Get-Content -LiteralPath 'server/src/main/resources/db/migration-2026-07-02-four-database-split.sql' -Raw |
  mysql --defaults-extra-file=config/mysql-client.cnf quiet_fleet_collection
```

迁移内容：

- 创建 `qfc_site`、`qfc_site_log`、`qfc_admin`、`qfc_admin_log`。
- 将旧 `SITE_USER` 账号迁移到 `qfc_site.site_user`。
- 将旧 `ADMIN` 账号迁移到 `qfc_admin.admin_user`。
- 将项目和文件元数据迁移到 `qfc_site`，并将旧项目默认归属到网站用户 `admin`。
- 将后台角色、权限和管理员角色关系迁移到 `qfc_admin`。
- 初始化后台日志和网站日志基础表。

如果本地已经执行过四库拆分脚本，需要继续执行项目归属迁移脚本，为 `qfc_site.project` 补充 `owner_user_id` 并把旧项目归属到网站用户 `admin`：

```powershell
Get-Content -LiteralPath 'server/src/main/resources/db/migration-2026-07-02-space-project-owner.sql' -Raw |
  mysql --defaults-extra-file=config/mysql-client.cnf
```

如果本地项目表仍使用旧的 `UNIQUE KEY uk_project_slug (slug)`，需要继续执行访问路径软删除迁移脚本。该脚本会移除旧唯一键，新增 `active_slug` 生成列，并只对未删除项目的访问路径做唯一约束：

```powershell
Get-Content -LiteralPath 'server/src/main/resources/db/migration-2026-07-11-project-active-slug.sql' -Raw |
  mysql --defaults-extra-file=config/mysql-client.cnf
```

## 本地文件存储

建议后端配置项：

```yaml
qfc:
  storage:
    root: ./storage
```

实际上传文件保存在服务端本地磁盘。数据库只保存相对路径、文件名、大小、类型和预览方式。

当前最小实现按项目内相对路径保存文件：

```text
storage/projects/<project-id>/<relative-path>
storage/avatars/site_user/<user-id>/avatar.<ext>
storage/avatars/admin/<user-id>/avatar.<ext>
```

示例：

```text
storage/projects/7/docs/README.md
storage/projects/7/docs/images/logo.png
```

账号中心支持头像上传、普通文件上传和浏览器文件夹上传。文件夹上传时，前端会把浏览器提供的 `webkitRelativePath` 作为 `relativePath` 提交给后端；Markdown 中的相对图片路径按当前 Markdown 文件所在目录解析。

头像上传会读取文件内容识别 PNG/JPEG/GIF/WebP 类型，不再只依赖客户端 `Content-Type`。项目文件的内联读取由后端按文件扩展名映射响应 MIME，并统一添加 `X-Content-Type-Options: nosniff`；不支持内联的文件走下载入口。

同一项目内重复上传相同 `relativePath` 时，后端会更新原文件记录，避免同一路径出现多条元数据。

账号中心支持移动文件到当前项目已有目录或项目根目录。移动时前端从当前项目文件树提取目录选项，不需要用户手动输入目录；后端保留文件名，更新 `relativePath` 和 `storagePath`，并把本地文件移动到新位置。目标目录已有同名文件时返回 `FILE_PATH_EXISTS`。

如果本地数据库已经执行过旧版 `schema.sql`，需要手动补字段：

```sql
ALTER TABLE project_file
  ADD COLUMN relative_path VARCHAR(500) NOT NULL DEFAULT '' AFTER storage_path,
  ADD KEY idx_project_file_relative_path (project_id, relative_path);
```

## 开发阶段

### 阶段 1：项目初始化

- 初始化 Spring Boot 后端项目。
- 初始化 Vue 3 前端项目。
- 建立基础目录结构和配置文件。
- 添加数据库连接配置。
- 添加基础启动文档。

### 阶段 2：后端基础能力

- 建表和初始化数据。已完成第一批脚本。
- 实现统一返回结构。已完成。
- 实现登录接口。已完成服务和控制器基础实现。
- 实现公开项目查询接口。已完成服务和控制器基础实现。
- 实现管理端项目接口。已完成服务和控制器基础实现。
- 实现文件上传、下载和预览接口。已完成最小后端实现。

当前已实现接口：

```text
GET /api/public/site
GET /api/public/projects
GET /api/public/projects/{slug}
GET /api/auth/csrf
POST /api/auth/login
POST /api/auth/admin/login
POST /api/auth/logout
POST /api/auth/register
GET /api/auth/me
PUT /api/auth/profile
POST /api/auth/avatar
POST /api/auth/admin/logout
GET /api/auth/admin/me
GET /api/public/avatars/{userId}/{filename}
GET /api/public/avatars/{accountType}/{userId}/{filename}
GET /api/space/projects
POST /api/space/projects
PUT /api/space/projects/{projectId}
DELETE /api/space/projects/{projectId}
GET /api/space/projects/{projectId}/files
POST /api/space/projects/{projectId}/files
PUT /api/space/files/{fileId}/path
DELETE /api/space/files/{fileId}
GET /api/admin/projects
POST /api/admin/projects
PUT /api/admin/projects/{projectId}
GET /api/public/projects/{slug}/files
GET /api/public/files/{fileId}/preview
GET /api/public/files/{fileId}/download
GET /api/public/files/{fileId}/content
GET /api/public/files/{fileId}/assets?path=<relative-path>
GET /api/admin/projects/{projectId}/files
POST /api/admin/projects/{projectId}/files
PUT /api/admin/files/{fileId}/path
DELETE /api/admin/files/{fileId}
GET /api/admin/site-users
POST /api/admin/site-users
PUT /api/admin/site-users/{userId}
POST /api/admin/site-users/{userId}/reset-password
GET /api/admin/admin-users
POST /api/admin/admin-users
PUT /api/admin/admin-users/{userId}
POST /api/admin/admin-users/{userId}/reset-password
GET /api/admin/roles
POST /api/admin/roles
PUT /api/admin/roles/{roleId}
GET /api/admin/permissions
```

主站登录接口 `/api/auth/login` 只校验 `qfc_site.site_user`，并写入网站用户 session；后台登录接口 `/api/auth/admin/login` 只校验 `qfc_admin.admin_user`，并写入后台管理员 session。两个库允许存在相同用户名，例如网站用户 `admin / 123456` 和后台管理员 `admin / 123456` 是两条独立账号，同一浏览器内也不能互相覆盖。

登录和其他写请求使用 Cookie Session + CSRF Token。前端 axios 会先调用 `GET /api/auth/csrf`，再给 `POST`、`PUT`、`DELETE`、`PATCH` 请求添加 `X-CSRF-Token`；如果后端返回 `CSRF_TOKEN_INVALID`，前端会清空旧 token 并重试一次。后端登录成功后会刷新 Session ID，主站登录和后台登录都会保留已有 CSRF Token 继续使用。

主站个人空间项目接口使用 `/api/space/**`，只要求网站用户 session，不读取后台角色和后台权限，并按 `project.owner_user_id` 限制只能操作当前网站用户自己的项目。`DELETE /api/space/projects/{projectId}` 只做软删除，写入 `project.deleted_at`、`project.deleted_by_user_id`，并记录 `qfc_site_log.site_operation_log`。后台管理接口使用 `/api/admin/**`，只要求后台管理员 session，并按后台权限校验；后台操作日志写入 `qfc_admin_log`。

说明：第一版角色固定为 `SUPER_ADMIN` 和 `NORMAL_ADMIN`，`POST /api/admin/roles` 保留接口但返回 `ROLE_CREATION_DISABLED`，后台前端不展示“新增角色”入口。

`POST /api/space/projects/{projectId}/files` 和 `POST /api/admin/projects/{projectId}/files` 支持可选表单字段：

```text
relativePath
```

该字段用于保存文件在项目文件夹中的相对路径，例如 `docs/README.md` 或 `docs/images/logo.png`。

`PUT /api/space/files/{fileId}/path` 和 `PUT /api/admin/files/{fileId}/path` 请求体：

```json
{
  "targetDirectory": "docs/images"
}
```

`targetDirectory` 为空字符串时表示移动到项目根目录；非空时表示移动到当前项目已有目录。后端会保留原文件名，目标路径存在同名文件时返回 `FILE_PATH_EXISTS`。

### 阶段 3：前端页面

最小接入已完成：

- 首页和项目广场读取公开项目。
- 项目展示页读取项目详情和文件列表。
- 文件预览页按 `previewType` 展示 Markdown、PDF、图片、Excel、Word 文本预览或下载入口，并提供返回项目页按钮。
- Markdown 文件预览页会抽取标题生成标题索引，点击索引项可跳转到正文对应标题位置。
- 登录页调用后端网站用户登录/注册接口，不提供后台登录入口。
- 顶部账号入口进入账号中心；账号中心支持编辑昵称和简介、上传头像、创建项目、选择项目、上传单个/多个文件、上传整个文件夹、以目录树查看文件、将文件移动到已有目录或项目根目录、删除文件和进入访客视角。

说明：主站当前页面以“可用闭环”为优先，样式和交互仍是基础版本；后台管理已切换为 Element Plus 组件库，采用表格列表、弹窗表单、标签、消息提示和标准后台 Shell。

### 阶段 3.1：独立后台前端

- 主站移除 `/admin` 路由和导航入口。
- 初始化 `admin-web/` 独立后台前端。
- 接入 Element Plus 和中文语言包。
- 实现后台登录页，使用 Element Plus 卡片、表单、输入框和消息提示。
- 实现标准后台 Shell：左侧模块路由栏，右侧顶部操作区，主体区域展示当前路由模块。
- 实现后台模块路由，包含 `/site-users` 网站用户、`/admin-users` 后台管理员、`/roles` 角色权限。
- 主站和后台不提供互跳入口；后台地址由部署或文档提供。
- 网站用户和后台管理员模块默认展示 `el-table` 列表，新增和编辑通过 `el-dialog` 打开表单，不在列表页常驻新增表单。
- 角色权限模块使用 Element Plus 表格、分区表单和复选框组维护固定角色权限。

后台页面开发约定：

- 列表页不直接展示常驻编辑区；新增、编辑、重置密码等操作必须通过按钮、操作列、弹窗或独立路由进入。
- 表单字段较少、提交链路简单时使用 `el-dialog`；字段多、依赖关系复杂、需要分区编辑或需要保存草稿时使用独立编辑路由。
- 同一个后台模块不要写成一个巨大的 Vue 文件。列表查询、表格展示、表单弹窗、详情展示和权限配置应拆成可读的小组件。
- 页面布局优先使用管理后台常见结构：筛选区、工具栏、表格、分页、操作列和状态反馈。避免大卡片堆叠、大面积留白、营销页式标题和装饰性视觉元素。
- 新增后台模块前，先确认模块默认入口、主要列表字段、操作列、编辑承载方式和是否需要详情/日志/关联子页面。

### 阶段 4：联调和验证

- 验证管理员登录。
- 验证创建项目。
- 验证上传 Markdown、PDF、图片、Excel、Word。
- 验证公开项目访问。
- 验证不支持预览的文件可以下载。
- 验证刷新页面、接口错误和未登录拦截。

## 启动命令

### 已验证命令

后端基础测试：

```powershell
mvn -f server/pom.xml test -B -ntp
```

验证结果：通过，`Tests run: 46, Failures: 0, Errors: 0, Skipped: 0`。

前端安装依赖：

```powershell
npm --prefix web install
```

验证结果：通过，`found 0 vulnerabilities`。

前端生产构建：

```powershell
npm --prefix web run build
```

验证结果：通过，Vite 成功生成 `web/dist`。

前端文件树工具测试：

```powershell
cd web
node --experimental-strip-types tests\fileTree.test.mjs src\utils\fileTree.ts
```

验证结果：通过。

后台前端安装依赖：

```powershell
npm --prefix admin-web install
```

验证结果：通过。当前依赖包括 Element Plus 和 `@element-plus/icons-vue`。

后台前端生产构建：

```powershell
npm --prefix admin-web run build
```

验证结果：通过，Vite 成功生成 `admin-web/dist`。接入 Element Plus 后，构建会输出来自依赖包 `@vueuse/core` 的 Rolldown `INVALID_ANNOTATION` 警告；当前不影响构建产物生成。

前端本地开发服务：

```powershell
npm --prefix web run dev -- --host 0.0.0.0 --port 5173
```

验证结果：通过，Vite 在 `http://127.0.0.1:5173/` 和当前电脑的内网地址启动；浏览器检查首页、项目广场、项目页、文件预览页、登录页和个人空间路由均可渲染。

### 本地启动命令

默认启动方式是开发环境启动：后端使用 `spring-boot:run`，主站前端和后台前端使用 `npm run dev`。不要把日常本地启动理解成部署流程；只有正式部署或验收部署时才执行前端 `build`，并用后端 Jar 包运行。

Windows 本地优先使用可双击脚本：

```text
scripts\start-dev.bat
scripts\restart-backend.bat
scripts\organize-logs.bat
```

其中 `start-dev.bat` 会启动后端、主站前端和后台前端，并在启动前清理本项目占用 `8081/5173/5174` 的旧进程；`restart-backend.bat` 只重启后端，适合后端代码或配置修改后快速刷新；`organize-logs.bat` 只整理旧日志。

后端启动命令：

```powershell
mvn -f server/pom.xml -Dspring-boot.run.arguments=--spring.config.additional-location=file:../config/ spring-boot:run
```

说明：本机数据库账号密码读取 `config/application-local.yml`。当前后端默认端口为 `8081`，四个数据库 URL 默认指向 `[::1]:3306` 下的 `qfc_site`、`qfc_site_log`、`qfc_admin`、`qfc_admin_log`；如果端口或库名不同，修改 `config/application-local.yml`。

验证结果：通过，Spring Boot 已在 `http://127.0.0.1:8081` 启动，并连通本机 Docker MySQL 5.7。

安全修复后后端重启验证：`GET http://127.0.0.1:8081/api/auth/csrf` 返回 `SUCCESS`，携带 `X-CSRF-Token` 登录后台 `admin / 123456` 返回 `SUCCESS`，账号类型为 `ADMIN`。

前端开发服务：

```powershell
npm --prefix web run dev -- --host 0.0.0.0 --port 5173
```

验证结果：通过，Vite 已在 `http://127.0.0.1:5173/` 和 `http://192.168.0.98:5173/` 启动，代理 `/api` 到 `http://127.0.0.1:8081`。

后台前端开发服务：

```powershell
npm --prefix admin-web run dev -- --host 0.0.0.0 --port 5174
```

验证结果：通过，Vite 已在 `http://127.0.0.1:5174/` 和 `http://192.168.0.98:5174/` 启动，代理 `/api` 到 `http://127.0.0.1:8081`。未登录访问 `/` 会重定向到 `/login?redirect=/site-users`；登录后后台使用 Element Plus 左侧菜单、顶部操作区和右侧路由展示区域。网站用户模块默认显示用户列表，点击“新增用户”后才打开新增弹窗。

当前已验证的最小访问地址：

```text
公开项目页：http://127.0.0.1:5173/p/example
Markdown 预览页：http://127.0.0.1:5173/p/example/files/1
主站登录/注册页：http://127.0.0.1:5173/login
后台登录页：http://127.0.0.1:5174/login
后台网站用户模块：http://127.0.0.1:5174/site-users
后台管理员模块：http://127.0.0.1:5174/admin-users
后台角色权限模块：http://127.0.0.1:5174/roles
```

### 公司内网访问

当前开发机内网地址：

```text
http://192.168.0.98:5173/
```

前端开发服务需要监听 `0.0.0.0:5173`，同事通过上面的内网地址访问页面。前端继续使用相对路径 `/api`，由 Vite 代理到本机后端 `http://127.0.0.1:8081`，因此后端端口无需直接暴露给内网。

Windows 防火墙已添加入站规则：

```text
QFC Frontend 5173 (LocalSubnet)
```

规则仅放行 TCP `5173`，远程地址限制为 `LocalSubnet`。

## GitHub

远程仓库已创建：

```text
https://github.com/qfc1109/quiet-fleet-collection.git
```

当前本地目录已初始化 Git，默认分支为 `main`，远程仓库已配置为 `origin`。当前尚未创建本地提交，也尚未 push。

安全规则：

- 不自动 push。
- 不自动创建 commit。
- 提交前只提交本轮相关文件。
- push 前必须等待用户明确指令。
