# 本地 CurseForge 更新工具

Python 3.11+、PowerShell 与本地构建环境即可，不需要额外 Python 包。工具位于 Core 仓库，统一管理多个项目；不属于游戏运行代码，无需提高模组版本。

双击仓库根目录 `Publish-CurseForge.cmd`：选择项目（默认 Core＋Iron）→选择是否构建（默认是）→显示准备好的文件和更新日志→输入 `y` 上传。默认请求通过审核后自动发布，`manual_release=true` 则等待网站手动发布。工具调用 [CurseForge 官方 Upload API](https://support.curseforge.com/support/solutions/articles/9000197321-curseforge-api)，上传后仍需平台审核。

## 一次性设置

首次启动复制 `curseforge.example.json` 为忽略的 `curseforge.local.json`。本机目录和 JDK 填在本地配置；已预填项目 ID：

| 键 | 项目 | ID | 平台 |
| --- | --- | --- | --- |
| conflux | [Ars Conflux](https://www.curseforge.com/minecraft/mc-mods/ars-conflux) | 1737308 | 双版本 |
| iron | [Iron & Nouveau](https://www.curseforge.com/minecraft/mc-mods/irons-nouveau) | 1726248 | 双版本 |
| goety | [Goety & Nouveau](https://www.curseforge.com/minecraft/mc-mods/goety-nouveau) | 1739392 | 双版本 |
| bomd | [BOMD & Nouveau](https://www.curseforge.com/minecraft/mc-mods/bomd-nouveau) | 1739402 | 双版本 |
| cataclysm | Cataclysm & Nouveau | 尚需填写 | 仅 NeoForge |

从 [CurseForge 作者后台](https://authors.curseforge.com/) 的 API Tokens 页面生成 **上传 Token**，双击 `Set-CurseForge-Token.cmd`，或在本机执行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\set-curseforge-token.ps1
```

输入隐藏，写入当前 Windows 用户的 `CURSEFORGE_API_TOKEN` 环境变量。无需把 Token 发给开发者或放入源码。它不同于供第三方查询模组数据的 CurseForge Core API Key。Gradle 子进程不继承这个 Token。

## 每次更新

1. 更新各仓库的 `VERSION`，完成所需的开发验证。
2. 在本工具的 `release-notes/<项目键>/<版本号>.md` 写本次更新日志，不能空白；不从可能含内部记录的 Git 历史自动拼接。
3. 双击入口，或执行下列命令。只选择需要更新的项目；添加更多项目只改本地 JSON，无需复制脚本。

```powershell
# 构建和准备 Core + Iron 双版本；不上传，也不需要 Token。
.\tools\publish-curseforge.ps1 -Projects conflux,iron -Build

# 已有发行包时只准备（仍检查 JAR 和 VERSION）。
.\tools\publish-curseforge.ps1 -Projects conflux,iron

# 一条命令构建并上传；显式 -Upload 才会发起上传请求。
.\tools\publish-curseforge.ps1 -Projects conflux,iron -Build -Upload

# 单独更新 Goety 的 Forge 版本，作为 beta。
.\tools\publish-curseforge.ps1 -Projects goety -Platform forge -ReleaseType beta -Build -Upload

# 上传刚刚展示/审核过的计划，不再构建或替换文件。
.\tools\publish-curseforge.ps1 -Plan .\build\curseforge\某批次\plan.json -Upload

# 只读验证 Token 和准确版本标签，完全不上传。
.\tools\publish-curseforge.ps1 -Plan .\build\curseforge\某批次\plan.json -CheckApi
```

默认构建不会启动客户端、GameTest 或重复玩法验证，只打包发行与源码包；**上传的是二进制发行 JAR**，源码包暂不自动上传。Forge 显式执行 `jarJar` 和 `reobfJarJar`，不选择 dev JAR。Cataclysm 配置带 `verifyDistributions`，保留素材打包检查。

可用 `-NeoJavaHome` / `-ForgeJavaHome` 覆盖 JDK；`-Offline` 使用缓存依赖；`-ManualRelease` 让审核通过后等待手动发布。需要保持平时构建隔离和串行约定，别同时启动多份发布构建。

## 上传文件与进度

工具根据 JAR 的真实 modid、版本、加载器、MC 版本与依赖声明生成上传信息。未配置的必需依赖会阻止上传；已配置的可选依赖标记为 optional。关系只描述直接依赖，传递依赖由原项目处理。

全部项目先通过检查，再把产物复制到 `build/curseforge/<批次>/`，保存 SHA-256 与完整 `plan.json`。准备失败不上传任何文件；上传前再次检查整批快照哈希。计划保留易读的版本名，发送时读取 API 的版本与类型目录，将 MC 名称限制到对应 `Minecraft 1.20` / `Minecraft 1.21` 类型、加载器限制到 `Modloader`，提交唯一数字 `gameVersions`。运行环境同样限制在 `Environment` 分组，默认同时选择 `Client`、`Server`；可用全局或项目级 `environments` 数组指定单侧。旧计划缺少环境时按既有双侧模组补齐这两项。同名 Bukkit / 通用条目不会混入，目录解析失败不开始上传；旧计划同样自动转换，无需重新构建。

元数据作为普通 multipart 表单字段发送。HTTP 拒绝的具体错误经 Token 脱敏后显示并记录在 `uploads.json` 的 `http_status` / `error`。Core 在附属之前上传；API 返回文件 ID 不等于 Core 已审核公开，必要时可分别发布。

`build/curseforge/uploads.json` 记录项目 ID＋文件哈希＋返回的文件 ID。重新执行同一计划会跳过成功文件；不会自动重试 POST。请求超时、服务端异常或中断后，结果可能不确定，需要先去网站确认，避免重复上传。

确认后，在本机处理日志给出的键：

```powershell
# 确认已上传：记录网站上实际的文件 ID。
python .\tools\curseforge_publish.py --resolve-unknown "项目ID:SHA256" 12345678
# 确认网站没有文件：0 允许下一次显式上传尝试。
python .\tools\curseforge_publish.py --resolve-unknown "项目ID:SHA256" 0
```

发布程序异常退出可能留下 `.lock`。确认对应进程已结束，再删除日志指明的那个锁文件。不要删除上传记录来绕过不确定结果。

## 本次验证

18 项临时 JAR / 模拟传输测试通过，检查双版本元数据、Markdown、整批失败、快照变更、重复跳过、部分成功续传、超时处理、Token 不进入输出、multipart 请求、锁、双 JDK 构建参数、同名版本隔离和错误响应脱敏。另对本地真实 Core / Iron 发行包生成四文件计划，并解析 PowerShell 入口。

2026-10-13 HTTP 400 排查中已使用本机 Token 通过两个只读线上目录接口，并核对双版本/加载器 ID；没有提交新的上传请求。先前错误响应未保存，不能断言拒绝来自哪一字段；新的请求格式和错误诊断已补齐，实际上传接收结果仍需下一次明确上传确认。


2026-10-13 实际上传 0.1.27 时，API 返回 HTTP 400 / 1021，明确要求至少一个运行环境版本标签。已通过只读目录确认 `Environment` 分组 ID 75208，`Client` / `Server` 标签 ID 9638 / 9639，补充默认双侧标签和独立配置，并增加分组同名排除、旧计划兼容、单侧配置与无效值预检。重新上传仍使用已验证的原二进制，不重新构建或改模组版本。


补齐运行环境后，0.1.27 的两个发行包实际上传成功：NeoForge 1.21.1 文件 ID `9153753`，Forge 1.20.1 文件 ID `9153755`，项目 ID `1737308`。API 均返回文件 ID，上传进程正常退出，台账记录为 uploaded。计划 `build/curseforge/20261013-123015-6d4aa53d/plan.json`，`isMarkedForManualRelease=false`，审核通过后自动发布；这里只确认接收成功，不代表已经审核公开。发布工具修复提交 `253bc5d`，不改已验证的 0.1.27 二进制。


Iron 0.17.22 首次上传时，CF 返回 HTTP 400 / 1002，明确要求 `relations.projects[].projectID` 为整数。发布工具已修正依赖 ID 类型，并在发送旧计划时做深复制归一化，保留原审核快照。新增 ID 类型/无效值检查，实际 multipart 测试和旧计划测试也覆盖带数字项目 ID 的依赖；共 18 项测试通过。已验证的模组 JAR 不变。


2026-10-13，依赖项目 ID 类型修正后，Iron & Nouveau 0.17.22 两份文件均上传成功：NeoForge 1.21.1 文件 ID `9153840`，Forge 1.20.1 文件 ID `9153841`，项目 ID `1726248`。上传进程退出码 0，台账状态均为 uploaded，保留已验证的发行 JAR 哈希。计划 `build/curseforge/20261013-124850-e8c1b112/plan.json`，审核通过后自动发布；API 接收不等于已经审核公开。发布工具修复提交 `235ae86`。
