package com.gauzynote.system.service;

import com.gauzynote.common.domain.entity.SysUser;
import com.gauzynote.common.domain.model.LoginUser;
import com.gauzynote.common.enums.SysResourceNodeType;
import com.gauzynote.system.domain.entity.Note;
import com.gauzynote.system.domain.entity.SysResourceNode;
import com.gauzynote.system.mapper.SysResourceNodeMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysResourceNodeServiceTest {

    @Mock
    private SysResourceNodeMapper sysResourceNodeDao;

    @Mock
    private NoteService noteService;

    @InjectMocks
    private SysResourceNodeService sysResourceNodeService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldSelectOnlyCurrentUsersNoteNodeWhenRenamingNote() {
        mockLogin(7L);
        Note note = new Note();
        note.setNoteId(143L);
        note.setNoteName("未命名1");
        SysResourceNode node = new SysResourceNode();
        node.setNodeId(9L);
        node.setNodeType(SysResourceNodeType.NOTE.getCode());
        when(sysResourceNodeDao.selectByRelatedIdAndUserIdAndNodeType(143L, 7L, SysResourceNodeType.NOTE.getCode()))
                .thenReturn(node);
        when(sysResourceNodeDao.selectByParentIdAndNodeName(7L, null, "未命名1", SysResourceNodeType.NOTE.getCode()))
                .thenReturn(null);
        when(sysResourceNodeDao.updateNodeName(any(SysResourceNode.class))).thenReturn(1);

        int rows = sysResourceNodeService.updateNodeNameByNote(note);

        Assertions.assertEquals(1, rows);
        verify(sysResourceNodeDao).selectByRelatedIdAndUserIdAndNodeType(143L, 7L, SysResourceNodeType.NOTE.getCode());
        verify(noteService).updateNoteName(any(Note.class));
    }

    private void mockLogin(Long userId) {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(userId);
        LoginUser loginUser = new LoginUser(userId, "user", "pwd", LoginUser.LOGIN_METHOD_PASSWORD, sysUser);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, null));
    }
}
