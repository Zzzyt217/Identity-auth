# AI 智能风控 - Python 异常检测服务

本目录为步骤 4 的 Python 侧：训练异常检测模型并提供 POST /predict 接口，供 Java AiRiskEngine 调用。

## 快速启动（请按顺序执行）

1. **安装依赖**（在本目录下）  
   ```bash
   pip install -r requirements.txt
   ```

2. **训练模型**（首次或更新模型时）  
   ```bash
   python train.py
   ```  
   会生成 `model.pkl`。

3. **启动服务**（保持终端不关）  
   ```bash
   python app.py
   ```  
   服务地址：http://localhost:5000 ，预测接口：POST /predict

4. 在 Java 的 `application.properties` 中将 `ai.risk.model.enabled=true` 并重启应用后，风险引擎会请求本服务；未启动本服务时 Java 自动降级为仅规则评分。

详细说明见：`auth-service/docs/AI智能风控-步骤4-Python异常检测模型与引擎调用.md`
