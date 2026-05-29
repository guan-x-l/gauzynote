package com.gauzynote.system.service;

import com.gauzynote.common.domain.AjaxResult;
import com.gauzynote.common.domain.entity.SysUser;
import com.gauzynote.common.enums.UserTypeEnum;
import com.gauzynote.common.exception.ServiceException;
import com.gauzynote.common.utils.MessageUtils;
import com.gauzynote.common.utils.SecurityUtils;
import com.gauzynote.system.domain.entity.SysDictData;
import com.gauzynote.system.domain.entity.SysDictType;
import com.gauzynote.system.mapper.SysDictDataMapper;
import com.gauzynote.system.mapper.SysDictTypeMapper;
import com.gauzynote.system.mapper.SysUserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Service("SysDictDataService")
public class SysDictDataService {
    @Resource
    private SysDictDataMapper dictDataMapper;
    @Resource
    private SysDictTypeMapper dictTypeMapper;
    @Resource
    private SysUserMapper sysUserMapper;

    /**
     * 查询字典数据列表（支持条件过滤，仅返回系统公共数据 + 当前用户私有数据）
     */
    public List<SysDictData> selectDictDataList(SysDictData dictData) {
        List<SysDictData> list = dictDataMapper.selectDictDataList(dictData, SecurityUtils.getUserId());
        fillOperatePermission(list);
        return list;
    }

    /**
     * 根据字典类型查询启用状态的数据列表（仅包含全局数据与当前用户私有数据）
     */
    public List<SysDictData> selectDictDataByType(String dictType) {
        List<SysDictData> list = dictDataMapper.selectDictDataByType(dictType, SecurityUtils.getUserId());
        fillOperatePermission(list);
        return list;
    }

    /**
     * 根据主键查询字典数据详情（按当前用户数据权限过滤）
     */
    public SysDictData selectDictDataById(Long dictCode) {
        SysDictData dictData = dictDataMapper.selectDictDataById(dictCode, SecurityUtils.getUserId());
        if (!ObjectUtils.isEmpty(dictData)) {
            dictData.setCanOperate(canOperate(dictData.getUserId()));
        }
        return dictData;
    }

    /**
     * 新增字典数据（校验字典类型存在 + dictValue 唯一性 + 默认值处理）
     */
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult insertDictData(SysDictData dictData) {
        normalizeOwnerByRole(dictData);
        ensureDictTypeExists(dictData.getDictType());
        ensureOwnerUserExists(dictData.getUserId());
        if (!checkDictDataUnique(dictData)) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), MessageUtils.message("operation.failed"));
        }
        dictData.setCreateBy(SecurityUtils.getUsername());
        if (ObjectUtils.isEmpty(dictData.getStatus())) {
            dictData.setStatus("0");
        }
        if (ObjectUtils.isEmpty(dictData.getIsDefault())) {
            dictData.setIsDefault("N");
        }
        int rows = dictDataMapper.insertDictData(dictData);
        return rows > 0 ? AjaxResult.success(dictData.getDictCode()) : AjaxResult.error();
    }

    /**
     * 修改字典数据（校验字典类型存在 + dictValue 唯一性）
     */
    @Transactional(rollbackFor = Exception.class)
    public int updateDictData(SysDictData dictData) {
        if (ObjectUtils.isEmpty(dictData.getDictCode())) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), MessageUtils.message("operation.failed"));
        }
        SysDictData source = selectDictDataById(dictData.getDictCode());
        if (ObjectUtils.isEmpty(source) || !canOperate(source.getUserId())) {
            throw new ServiceException(HttpServletResponse.SC_FORBIDDEN, MessageUtils.message("no.permission.for.current.operation"));
        }
        normalizeOwnerByRole(dictData);
        ensureDictTypeExists(dictData.getDictType());
        ensureOwnerUserExists(dictData.getUserId());
        if (!checkDictDataUnique(dictData)) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), MessageUtils.message("operation.failed"));
        }
        dictData.setUpdateBy(SecurityUtils.getUsername());
        return dictDataMapper.updateDictData(dictData, SecurityUtils.getUserId(), isAdmin());
    }

    /**
     * 根据主键删除字典数据
     */
    @Transactional(rollbackFor = Exception.class)
    public int deleteDictDataById(Long dictCode) {
        SysDictData source = selectDictDataById(dictCode);
        if (ObjectUtils.isEmpty(source) || !canOperate(source.getUserId())) {
            throw new ServiceException(HttpServletResponse.SC_FORBIDDEN, MessageUtils.message("no.permission.for.current.operation"));
        }
        return dictDataMapper.deleteDictDataById(dictCode, SecurityUtils.getUserId(), isAdmin());
    }

    /**
     * 校验同一 dictType 下 dictValue 是否唯一
     */
    public boolean checkDictDataUnique(SysDictData dictData) {
        Long dictCode = ObjectUtils.isEmpty(dictData.getDictCode()) ? -1L : dictData.getDictCode();
        SysDictData info = dictDataMapper.selectDictDataByTypeAndValue(dictData.getDictType(), dictData.getDictValue(), dictData.getUserId());
        if (!ObjectUtils.isEmpty(info) && !info.getDictCode().equals(dictCode)) {
            return false;
        }
        return true;
    }

    /**
     * 校验 dictType 是否存在（字典数据必须挂在已存在的字典类型上）
     */
    private void ensureDictTypeExists(String dictType) {
        if (ObjectUtils.isEmpty(dictType)) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), MessageUtils.message("operation.failed"));
        }
        Long currentUserId = SecurityUtils.getUserId();
        SysDictType info = dictTypeMapper.selectDictTypeByTypeAndUserId(dictType, null);
        if (ObjectUtils.isEmpty(info)) {
            info = dictTypeMapper.selectDictTypeByTypeAndUserId(dictType, currentUserId);
        }
        if (ObjectUtils.isEmpty(info)) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), MessageUtils.message("operation.failed"));
        }
    }

    /**
     * 校验 userId 对应用户是否存在（null 表示全局字典数据）
     */
    private void ensureOwnerUserExists(Long userId) {
        if (ObjectUtils.isEmpty(userId)) {
            return;
        }
        SysUser info = sysUserMapper.selectUserById(userId);
        if (ObjectUtils.isEmpty(info)) {
            throw new ServiceException(HttpStatus.BAD_REQUEST.value(), MessageUtils.message("operation.failed"));
        }
    }

    /**
     * 根据当前用户角色规范化 userId：管理员允许 null/本人，非管理员强制本人
     */
    private void normalizeOwnerByRole(SysDictData dictData) {
        Long currentUserId = SecurityUtils.getUserId();
        if (isAdmin()) {
            if (!ObjectUtils.isEmpty(dictData.getUserId()) && !currentUserId.equals(dictData.getUserId())) {
                throw new ServiceException(HttpServletResponse.SC_FORBIDDEN, MessageUtils.message("no.permission.for.current.operation"));
            }
            return;
        }
        dictData.setUserId(currentUserId);
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
    private void fillOperatePermission(List<SysDictData> dictDataList) {
        if (ObjectUtils.isEmpty(dictDataList)) {
            return;
        }
        for (SysDictData dictData : dictDataList) {
            dictData.setCanOperate(canOperate(dictData.getUserId()));
        }
    }

    /**
     * 判断当前登录用户是否为管理员
     */
    private boolean isAdmin() {
        return UserTypeEnum.ADMIN.getCode().equals(SecurityUtils.getLoginUser().getSysUser().getUserType());
    }
}
