# [Mirai New Bing](https://github.com/cssxsh/mirai-new-bing)

> 基于 Mirai Console 的 New Bing Chat Bot 插件

[![maven-central](https://img.shields.io/maven-central/v/xyz.cssxsh.mirai/mirai-new-bing)](https://search.maven.org/artifact/xyz.cssxsh.mirai/mirai-new-bing)
[![test](https://github.com/cssxsh/mirai-new-bing/actions/workflows/test.yml/badge.svg)](https://github.com/cssxsh/mirai-new-bing/actions/workflows/test.yml)

由于微软还未在中国大陆开放 `new bing` 的使用，以国内IP去访问 `bing` 会导致跳转 `404` 而无法使用  
故需要配置代理 `proxy`

## 效果

![example](.github/screenshot.png)

## 配置

`bing.yml` 基本配置

*   `proxy` 代理, 协议支持 `socks` 和 `http`, 例如 `socks://127.0.0.1:7890`
*   `timeout` API超时时间
*   `cookie` New Bing 网页 Cookie
*   `chat_prefix` 触发前缀, 默认 `bing`
*   `show_source_attributions` 输出来源信息, 默认 `true`

## 安装

### MCL 指令安装

**请确认 mcl.jar 的版本是 2.1.0+**  
`./mcl --update-package xyz.cssxsh.mirai:mirai-new-bing --channel maven-stable --type plugins`

### 手动安装

1.  从 [Releases](https://github.com/cssxsh/mirai-new-bing/releases) 或者 [Maven](https://repo1.maven.org/maven2/xyz/cssxsh/mirai/mirai-new-bing/) 下载 `mirai2.jar`
2.  将其放入 `plugins` 文件夹中

## [爱发电](https://afdian.net/@cssxsh)

![afdian](.github/afdian.jpg)