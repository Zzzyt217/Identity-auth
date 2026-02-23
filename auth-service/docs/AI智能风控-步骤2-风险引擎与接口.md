# AI 智能风控 - 步骤 2：风险引擎与接口

**目标**：在步骤 1 行为记录的基础上，实现“AI 风险分析模块”：从近期行为计算特征、按规则评分、输出风险等级与说明，并对外提供 `GET /api/risk/check` 接口。不修改步骤 1 及现有业务代码。

---

## 一、步骤概述

1. 定义风险检查结果的数据结构（API 返回体）。
2. 实现风险引擎：读取某用户近期行为 → 计算特征（1 分钟/5 分钟窗口内各类型次数）→ 规则评分 → 分档得到风险等级与是否触发风险。
3. 新增 `GET /api/risk/check`：从 Session 取当前用户，调用风险引擎，返回 JSON。

---

## 二、实现内容

### 2.1 风险结果结构

- **类**：`com.test.risk.RiskResult`
- **字段**（供前端与文档使用）：
  - `riskDetected`：boolean，是否判定存在风险（需提示用户）
  - `riskLevel`：String，风险等级，取值为 `"高"`、`"中"`、`"低"`
  - `score`：double，风险分数（可解释的标量，便于展示与调试）
  - `message`：String，简短说明文案（如“检测到短时间多次链上验证或身份查询，请确认是否为本人操作。”）

### 2.2 风险引擎（AiRiskEngine）

- **类**：`com.test.risk.AiRiskEngine`，`@Service`
- **依赖**：`BehaviorRecordService`（步骤 1 提供）
- **核心方法**：`evaluateRisk(userId)` → `RiskResult`

**流程简述**：

1. **取数据**：当前时间 `now`，取最近 5 分钟内的行为记录（`getRecentRecords(userId, now - 5分钟)`）。
2. **特征**：
   - 最近 1 分钟内：链上验证次数、身份查询次数；
   - 最近 5 分钟内：链上验证次数、身份查询次数。
3. **评分**（规则公式，可解释）：
   - `score = 2.0×验证1分钟 + 1.0×查询1分钟 + 0.5×验证5分钟 + 0.3×查询5分钟`
   - 近期、高敏感行为权重大，体现“短时密集更可疑”。
4. **分档与决策**：
   - `score >= 60` → 风险等级 `"高"`，`riskDetected = true`；
   - `score >= 30` → 风险等级 `"中"`，`riskDetected = true`；
   - 否则 → 风险等级 `"低"`，`riskDetected = false`。
5. **文案**：当 `riskDetected` 为 true 时，`message` 为“检测到短时间多次链上验证或身份查询，请确认是否为本人操作。”；否则为“当前行为在正常范围内。”等。

若无近期记录，直接返回低风险、分数 0、对应说明。

### 2.3 风险检查接口

- **类**：`com.test.Controller.RiskController`
- **路径**：`GET /api/risk/check`
- **鉴权**：从当前 `HttpSession` 取 `user`（测试用户 user1/user2/user3），若无则使用 `"anonymous"` 作为 userId。
- **逻辑**：调用 `AiRiskEngine#evaluateRisk(userId)`，将返回的 `RiskResult` 序列化为 JSON 返回。
- **返回示例**：
  - 有风险：`{"riskDetected":true,"riskLevel":"高","score":65.0,"message":"检测到短时间多次链上验证或身份查询，请确认是否为本人操作。"}`
  - 无风险：`{"riskDetected":false,"riskLevel":"低","score":5.0,"message":"当前行为在正常范围内。"}`

---

## 三、文件清单

| 类型 | 路径 |
|------|------|
| 新增 | `auth-service/src/main/java/com/test/risk/RiskResult.java` |
| 新增 | `auth-service/src/main/java/com/test/risk/AiRiskEngine.java` |
| 新增 | `auth-service/src/main/java/com/test/Controller/RiskController.java` |

---

## 四、自检

- [ ] `RiskResult` 具备上述四个字段及 getter/setter，可被 Spring MVC 序列化为 JSON。
- [ ] `AiRiskEngine` 仅依赖 `BehaviorRecordService`，不依赖链上、身份等业务服务；`evaluateRisk` 仅做特征计算与规则评分，无副作用。
- [ ] `GET /api/risk/check` 能从 Session 正确取 user，未登录或无 user 时使用 `"anonymous"`，返回结构符合 `RiskResult`。
- [ ] 未修改步骤 1 的行为记录逻辑及链上验证、身份查询等现有接口。

完成以上即**步骤 2 结束**。可进行步骤 3（或步骤 5）：风险报告页调用 `/api/risk/check` 并展示结果；后续可选步骤 4：Python 轻量级异常检测模型与引擎调用。
