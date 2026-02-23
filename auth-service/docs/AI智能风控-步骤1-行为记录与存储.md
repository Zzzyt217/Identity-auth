# AI 智能风控 - 步骤 1：行为记录与存储

**目标**：把“谁在什么时候做了哪种操作”记录下来，供后续风险引擎计算特征。不修改原有业务逻辑，仅在链上验证、身份查询入口增加一次记录调用。

---

## 一、步骤概述

1. 定义行为记录数据结构与行为类型常量。
2. 实现内存存储：按用户保存近期行为，限制条数与时间窗口，避免无限增长。
3. 在链上验证、身份查询的入口各调用一次记录接口。

---

## 二、实现内容

### 2.1 行为记录数据结构

- **类**：`com.test.risk.BehaviorRecord`
- **字段**：
  - `userId`：当前用户（Session 的 `user` 或 `"anonymous"`）
  - `actionType`：行为类型，见下表
  - `timestamp`：操作时间（毫秒）

- **行为类型常量**（在 `BehaviorRecordService` 中定义）：

| 常量 | 含义 |
|------|------|
| `CHAIN_VERIFY` | 链上验证（身份验证 / 操作认证 均记为此类） |
| `IDENTITY_QUERY` | 身份查询（按 DID 或工号查询） |

### 2.2 行为记录存储服务

- **类**：`com.test.risk.BehaviorRecordService`
- **存储**：内存 `ConcurrentHashMap<String, Deque<BehaviorRecord>>`，key 为 `userId`，value 为该用户最近若干条记录。
- **策略**：
  - 单用户最多保留 **500 条**；
  - 只保留最近 **5 分钟**内的记录，超时或超条数时从队头删除。
- **提供方法**：
  - `record(userId, actionType, timestamp)`：记录一次行为（链上验证、身份查询入口调用）。
  - `getRecentRecords(userId, since)`：获取某用户在某时间戳之后的记录（供步骤 2 风险引擎使用）。

### 2.3 打点位置（对现有代码的改动）

- **链上验证**
  - 入口 1：`ChainVerifyController#verifyIdentity`（GET `/api/chain/verify/identity`）  
    在方法开始处（参数校验之后）：从 Session 取 `user` 作为 userId，若无则为 `"anonymous"`，调用  
    `behaviorRecordService.record(userId, ACTION_CHAIN_VERIFY, System.currentTimeMillis())`。
  - 入口 2：`ChainVerifyController#verifyAudit`（GET `/api/chain/verify/audit`）  
    同样在方法开始处取 userId 并调用 `record(userId, ACTION_CHAIN_VERIFY, ...)`。

- **身份查询**
  - 入口：`IdentityController#query`（GET `/api/identity/query`）  
    在方法开始处取 userId，调用  
    `behaviorRecordService.record(userId, ACTION_IDENTITY_QUERY, System.currentTimeMillis())`。

- **说明**：仅新增上述一行调用，不改变原有校验、查库、上链、返回等逻辑。

---

## 三、文件清单

| 类型 | 路径 |
|------|------|
| 新增 | `auth-service/src/main/java/com/test/risk/BehaviorRecord.java` |
| 新增 | `auth-service/src/main/java/com/test/risk/BehaviorRecordService.java` |
| 修改 | `auth-service/src/main/java/com/test/Controller/ChainVerifyController.java`（注入 BehaviorRecordService，两处 record 调用 + HttpSession 参数） |
| 修改 | `auth-service/src/main/java/com/test/Controller/IdentityController.java`（注入 BehaviorRecordService，一处 record 调用 + HttpSession 参数） |

---

## 四、自检

- [ ] `BehaviorRecord` 与 `BehaviorRecordService` 已创建，且服务被 Spring 扫描（`@Service`）。
- [ ] 链上验证（身份验证、操作认证）请求时，Session 中有 `user` 则按该用户记录，否则按 `anonymous` 记录。
- [ ] 身份查询请求时，同样按当前用户或 `anonymous` 记录。
- [ ] 未改动链上验证、身份查询的原有业务逻辑与返回值。

完成以上即**步骤 1 结束**。可进行步骤 2：风险引擎（特征计算 + 规则评分 + 可选模型）与 `GET /api/risk/check` 接口。
