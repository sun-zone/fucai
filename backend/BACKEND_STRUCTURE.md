# 后端项目结构说明

后端位于 `backend/`，基于 Spring Boot 2.7 + Spring MVC + Spring Data JPA + MySQL + Jsoup 构建，主要负责抓取彩票历史数据、持久化开奖记录、提供 API 给前端查询和分析。

## 目录结构

```text
backend/
├── pom.xml
└── src/
    └── main/
        ├── java/
        │   └── com/example/fucai/
        │       ├── FucaiBackendApplication.java
        │       ├── config/
        │       │   └── WebConfig.java
        │       ├── controller/
        │       │   ├── DltController.java
        │       │   ├── Kl8Controller.java
        │       │   └── SsqController.java
        │       ├── entity/
        │       │   ├── DltRecord.java
        │       │   ├── Kl8Record.java
        │       │   └── SsqRecord.java
        │       ├── repository/
        │       │   ├── DltRecordRepository.java
        │       │   ├── Kl8RecordRepository.java
        │       │   └── SsqRecordRepository.java
        │       └── service/
        │           ├── DltCrawlerService.java
        │           ├── DltRecommendationFilterRequest.java
        │           ├── DltRecommendationFilterService.java
        │           ├── Kl8CrawlerService.java
        │           ├── RepeatMatch.java
        │           ├── RepeatStat.java
        │           ├── SsqCrawlerService.java
        │           └── SsqFetchResult.java
        └── resources/
            └── application.yml
```

## 根目录文件

### `pom.xml`

Maven 项目配置。

主要内容：

- Spring Boot 版本：`2.7.18`
- Java 版本：`8`
- 打包方式：`jar`
- 主要依赖：
  - `spring-boot-starter-web`：提供 REST API。
  - `spring-boot-starter-data-jpa`：数据库 ORM 和 Repository。
  - `jsoup`：抓取并解析网页。
  - `mysql-connector-j`：连接 MySQL。

## 启动入口

### `FucaiBackendApplication.java`

Spring Boot 启动类。

作用：

- 启动后端应用。
- 扫描当前包下的 controller、service、repository、entity 等 Spring 组件。

## 配置文件

### `src/main/resources/application.yml`

后端运行配置。

主要配置：

- 服务端口：`8080`
- MySQL 连接：
  - 数据库：`fucai`
  - 用户名：`root`
  - 密码：`root1234`
- JPA 配置：
  - `ddl-auto: update`，实体变化会自动更新表结构。
- 抓取源配置：
  - 双色球走势地址
  - 双色球历史地址
  - 大乐透历史地址
  - 快乐8历史地址
  - 快乐8开奖日期补充地址

## 配置类

### `config/WebConfig.java`

Web 配置类。

作用：

- 对 `/api/**` 开启跨域访问。
- 允许前端开发服务器访问后端接口。
- 允许常见 HTTP 方法：`GET`、`POST`、`PUT`、`DELETE`、`OPTIONS`。

## Controller 层

Controller 负责接收前端请求，并调用对应 Service。

### `controller/SsqController.java`

双色球接口。

基础路径：`/api/ssq`

接口：

- `POST /api/ssq/fetch`：按参数抓取最近数据，默认 1000 期。
- `POST /api/ssq/sync-history`：同步双色球历史数据。
- `POST /api/ssq/fetch-recent?expect=10`：抓取最近几期。
- `GET /api/ssq`：查询双色球历史记录。
- `GET /api/ssq/repeats?matchCount=3`：重复号码分析。

### `controller/DltController.java`

大乐透接口。

基础路径：`/api/dlt`

接口：

- `POST /api/dlt/sync-history`：同步大乐透历史数据。
- `POST /api/dlt/fetch-recent?expect=10`：抓取最近几期。
- `GET /api/dlt`：查询大乐透历史记录。
- `GET /api/dlt/repeats?matchCount=3`：重复号码分析。
- `POST /api/dlt/recommendation/filter`：按所选期号之前的历史记录过滤和修正推荐组合。

### `controller/Kl8Controller.java`

快乐8接口。

基础路径：`/api/kl8`

接口：

- `POST /api/kl8/sync-history`：同步快乐8历史数据。
- `POST /api/kl8/fetch-recent?expect=10`：抓取最近几期。
- `GET /api/kl8`：查询快乐8历史记录。
- `GET /api/kl8/repeats?matchCount=10`：重复号码分析。

## Entity 层

Entity 对应数据库表。

### `entity/SsqRecord.java`

双色球开奖记录实体。

数据库表：`ssq_record`

主要字段：

- `id`：主键。
- `expect`：期号，唯一。
- `red1` 到 `red6`：6 个红球。
- `blue`：蓝球。
- `drawDate`：开奖日期。
- `createdAt`：创建时间。

