package com.gauzynote.system.mapper;

import com.gauzynote.system.domain.entity.Task;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 任务表(Task)数据库访问层
 */
public interface TaskMapper {
    /**
     * 查询当前用户任务列表
     *
     * @param userId 当前用户id
     * @param status 状态（可选）
     * @param startDate 截止日期起始（可选）
     * @param endDate 截止日期结束（可选）
     * @return 任务列表
     */
    List<Task> selectList(@Param("userId") Long userId, @Param("status") String status,
                          @Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 查询当前用户最大的任务排序值。
     *
     * @param userId 当前用户id
     * @return 最大排序值
     */
    Integer selectMaxSort(@Param("userId") Long userId);

    /**
     * 查询当前用户最大的看板排序值。
     *
     * @param userId 当前用户id
     * @return 最大看板排序值
     */
    Integer selectMaxKanbanSort(@Param("userId") Long userId);

    /**
     * 查询当前用户最大的甘特图排序值。
     *
     * @param userId 当前用户id
     * @return 最大甘特图排序值
     */
    Integer selectMaxGanttSort(@Param("userId") Long userId);

    /**
     * 通过ID查询单条数据
     *
     * @param taskId 主键
     * @param userId 当前用户id
     * @return 实例对象
     */
    Task selectById(@Param("taskId") Long taskId, @Param("userId") Long userId);

    /**
     * 新增数据
     *
     * @param task 实例对象
     * @return 影响行数
     */
    int insert(Task task);

    /**
     * 修改数据
     *
     * @param task 实例对象
     * @return 影响行数
     */
    int update(Task task);

    /**
     * 更新任务排序。
     *
     * @param taskId 任务id
     * @param userId 当前用户id
     * @param sort 排序值
     * @return 影响行数
     */
    int updateSort(@Param("taskId") Long taskId, @Param("userId") Long userId, @Param("sort") Integer sort);

    /**
     * 更新任务看板排序。
     *
     * @param taskId 任务id
     * @param userId 当前用户id
     * @param kanbanSort 看板排序值
     * @return 影响行数
     */
    int updateKanbanSort(@Param("taskId") Long taskId, @Param("userId") Long userId, @Param("kanbanSort") Integer kanbanSort);

    /**
     * 更新任务甘特图排序。
     *
     * @param taskId 任务id
     * @param userId 当前用户id
     * @param ganttSort 甘特图排序值
     * @return 影响行数
     */
    int updateGanttSort(@Param("taskId") Long taskId, @Param("userId") Long userId, @Param("ganttSort") Integer ganttSort);

    /**
     * 通过主键删除数据（逻辑删除）
     *
     * @param taskId 主键
     * @param userId 当前用户id
     * @return 影响行数
     */
    int deleteById(@Param("taskId") Long taskId, @Param("userId") Long userId);
}
