# AI 智能风控 - 步骤 4：Python 轻量级异常检测模型与引擎调用（实现描述）

**说明**：本文档仅描述实现思路与流程，不包含具体代码。确认后再进行代码实现。

---

## 一、是否必须使用 PyCharm？

**不需要。** 使用任意方式均可：

- **命令行**：在项目目录下用 `python app.py` 启动服务、`python train.py` 训练模型即可。
- **VS Code / Cursor / 记事本**：编辑 `.py` 与 `requirements.txt` 后，在终端执行上述命令。
- **PyCharm**：若你习惯用 PyCharm，可用它打开 Python 子目录、配置解释器、运行/调试；**仅为可选**，非步骤 4 的必需环境。

只要本机已安装 **Python 3.7+** 和依赖包（见下文），用哪种编辑器/IDE 都可以完成步骤 4。

---

## 二、步骤 4 要做什么（整体）

在现有「规则评分」之上，增加一个 **Python 微服务**：

1. **Python 端**：用 sklearn 的 **Isolation Forest**（或 One-Class SVM）在**模拟数据**上训练一个异常检测模型；对外提供 **HTTP 接口**（如 `POST /predict`），入参为**特征向量**，出参为**异常分数**（或等级）。
2. **Java 端**：`AiRiskEngine` 在算完特征后，**可选**地请求该 Python 接口；若启用且调用成功，则用模型输出与规则分数**组合**（或仅用模型输出）得到最终风险等级；若 Python 未启动或超时，则**降级为仅用规则评分**，保证功能可用。

这样既保留“规则引擎”的可解释性，又增加“调用 AI 模型”的展示点，答辩/文档可写：**结合规则引擎与轻量级异常检测模型，实现链上行为智能分析**。

---

## 三、Python 侧实现描述

### 3.1 放置位置

- 在 `auth-service` 下新建子目录，例如 **`python-risk`**（或 `ai-model`），与 `src`、`docs` 平级。
- 该目录内仅放 Python 相关文件，不混入 Java 代码。

### 3.2 依赖与环境

- **Python 版本**：3.7 及以上。
- **依赖包**（通过 `requirements.txt` 管理）：
  - `flask` 或 `fastapi` + `uvicorn`：提供 HTTP 服务；
  - `scikit-learn`：Isolation Forest（及可选 numpy、pandas 用于生成训练数据）。
- 使用方式：在该目录下执行 `pip install -r requirements.txt`（建议使用虚拟环境 `venv`，非强制）。

### 3.3 特征与 Java 对齐

- 与当前 `AiRiskEngine` 一致，使用 **4 个特征**（均为数值）：
  - `count_verify_1m`：最近 1 分钟链上验证次数；
  - `count_query_1m`：最近 1 分钟身份查询次数；
  - `count_verify_5m`：最近 5 分钟链上验证次数；
  - `count_query_5m`：最近 5 分钟身份查询次数。
- 可选扩展：`avg_interval`（最近若干次请求的平均间隔，秒）。首版可仅用上述 4 维，便于与 Java 一致。

### 3.4 训练数据与模型

- **无需真实业务数据**：用脚本生成**模拟数据**：
  - **正常样本**：特征值偏小、分散（如 1 分钟内 0～2 次验证、0～3 次查询，5 分钟内次数略高但仍在合理范围）；
  - **异常样本**：特征值偏大、短时密集（如 1 分钟内 5～15 次验证、多次查询），数量可少于正常样本。
- **模型**：使用 `sklearn.ensemble.IsolationForest` 训练；训练完成后将模型**持久化**（如 `joblib.dump` 存为 `model.pkl`），预测服务启动时加载该文件，无需每次启动都重新训练。
- **训练脚本**：单独脚本（如 `train.py`）负责生成数据、训练、保存模型；**预测服务**（如 `app.py`）只负责加载模型并对外提供接口。

### 3.5 预测接口

- **路径**：例如 `POST /predict`（或 `/risk/predict`）。
- **请求体**：JSON，包含上述特征，例如：  
  `{ "count_verify_1m": 2, "count_query_1m": 1, "count_verify_5m": 5, "count_query_5m": 3 }`
- **响应**：JSON，至少包含**异常相关输出**，供 Java 使用，例如：
  - 方案 A：`{ "anomaly_score": 0.12 }`（数值越小越异常，Isolation Forest 的 `score_samples` 可转为 0～1 或 -1～1 的某种尺度）；
  - 方案 B：`{ "risk_level": "高" | "中" | "低" }`（在 Python 内根据异常分阈值分档后返回）。
