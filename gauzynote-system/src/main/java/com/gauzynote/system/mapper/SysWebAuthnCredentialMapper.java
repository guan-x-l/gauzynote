package com.gauzynote.system.mapper;

import com.gauzynote.system.domain.entity.SysWebAuthnCredential;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * WebAuthn 凭证 Mapper
 */
public interface SysWebAuthnCredentialMapper {
    /**
     * 按用户查询启用中的凭证列表
     *
     * @param userId 用户ID
     * @return 凭证列表
     */
    List<SysWebAuthnCredential> selectActiveByUserId(@Param("userId") Long userId);

    /**
     * 按凭证ID查询启用中的凭证
     *
     * @param credentialId 凭证ID
     * @return 凭证
     */
    SysWebAuthnCredential selectActiveByCredentialId(@Param("credentialId") String credentialId);

    /**
     * 新增凭证
     *
     * @param credential 凭证信息
     * @return 影响行数
     */
    int insertCredential(SysWebAuthnCredential credential);

    /**
     * 更新凭证计数与最后使用时间
     *
     * @param credentialRecordId 凭证主键
     * @param signCount 最新计数
     * @return 影响行数
     */
    int updateUsage(@Param("credentialRecordId") Long credentialRecordId, @Param("signCount") Long signCount);

    /**
     * 修改凭证名称
     *
     * @param userId 用户ID
     * @param credentialId 凭证ID
     * @param credentialName 凭证名称
     * @return 影响行数
     */
    int updateCredentialName(@Param("userId") Long userId, @Param("credentialId") String credentialId, @Param("credentialName") String credentialName);

    /**
     * 软删除凭证
     *
     * @param userId 用户ID
     * @param credentialId 凭证ID
     * @return 影响行数
     */
    int revokeCredential(@Param("userId") Long userId, @Param("credentialId") String credentialId);
}
