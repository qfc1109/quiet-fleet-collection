# 轻帆集 / Quiet Fleet Collection 设计文档

## 总体设计

第一版采用前后端分离架构，并拆分主站和后台站点：

- 主站前端 `web/`：Vue 3，负责公开展示页、登录/注册页、账号中心和文件预览界面。
- 后台前端 `admin-web/`：Vue 3 + Element Plus，负责独立后台管理登录、网站用户管理、后台管理员管理和角色权限管理。
- 后端：Spring Boot，负责账号登录、项目管理、文件上传、文件元数据、文件访问接口和后台用户权限接口。
- 数据库：MySQL 5.7，拆分为网站业务库、网站日志库、后台管理库和后台日志库。
- 文件存储：本地磁盘，数据库只保存文件路径和元信息。

第一版先服务个人使用和局域网访问。系统内置一个超级管理员账号，访客无需登录即可浏览公开内容；网站用户可在主站注册；后台管理员账号由后台维护。

## 模块划分

### 首页模块

职责：

- 展示站点名称、介绍和项目入口。
- 引导访客进入项目广场。
- 首页不直接展示项目列表，为后续站点介绍和视觉样式扩展预留空间。
- 提供主站登录入口，不展示后台管理入口。

### 项目广场模块

职责：

- 直接展示公开项目列表。
- 作为同事发现和进入公开项目的主要入口。
- 不额外展示公开项目数量或二级“公开项目”标题。

### 项目展示模块

职责：

- 展示项目基础信息。
- 展示项目下的文件列表。
- 根据文件类型进入预览或下载。

### 主站登录/注册模块

职责：

- 网站用户登录。
- 网站用户注册。
- 登录成功后进入账号中心或回到登录前的主站页面。
- 不提供后台登录入口；后台管理系统通过独立地址访问。

密码不建议明文入库。第一版仍使用 `123456` 作为登录密码，但数据库中保存 BCrypt hash。

### 账号中心模块

职责：

- 登录后点击顶部账号入口进入账号中心。
- 编辑当前账号昵称和简介。
- 上传当前账号头像。
- 管理员创建和编辑项目。
- 上传文件到指定项目。
- 管理项目文件。
- 通过已有目录下拉选择移动文件，支持移动到项目根目录。
- 通过“访客视角”查看公开展示页面。

### 独立后台管理模块

职责：

- 管理网站用户账号。
- 管理后台管理员账号。
- 管理后台角色和权限。
- 提供标准管理后台布局，左侧为模块路由栏，右侧为当前模块展示区域，顶部保留账号和退出操作。
- 使用 Element Plus 组件库承载后台表格、弹窗、表单、按钮、标签和消息提示，避免手写半成品管理控件。
- 用户管理默认展示用户列表，新增用户只作为按钮入口；点击新增或编辑后再打开弹窗表单。

说明：

- 用户管理面向网站使用者。
- 后台管理员账号通过角色获得后台权限。
- 角色权限作为后台管理里的小模块，不出现在主站导航中。
- 后台管理由 `admin-web/` 独立承载，不再使用主站 `/admin` 页面。
- 后台管理系统不提供“打开主站”入口，主站也不提供“返回后台”入口，避免两个站点的账号模型互相误导。
- 第一版后台模块路由包括网站用户、后台管理员和角色权限；无 `ROLE_MANAGE` 权限的后台账号只能看到网站用户模块。
- 网站用户和后台管理员都采用列表优先的信息架构：主区域展示 `el-table` 数据列表，操作列提供编辑和重置密码；创建和编辑通过 `el-dialog` 完成。

后台管理页面的信息架构约束：

- 每个后台模块优先拆成列表页、编辑入口、详情入口和必要的子页面，不把一个模块的全部状态塞进同一个页面。
- 列表页是模块默认入口，承载筛选、表格、分页、批量操作和操作列。
- 新增和编辑必须由明确按钮触发；轻量编辑使用弹窗，复杂编辑使用独立编辑路由。
- 详情、日志、关联数据、权限配置等辅助信息不要长期占用列表页主区域，优先通过详情页、抽屉、弹窗或子路由承载。
- 后台视觉应接近标准管理系统：左侧导航、顶部账号区、内容工具栏、数据表格、分页和清晰的操作列。避免把后台做成展示页、门户页或组件堆叠页。
- 同一页面内只保留当前任务需要的信息层级，避免为了“功能可见”牺牲可扫读性和维护性。

## 数据模型

### 数据库边界

第一版后端连接四个 MySQL 数据库：