- **端口**：固定一个端口，如 **5000**，便于 Java 配置（如 `http://localhost:5000/predict`）。

### 3.6 运行方式

- 开发/演示：在该 Python 目录下执行 `python app.py`（或 `uvicorn app:app --host 0.0.0.0 --port 5000`），服务常驻；需要更新模型时，先运行 `train.py` 生成新的 `model.pkl`，再重启 `app.py`。
- 不启动 Python 服务时，系统仍可正常运行：Java 侧降级为仅规则评分（见下文）。

---

## 四、Java 侧实现描述

### 4.1 与 AiRiskEngine 的衔接

- **现有逻辑**：`AiRiskEngine#evaluateRisk(userId)` 已具备“取行为 → 算 4 个特征 → 规则评分 → 分档”的完整流程。
- **步骤 4 新增**：
  1. 在算完 4 个特征后，**若启用模型调用**（通过配置项或 feature flag，如 `ai.risk.model.enabled=true`），则用 **RestTemplate 或 WebClient** 向 Python 服务发送 **POST** 请求，请求体为 `{ count_verify_1m, count_query_1m, count_verify_5m, count_query_5m }`。
  2. 若请求**成功**且返回体包含异常分或等级：
     - **组合方式**：例如用规则分数与模型异常分的加权组合得到最终 score，再按现有阈值分档；或
     - **替代方式**：直接采用模型返回的等级作为最终 `riskLevel`，规则分数仅作备用或展示。
  3. 若请求**失败**（Python 未启动、超时、网络错误等）：**不抛异常**，直接使用当前规则评分与分档结果，即与“未启用模型”时行为一致。

### 4.2 配置项（建议）

- `ai.risk.model.enabled`：是否调用 Python 模型（true/false），默认可设为 false，便于未部署 Python 时仍正常使用。
- `ai.risk.model.url`：Python 预测接口完整 URL，如 `http://localhost:5000/predict`。
- `ai.risk.model.timeoutMs`：超时时间（毫秒），超时则降级为规则。

### 4.3 不改动现有逻辑的前提

- 规则评分与分档的代码**保留**；仅在“启用模型且调用成功”时，用模型结果**覆盖或叠加**最终输出。
- 不修改步骤 1（行为记录）、步骤 2（引擎核心特征计算）、步骤 3（风险报告页）的既有行为；仅扩展 `AiRiskEngine` 的“决策”部分。

---

## 五、数据流小结

```
用户行为（步骤 1 已记录）
    ↓
AiRiskEngine：取近期记录 → 计算 count_verify_1m, count_query_1m, count_verify_5m, count_query_5m
    ↓
    ├─ 若未启用模型或调用失败 → 仅用规则分数分档 → RiskResult
    └─ 若启用且调用成功 → 将 4 个特征 POST 到 Python /predict
                                ↓
                        Python 加载 model.pkl → 输出 anomaly_score 或 risk_level
                                ↓
                        Java 将模型输出与规则组合（或直接采用）→ RiskResult
```

---

## 六、文件与目录规划（实现时参考）

| 类型 | 路径（示例） |
|------|------------------|
| 目录 | `auth-service/python-risk/` |
| 依赖 | `auth-service/python-risk/requirements.txt` |
| 训练 | `auth-service/python-risk/train.py`（生成数据、训练、保存 model.pkl） |
| 服务 | `auth-service/python-risk/app.py`（加载模型、暴露 POST /predict） |
| 模型 | `auth-service/python-risk/model.pkl`（训练后生成，可不提交版本库） |
| 配置 | `auth-service/src/main/resources/application.yml` 或 `application.properties` 中增加 `ai.risk.model.*` |
| 修改 | `auth-service/src/main/java/com/test/risk/AiRiskEngine.java`（在现有流程中增加“可选调用 Python”的分支） |

---

## 七、自检要点（实现后）

- [ ] Python 目录可独立运行：`pip install -r requirements.txt` 后，`train.py` 能生成 `model.pkl`，`app.py` 能启动并响应 `POST /predict`。
- [ ] 请求体为 4 个特征时，Python 返回合法 JSON；异常样本输入时，异常分或等级能体现“异常”。
- [ ] Java 配置为不启用模型时，行为与步骤 2、3 完全一致（仅规则）。
- [ ] Java 配置为启用模型且 Python 未启动（或超时）时，不报错，降级为规则评分。
- [ ] 未修改步骤 1、2、3 的既有逻辑与接口契约。

---

确认该描述后，再进行步骤 4 的代码实现（Python 项目 + Java 引擎扩展 + 配置 + 步骤 4 文档）。
