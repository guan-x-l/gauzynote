import {toRefs, computed, watch} from 'vue';
import {isUndefined} from "@/utils/index.js";
import {useState} from "@/hooks/useState.js";

/**
 * useMergeState，实现「外部传入值+内部本地值」的合并管理
 * @template T - 初始默认值的类型（核心类型）
 * @template E - 外部props.value的类型
 * @param {T} defaultValue - 状态的默认值
 * @param {Object} props - 外部传入的属性对象，包含value属性
 * @param {E} props.value - 外部控制的值，类型为E（T | undefined）
 * @returns {[import('vue').ComputedRef<T>, (val: E) => void, import('vue').Ref<T>]}
 */
export function useMergeState(
    defaultValue,
    props
) {
    const {value} = toRefs(props);
    const [localValue, setLocalValue] = useState(
        !isUndefined(value.value) ? value.value : defaultValue
    );
    watch(value, (newVal) => {
        isUndefined(newVal) && setLocalValue(undefined);
    });

    const mergeValue = computed(() =>
        !isUndefined(value.value) ? value.value : localValue.value
    );

    return [mergeValue, setLocalValue, localValue];
}