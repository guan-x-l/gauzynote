
/**
 * 图标基础 props 定义
 */
export const iconBaseProps = {
    /**
     * 线条宽度
     * @type {number}
     * @default 2
     */
    strokeWidth: {
        type: Number,
        default: 2,
    },
    /**
     * 端点类型
     * @type {import('vue').PropType.<'butt' | 'round' | 'square'>}
     * @default round
     */
    strokeLinecap: {
        type: String,
        default: 'round',
    },
    /**
     * 拐角类型
     * @type {import('vue').PropType.<'arcs' | 'bevel' | 'miter' | 'miter-clip' | 'round'>}
     * @default round
     */
    strokeLinejoin: {
        type: String,
        default: 'round',
    },
    /**
     * 旋转角度
     */
    rotate: {
        type: [String, Number],
    },
    /**
     * 是否旋转
     */
    spin: {
        type: Boolean,
        default: false,
    },
    /**
     * 尺寸
     */
    size: {
        type: [String, Number],
    },
    /**
     * 背景填充
     */
    fill: {
        type: String,
        default: 'none',
    },
}