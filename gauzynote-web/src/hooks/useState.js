import { ref } from 'vue';

/**
 * @template T
 * @param {T=} defaultValue - 状态的默认值，可选参数
 * @returns {[import('vue').Ref<T>, (newValue: T) => void]} - 当前ref，更新状态值
 */
export function useState(defaultValue){
    /**
     * @type {import('vue').Ref<T>}
     */
    const value = ref(defaultValue);
    /**
     * @template T
     * @param {T} newValue
     */
    const setValue = (newValue) => {
        value.value = newValue;
    };

    return [value, setValue];
}