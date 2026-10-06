# 福彩历史数据分析

一个由 Spring Boot 后端和 Vue 2 前端组成的彩票历史数据分析项目，支持双色球、大乐透和快乐8。

## 功能

- 同步和查询双色球、大乐透、快乐8历史开奖数据
- 获取最近几期开奖数据
- 开奖日期补充和历史记录更新
- 历史记录分页展示
- 重复号码分析
- 方格分析、走势连线、遗漏统计
- 共现统计和下期出现统计
- 双色球、大乐透、快乐8单期分析
- 快乐8新旧两套单期推荐分析
- 快乐8自动调权
- 大乐透推荐组合后端过滤：
  - 排除历史前区完全重复组合
  - 排除历史完整前区加后区组合
  - 排除前区四连号

## 项目结构

```text
fucai/
├── backend/                  # Spring Boot 后端
│   ├── src/main/java/         # Controller、Service、Entity、Repository
│   ├── src/main/resources/    # application.yml
│   ├── BACKEND_STRUCTURE.md   # 后端文件说明
│   └── pom.xml
├── front/                    # Vue 2 前端
│   ├── src/                   # 页面和分析组件
│   ├── FRONTEND_STRUCTURE.md  # 前端文件说明
│   ├── package.json
│   └── vue.config.js
├── .gitignore
└── README.md
```

## 环境要求

- Java 8 或更高版本
- Maven 3.6+
- Node.js 18+
- npm
- MySQL 8

## 数据库配置

后端默认连接本机 MySQL 的 `fucai` 数据库。数据库配置位于：

```text
backend/src/main/resources/application.yml
```

可以通过环境变量覆盖默认值：

```bash
export DB_USERNAME=root
export DB_PASSWORD=你的数据库密码
```

启动前请确认 MySQL 已运行，并且用户具备创建数据库和表的权限。

## 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端默认地址：

```text
http://localhost:8080
```

常用接口：

```text
POST /api/ssq/sync-history
POST /api/ssq/fetch-recent?expect=10
GET  /api/ssq

POST /api/dlt/sync-history
POST /api/dlt/fetch-recent?expect=10
GET  /api/dlt
POST /api/dlt/recommendation/filter

POST /api/kl8/sync-history
POST /api/kl8/fetch-recent?expect=10
GET  /api/kl8
```

## 启动前端

```bash
cd front
npm install
npm run serve
```

前端默认地址：

```text
http://localhost:8081
```

前端通过 `front/vue.config.js` 将 `/api` 请求代理到 `http://localhost:8080`。

## 构建

构建前端：

```bash
cd front
npm run build
```

构建后端：

```bash
cd backend
mvn clean package
```

## 说明

- 历史数据来源配置在后端 `application.yml` 中。
- 推荐和分析结果不会替代实际开奖概率，仅用于历史数据分析。
- `backend/target`、`front/node_modules`、`front/dist`、IDE 配置和本地环境文件不会提交到 Git。