- `qfc_site`：保存网站业务数据，例如网站用户、项目和文件元数据。
- `qfc_site_log`：保存网站登录、访问和操作日志。当前先建表预留，日志写入链路后续补充。
- `qfc_admin`：保存后台管理系统自身数据，例如后台管理员、角色和权限。
- `qfc_admin_log`：保存后台登录、后台操作和控制台日志。当前先建表预留，日志写入链路后续补充。

### site_user

保存在 `qfc_site`，只保存网站用户账号。

核心字段：

- `id`
- `username`
- `password_hash`
- `display_name`
- `bio`
- `avatar_url`
- `status`
- `created_at`
- `updated_at`

说明：

- `status` 当前使用 `ENABLED` 和 `DISABLED`。
- 主站注册只写入 `site_user`。

### admin_user

保存在 `qfc_admin`，只保存后台管理员账号。

核心字段：

- `id`
- `username`
- `password_hash`
- `display_name`
- `bio`
- `avatar_url`
- `status`
- `created_at`
- `updated_at`

说明：

- 第一版初始化一条 `admin` 后台账号。
- 后台管理员通过 `admin_user_role` 绑定后台角色。

### admin_role

保存在 `qfc_admin`，保存后台角色。

核心字段：

- `id`
- `code`
- `name`
- `description`
- `built_in`
- `created_at`
- `updated_at`

第一版初始化：

- `SUPER_ADMIN`
- `NORMAL_ADMIN`

### permission

保存在 `qfc_admin`，保存后台权限点。

第一版初始化：

- `USER_VIEW`
- `USER_MANAGE`
- `ROLE_VIEW`
- `ROLE_MANAGE`
- `PROJECT_MANAGE`
- `FILE_MANAGE`
- `ISSUE_MANAGE`

### admin_user_role / role_permission

保存后台管理员账号和角色、角色和权限之间的多对多关系。

### 登录态边界

主站网站用户和后台管理员必须使用不同登录态：

- 主站登录、注册、账号中心和个人项目接口只读取网站用户 session。
- 后台登录、后台管理接口只读取后台管理员 session。
- 同一浏览器可以同时存在网站用户 `admin` 和后台管理员 `admin`，二者互不覆盖。
- 主站退出只清理网站用户 session；后台退出只清理后台管理员 session。
- 后台 `PROJECT_MANAGE` 权限只控制后台管理系统里的全站项目管理能力，不控制主站账号中心里的个人项目管理模块。
- 所有写请求通过 `/api/auth/csrf` 获取并提交 `X-CSRF-Token`，后端在进入业务 Controller 前校验 token。
- 主站和后台登录成功后都调用 `changeSessionId()`，避免攻击者复用登录前的 Session ID。

### project

保存在 `qfc_site`，保存主站项目/仓库信息。后台管理系统可以查看和维护全站项目；主站账号中心只展示当前网站用户自己的项目。

核心字段：

- `id`
- `owner_user_id`
- `name`
- `slug`
- `description`
- `cover_url`
- `visibility`
- `sort_order`
- `created_at`
- `updated_at`
- `deleted_at`
- `deleted_by_user_id`

说明：

- `owner_user_id` 关联 `site_user.id`，用于区分主站个人项目归属。
- `slug` 用于公开访问路径。
- `visibility` 第一版主要使用 `PUBLIC`，后续可扩展私有项目。
- 主站账号中心删除项目时只做软删除：填写 `deleted_at` 和 `deleted_by_user_id`，普通项目列表和公开访问过滤已删除项目，并向 `qfc_site_log.site_operation_log` 写入 `PROJECT_SOFT_DELETE` 操作日志。后台管理员在后台系统执行的操作才写入 `qfc_admin_log`。

### project_file

保存在 `qfc_site`，保存文件元数据。

核心字段：

- `id`
- `project_id`
- `original_name`
- `stored_name`
- `file_ext`
- `mime_type`
- `file_size`
- `storage_path`
- `relative_path`
- `preview_type`
- `created_at`
- `updated_at`

说明：

- `storage_path` 保存本地磁盘相对路径。
- `relative_path` 保存文件在项目中的相对路径，例如 `docs/README.md` 或 `docs/images/logo.png`。
- `preview_type` 用于前端判断预览方式，例如 `MARKDOWN`、`PDF`、`IMAGE`、`EXCEL`、`WORD`、`DOWNLOAD_ONLY`。

## 文件存储

第一版文件存储在后端服务配置的本地目录中。

推荐结构：

