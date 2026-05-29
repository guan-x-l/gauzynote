package com.gauzynote.system.service;

import com.gauzynote.common.domain.AjaxResult;
import com.gauzynote.common.exception.ServiceException;
import com.gauzynote.common.enums.UserTypeEnum;
import com.gauzynote.common.utils.MessageUtils;
import com.gauzynote.common.utils.SecurityUtils;
import com.gauzynote.system.domain.entity.SysDictType;
import com.gauzynote.system.mapper.SysDictDataMapper;
import com.gauzynote.system.mapper.SysDictTypeMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Service("SysDictTypeService")
public class SysDictTypeService {
    @Resource
    private SysDictTypeMapper dictTypeMapper;
    @Resource
    private SysDictDataMapper dictDataMapper;

    /**
     * 查询字典类型列表（返回系统公共数据 + 当前用户私有数据）
     */
    public List<SysDictType> selectDictTypeList(SysDictType dictType) {
        List<SysDictType> dictTypes = dictTypeMapper.selectDictTypeList(dictType, SecurityUtils.getUserId());
        fillOperatePermission(dictTypes);
        return dictTypes;
    }

    /**
     * 查询启用状态的字典类型（用于下拉选项，返回系统公共 + 当前用户私有）
     */
    public List<SysDictType> selectDictTypeAll() {
        List<SysDictType> dictTypes = dictTypeMapper.selectDictTypeAll(SecurityUtils.getUserId());
        fillOperatePermission(dictTypes);
        return dictTypes;
    }

    /**
     * 根据主键查询字典类型详情（按当前用户可见范围返回）
     */
    public SysDictType selectDictTypeById(Long dictId) {
        SysDictType dictType = dictTypeMapper.selectDictTypeById(dictId, SecurityUtils.getUserId());
        if (!ObjectUtils.isEmpty(dictType)) {
            dictType.setCanOperate(canOperate(dictType.getUserId()));
        }
        return dictType;
    }

    /**
     * 根据 dictType 查询字典类型（用于唯一性校验/关联校验）
     */
    public SysDictType selectDictTypeByType(String dictType) {
        return dictTypeMapper.selectDictTypeByType(dictType, SecurityUtils.getUserId());
    }

    /**
     * 新增字典类型（含唯一性校验与默认值处理）
     */
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult insertDictType(SysDictType dictType) {
        normalizeOwnerByRole(dictType);
        if (!checkDictTypeUnique(dictType)) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), MessageUtils.message("operation.failed"));
        }
        dictType.setCreateBy(SecurityUtils.getUsername());
        if (ObjectUtils.isEmpty(dictType.getStatus())) {
            dictType.setStatus("0");
        }
        int rows = dictTypeMapper.insertDictType(dictType);
        return rows > 0 ? AjaxResult.success(dictType.getDictId()) : AjaxResult.error();
    }

    /**
     * 修改字典类型（含唯一性校验）
     */
    @Transactional(rollbackFor = Exception.class)
    public int updateDictType(SysDictType dictType) {
        if (ObjectUtils.isEmpty(dictType.getDictId())) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), MessageUtils.message("operation.failed"));
        }
        SysDictType source = dictTypeMapper.selectDictTypeById(dictType.getDictId(), SecurityUtils.getUserId());
        if (ObjectUtils.isEmpty(source) || !canOperate(source.getUserId())) {
            throw new ServiceException(HttpServletResponse.SC_FORBIDDEN, MessageUtils.message("no.permission.for.current.operation"));
        }
        normalizeOwnerByRole(dictType);
        if (!checkDictTypeUnique(dictType)) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), MessageUtils.message("operation.failed"));
        }
        dictType.setUpdateBy(SecurityUtils.getUsername());
        return dictTypeMapper.updateDictType(dictType, SecurityUtils.getUserId(), isAdmin());
    }

    /**
     * 删除字典类型（若存在关联字典数据则禁止删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public int deleteDictTypeById(Long dictId) {
        SysDictType dictType = dictTypeMapper.selectDictTypeById(dictId, SecurityUtils.getUserId());
        if (ObjectUtils.isEmpty(dictType) || !canOperate(dictType.getUserId())) {
            throw new ServiceException(HttpServletResponse.SC_FORBIDDEN, MessageUtils.message("no.permission.for.current.operation"));
        }
        int count = dictDataMapper.countDictDataByType(dictType.getDictType());
        if (count > 0) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), MessageUtils.message("operation.failed"));
        }
        return dictTypeMapper.deleteDictTypeById(dictId, SecurityUtils.getUserId(), isAdmin());
    }

    /**
     * 校验 dictType 是否唯一（同一归属范围内不允许重复）
     */
    public boolean checkDictTypeUnique(SysDictType dictType) {
        Long dictId = ObjectUtils.isEmpty(dictType.getDictId()) ? -1L : dictType.getDictId();
        SysDictType info = dictTypeMapper.selectDictTypeByTypeAndUserId(dictType.getDictType(), dictType.getUserId());
        if (!ObjectUtils.isEmpty(info) && !info.getDictId().equals(dictId)) {
            return false;
        }
        return true;
    }

    /**
     * 根据当前用户角色规范化 userId：管理员允许 null/本人，非管理员强制本人
     */
    private void normalizeOwnerByRole(SysDictType dictType) {
        Long currentUserId = SecurityUtils.getUserId();
        if (isAdmin()) {
            if (!ObjectUtils.isEmpty(dictType.getUserId()) && !currentUserId.equals(dictType.getUserId())) {
                throw new ServiceException(HttpServletResponse.SC_FORBIDDEN, MessageUtils.message("no.permission.for.current.operation"));
            }
            return;
        }
        dictType.setUserId(currentUserId);
    }

    /**
     * 判断当前用户是否可操作目标归属的数据
     */
    private boolean canOperate(Long ownerUserId) {
        Long currentUserId = SecurityUtils.getUserId();
        if (isAdmin()) {
            return ObjectUtils.isEmpty(ownerUserId) || currentUserId.equals(ownerUserId);
        }
        return !ObjectUtils.isEmpty(ownerUserId) && currentUserId.equals(ownerUserId);
    }

    /**
     * 标记列表项是否可操作（供前端动态显示操作按钮）
     */
    private void fillOperatePermission(List<SysDictType> dictTypes) {
        if (ObjectUtils.isEmpty(dictTypes)) {
            return;
        }
        for (SysDictType dictType : dictTypes) {
            dictType.setCanOperate(canOperate(dictType.getUserId()));
        }
    }

    /**
     * 判断当前登录用户是否为管理员
     */
    private boolean isAdmin() {
        return UserTypeEnum.ADMIN.getCode().equals(SecurityUtils.getLoginUser().getSysUser().getUserType());
    }
}
