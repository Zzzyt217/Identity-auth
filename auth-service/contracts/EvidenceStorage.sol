pragma solidity ^0.4.25;

/**
 * 存证合约：key -> 数据 hash，供身份/审计上链存证与链上验证
 * 与 auth-service 业务配合：key 可为 DID 或 audit_日志id，value 为对应内容的 hash
 */
contract EvidenceStorage {
    mapping(string => string) private _records;

    event SetRecord(string key, string hash);

    function set(string key, string hash) public {
        _records[key] = hash;
        emit SetRecord(key, hash);
    }

    function get(string key) public view returns (string) {
        return _records[key];
    }
}
