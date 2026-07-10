<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# leeks Changelog

## [Unreleased]

- [ ] nothing

## [3.0.0]
- 基于[intellij-platform-plugin-template](https://github.com/JetBrains/intellij-platform-plugin-template)重构插件,适配 `IDE 2025.*`
  
[3.0.0]: https://github.com/JetBrains/gradle-changelog-plugin/releases/tag/v3.0.0

## [3.0.1]
- [x] 对齐 `coin,stock,fund` 抽象架构
- [x] intellijPlatform API 更新
- [x] 优化`hander`线程池与定时任务线程逻辑
- [x] 增加 `eastmoney.com` 东方财富 数据源
- [x] 优化 K线/分时图片弹窗,异步加载图片
- [x] 优化 日志与异常记录
- [x] 表格条纹(斑马线) 功能修复


[3.0.1]: https://github.com/JetBrains/gradle-changelog-plugin/releases/tag/v3.0.1

## [3.0.2]
- [x] 继续优化 K线/分时图片弹窗
- [x] 将 `HttpClientManager` 底层实现切换为 JDK `java.net.http.HttpClient`


[3.0.2]: https://github.com/JetBrains/gradle-changelog-plugin/releases/tag/v3.0.2

## [3.0.3]
- [x] 修复基金估值图弹窗无法触发、右键可能选择错误数据行的问题
- [x] 优化 K线/分时图片加载，增加请求超时并避免重复下载
- [x] 使用 `HttpClientManager` 统一管理代理请求与直连图片请求
- [x] 修复 Cron 表达式校验、代理测试和设置修改状态判断
- [x] 优化基金、股票和虚拟币配置解析，兼容单条持仓与旧版代码列表
- [x] 将代理测试和行情刷新移出 EDT，减少设置操作造成的界面卡顿
- [x] 更新插件图标


[3.0.3]: https://github.com/JetBrains/gradle-changelog-plugin/releases/tag/v3.0.3
