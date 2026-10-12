# 执行锚点第一批交付

2026-10-13，Conflux0.1.20，配套Iron0.17.17和灾变dev.34。Core开发工作树 `parallel-workspaces/execution-anchors-core/Ars-Conflux`，分支 `feat/execution-anchors`；附属固定源代码提交 `c4ed6f50f853b20da06246ed32e66cdb2880bb57`。附属副本的受版本控制文件保持干净。

中立锚点支持实体／位置采样、增益默认授权以及自定义实体授权钩子；执行占用针对实际actor，动作和共享引导分通道。保留旧驱动的非独占活动语义，防止无意限制Goety等既有持续法术。会话登记提前到driver.start之前，嵌套调用可见占用；重复启动不取消已活跃的会话，结束只释放本会话声明。入射方向新增明确清除与零向量遮蔽规则。没有添加原模组类引用、Boss渲染或新的玩法限制。

## 验证与范围

- NeoForge `build-anchors-release.log`：7项required通过，包含旧Core生命周期与新锚点／声明／清理／几何用例。最终中立授权扩展的 `build-anchors-policy-final.log`：3项required通过。
- Forge `forge/build-anchors-forge-release.log`：4项required通过，包含新锚点／声明和旧计费用例。中立授权钩子的最终Forge包另行编译，`forge/build-anchors-forge-package-final.log` BUILD SUCCESSFUL；未为仅新增的重载重复启动Forge服务器。
- Iron Neo13项、Forge9项回归通过；最终兼容基线下另验证4项Forge锚点／授权。原生NEG和Ars延迟在Neo实际执行，Forge没有NEG环境不声称验证NEG。
- 灾变81项回归和1项真实Iron共存通过：同一友军同时执行原生吐息／剑舞、共享引导与独占动作并存、解除友军两侧均清理。真实世界没有生成Boss代理。
- 最后两份Core补丁仅保留旧驱动并行语义、增加中立授权入口；附属最终构建统一固定上述提交。完整详细日志和附属产物见各自handoff。
- 本批没有启动图形客户端，服务端通过和字节码检查不等于实际动画／端点画面验证。其他附属及其全部能力、跨维度实际续接、所有剩余灾变动作的完整锚点迁移留后续验证。

## Core产物 SHA256

- Neo二进制 `build/libs/ars-conflux-0.1.20-mc1.21.1-neoforge.jar`：`8f8aa5a3324ee7cd3cfcb446814f3307f28d1e3a530691dc946a8c5b14d8013a`。
- Neo源码 `build/libs/ars-conflux-0.1.20-sources.jar`：`b9d48d7cbb0262b1a801779b9801d543f9b5c994174b55ebedb4051d24f24f72`。
- Forge二进制 `forge/build/libs/ars-conflux-0.1.20-mc1.20.1-forge.jar`：`0edbfe49a762aae8fcb0981bfff007dd97342584602877d08b6879aa6f65ed6b`。
- Forge源码 `forge/build/libs/ars-conflux-0.1.20-sources.jar`：`ecd1a118da4f39412e379ef018cc901a0e5491325aa421d6cc8640d3675e3c52`。

串行部署既有万法归一和Forge测试实例；旧包保存于各实例 `mod-backups/2026-10-13-execution-anchors`。全部本次测试会话已结束；未停止用户游戏，未推送或发布。
