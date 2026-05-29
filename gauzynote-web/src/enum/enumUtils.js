import Enum from "@/enum/Enum.js";

/**
 * 通用枚举：获取所有枚举实例
 * @this {Function} 枚举类（如 UserStatus）
 * @returns {Array} 枚举实例数组（过滤掉非实例的静态属性）
 */
export function enumValues() {
    return Object.values(this)
        .filter(v => {
            // 1. 排除函数（避免筛选到 values、getByCode 等静态方法）
            // 2. 确保是当前枚举类的实例
            return typeof v !== 'function' && (v instanceof this || v instanceof Enum);
        }).sort((a, b) => a.getSortIndex() - b.getSortIndex());
}

/**
 * 通用枚举：根据 code 获取枚举实例
 * @this {Function} 枚举类（如 UserStatus）
 * @param {string|number} code 枚举编码
 * throws {Error} 无匹配时抛出错误
 */
export function enumGetByCode(code) {
    const enumInstances = this.values();
    const match = enumInstances.find(instance => instance.getCode() === code);
    if (!match) {
        // throw new Error(`未知的 ${this.name} 枚举值：${code}`);
        return null
    }
    return match;
}
/**
 * 通用枚举：根据 info 获取枚举实例
 * @this {Function} 枚举类
 * @param {string|number} info 枚举信息
 * throws {Error} 无匹配时抛出错误
 */
export function enumGetByInfo(info) {
    const enumInstances = this.values();
    const match = enumInstances.find(instance => instance.getInfo() === info);
    if (!match) {
        // throw new Error(`未知的 ${this.name} 枚举值：${info}`);
        return null
    }
    return match;
}

/**
 * 通用枚举：获取所有枚举实例并转为object
 * @this {Function} 枚举类
 * throws {Error} 处理失败抛出错误
 */
export function enumsToObject(enums) {
    return enums.map(enumInstance => enumInstance.toObject());
}