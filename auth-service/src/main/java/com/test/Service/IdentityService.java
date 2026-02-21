package com.test.service;

import com.test.Entity.Identity;

/**
 * DID 身份服务（方案A：应用生成 DID，存库）
 */
public interface IdentityService {

    /**
     * 注册身份：生成 DID 并入库
     *
     * @param employeeId 工号
     * @param name       姓名
     * @param department 部门
     * @param position   职位（可选）
     * @param role       权限角色：员工、超级管理员，为空时默认员工
     * @return 创建后的身份（含 DID）
     */
    Identity register(String employeeId, String name, String department, String position, String role);

    /**
     * 按主键查询
     */
    Identity getById(Long id);

    /**
     * 按 DID 查询
     */
    Identity getByDid(String did);

    /**
     * 按工号查询
     */
    Identity getByEmployeeId(String employeeId);

    /**
     * 查询全部身份（权限管理列表用）
     */
    java.util.List<Identity> listAll();

    /**
     * 修改权限角色
     *
     * @param id   身份主键
     * @param role 角色：员工、超级管理员
     */
    void updateRole(Long id, String role);

    /**
     * 注销 DID：删除该身份记录
     */
    void revoke(Long id);
}
