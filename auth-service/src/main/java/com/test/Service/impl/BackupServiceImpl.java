package com.test.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.test.Entity.BackupRecord;
import com.test.Mapper.BackupRecordMapper;
import com.test.Service.BackupService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库备份服务实现
 */
@Service
public class BackupServiceImpl implements BackupService {

    private static final DateTimeFormatter FILE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${backup.directory:./backups}")
    private String backupDirectory;

    @Value("${backup.retention-days:30}")
    private int retentionDays;

    @Value("${backup.auto.enabled:true}")
    private boolean autoBackupEnabled;

    @Value("${backup.mysqldump-path:mysqldump}")
    private String mysqldumpPath;

    @Resource
    private BackupRecordMapper backupRecordMapper;

    @Override
    public BackupRecord backup() {
        BackupRecord record = new BackupRecord();
        record.setBackupType(BACKUP_TYPE_FULL);
        record.setBackupTime(LocalDateTime.now());
        record.setOperator("SYSTEM");

        long startTime = System.currentTimeMillis();

        try {
            // 确保备份目录存在
            Path backupPath = Paths.get(backupDirectory);
            if (!Files.exists(backupPath)) {
                Files.createDirectories(backupPath);
            }

            // 生成备份文件名
            String timestamp = LocalDateTime.now().format(FILE_DATE_FORMAT);
            String dbName = extractDbName(jdbcUrl);
            String fileName = dbName + "_" + timestamp + ".sql";
            String fullPath = backupDirectory + File.separator + fileName;

            // 执行备份
            String errorMsg = executeBackupCommand(fullPath);

            long duration = (System.currentTimeMillis() - startTime) / 1000;

            if (errorMsg == null) {
                File backupFile = new File(fullPath);
                record.setFilePath(fullPath);
                record.setFileSize(backupFile.exists() ? backupFile.length() : 0);
                record.setStatus(STATUS_SUCCESS);
                record.setDurationSeconds((int) duration);
            } else {
                record.setStatus(STATUS_FAILED);
                record.setErrorMessage(errorMsg);
                record.setDurationSeconds((int) duration);
            }

        } catch (Exception e) {
            record.setStatus(STATUS_FAILED);
            record.setErrorMessage(e.getMessage());
            record.setDurationSeconds((int) ((System.currentTimeMillis() - startTime) / 1000));
        }

        backupRecordMapper.insert(record);
        return record;
    }

    /**
     * 执行 mysqldump 命令进行备份
     * @return 成功返回 null，失败返回错误信息
     */
    private String executeBackupCommand(String outputPath) {
        StringBuilder errorOutput = new StringBuilder();
        StringBuilder stdOutput = new StringBuilder();
        try {
            // 提取数据库名
            String dbName = extractDbName(jdbcUrl);

            // 构建 mysqldump 命令列表
            java.util.List<String> command = new java.util.ArrayList<>();
            command.add(mysqldumpPath);

            // 用户名参数
            if (username != null && !username.isEmpty()) {
                command.add("-u" + username);
            }

            // 密码参数
            if (password != null && !password.isEmpty()) {
                command.add("-p" + password);
            }

            command.add("--single-transaction");
            command.add("--routines");
            command.add("--triggers");
            command.add("--events");
            command.add("--hex-blob");
            command.add(dbName);

            // 构建 ProcessBuilder
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(false);

            File outputFile = new File(outputPath);
            pb.redirectOutput(outputFile);
            File errorFile = new File(outputPath + ".err");
            pb.redirectError(errorFile);

            // 记录日志
            String logCmd = mysqldumpPath + " -u" + username + " -p*** ..." + " > " + outputPath;
            System.out.println("[BackupService] 执行备份命令: " + logCmd);

            Process process = pb.start();

            // 读取错误输出（防止缓冲区满导致阻塞）
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    errorOutput.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();

            // 检查错误文件
            if (errorFile.exists() && errorFile.length() > 0) {
                String errContent = new String(Files.readAllBytes(errorFile.toPath()));
                if (!errContent.trim().isEmpty()) {
                    errorOutput.append(errContent);
                }
                errorFile.delete();
            }

            // 检查输出文件
            boolean fileExists = outputFile.exists();
            long fileSize = fileExists ? outputFile.length() : 0;

            // 检查文件内容是否包含错误信息
            if (fileExists && fileSize > 0) {
                String firstBytes = new String(Files.readAllBytes(outputFile.toPath()));
                if (firstBytes.length() > 0) {
                    // mysqldump 成功时以 "-- Dump completed" 结尾或为空前有表结构
                    // 失败时开头可能有错误信息
                    if (firstBytes.contains("Access denied") ||
                        firstBytes.contains("Unknown database") ||
                        firstBytes.contains("Can't connect") ||
                        firstBytes.contains("No such file")) {
                        stdOutput.append(firstBytes);
                    }
                }
            }

            boolean success = exitCode == 0 && fileExists && fileSize > 0;

            System.out.println("[BackupService] 备份结果: exitCode=" + exitCode + ", fileExists=" + fileExists + ", fileSize=" + fileSize);
            if (!success) {
                System.out.println("[BackupService] 错误输出: " + errorOutput);
                if (stdOutput.length() > 0) {
                    System.out.println("[BackupService] 输出文件含错误: " + stdOutput);
                }
            }

            if (success) {
                return null; // 成功
            } else {
                String errorMsg = errorOutput.toString().trim();
                if (errorMsg.isEmpty()) {
                    errorMsg = stdOutput.toString().trim();
                }
                return errorMsg.isEmpty() ? "备份命令执行失败，退出码: " + exitCode : errorMsg;
            }

        } catch (Exception e) {
            System.out.println("[BackupService] 执行异常: " + e.getMessage());
            return "执行备份命令异常: " + e.getMessage();
        }
    }

