# AI 智能风控 - 步骤 6：风险弹窗

**目标**：在用户完成一次链上验证或身份查询后，若本次请求的响应中附带的风险检查结果为“存在风险”（riskDetected 为 true），则在前端弹出 Modal 提示风险等级与说明，不阻断业务流程。

---

## 一、步骤概述

1. **后端**：在链上验证（身份验证、操作认证）与身份查询的接口返回中，增加 **risk** 字段，值为当前用户的一次 `AiRiskEngine.evaluateRisk(userId)` 结果（riskDetected、riskLevel、score、message）。
2. **前端**：在身份验证页、操作认证页、身份查询页的请求成功回调中，若 `res.data.risk && res.data.risk.riskDetected === true`，则显示风险弹窗（Modal），展示风险等级与 message；用户点击「确定」关闭弹窗。

---

## 二、实现内容

### 2.1 后端

- **ChainVerifyController**
  - 注入 `AiRiskEngine`。
  - 新增私有方法 `attachRisk(Map result, String userId)`：调用 `aiRiskEngine.evaluateRisk(userId)`，将返回的 RiskResult 转为 Map（riskDetected、riskLevel、score、message）并放入 `result.put("risk", risk)`。
  - 在 **verifyIdentity** 的每条返回路径前调用 `attachRisk(result, userId)`（包括参数错误、无记录、链未就绪、PASS/MISMATCH/ERROR 等）。
  - 在 **verifyAudit** 中：两条早期返回前调用 `attachRisk(result, userId)`；对 `verifyAuditRecord`、`verifyIdentityRecord` 的返回值在 return 前调用 `attachRisk(返回值, userId)`；最后一条 NO_RECORD 返回前同样调用 `attachRisk(result, userId)`。

- **IdentityController**
  - 注入 `AiRiskEngine`。
  - 在 **query** 方法中，当查询成功、构建好返回的 `data` 后，调用 `aiRiskEngine.evaluateRisk(userId)`，将结果放入 `data.put("risk", risk)`，再 return data。

### 2.2 前端

- **verify-identity.html**（身份验证）
  - 增加风险弹窗：遮罩层 + 弹窗框，内含标题「AI 智能风控提示」、风险等级徽章、说明文案、「确定」按钮。
  - 在 `axios.get('/api/chain/verify/identity')` 的 then 中，展示完验证结果后，若 `res.data.risk && res.data.risk.riskDetected`，则设置弹窗内容（riskLevel、message）并显示弹窗；「确定」点击后关闭弹窗。

- **verify-audit.html**（操作认证）
  - 同上，增加相同风格的风险弹窗；在 `axios.get('/api/chain/verify/audit')` 的 then 中根据 `res.data.risk.riskDetected` 决定是否弹窗。

- **query.html**（身份查询）
  - 同上，增加风险弹窗；在 `axios.get('/api/identity/query')` 的 then 中，当 `res.data.success` 且 `res.data.risk && res.data.risk.riskDetected` 时显示弹窗。

弹窗仅做提示，不阻止用户继续操作；验证/查询结果照常展示。

---

## 三、文件清单

| 类型 | 路径 |
|------|------|
| 修改 | `auth-service/src/main/java/com/test/Controller/ChainVerifyController.java`（AiRiskEngine、attachRisk、各返回前附加 risk） |
| 修改 | `auth-service/src/main/java/com/test/Controller/IdentityController.java`（AiRiskEngine、query 返回中附加 risk） |
| 修改 | `auth-service/src/main/resources/templates/verify-identity.html`（弹窗 HTML/CSS/JS） |
| 修改 | `auth-service/src/main/resources/templates/verify-audit.html`（弹窗 HTML/CSS/JS） |
| 修改 | `auth-service/src/main/resources/templates/query.html`（弹窗 HTML/CSS/JS） |

---

## 四、自检

- [ ] 链上验证（身份验证、操作认证）接口的响应中均包含 `risk` 对象（含 riskDetected、riskLevel、score、message）。
- [ ] 身份查询成功时的响应中包含 `risk` 对象。
- [ ] 当近期行为触发风险时（如短时多次验证/查询），三个页面在请求成功后均能弹出风险提示框，展示等级与说明；点击「确定」后弹窗关闭。
- [ ] 未改变验证/查询本身的业务逻辑与成功/失败判定。

完成以上即**步骤 6 结束**。
