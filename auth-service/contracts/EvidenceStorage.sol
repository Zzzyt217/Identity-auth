pragma solidity ^0.4.25;

/**
 * 存证合约：key -> 数据 hash，供身份/审计上链存证与链上验证
 * 增强版：保留原有 set/get，新增统计查询功能
 */
contract EvidenceStorage {
    // ==================== 原有结构（不变） ====================
    mapping(string => string) private _records;
    
    // ==================== 增强：计数器 ====================
    uint public totalRecords;
    
    // ==================== 增强：时间戳记录 ====================
    mapping(string => uint) private _timestamps;

    // ==================== 事件 ====================
    event SetRecord(string key, string hash);
    event QueryRecord(string indexed key, address queriedBy);

    // ==================== 原有方法（微小改动：添加计数和时间戳） ====================
    function set(string key, string hash) public {
        // 只有新记录才计数，已有记录覆盖不重复计数
        if (bytes(_records[key]).length == 0) {
            totalRecords++;
        }
        _records[key] = hash;
        // 记录时间戳
        _timestamps[key] = block.timestamp;
        emit SetRecord(key, hash);
    }

    function get(string key) public view returns (string) {
        return _records[key];
    }
    
    // ==================== 新增方法 ====================
    
    /**
     * @dev 检查存证是否存在
     * @param key 存证键
     * @return 是否存在
     */
    function exists(string key) public view returns (bool) {
        return bytes(_records[key]).length > 0;
    }
    
    /**
     * @dev 获取存证时间戳
     * @param key 存证键
     * @return 上链时间戳（未找到返回0）
     */
    function getTimestamp(string key) public view returns (uint) {
        if (bytes(_records[key]).length > 0) {
            return _timestamps[key];
        }
        return 0;
    }
    
    /**
     * @dev 获取总存证数
     * @return 总存证数量
     */
    function getTotalRecords() public view returns (uint) {
        return totalRecords;
    }
}
