/**
 * 延时
 * @param delay 延时时间
 */
export function sleep(delay = 300) {
    return new Promise(resolve => setTimeout(resolve, delay))
}

/**
 * 定时器
 * @param callback 回调
 * @param interval 间隔时长
 * @param autoStart 是否自动开始
 * @param pauseNumber 自动暂停次数
 * @param args
 */
export function pausedInterval(callback, interval, autoStart = true, pauseNumber, ...args) {
    let timerId = null;
    let remaining = interval;
    let startTime;
    let isPaused = true;
    // 循环次数，回调前计数
    let cyclesNumber= 0
    let _args = args

    /**
     * 开始
     */
    const start = (...args) => {
        if (args !== undefined) _args = args
        // 如果已经有计时器运行，则不做任何操作
        if (timerId !== null || isPaused) return;

        startTime = new Date(); // 更新开始时间
        timerId = setTimeout(nextTick, remaining);
    };

    /**
     * 暂停
     */
    const pause = () => {
        if (timerId != null) {
            isPaused = true; // 标记为已暂停
            // 清除定时器，并计算剩余时间
            clearTimeout(timerId);
            remaining -= new Date() - startTime;
            timerId = null; // 确保计时器ID被重置
        }
    };

    const nextTick = () => {
        if (isPaused) return
        if (pauseNumber && cyclesNumber >= pauseNumber) return
        cyclesNumber++
        callback(..._args);
        if (isPaused) return
        // 自动继续
        remaining = interval;
        timerId = null;
        start();
    };
    /**
     * 继续（从暂停状态恢复）
     */
    const continueTimer = (...args) => {
        isPaused = false;
        start(...args)
    };
    /**
     * 重新开始
     */
    const restart = (...args) => {
        pause()
        remaining = interval
        isPaused = false;
        cyclesNumber= 0
        start(...args);
    }


    if (autoStart) {
        isPaused = false
        start(); // 根据autoStart决定是否立即开始定时器
    }

    return {
        cyclesNumber: ()=> cyclesNumber,
        isPaused: ()=> isPaused,
        pause,
        restart,
        start,
        continueTimer  // 使用start作为resume的实现
    };
}
