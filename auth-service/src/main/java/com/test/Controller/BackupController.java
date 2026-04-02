package com.test.Controller;

import com.test.Entity.BackupRecord;
import com.test.Service.BackupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库备份管理接口
 */
@RestController
@RequestMapping("/api/backup")
public class BackupController {

    @Resource
    private BackupService backupService;

    /**
     * 手动执行数据库备份
     */
    @PostMapping("/backup")
    public ResponseEntity<Map<String, Object>> doBackup() {
        try {
            BackupRecord record = backupService.backup();

            Map<String, Object> result = new HashMap<>();
            if ("SUCCESS".equals(record.getStatus())) {
                result.put("success", true);
                result.put("message", "备份成功");
                result.put("data", record);
                return ResponseEntity.ok(result);
            } else {
                result.put("success", false);
                result.put("message", "备份失败: " + record.getErrorMessage());
                result.put("data", record);
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "备份异常: " + e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 获取所有备份记录
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listBackups() {
        List<BackupRecord> records = backupService.listAll();

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", records);
        result.put("total", records.size());
        return ResponseEntity.ok(result);
    }

    /**
     * 获取备份统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = backupService.getStatistics();

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", stats);
        return ResponseEntity.ok(result);
    }

    /**
     * 下载备份文件
     */
    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadBackup(@PathVariable Long id) {
        BackupRecord record = backupService.getById(id);
        if (record == null || record.getFilePath() == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            File file = new File(record.getFilePath());
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }

            byte[] fileContent = Files.readAllBytes(file.toPath());

            return ResponseEntity.ok()
                    .header("Content-Type", "application/octet-stream")
                    .header("Content-Disposition",
                            "attachment; filename=\"" + file.getName() + "\"")
                    .header("Content-Length", String.valueOf(fileContent.length))
                    .body(fileContent);

        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 删除备份记录及文件
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteBackup(@PathVariable Long id) {
        boolean success = backupService.delete(id);

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "删除成功" : "删除失败，记录不存在");
        return success ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
    }

    /**
     * 手动清理过期备份
     */
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupOldBackups(
            @RequestParam(defaultValue = "30") int retentionDays) {

        int cleanedCount = backupService.cleanupOldBackups(retentionDays);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "清理完成");
        result.put("cleanedCount", cleanedCount);
        result.put("retentionDays", retentionDays);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取备份配置信息
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("backupDirectory", backupService.getBackupDirectory());
        config.put("autoBackupEnabled", true);
        config.put("defaultRetentionDays", 30);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", config);
        return ResponseEntity.ok(result);
    }
}
