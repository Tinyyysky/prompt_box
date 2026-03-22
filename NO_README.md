# 🦞 提示词盒子 (Prompt Box)

AI 提示词管理工具。单文件、零依赖、跨平台。

## 功能特性

- **提示词管理** — 创建、编辑、删除、复制，支持 Markdown 格式
- **文件夹分类** — 自定义文件夹及颜色标记
- **标签系统** — 支持重命名、删除、合并，批量操作
- **全文搜索** — 搜索标题、内容、标签，支持范围切换
- **复制统计与历史** — 记录复制次数，按时间线查看历史
- **附件存储** — 支持图片预览和文件存储
- **模板系统** — 内置常用提示词模板，支持自定义
- **版本历史** — 每次保存自动记录版本
- **批量操作** — 批量打标签、删除、移动
- **数据备份** — JSON 格式导出/导入，支持分享
- **Markdown 编辑器** — 编辑与预览双模式
- **变量占位符** — 支持 `{{变量名}}`，使用时弹窗填入
- **多维表格同步** — 一键同步到飞书多维表格
- **主题定制** — 深色/浅色模式 + 自定义主题色
- **Android 适配** — 状态栏、导航栏、键盘全面适配，兼容小米 HyperOS
- **手势操作** — 左滑删除、长按菜单、多选批量
- **更新日志** — App 内查看版本更新记录

## 技术栈

- **前端**：HTML + CSS + JavaScript（单文件，无框架）
- **存储**：IndexedDB / localStorage
- **Android**：WebView 壳 + JavaScript Bridge
- **同步**：飞书多维表格 API

## 安装

### Android

从 [Releases](https://github.com/Tinyyysky/prompt_box/releases) 下载最新 APK 安装。

### PWA

浏览器打开 `index.html`，点击「添加到主屏幕」。

## 本地开发

```bash
# 直接打开
open index.html

# 或使用 HTTP 服务
python3 -m http.server 8080
```

## Android 构建

依赖：
- JDK 17
- Android SDK Build Tools 34.0.0

```bash
# GitHub Actions 自动构建
git push origin main

# 本地签名
zipalign -f 4 app-release-unsigned.apk aligned.apk
apksigner sign --ks promptbox.keystore aligned.apk
```

## 版本历史

| 版本 | 日期 | 更新内容 |
|------|------|----------|
| v1.0.0 | 2026-03-20 | 初始版本：提示词 CRUD、文件夹、标签、搜索、模板、飞书同步 |
| v1.0.1 | 2026-03-21 | Android APK 封装，状态栏/导航栏/键盘适配，小米 HyperOS 兼容 |
| v1.0.2 | 2026-03-22 | 统一模态框样式，状态栏颜色同步，版本号显示 |
| v1.0.2.1 | 2026-03-22 | 修复按钮显示问题，深色模式状态栏颜色，键盘自适应 |
| v1.0.2.2 | 2026-03-22 | 更新日志层级修复，添加 GitHub 链接 |

## License

MIT

## 开发者

[皮皮灭](https://github.com/Tinyyysky)