### `entity/DltRecord.java`

大乐透开奖记录实体。

数据库表：`dlt_record`

主要字段：

- `id`：主键。
- `expect`：期号，唯一。
- `front1` 到 `front5`：前区号码。
- `back1`、`back2`：后区号码。
- `drawDate`：开奖日期。
- `createdAt`：创建时间。

### `entity/Kl8Record.java`

快乐8开奖记录实体。

数据库表：`kl8_record`

主要字段：

- `id`：主键。
- `expect`：期号，唯一。
- `num1` 到 `num20`：20 个开奖号码。
- `drawDate`：开奖日期。
- `createdAt`：创建时间。

## Repository 层

Repository 负责数据库访问，基于 Spring Data JPA。

### `repository/SsqRecordRepository.java`

双色球记录 Repository。

主要作用：

- 保存双色球记录。
- 按期号查询记录。
- 查询全部记录。

### `repository/DltRecordRepository.java`

大乐透记录 Repository。

主要作用：

- 保存大乐透记录。
- 按期号查询记录。
- 查询全部记录。

### `repository/Kl8RecordRepository.java`

快乐8记录 Repository。

主要作用：

- 保存快乐8记录。
- 按期号查询记录。
- 查询全部记录。

## Service 层

Service 是主要业务层，负责抓取、解析、入库和重复分析。

### `service/SsqCrawlerService.java`

双色球业务服务。

主要职责：

- 从配置的双色球源网站抓取数据。
- 解析最近数据和历史数据。
- 保存或更新 `ssq_record`。
- 查询全部双色球记录，按期号倒序返回。
- 分析每期红球与其他历史期号恰好重复 N 个号码的情况。

### `service/DltCrawlerService.java`

大乐透业务服务。

主要职责：

- 抓取大乐透历史数据。
- 按最近 N 期截取并保存。
- 保存或更新 `dlt_record`。
- 查询全部大乐透记录。
- 分析前区号码重复情况。

### `service/DltRecommendationFilterRequest.java`

大乐透推荐过滤接口的请求模型，接收前端原始推荐组、前区/后区号码和所选期号。

### `service/DltRecommendationFilterService.java`

大乐透推荐组合的后端过滤服务。

主要职责：

- 只使用所选期号之前的历史记录。
- 排除历史上已经出现过的前区五号码组合。
- 排除历史上已经出现过的完整前区加后区组合。
- 排除包含四个连续前区号码的组合。
- 原组合被过滤时，从合法组合中选择与原组合重合度最高的组合补位。
- 不创建全量组合表，组合在请求处理时动态判断。

### `service/Kl8CrawlerService.java`

快乐8业务服务。

主要职责：

- 抓取快乐8历史数据。
- 抓取或补充开奖日期。
- 保存或更新 `kl8_record`。
- 查询全部快乐8记录。
- 分析号码重复情况。

### `service/RepeatStat.java`

重复分析结果 DTO。

作用：

- 表示某一期的重复分析汇总。
- 包含当前期号、当前号码、后区号码、匹配到的历史期列表。
- 前端重复分析表格直接消费这个结构。

### `service/RepeatMatch.java`

重复分析中的单条匹配 DTO。

作用：

- 表示某一期与另一历史期的匹配结果。
- 包含匹配期号、该期号码、后区号码、重复号码列表。

### `service/SsqFetchResult.java`

抓取保存结果 DTO。

作用：

- 返回给前端本次解析、插入、跳过的数据数量。
- 字段含义：
  - `parsedCount`：解析到的记录数。
  - `insertedCount`：新增记录数。
  - `skippedCount`：已存在或更新处理的记录数。

## 数据流说明

1. 前端调用 `/api/{lottery}/sync-history` 或 `/api/{lottery}/fetch-recent`。
2. Controller 将请求转给对应 CrawlerService。
3. Service 使用 Jsoup 请求源网站 HTML。
4. Service 解析 HTML 表格，生成 Entity。
5. Service 根据期号判断新增或更新。
6. Repository 将数据保存到 MySQL。
7. 前端调用 `/api/{lottery}` 获取历史记录。
8. 前端调用 `/api/{lottery}/repeats` 获取重复分析结果。

## 运行方式

```bash
cd backend
mvn spring-boot:run
```

后端默认运行在：

```text
http://localhost:8080
```

## 数据库说明

默认连接本地 MySQL：

```text
jdbc:mysql://localhost:3306/fucai
```

如果数据库不存在，连接参数中包含：

```text
createDatabaseIfNotExist=true
```

JPA 配置为：

```yaml
ddl-auto: update
```

所以启动后会根据实体自动创建或更新表结构。
