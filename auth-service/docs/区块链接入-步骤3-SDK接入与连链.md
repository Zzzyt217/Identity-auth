# 步骤 3：FISCO Java SDK 接入与连链测试（细化版）

**前提**：步骤 1、步骤 2 已完成（链信息与证书、存证合约地址已拿到）。

**目标**：在 auth-service 中引入 FISCO Web3SDK，配置节点与证书，启动时连链并可选执行 getBlockNumber 校验；为步骤 4 业务上链做准备。

---

## 第一部分：依赖与配置（已实现）

### 步骤 3.1 依赖

在 `auth-service/pom.xml` 中已添加：

```xml
<dependency>
    <groupId>org.fisco-bcos</groupId>
    <artifactId>web3sdk</artifactId>
    <version>2.4.1</version>
</dependency>
```

若拉取依赖失败（如 solcJ 等），可增加 Ethereum 仓库（按需启用）：

```xml
<repository>
    <id>ethereum</id>
    <url>https://dl.bintray.com/ethereum/maven</url>
</repository>
```

### 步骤 3.2 配置项

在 `application.properties` 中已增加（请按步骤 1、步骤 2 记录表填写）：

| 配置项 | 说明 | 示例 / 记录表 |
|--------|------|----------------|
| `fisco.enabled` | 是否启用上链 | `true` / `false`（不连链时写 false） |
| `fisco.node` | 节点地址 虚拟机IP:Channel端口 | 步骤 1【G】+【B】，如 `192.168.1.100:20200` |
| `fisco.group-id` | 群组 ID | 步骤 1【J】，一般为 `1` |
| `fisco.ca-cert` | CA 证书路径（classpath） | `conf/ca.crt` |
| `fisco.ssl-cert` | SDK 证书路径 | `conf/sdk.crt` |
| `fisco.ssl-key` | SDK 私钥路径 | `conf/sdk.key` |
| `fisco.evidence-contract-address` | 存证合约地址 | 步骤 2【L】，0x 开头 |

**示例**（本机连虚拟机链）：

```properties
fisco.enabled=true
fisco.node=192.168.1.100:20200
fisco.group-id=1
fisco.ca-cert=conf/ca.crt
fisco.ssl-cert=conf/sdk.crt
fisco.ssl-key=conf/sdk.key
fisco.evidence-contract-address=0x你的步骤2合约地址
```

---

## 第二部分：代码说明（已实现）

### 步骤 3.3 配置类与连链

- **`com.test.config.BlockchainConfig`**
  - 仅在 `fisco.enabled=true` 时生效。
  - 使用步骤 1 的节点地址、证书路径、groupId 构建 Channel 连接并创建 `Web3j`。
  - 启动时调用 `getBlockNumber()`，成功则在日志中打印当前区块高度。

### 步骤 3.4 证书与节点

- 证书从 `classpath` 下 `conf/` 读取（与步骤 1 拷入的 `ca.crt`、`sdk.crt`、`sdk.key` 一致）。
- 节点地址为「虚拟机 IP + Channel 端口」，确保本机可访问该端口（防火墙、网络互通）。

---

## 第三部分：自检与记录表

### 步骤 3.5 启动自检

1. 填写上述配置（尤其是 `fisco.node`、`fisco.evidence-contract-address`）。
2. 在本机 IDEA 启动 auth-service。
3. 查看日志：
   - 出现 `[区块链] 连链成功, groupId=1, 当前区块高度=xxx` 表示步骤 3 连链成功。
   - 若出现「getBlockNumber 失败」或连接超时，请检查：虚拟机 IP、Channel 端口、证书路径、本机与虚拟机网络。

### 步骤 3 记录表（可选）

| 序号 | 项目 | 你的值 |
|------|------|--------|
| **N** | 本机启动后是否看到「连链成功」日志 | 是 / 否 |
| **O** | 当前区块高度（日志中的数值） | |

---

## 完成后自检

- [ ] `pom.xml` 中 web3sdk 依赖已存在且可解析。
- [ ] `application.properties` 中 `fisco.node`、`fisco.evidence-contract-address` 已按步骤 1、2 填写。
- [ ] 启动应用后日志中有「连链成功」及区块高度（或已排查网络/证书问题）。

完成以上即**步骤 3 结束**。步骤 4 为业务上链：注册身份与写审计日志时自动调用存证合约并回写 `chain_tx_hash`、`chain_block_number`。
