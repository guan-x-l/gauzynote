import dayjs from "dayjs";
import "dayjs/locale/zh-cn";
import isoWeek from "dayjs/plugin/isoWeek";
import weekday from "dayjs/plugin/weekday";
import weekOfYear from "dayjs/plugin/weekOfYear";
import advancedFormat from "dayjs/plugin/advancedFormat";
import localizedFormat from "dayjs/plugin/localizedFormat";
import isToday from "dayjs/plugin/isToday";

dayjs.extend(isoWeek);
dayjs.extend(weekday);
dayjs.extend(weekOfYear);
dayjs.extend(advancedFormat);
dayjs.extend(localizedFormat);
dayjs.extend(isToday);
// 导出配置好的 dayjs
export default dayjs
