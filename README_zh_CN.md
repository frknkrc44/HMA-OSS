<div align="center">
  <h2>HMA-OSS</h2>

  <img src="HideMyAss-OSS.svg" alt="HMA-OSS Logo" style="max-width:360px;width:60%;height:auto;">

  <p>
    <a href="https://github.com/frknkrc44/HMA-OSS" style="text-decoration:none">
      <img src="https://img.shields.io/github/stars/frknkrc44/HMA-OSS?label=Stars&logo=github">
    </a>
    <a href="https://github.com/frknkrc44/HMA-OSS/actions" style="text-decoration:none">
      <img src="https://img.shields.io/github/actions/workflow/status/frknkrc44/HMA-OSS/main.yml?branch=master&logo=github">
    </a>
    <a href="https://github.com/frknkrc44/HMA-OSS/releases/latest" style="text-decoration:none">
      <img src="https://img.shields.io/github/v/release/frknkrc44/HMA-OSS?label=Release">
    </a>
    <a href="https://apt.izzysoft.de/fdroid/index/apk/org.frknkrc44.hma_oss" style="text-decoration:none">
      <img src="https://img.shields.io/endpoint?url=https://apt.izzysoft.de/fdroid/api/v1/shield/org.frknkrc44.hma_oss&label=IzzyOnDroid">
    </a>
    <a href="https://shields.rbtlog.dev/org.frknkrc44.hma_oss" style="text-decoration:none">
      <img src="https://shields.rbtlog.dev/simple/org.frknkrc44.hma_oss">
    </a>
    <a href="https://github.com/frknkrc44/HMA-OSS/releases/latest" style="text-decoration:none">
      <img src="https://img.shields.io/github/downloads/frknkrc44/HMA-OSS/total">
    </a>
    <a href="https://t.me/aerathfuns" style="text-decoration:none">
      <img src="https://img.shields.io/badge/Telegram-Channel-blue.svg?logo=telegram">
    </a>
    <a href="https://choosealicense.com/licenses/gpl-3.0/" style="text-decoration:none">
      <img src="https://img.shields.io/github/license/frknkrc44/HMA-OSS?label=License">
    </a>
  </p>
</div>

---

- [English](README.md)
- **中文（简体）**
- [Türkçe](README_tr.md)
- [日本語](README_ja.md)
- [Indonesia](README_id.md)

## 关于该模块
虽然“检测安装的应用”是不正确的做法，但是并不是所有的与 root 相关联的插件类应用都提供了随机包名支持。这就意味着检测到安装了此类应用（如 Fake Location 、存储空间隔离）与检测到了 root 本身区别不大。（会使用检测手段的 app 可不会认为你是在“我就蹭蹭不进去”）  
与此同时，部分“不安分”的应用会使用各种漏洞绕过系统权限来获取你的应用列表，从而对你建立用户画像。（如陈叔叔将安装了 V2Ray 的用户分为一类），或是类似于某某校园某某乐跑的软件会要求你卸载作弊软件。  
该模块提供了一些检测方式用于测试您是否成功地隐藏了某些特定的包名，如 Magisk/Edxposed Manager；同时可作为 Xposed 模块用于隐藏应用列表或特定应用，保护隐私。  

## 更新日志
[参考发布页面](https://github.com/frknkrc44/HMA-OSS/commits)  
