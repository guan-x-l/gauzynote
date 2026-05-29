package com.gauzynote.system.mapper;

import com.gauzynote.common.domain.entity.OperLog;

/**
 * 操作日志表(OperLog)表数据库访问层
 *
 */
public interface OperLogMapper {
    /**
     * 新增数据
     *
     * @param operLog 实例对象
     * @return 影响行数
     */
    int insert(OperLog operLog);
}

