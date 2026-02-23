# Python 风险模型 - 操作流程（PyCharm / IntelliJ IDEA + Python 3.13）

本说明面向已安装 **Python 3.13** 的本机，用最简步骤完成：**打开 python-risk → 安装依赖 → 训练模型 → 启动服务**。  
可用 **PyCharm**，也可在 **IntelliJ IDEA** 中完成（见下方「用 IntelliJ IDEA 操作」）。

---

## 用 IntelliJ IDEA 操作（不用 PyCharm）

**可以。** 在 IntelliJ IDEA 里安装 **Python 插件** 后，就能像 PyCharm 一样识别并运行 `.py` 文件；**不装插件**也可以，只用 IDEA 自带的 **Terminal** 执行命令即可。

### 方式一：只用电终端（无需装插件）

1. 用 **IDEA 打开你的毕设项目**（例如 `Identity_authentication（毕设）` 或 `auth-service`），左侧应能看到 **python-risk** 文件夹。
2. 点击 IDEA 底部 **Terminal** 标签，打开终端。
3. 进入 python-risk 目录（路径按你本机改，含括号用英文双引号）：
   ```bash
   cd auth-service/python-risk
   ```
   若当前在项目根目录，可用：`cd "auth-service/python-risk"`。
4. **安装依赖**：执行  
   `python3 -m pip install -r requirements.txt`  
   出现 “Successfully installed ...” 即完成。
5. **训练模型**：执行  
   `python train.py`  
   看到“模型已保存至 ... model.pkl”即成功；在项目树里刷新可看到 **model.pkl**。
6. **启动服务**：执行  
   `python app.py`  
   看到“服务启动在 http://127.0.0.1:5000”后**不要关终端**，服务会一直跑。需要停时在终端按 `Ctrl+C`。

这样 Python 端就已在 IDEA 里完成，无需 PyCharm。

### 方式二：安装 Python 插件后像 PyCharm 一样运行

1. 在 IDEA 中：**File → Settings**（Windows）或 **IntelliJ IDEA → Preferences**（Mac）→ 左侧 **Plugins**，搜索 **Python**，安装 **Python** 或 **Python Community Edition** 插件，重启 IDEA。
2. 为项目配置 Python 解释器：**File → Settings → Languages & Frameworks → Python**，右侧 **Python Interpreter** 选或添加本机 **Python 3.13**。
3. 在项目树中展开 **python-risk**，**右键** `train.py` → **Run 'train'**；再 **右键** `app.py` → **Run 'app'**，即可像 PyCharm 一样一键运行，无需在终端输入命令。

**小结**：不装插件就用 IDEA 的 Terminal 按上面命令执行；装 Python 插件后可在 IDEA 里右键运行 `train.py` 和 `app.py`，两种方式都能解决 Python 端启动。

---

## 一、用 PyCharm 打开 python-risk 目录

1. 打开 **PyCharm**。
2. 菜单栏选 **File → Open**（或 **打开**）。
3. 在弹窗里进入你的项目路径，选中 **python-risk** 文件夹（不要选上一级的 auth-service 或整个毕设文件夹）。
   - 示例：`F:\Identity_authentication（毕设）\auth-service\python-risk`
4. 点击 **确定 / Open**。
5. 若提示 “Open as Project” 或 “Trust Project”，选 **Trust / 信任** 或 **Open as Project**。
6. 打开后，左侧应能看到：`requirements.txt`、`train.py`、`app.py`、`README.md`。

**说明**：这里只是“用 PyCharm 打开 python-risk 这个文件夹”，不需要新建项目或选“New Project”。

---

## 二、指定 Python 解释器（用本机 3.13）

1. 菜单栏 **File → Settings**（Windows/Linux）或 **PyCharm → Preferences**（Mac）。
2. 左侧选 **Project: python-risk → Python Interpreter**。
3. 右上角若显示 “No interpreter”，或解释器不是 3.13：
   - 点右侧 **齿轮图标** → **Add...**；
   - 选 **Existing**（或“现有环境”）；
   - 在列表里选 **Python 3.13**，或点 **…** 手动选本机 Python 3.13 的 `python.exe`（常见位置如 `C:\Users\你的用户名\AppData\Local\Programs\Python\Python313\python.exe`）；
   - 确定后，解释器一栏应显示 Python 3.13。
4. 点 **OK** 关闭设置。

---

## 三、安装依赖（安装 requirements.txt 里的包）

**含义**：`requirements.txt` 里写了本项目需要的 Python 包（如 flask、scikit-learn），需要在本机装一次，后面运行 `train.py` 和 `app.py` 才能成功。

**操作（任选一种）**：

**方式 A：用 PyCharm 自带终端（推荐）**

