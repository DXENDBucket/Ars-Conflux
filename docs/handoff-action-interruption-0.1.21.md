# 会话替换交付

2026-10-13，Conflux0.1.21，代码 `884fd49b81b8d30e56432cbdef93060705bc1613`，本地标签 `v0.1.21`，工作树 `parallel-workspaces/execution-interrupts-core/Ars-Conflux`。对应Iron0.17.18与灾变dev.36；两份附属Core副本固定此SHA，受控文件未改变。

中立API `startReplacing` 在授权及启动付款预留之后结束冲突会话，旧close先于新driver.start；默认start仍拒绝冲突。STARTING防止清理回调与启动回调抢走交接占用，重复启动不会取消自己。继续使用DENIED结束原因，不新增可能影响已编译附属switch的枚举。repeat付款行为保持原样，不在初始化时多收一次。

旧活动标记改为独立非独占ACTIVITY，避免被误当作身体动作；增加声明过滤版isCasting供施法入口按语义检查。新启动异常仍清理新声明且不收费，但不逆向恢复已结束的旧原生动画。Core没有原模组类型引用、身体运动或图形实现。

- `build-replacement.log`：Neo8项required通过，含原会话／计费及锚点、替换、失败保留、先清理再开始、共享引导／独立actor、嵌套交接和重复启动。
- `forge/build-replacement-forge.log`：Forge5项required通过，公共替换用例与旧计费用例；最终jarJar/reobf/sources构建成功。
- Iron两平台各5项通过；灾变首轮28项通过27项，钩锚MISS漏判修复后对应回归通过，详细经过见附属交接。没有重跑无关完整矩阵或启动图形客户端。
- Neo二进制SHA256 `cd25edab85b203d31e22c1b25d7ecea5336e25609043c1124c3b4cb185232f14`；源码 `eff51a251b3fc523e4eb8df3546923f53094292d06690e68d5df57406ae46354`。
- Forge二进制SHA256 `31647fbda85dc333d242d79fa24ca76dc07861f77cd22697b94285cf8084a730`；源码 `02f60959c2019f4957fecc4efb77a958e81c57b126e38e7fcd587bce86fa667b`。
- 产物位于build/libs及forge/build/libs，Forge为最终重混淆jarJar；许可／测试类排除／Jar-in-Jar范围 `[0.4.1,)`检查通过。
- 与Iron串行更新万法归一及1.20.1测试实例，灾变只更新万法归一。旧包保存各实例 `mod-backups/2026-10-13-action-interruption`，复制哈希和单包数量核对通过。未修改万途归零、未推送或发布。
