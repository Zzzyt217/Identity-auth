package com.test.blockchain;

import org.fisco.bcos.web3j.abi.datatypes.Type;
import org.fisco.bcos.web3j.abi.datatypes.Utf8String;
import org.fisco.bcos.web3j.crypto.Credentials;
import org.fisco.bcos.web3j.protocol.exceptions.TransactionException;
import org.fisco.bcos.web3j.protocol.Web3j;
import org.fisco.bcos.web3j.protocol.core.methods.response.TransactionReceipt;
import org.fisco.bcos.web3j.tx.gas.ContractGasProvider;

import java.util.Arrays;
import java.util.Collections;

/**
 * 存证合约 EvidenceStorage 的 Java 封装（与 contracts/EvidenceStorage.sol 对应）。
 * set(string key, string hash) / get(string key)，用于身份与审计上链。
 */
public final class EvidenceStorageContract {

    private static final String BIN = "Bin file not provided";

    /**
     * 调用 set(key, hash)，写入一条存证，返回交易回执（含 transactionHash、blockNumber）。
     */
    public static TransactionReceipt set(Web3j web3j, Credentials credentials,
                                        ContractGasProvider gasProvider, String contractAddress,
                                        String key, String hash) throws Exception, TransactionException {
        org.fisco.bcos.web3j.abi.datatypes.Function function = new org.fisco.bcos.web3j.abi.datatypes.Function(
                "set",
                Arrays.asList(new Utf8String(key), new Utf8String(hash)),
                Collections.emptyList());
        EvidenceStorageWrapper contract = EvidenceStorageWrapper.load(contractAddress, web3j, credentials, gasProvider);
        return contract.executeTransaction(function);
    }

    /**
     * 调用 get(key)，查询链上存证的 hash 值。
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static String get(Web3j web3j, Credentials credentials,
                            ContractGasProvider gasProvider, String contractAddress,
                            String key) throws Exception {
        org.fisco.bcos.web3j.abi.datatypes.Function function = new org.fisco.bcos.web3j.abi.datatypes.Function(
                "get",
                Collections.singletonList(new Utf8String(key)),
                Collections.singletonList(org.fisco.bcos.web3j.abi.TypeReference.create(Utf8String.class)));
        EvidenceStorageWrapper contract = EvidenceStorageWrapper.load(contractAddress, web3j, credentials, gasProvider);
        Type result = contract.executeCallSingleValueReturn(function);
        return result == null ? "" : result.getValue().toString();
    }

    /**
     * 内部包装类，继承 Contract，仅用于执行 Function（不依赖 BIN 部署）。
     */
    static class EvidenceStorageWrapper extends org.fisco.bcos.web3j.tx.Contract {

        protected EvidenceStorageWrapper(String contractAddress, Web3j web3j, Credentials credentials,
                                        ContractGasProvider gasProvider) {
            super(BIN, contractAddress, web3j, credentials,
                    gasProvider != null ? gasProvider : new org.fisco.bcos.web3j.tx.gas.StaticGasProvider(
                            java.math.BigInteger.valueOf(300000000),
                            java.math.BigInteger.valueOf(300000000)));
        }

        public static EvidenceStorageWrapper load(String contractAddress, Web3j web3j, Credentials credentials,
                                                  ContractGasProvider gasProvider) {
            return new EvidenceStorageWrapper(contractAddress, web3j, credentials, gasProvider);
        }

        public TransactionReceipt executeTransaction(org.fisco.bcos.web3j.abi.datatypes.Function function) throws java.io.IOException, TransactionException {
            return super.executeTransaction(function);
        }

        public Type executeCallSingleValueReturn(org.fisco.bcos.web3j.abi.datatypes.Function function) throws java.io.IOException {
            return super.executeCallSingleValueReturn(function);
        }
    }
}
