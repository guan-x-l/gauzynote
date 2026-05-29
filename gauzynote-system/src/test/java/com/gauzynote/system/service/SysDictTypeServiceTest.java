package com.gauzynote.system.service;

import com.gauzynote.common.domain.AjaxResult;
import com.gauzynote.common.domain.entity.SysUser;
import com.gauzynote.common.domain.model.LoginUser;
import com.gauzynote.common.exception.ServiceException;
import com.gauzynote.system.domain.entity.SysDictType;
import com.gauzynote.system.mapper.SysDictDataMapper;
import com.gauzynote.system.mapper.SysDictTypeMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysDictTypeServiceTest {

    @Mock
    private SysDictTypeMapper dictTypeMapper;

    @Mock
    private SysDictDataMapper dictDataMapper;

    @InjectMocks
    private SysDictTypeService dictTypeService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 非管理员查询列表时：公共数据可见但不可操作，本人数据可操作
     */
    @Test
    void shouldMarkCanOperateForNonAdminList() {
        mockLogin(2L, "userA", "1");
        SysDictType globalType = new SysDictType();
        globalType.setDictId(1L);
        globalType.setUserId(null);
        SysDictType ownType = new SysDictType();
        ownType.setDictId(2L);
        ownType.setUserId(2L);
        when(dictTypeMapper.selectDictTypeList(any(SysDictType.class), eq(2L))).thenReturn(Arrays.asList(globalType, ownType));

        List<SysDictType> result = dictTypeService.selectDictTypeList(new SysDictType());

        Assertions.assertEquals(2, result.size());
        Assertions.assertFalse(result.get(0).getCanOperate());
        Assertions.assertTrue(result.get(1).getCanOperate());
    }

    /**
     * 非管理员新增时强制归属到本人 user_id
     */
    @Test
    void shouldForceUserIdToCurrentWhenInsertByNonAdmin() {
        mockLogin(2L, "userA", "1");
        SysDictType dictType = new SysDictType();
        dictType.setDictName("私有类型");
        dictType.setDictType("private_type");
        dictType.setUserId(null);
        when(dictTypeMapper.selectDictTypeByTypeAndUserId("private_type", 2L)).thenReturn(null);
        when(dictTypeMapper.insertDictType(dictType)).thenAnswer(invocation -> {
            dictType.setDictId(100L);
            return 1;
        });

        AjaxResult result = dictTypeService.insertDictType(dictType);

        Assertions.assertEquals(2L, dictType.getUserId());
        Assertions.assertEquals("userA", dictType.getCreateBy());
        Assertions.assertEquals(200, result.get("code"));
    }

    /**
     * 管理员不能新增他人私有数据（仅允许公共数据或本人数据）
     */
    @Test
    void shouldRejectInsertOtherUserDataByAdmin() {
        mockLogin(1L, "admin", "0");
        SysDictType dictType = new SysDictType();
        dictType.setDictType("x");
        dictType.setUserId(2L);

        Assertions.assertThrows(ServiceException.class, () -> dictTypeService.insertDictType(dictType));
    }

    /**
     * 非管理员不能删除公共数据
     */
    @Test
    void shouldRejectDeleteGlobalDataByNonAdmin() {
        mockLogin(2L, "userA", "1");
        SysDictType globalType = new SysDictType();
        globalType.setDictId(1L);
        globalType.setDictType("sys_user_sex");
        globalType.setUserId(null);
        when(dictTypeMapper.selectDictTypeById(1L, 2L)).thenReturn(globalType);

        Assertions.assertThrows(ServiceException.class, () -> dictTypeService.deleteDictTypeById(1L));
        verify(dictTypeMapper, never()).deleteDictTypeById(eq(1L), eq(2L), eq(false));
    }

    /**
     * 管理员可以删除公共数据
     */
    @Test
    void shouldAllowDeleteGlobalDataByAdmin() {
        mockLogin(1L, "admin", "0");
        SysDictType globalType = new SysDictType();
        globalType.setDictId(1L);
        globalType.setDictType("sys_user_sex");
        globalType.setUserId(null);
        when(dictTypeMapper.selectDictTypeById(1L, 1L)).thenReturn(globalType);
        when(dictDataMapper.countDictDataByType("sys_user_sex")).thenReturn(0);
        when(dictTypeMapper.deleteDictTypeById(1L, 1L, true)).thenReturn(1);

        int rows = dictTypeService.deleteDictTypeById(1L);

        Assertions.assertEquals(1, rows);
        verify(dictTypeMapper).deleteDictTypeById(1L, 1L, true);
    }

    private void mockLogin(Long userId, String username, String userType) {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(userId);
        sysUser.setUsername(username);
        sysUser.setUserType(userType);
        LoginUser loginUser = new LoginUser(userId, username, "pwd", LoginUser.LOGIN_METHOD_PASSWORD, sysUser);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(loginUser, null, null);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
