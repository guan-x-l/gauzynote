package com.gauzynote.system.service;

import com.gauzynote.common.domain.AjaxResult;
import com.gauzynote.common.domain.entity.SysUser;
import com.gauzynote.common.domain.model.LoginUser;
import com.gauzynote.common.exception.ServiceException;
import com.gauzynote.system.domain.entity.SysDictData;
import com.gauzynote.system.domain.entity.SysDictType;
import com.gauzynote.system.mapper.SysDictDataMapper;
import com.gauzynote.system.mapper.SysDictTypeMapper;
import com.gauzynote.system.mapper.SysUserMapper;
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
class SysDictDataServiceTest {

    @Mock
    private SysDictDataMapper dictDataMapper;

    @Mock
    private SysDictTypeMapper dictTypeMapper;

    @Mock
    private SysUserMapper sysUserMapper;

    @InjectMocks
    private SysDictDataService dictDataService;

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
        SysDictData globalData = new SysDictData();
        globalData.setDictCode(1L);
        globalData.setUserId(null);
        SysDictData ownData = new SysDictData();
        ownData.setDictCode(2L);
        ownData.setUserId(2L);
        when(dictDataMapper.selectDictDataList(any(SysDictData.class), eq(2L))).thenReturn(Arrays.asList(globalData, ownData));

        List<SysDictData> result = dictDataService.selectDictDataList(new SysDictData());

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
        SysDictData dictData = new SysDictData();
        dictData.setDictType("custom_type");
        dictData.setDictLabel("标签");
        dictData.setDictValue("VALUE_A");
        dictData.setUserId(null);

        SysDictType ownType = new SysDictType();
        ownType.setDictType("custom_type");
        ownType.setUserId(2L);
        SysUser user = new SysUser();
        user.setUserId(2L);

        when(dictTypeMapper.selectDictTypeByTypeAndUserId("custom_type", null)).thenReturn(null);
        when(dictTypeMapper.selectDictTypeByTypeAndUserId("custom_type", 2L)).thenReturn(ownType);
        when(sysUserMapper.selectUserById(2L)).thenReturn(user);
        when(dictDataMapper.selectDictDataByTypeAndValue("custom_type", "VALUE_A", 2L)).thenReturn(null);
        when(dictDataMapper.insertDictData(dictData)).thenAnswer(invocation -> {
            dictData.setDictCode(100L);
            return 1;
        });

        AjaxResult result = dictDataService.insertDictData(dictData);

        Assertions.assertEquals(2L, dictData.getUserId());
        Assertions.assertEquals(200, result.get("code"));
    }

    /**
     * 管理员不能新增他人私有数据（仅允许公共数据或本人数据）
     */
    @Test
    void shouldRejectInsertOtherUserDataByAdmin() {
        mockLogin(1L, "admin", "0");
        SysDictData dictData = new SysDictData();
        dictData.setUserId(2L);

        Assertions.assertThrows(ServiceException.class, () -> dictDataService.insertDictData(dictData));
    }

    /**
     * 非管理员不能删除公共数据
     */
    @Test
    void shouldRejectDeleteGlobalDataByNonAdmin() {
        mockLogin(2L, "userA", "1");
        SysDictData globalData = new SysDictData();
        globalData.setDictCode(1L);
        globalData.setUserId(null);
        when(dictDataMapper.selectDictDataById(1L, 2L)).thenReturn(globalData);

        Assertions.assertThrows(ServiceException.class, () -> dictDataService.deleteDictDataById(1L));
        verify(dictDataMapper, never()).deleteDictDataById(eq(1L), eq(2L), eq(false));
    }

    /**
     * 管理员可以删除公共数据
     */
    @Test
    void shouldAllowDeleteGlobalDataByAdmin() {
        mockLogin(1L, "admin", "0");
        SysDictData globalData = new SysDictData();
        globalData.setDictCode(1L);
        globalData.setUserId(null);
        when(dictDataMapper.selectDictDataById(1L, 1L)).thenReturn(globalData);
        when(dictDataMapper.deleteDictDataById(1L, 1L, true)).thenReturn(1);

        int rows = dictDataService.deleteDictDataById(1L);

        Assertions.assertEquals(1, rows);
        verify(dictDataMapper).deleteDictDataById(1L, 1L, true);
    }

    private void mockLogin(Long userId, String username, String userType) {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(userId);
        sysUser.setUsername(username);
        sysUser.setUserType(userType);
        LoginUser loginUser = new LoginUser(userId, username, "pwd",LoginUser.LOGIN_METHOD_PASSWORD, sysUser);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(loginUser, null, null);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
