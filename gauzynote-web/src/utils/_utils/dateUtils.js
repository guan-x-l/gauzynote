import dayjs from "dayjs";

/**
 * 获取一周起始日期（周一/周日根据 dayjs 国际化配置）
 * @returns {Date}
 */
export function getWeekStart(date = new Date()) {
    return dayjs(date).startOf('week').toDate()
}
/**
 * 根据周起始日 + 索引获取当天日期
 */
export function getWeekdayDate(weekStart, dayIndex) {
    return dayjs(weekStart).weekday(dayIndex).startOf('date').toDate();
}
