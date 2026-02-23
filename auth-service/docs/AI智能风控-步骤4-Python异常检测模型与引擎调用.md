# AI 智能风控 - 步骤 4：Python 异常检测模型与引擎调用

**目标**：在规则评分基础上，增加 Python 轻量级异常检测服务；Java 风险引擎在配置启用时请求该服务，成功则采用模型返回的风险等级，失败则降级为仅规则评分。

> **若你使用 PyCharm 和 Python 3.13**，请直接看更细的操作说明：**[AI智能风控-步骤4-Python操作流程（PyCharm）.md](./AI智能风控-步骤4-Python操作流程（PyCharm）.md)**，按其中“打开 python-risk → 指定解释器 → 安装依赖 → 训练模型 → 启动 app.py”的顺序操作即可。

---

## 一、步骤概述

1. 在 `auth-service/python-risk/` 下新增 Python 项目：依赖、训练脚本、预测服务。
2. Java 配置中增加“是否启用模型、模型 URL、超时”。
3. `AiRiskEngine` 在算完特征后，若启用则 POST 到 Python，用返回的 `risk_level` 作为最终等级；否则或失败时仍用规则分档。

---

## 二、文件清单

| 类型 | 路径 |
|------|------|
| 新增 | `auth-service/python-risk/requirements.txt` |
| 新增 | `auth-service/python-risk/train.py`（生成数据、训练、保存 model.pkl） |
| 新增 | `auth-service/python-risk/app.py`（加载模型、POST /predict） |
| 修改 | `auth-service/src/main/resources/application.properties`（ai.risk.model.*） |
| 修改 | `auth-service/src/main/java/com/test/risk/AiRiskEngine.java`（可选调用 Python + 降级） |

---

## 三、Python 使用与启动（请按顺序操作）

### 第一步：安装 Python 环境（若尚未安装）

- 本机需已安装 **Python 3.7 及以上**。
- 在命令行执行 `python --version` 或 `python3 --version` 确认版本。
- 无需 PyCharm，使用命令行或任意编辑器即可。

### 第二步：进入 python-risk 目录并安装依赖

在终端（PowerShell 或 CMD）中执行：

```bash
cd F:\Identity_authentication（毕设）\auth-service\python-risk
pip install -r requirements.txt
```

若使用 `python3` 或希望隔离环境，可先创建虚拟环境（可选）：

```bash
cd F:\Identity_authentication（毕设）\auth-service\python-risk
python -m venv venv
venv\Scripts\activate
pip install -r requirements.txt
```

**说明**：路径中的括号若在命令行中报错，可先 `cd` 到 `auth-service` 再 `cd python-risk`，或给路径加引号。

### 第三步：训练模型（生成 model.pkl）

在同一目录下执行：

```bash
python train.py
```

成功时会输出类似：

- `模型已保存至: ...\python-risk\model.pkl`
- `特征顺序: ['count_verify_1m', 'count_query_1m', 'count_verify_5m', 'count_query_5m']`

**请确认当前目录下已生成 `model.pkl` 文件**，否则下一步启动服务会报错。

### 第四步：启动 Python 预测服务

仍在该目录下执行：

```bash
python app.py
```

成功时会输出：

- `模型已加载，服务启动在 http://127.0.0.1:5000`
- `预测接口: POST http://127.0.0.1:5000/predict`

**请保持该终端窗口不要关闭**，服务会一直运行。关闭窗口即停止服务，Java 将自动降级为仅规则评分。

### 第五步：可选 - 自测 Python 接口

新开一个终端，用 curl 或 Postman 测试（PowerShell 示例）：

```powershell
Invoke-RestMethod -Uri "http://localhost:5000/predict" -Method POST -ContentType "application/json" -Body '{"count_verify_1m":10,"count_query_1m":5,"count_verify_5m":20,"count_query_5m":15}'
```

若返回包含 `anomaly_score` 和 `risk_level` 的 JSON，说明 Python 服务正常。

---

## 四、Java 侧配置与行为

- **application.properties** 中已增加：
  - `ai.risk.model.enabled=false`：默认不启用，避免未启动 Python 时请求失败。
  - `ai.risk.model.url=http://localhost:5000/predict`：Python 预测地址。
  - `ai.risk.model.timeoutMs=2000`：超时时间（毫秒）。

- **启用模型**：将 `ai.risk.model.enabled=true` 后重启 auth-service，并**确保 Python 已按上文第三步、第四步启动**；否则请求会超时，引擎自动降级为规则评分。

- **AiRiskEngine**：在算完 4 个特征后，若 `modelEnabled` 为 true，则 POST 上述 4 个特征到 `modelUrl`；若返回中有 `risk_level`（高/中/低），则以此作为最终风险等级；若请求异常或超时，则使用原有规则分档结果。

---

## 五、推荐使用顺序（简要）

1. 安装 Python 3.7+，在 `python-risk` 目录执行 `pip install -r requirements.txt`。
2. 执行 `python train.py`，确认生成 `model.pkl`。
3. 执行 `python app.py`，保持窗口不关。
4. 将 `ai.risk.model.enabled` 改为 `true`，重启 auth-service。
5. 使用测试用户进行链上验证/身份查询后，打开「风险报告」页，可看到规则与模型共同作用的结果；关闭 Python 后刷新，则仅为规则评分。

---

## 六、自检

- [ ] `python-risk` 目录下存在 `requirements.txt`、`train.py`、`app.py`。
- [ ] 执行 `train.py` 后存在 `model.pkl`；执行 `app.py` 后终端无报错且提示服务已启动。
- [ ] `ai.risk.model.enabled=false` 时，系统行为与步骤 2、3 一致（仅规则）。
- [ ] `ai.risk.model.enabled=true` 且 Python 服务已启动时，风险报告页能正常返回结果；关闭 Python 后不报错，仅降级为规则评分。

完成以上即**步骤 4 结束**。