```text
storage/
  avatars/
    site_user/
      <user-id>/
        avatar.<ext>
    admin/
      <user-id>/
        avatar.<ext>
  projects/
    <project-id>/
      <relative-path>
```

规则：

- 上传时保留原始文件名用于展示，同时保存项目内相对路径。
- 普通文件上传使用文件名作为相对路径；文件夹上传使用浏览器提供的文件夹相对路径。
- 同一项目内重复上传相同相对路径时更新原文件记录。
- 移动文件时保留文件名，只调整项目内目录；后端同步更新 `relative_path` 和 `storage_path`，并移动本地文件。
- 移动目标目录下已有同名文件时拒绝移动，返回 `FILE_PATH_EXISTS`。
- 下载接口通过文件 ID 读取，不直接暴露服务器绝对路径。
- Markdown 相对图片路径按当前 Markdown 文件所在目录解析，并通过公开资源接口读取同项目文件。
- 头像文件上传时按服务端识别的图片魔数确定扩展名，避免只信任客户端传入的 MIME 类型；头像通过公开头像接口读取，数据库只保存可访问的 `avatar_url`。
- 公开文件和头像的 inline 响应添加 `X-Content-Type-Options: nosniff`，避免浏览器把下载型内容误解释为可执行脚本。
- 文件大小限制和允许类型放入后端配置，便于后续调整。

## 预览策略

### Markdown

后端读取 Markdown 内容，并把相对图片路径改写为同项目资源接口地址。前端使用 Markdown 渲染库转为 HTML，并用 HTML 清洗库处理基础 XSS 风险，避免直接信任原始 HTML。

文件预览页会从 Markdown 标题生成文档标题索引，索引项点击后跳转到正文对应标题位置。预览页顶部提供返回项目页入口，避免用户进入文档后只能依赖浏览器后退。

### PDF

前端使用浏览器内嵌能力展示 PDF 文件流。

### 图片

前端直接展示图片文件流。

### Excel

后端解析 Excel 的第一个工作表或前几个工作表，返回二维表格数据，前端做基础表格展示。

第一版不追求完整还原公式、样式、合并单元格和图表。

### Word

后端使用 POI 提取 Word 文本内容，前端展示基础文本预览，并明确提示图片和复杂版式暂不在线展示。Word 预览页始终保留下载入口，用户需要查看图片、批注、分页和复杂排版时下载原文件查看。

后续如果需要更完整的 DOCX 图片和版式预览，可以在前端基于原文件流接入 `docx-preview`、`mammoth` 等渲染方案，或在后端引入文档转换服务。第一版暂不引入这类重型转换链路。

### 其他文件

展示文件名、类型、大小、上传时间，并提供下载。

## 接口草案

公开接口：

- `GET /api/public/site`：获取站点信息。
- `GET /api/public/projects`：获取公开项目列表。
- `GET /api/public/projects/{slug}`：获取项目详情。
- `GET /api/public/projects/{slug}/files`：获取项目文件列表。
- `GET /api/public/files/{fileId}/preview`：获取文件预览数据或文件流。
- `GET /api/public/files/{fileId}/download`：下载文件。
- `GET /api/public/files/{fileId}/content`：内嵌读取文件内容。
- `GET /api/public/files/{fileId}/assets?path=<relative-path>`：读取 Markdown 引用的同项目资源。
- `GET /api/public/avatars/{accountType}/{userId}/{filename}`：读取账号头像。
- `GET /api/public/avatars/{userId}/{filename}`：读取旧版网站用户头像路径，作为兼容入口。

登录接口：

- `GET /api/auth/csrf`：获取当前 Session 的 CSRF Token。
- `POST /api/auth/login`：网站用户登录。
- `POST /api/auth/register`：注册网站用户并登录。
- `POST /api/auth/logout`：网站用户退出登录。
- `GET /api/auth/me`：获取当前网站用户。
- `PUT /api/auth/profile`：更新当前网站用户昵称和简介。
- `POST /api/auth/avatar`：上传当前网站用户头像。
- `POST /api/auth/admin/login`：后台管理员登录。
- `POST /api/auth/admin/logout`：后台管理员退出登录。
- `GET /api/auth/admin/me`：获取当前后台管理员。

主站个人空间接口：

- `GET /api/space/projects`：当前网站用户的项目列表。
- `POST /api/space/projects`：当前网站用户创建项目。
- `PUT /api/space/projects/{projectId}`：当前网站用户编辑项目。
- `GET /api/space/projects/{projectId}/files`：当前网站用户查看项目文件。
- `POST /api/space/projects/{projectId}/files`：当前网站用户上传文件，可通过 `relativePath` 保留文件夹路径。
- `PUT /api/space/files/{fileId}/path`：当前网站用户移动文件到项目已有目录或项目根目录。
- `DELETE /api/space/files/{fileId}`：当前网站用户删除文件。

