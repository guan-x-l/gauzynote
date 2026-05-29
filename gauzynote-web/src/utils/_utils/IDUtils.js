/**
 * 生成基于当前时间和随机数的唯一ID
 * 格式：[时间戳的36进制表示]-[随机字符串]
 * 时间戳确保ID的大致顺序，随机字符串确保唯一性
 * @returns {string} 唯一ID字符串
 */
export function createUniqueId() {
    return Date.now().toString(36) + Math.random().toString(36).substring(2, 5);
}

/**
 * 生成基于当前时间和随机数的唯一ID
 * 格式：[时间戳]-[随机数字]
 * 时间戳确保ID的大致顺序，随机字符串确保唯一性
 * @returns {number} 唯一ID字符串
 */
export function createUniqueTimeId() {
    return Date.now() + Math.floor(Math.random() * 1000);
}

/**
 * 创建UUID
 * @returns {string}
 */
export function createUUID () {
    if (typeof crypto === 'object') {
        if (typeof crypto.randomUUID === 'function') {
            return crypto.randomUUID();
        }
        if (typeof crypto.getRandomValues === 'function' && typeof Uint8Array === 'function') {
            const callback = (c) => {
                const num = Number(c);
                return (num ^ (crypto.getRandomValues(new Uint8Array(1))[0] & (15 >> (num / 4)))).toString(16);
            };
            return ([1e7]+-1e3+-4e3+-8e3+-1e11).replace(/[018]/g, callback);
        }
    }
    let timestamp = new Date().getTime();
    let performNow = (typeof performance !== 'undefined' && performance.now && performance.now() * 1000) || 0;
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
        let random = Math.random() * 16;
        if (timestamp > 0) {
            random = (timestamp + random) % 16 | 0;
            timestamp = Math.floor(timestamp / 16);
        } else {
            random = (performNow + random) % 16 | 0;
            performNow = Math.floor(performNow / 16);
        }
        return (c === 'x' ? random : (random & 0x3) | 0x8).toString(16);
    });
}