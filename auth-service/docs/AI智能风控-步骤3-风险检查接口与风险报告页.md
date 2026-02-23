# AI 智能风控 - 步骤 3：风险检查接口与风险报告页（合并）

**目标**：在步骤 2 风险引擎与 `GET /api/risk/check` 的基础上，提供风险报告页：用户从首页「AI 智能风控」进入「风险报告」后，页面自动请求该接口并展示当前用户的风险等级、分数与说明。不修改步骤 1、步骤 2 及现有业务代码。

---

## 一、步骤概述

本步骤将「风险检查接口」与「风险报告页」合并为一步说明与实现。

- **风险检查接口**：已在步骤 2 中实现，路径为 `GET /api/risk/check`，从 Session 取当前用户并返回 `RiskResult`（riskDetected、riskLevel、score、message）的 JSON。本步骤仅使用该接口，不重复实现。
- **风险报告页**：新增页面路由与 Thymeleaf 模板，页面加载时调用 `/api/risk/check`，将返回结果展示为风险等级徽章、风险分数、说明文案及「返回首页」链接。

---

## 二、实现内容

### 2.1 风险检查接口（沿用步骤 2）

- **路径**：`GET /api/risk/check`
- **实现位置**：`com.test.Controller.RiskController#checkRisk`
- **返回**：`RiskResult` 的 JSON，字段包括 `riskDetected`、`riskLevel`（"高"/"中"/"低"）、`score`、`message`。
- **说明**：无新增接口代码，风险报告页直接调用此接口即可。

### 2.2 风险报告页路由

- **路径**：`GET /auth/risk-report`
- **实现位置**：`com.test.Controller.RegisterController#riskReportPage`
- **逻辑**：返回视图名 `"risk-report"`，对应模板 `risk-report.html`。无需登录校验，未登录时接口侧按 `"anonymous"` 评估风险。
- **入口**：首页「AI 智能风控」卡片下的「风险报告」按钮，链接即为 `/auth/risk-report`。

### 2.3 风险报告页模板

- **文件**：`auth-service/src/main/resources/templates/risk-report.html`
- **风格**：与现有页面（如身份验证、链上验证）一致：相同 header（Logo + BlockDIA）、卡片容器、底部 footer。
- **内容**：
  - 标题：「AI 智能风控 - 风险报告」，副标题说明为基于近期链上验证与身份查询行为的 AI 风险分析。
  - 加载态：页面打开后先显示「正在加载风险分析结果…」及 loading 图标。
  - 结果区（请求成功后展示）：
    - **风险等级**：以徽章形式展示「风险等级：高/中/低」，并按等级使用不同样式（高-红、中-黄、低-绿）。
    - **风险分数**：展示 `score` 数值。
    - **说明文案**：展示 `message`（如「检测到短时间多次链上验证或身份查询，请确认是否为本人操作。」或「当前行为在正常范围内。」）。
    - **返回首页**：链接至 `/`。
  - 请求方式：页面加载时通过 `axios.get('/api/risk/check')` 获取数据，成功则渲染结果区并隐藏加载态；失败则展示友好提示（如「无法获取风险分析结果，请稍后重试或确认已登录。」）。

---

## 三、文件清单

| 类型 | 路径 |
|------|------|
| 修改 | `auth-service/src/main/java/com/test/Controller/RegisterController.java`（新增 `GET /auth/risk-report`，返回 `"risk-report"`） |
| 新增 | `auth-service/src/main/resources/templates/risk-report.html`（风险报告页模板，调用 `/api/risk/check` 并展示结果） |

---

## 四、自检

- [ ] 首页「AI 智能风控」下「风险报告」链接为 `/auth/risk-report`，点击可进入风险报告页。
- [ ] 风险报告页加载时自动请求 `GET /api/risk/check`，无额外点击提交。
- [ ] 请求成功后展示风险等级（高/中/低）、分数、说明文案，且等级样式区分明显（高-红、中-黄、低-绿）。
- [ ] 请求失败或未登录时，页面有明确提示，且可「返回首页」。
- [ ] 未修改步骤 1、步骤 2 的代码及链上验证、身份查询等现有接口。

完成以上即**步骤 3 结束**。后续可选：步骤 4（Python 轻量级异常检测模型与引擎调用）、或链上验证/身份查询完成后根据风险结果弹窗提示（可选增强）。