管理接口：

- `GET /api/admin/projects`：管理端项目列表。
- `POST /api/admin/projects`：创建项目。
- `PUT /api/admin/projects/{projectId}`：编辑项目。
- `POST /api/admin/projects/{projectId}/files`：上传文件，可通过 `relativePath` 保留文件夹路径。
- `PUT /api/admin/files/{fileId}/path`：移动文件到项目已有目录或项目根目录。
- `DELETE /api/admin/files/{fileId}`：删除文件。
- `GET /api/admin/site-users`：网站用户列表。
- `POST /api/admin/site-users`：创建网站用户。
- `PUT /api/admin/site-users/{userId}`：编辑网站用户。
- `POST /api/admin/site-users/{userId}/reset-password`：重置网站用户密码。
- `GET /api/admin/admin-users`：后台管理员列表。
- `POST /api/admin/admin-users`：创建后台管理员。
- `PUT /api/admin/admin-users/{userId}`：编辑后台管理员和角色。
- `POST /api/admin/admin-users/{userId}/reset-password`：重置后台管理员密码。
- `GET /api/admin/roles`：角色列表。
- `POST /api/admin/roles`：创建角色。第一版固定两种角色，该接口保留但返回禁用错误。
- `PUT /api/admin/roles/{roleId}`：编辑角色权限。
- `GET /api/admin/permissions`：权限列表。

## 主站前端路由

公开路由：

- `/`：首页
- `/explore`：项目广场
- `/u`：旧入口，重定向到 `/explore`
- `/p/:slug`：项目展示页
- `/p/:slug/files/:fileId`：文件预览页
- `/login`：登录页

登录后路由：

- `/space`：账号中心，包含个人资料、头像和项目管理

## 后台前端路由

- `http://127.0.0.1:5174/login`：后台管理登录
- `http://127.0.0.1:5174/`：重定向到网站用户模块
- `http://127.0.0.1:5174/site-users`：网站用户管理
- `http://127.0.0.1:5174/admin-users`：后台管理员管理
- `http://127.0.0.1:5174/roles`：角色权限管理

后续新增后台模块时，列表路由只作为模块入口。复杂编辑页面可以按模块扩展为 `/xxx/:id/edit` 或 `/xxx/new`；简单编辑则保留在当前模块弹窗中。

## 权限设计

第一版权限保持最小可用：

- 公开接口不需要登录。
- 主站个人空间接口需要网站用户登录，不读取后台角色或后台权限。
- 管理接口需要后台管理员登录并拥有对应后台权限。
- `PROJECT_MANAGE` 管理后台全站项目。
- `FILE_MANAGE` 管理后台全站项目文件。
- `USER_VIEW` / `USER_MANAGE` 管理网站用户。
- `ROLE_VIEW` / `ROLE_MANAGE` 管理后台管理员和角色权限。
- `SUPER_ADMIN` 默认拥有全部权限，绑定初始化账号 `admin`。
- `NORMAL_ADMIN` 默认拥有网站用户、项目、文件和反馈管理权限，不拥有后台管理员和角色权限管理权限。
- 审核、日志写入链路、菜单级权限和数据级权限后续实现。
- 基于 Cookie Session 的写接口必须启用 CSRF 校验；前端 axios 统一拦截写请求并自动携带 token。

登录态使用基于 Cookie 的 Session 方案。主站和后台可以共享浏览器 Cookie，但后端用不同 session key 区分网站用户和后台管理员。

## 错误处理

后端接口统一返回结构：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {}
}
```

错误场景需要明确提示：

- 登录失败
- 未登录访问管理接口
- 项目不存在
- 文件不存在
- 目标目录下已存在同名文件
- 项目公开路径已存在
- 项目可见性非法
- 文件类型不支持预览
- 上传文件超出限制
- 文件保存失败
- CSRF Token 缺失或失效
- 未知运行时异常统一返回内部错误，不把堆栈暴露给前端

## 扩展点

- 更完整的多账号体系。
- 后台项目、文件、审核和业务配置管理。
- 菜单级权限和数据权限控制。
- 审核流程。
- 操作日志。
- 文件全文搜索。
- 评论和收藏。
- 项目 README/首页文档绑定。
- 视频和更多办公文档在线预览。
- Docker 和云服务器部署。