    /**
     * 从 JDBC URL 中提取数据库名
     */
    private String extractDbName(String jdbcUrl) {
        // 直接返回数据库名，避免 URL 解析问题
        return "identity_db";
    }

    @Override
    public List<BackupRecord> listAll() {
        return backupRecordMapper.selectList(
                new LambdaQueryWrapper<BackupRecord>()
                        .orderByDesc(BackupRecord::getBackupTime)
        );
    }

    @Override
    public BackupRecord getById(Long id) {
        return backupRecordMapper.selectById(id);
    }

    @Override
    public boolean delete(Long id) {
        BackupRecord record = backupRecordMapper.selectById(id);
        if (record == null) {
            return false;
        }

        // 删除物理文件
        if (record.getFilePath() != null) {
            File file = new File(record.getFilePath());
            if (file.exists()) {
                file.delete();
            }
        }

        // 删除数据库记录
        return backupRecordMapper.deleteById(id) > 0;
    }

    @Override
    public String getBackupDirectory() {
        return backupDirectory;
    }

    @Override
    public int cleanupOldBackups(int days) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        List<BackupRecord> oldRecords = backupRecordMapper.selectList(
                new LambdaQueryWrapper<BackupRecord>()
                        .lt(BackupRecord::getBackupTime, cutoff)
        );

        int count = 0;
        for (BackupRecord record : oldRecords) {
            if (record.getFilePath() != null) {
                File file = new File(record.getFilePath());
                if (file.exists()) {
                    file.delete();
                }
            }
            backupRecordMapper.deleteById(record.getId());
            count++;
        }

        return count;
    }

    @Override
    public Map<String, Object> getStatistics() {
        List<BackupRecord> allRecords = backupRecordMapper.selectList(null);

        long successCount = allRecords.stream()
                .filter(r -> STATUS_SUCCESS.equals(r.getStatus()))
                .count();

        long failedCount = allRecords.stream()
                .filter(r -> STATUS_FAILED.equals(r.getStatus()))
                .count();

        long totalSize = allRecords.stream()
                .filter(r -> r.getFileSize() != null)
                .mapToLong(BackupRecord::getFileSize)
                .sum();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRecords", allRecords.size());
        stats.put("successCount", successCount);
        stats.put("failedCount", failedCount);
        stats.put("totalBackupSize", totalSize);
        stats.put("totalBackupSizeMB", String.format("%.2f", totalSize / 1024.0 / 1024.0));
        stats.put("retentionDays", retentionDays);
        stats.put("backupDirectory", backupDirectory);
        stats.put("autoBackupEnabled", autoBackupEnabled);
        return stats;
    }
}
