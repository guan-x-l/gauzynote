package com.gauzynote.system.service;

import com.gauzynote.common.exception.ServiceException;
import com.gauzynote.common.utils.MessageUtils;
import com.gauzynote.common.utils.SecurityUtils;
import com.gauzynote.system.domain.entity.Task;
import com.gauzynote.system.mapper.TaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;

/**
 * 任务表(Task)服务类
 */
@Service
public class TaskService {
    @Resource
    private TaskMapper taskMapper;

    /**
     * 查询当前用户任务列表
     *
     * @param status 状态（可选）
     * @param startDate 截止日期起始（可选）
     * @param endDate 截止日期结束（可选）
     * @return 任务列表
     */
    public List<Task> selectList(String status, String startDate, String endDate) {
        return taskMapper.selectList(SecurityUtils.getUserId(), status, startDate, endDate);
    }

    /**
     * 查询当前用户单条任务
     *
     * @param taskId 任务id
     * @return 任务
     */
    public Task selectById(Long taskId) {
        return taskMapper.selectById(taskId, SecurityUtils.getUserId());
    }

    /**
     * 新增任务
     *
     * @param task 任务实体
     * @return 主键
     */
    public Long insert(Task task) {
        Long userId = SecurityUtils.getUserId();
        task.setUserId(userId);
        if (task.getStatus() == null || task.getStatus().isEmpty()) {
            task.setStatus("0");
        }
        if (task.getPriority() == null || task.getPriority().isEmpty()) {
            task.setPriority("1");
        }
        if (task.getSort() == null) {
            Integer maxSort = taskMapper.selectMaxSort(userId);
            task.setSort((maxSort == null ? 0 : maxSort) + 1);
        }
        if (task.getKanbanSort() == null) {
            Integer maxKanbanSort = taskMapper.selectMaxKanbanSort(userId);
            task.setKanbanSort((maxKanbanSort == null ? 0 : maxKanbanSort) + 1);
        }
        if (task.getGanttSort() == null) {
            Integer maxGanttSort = taskMapper.selectMaxGanttSort(userId);
            task.setGanttSort((maxGanttSort == null ? 0 : maxGanttSort) + 1);
        }
        if ("2".equals(task.getStatus()) && task.getActualCompletionTime() == null) {
            task.setActualCompletionTime(new Date());
        }
        int insert = taskMapper.insert(task);
        if (insert <= 0) {
            throw new ServiceException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, MessageUtils.message("creation.failed"));
        }
        return task.getTaskId();
    }

    /**
     * 更新任务
     *
     * @param task 任务实体
     * @return 影响行数
     */
    public int update(Task task) {
        task.setUserId(SecurityUtils.getUserId());
        if ("2".equals(task.getStatus()) && task.getActualCompletionTime() == null) {
            task.setActualCompletionTime(new Date());
        }
        int update = taskMapper.update(task);
        if (update <= 0) {
            throw new ServiceException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, MessageUtils.message("modification.failed"));
        }
        return update;
    }

    /**
     * 删除任务（逻辑删除）
     *
     * @param taskId 任务id
     * @return 影响行数
     */
    public int deleteById(Long taskId) {
        int delete = taskMapper.deleteById(taskId, SecurityUtils.getUserId());
        if (delete <= 0) {
            throw new ServiceException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, MessageUtils.message("deletion.failed"));
        }
        return delete;
    }

    /**
     * 批量保存任务排序。
     *
     * @param taskList 仅包含 taskId 与 sort 的任务集合
     * @return 更新条数
     */
    @Transactional(rollbackFor = Exception.class)
    public int updateSortList(List<Task> taskList, String sortField) {
        if (taskList == null || taskList.isEmpty()) {
            return 0;
        }
        Long userId = SecurityUtils.getUserId();
        int affectedRows = 0;
        for (Task task : taskList) {
            if (task.getTaskId() == null) {
                continue;
            }
            int update = updateSort(task, userId, sortField);
            if (update <= 0) {
                throw new ServiceException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, MessageUtils.message("modification.failed"));
            }
            affectedRows += update;
        }
        return affectedRows;
    }

    private int updateSort(Task task, Long userId, String sortField) {
        if ("kanbanSort".equals(sortField)) {
            if (task.getKanbanSort() == null) {
                return 0;
            }
            return taskMapper.updateKanbanSort(task.getTaskId(), userId, task.getKanbanSort());
        }
        if ("ganttSort".equals(sortField)) {
            if (task.getGanttSort() == null) {
                return 0;
            }
            return taskMapper.updateGanttSort(task.getTaskId(), userId, task.getGanttSort());
        }
        if (task.getSort() == null) {
            return 0;
        }
        return taskMapper.updateSort(task.getTaskId(), userId, task.getSort());
    }
}
