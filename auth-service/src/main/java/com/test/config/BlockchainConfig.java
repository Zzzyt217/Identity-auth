package com.test.config;

import org.fisco.bcos.channel.client.Service;
import org.fisco.bcos.channel.handler.ChannelConnections;
import org.fisco.bcos.channel.handler.GroupChannelConnectionsConfig;
import org.fisco.bcos.web3j.crypto.Credentials;
import org.fisco.bcos.web3j.crypto.gm.GenCredential;
import org.fisco.bcos.web3j.protocol.Web3j;
import org.fisco.bcos.web3j.protocol.channel.ChannelEthereumService;
import org.fisco.bcos.web3j.tx.gas.StaticGasProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.math.BigInteger;
import java.util.Collections;

/**
 * 区块链连接配置（步骤 3）：仅在 fisco.enabled=true 时生效。
 * 使用步骤 1 记录表的节点地址、证书路径、groupId 连接链，启动时可选执行 getBlockNumber 校验。
 */
@Configuration
@ConditionalOnProperty(name = "fisco.enabled", havingValue = "true")
public class BlockchainConfig {

    private static final Logger log = LoggerFactory.getLogger(BlockchainConfig.class);
    private static final BigInteger GAS_PRICE = new BigInteger("300000000");
    private static final BigInteger GAS_LIMIT = new BigInteger("300000000");

    @Value("${fisco.node:127.0.0.1:20200}")
    private String nodeAddress;
    @Value("${fisco.group-id:1}")
    private String groupId;
    @Value("${fisco.ca-cert:conf/ca.crt}")
    private String caCertPath;
    @Value("${fisco.ssl-cert:conf/sdk.crt}")
    private String sslCertPath;
    @Value("${fisco.ssl-key:conf/sdk.key}")
    private String sslKeyPath;

    private final ResourceLoader resourceLoader;

    public BlockchainConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Bean
    public GroupChannelConnectionsConfig groupChannelConnectionsConfig() {
        GroupChannelConnectionsConfig config = new GroupChannelConnectionsConfig();
        Resource ca = resourceLoader.getResource("classpath:" + caCertPath);
        Resource sslCert = resourceLoader.getResource("classpath:" + sslCertPath);
        Resource sslKey = resourceLoader.getResource("classpath:" + sslKeyPath);
        config.setCaCert(ca);
        config.setSslCert(sslCert);
        config.setSslKey(sslKey);
        ChannelConnections connections = new ChannelConnections();
        connections.setGroupId(Integer.parseInt(groupId));
        connections.setConnectionsStr(Collections.singletonList(nodeAddress));
        config.setAllChannelConnections(Collections.singletonList(connections));
        return config;
    }

    @Bean
    public Service channelService(GroupChannelConnectionsConfig config) {
        Service service = new Service();
        service.setGroupId(Integer.parseInt(groupId));
        service.setAgencyName("fisco");
        service.setAllChannelConnections(config);
        return service;
    }

    @Bean
    public Web3j web3j(Service channelService) {
        try {
            channelService.run();
        } catch (Exception e) {
            log.warn("[区块链] Channel Service 启动异常: {}", e.getMessage());
        }
        ChannelEthereumService channelEthereumService = new ChannelEthereumService();
        channelEthereumService.setChannelService(channelService);
        channelEthereumService.setTimeout(10000);
        Web3j web3j = Web3j.build(channelEthereumService, channelService.getGroupId());
        try {
            BigInteger blockNumber = web3j.getBlockNumber().send().getBlockNumber();
            log.info("[区块链] 连链成功, groupId={}, 当前区块高度={}", groupId, blockNumber);
        } catch (Exception e) {
            log.warn("[区块链] 连链成功但 getBlockNumber 失败（请检查节点）: {}", e.getMessage());
        }
        return web3j;
    }

    @Bean
    public Credentials credentials() {
        return GenCredential.create();
    }

    @Bean
    public StaticGasProvider gasProvider() {
        return new StaticGasProvider(GAS_PRICE, GAS_LIMIT);
    }
}