1. PyCharm 底部点 **Terminal**（终端）标签，打开终端。
2. 确认当前路径是 python-risk（终端里会显示类似 `...\python-risk>` 或 `(venv) ...\python-risk>`）。
   - 若不是，输入：`cd "F:\Identity_authentication（毕设）\auth-service\python-risk"` 回车（路径按你本机改，含括号时用英文双引号包住）。
3. 在终端输入下面命令并回车：
   ```bash
   pip install -r requirements.txt
   ```
4. 等待安装结束，出现 “Successfully installed ...” 即表示依赖安装完成。

**方式 B：用 PyCharm 右键安装**

1. 在左侧项目树里**右键** `requirements.txt`。
2. 选 **Install All Requirements** 或 **Run 'pip install -r requirements.txt'**（若有该选项）。
3. 等待底部运行窗口结束，无报错即表示安装完成。

---

## 四、训练模型（生成 model.pkl）

**含义**：运行 `train.py` 会生成一批“正常/异常”的模拟数据，用它们训练一个异常检测模型，并保存成当前目录下的 **model.pkl** 文件。之后 `app.py` 会读取这个文件做预测。

**操作**：

1. 在 PyCharm 左侧项目树里**单击** `train.py`，让该文件处于当前编辑页。
2. **右键** `train.py` → 选 **Run 'train'**（或 **运行 'train'**）。
   - 或打开 `train.py` 后，点击代码区域右上角的 **绿色三角运行按钮**，再选 **Run 'train'**。
3. 看 PyCharm **底部 Run 窗口**：
   - 若成功，会看到类似两行输出：
     - `模型已保存至: F:\...\python-risk\model.pkl`
     - `特征顺序: ['count_verify_1m', 'count_query_1m', 'count_verify_5m', 'count_query_5m']`
   - 若报错 “No module named 'sklearn'” 等，说明依赖未装好，请回到 **第三节** 重新执行 `pip install -r requirements.txt`。
4. 在左侧项目树里**刷新**（右键项目根 → Reload from Disk），确认出现 **model.pkl** 文件。

**说明**：只需在首次或想重新训练时运行一次 `train.py`；以后每次启动预测服务前，只要本目录下有 `model.pkl` 即可。

---

## 五、启动预测服务（运行 app.py）

**含义**：`app.py` 会加载刚才的 `model.pkl`，在本机 5000 端口启动一个 HTTP 服务；Java 风险引擎会向 `http://localhost:5000/predict` 发请求，拿到模型给出的风险等级。

**操作**：

1. 在 PyCharm 左侧**单击** `app.py`。
2. **右键** `app.py` → **Run 'app'**（或点击代码区右上角绿色三角 → **Run 'app'**）。
3. 看底部 **Run** 窗口，应出现：
   - `模型已加载，服务启动在 http://127.0.0.1:5000`
   - `预测接口: POST http://127.0.0.1:5000/predict`
4. **不要关闭该 Run 窗口**，也不要点红色方块停止。只要 Run 窗口里 app 还在跑，Python 服务就一直在；关掉或停止后，Java 会自动只用规则评分。

**验证**：浏览器打开 `http://localhost:5000/health`，若页面显示 `{"model_loaded": true, "status": "ok"}`，说明服务正常。

---

## 六、简要顺序小结

| 顺序 | 做什么 | PyCharm / IDEA（装插件） | IDEA 仅用终端 |
|------|--------|--------------------------|----------------|
| 1 | 打开 **python-risk** | PyCharm: File → Open 选 python-risk<br>IDEA: 项目树中已有 python-risk | 同左 |
| 2 | 指定 **Python 3.13** | Settings → Python Interpreter | 终端用系统 Python 即可 |
| 3 | **安装依赖** | Terminal: `pip install -r requirements.txt` | 同上 |
| 4 | **训练模型** | 右键 train.py → Run 'train' | Terminal: `python train.py` |
| 5 | **启动服务** | 右键 app.py → Run 'app'，保持不关 | Terminal: `python app.py`，不关终端 |

之后在 Java 的 `application.properties` 里把 `ai.risk.model.enabled=true`，重启 auth-service，即可让风险引擎调用该 Python 服务。

---

## 七、常见问题

- **“pip 不是内部或外部命令”**：用 PyCharm 自带的 Terminal 一般不会出现；若用系统 CMD 出现，可改为在 PyCharm 里执行 `python -m pip install -r requirements.txt`。
- **“No module named 'flask'” / “No module named 'sklearn'”**：依赖未装好，在 **python-risk** 目录下再执行一次 `pip install -r requirements.txt`，并确认解释器选的是本机 3.13。
- **“未找到 model.pkl”**：先运行 **train.py**（第四节），再运行 app.py。
- **路径里有中文或括号**：在终端里用英文双引号把路径包起来，例如：`cd "F:\Identity_authentication（毕设）\auth-service\python-risk"`。
