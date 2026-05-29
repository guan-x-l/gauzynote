import { computed, ref } from "vue";
import { Message } from "@/components/index.js";
import localStorageUtil from "@/utils/lib/localStorageUtil.js";
import {commonKeys} from "@/constants/cacheKeys.js";

/**
 * 当前主题 ('light' | 'dark' | 'system')
 */
const currentTheme = ref(localStorageUtil.get(commonKeys.THEME) || 'system');

// 存储媒体查询实例，方便移除监听
let darkModeMediaQuery = null;

export function useThemeStore() {
    /**
     * 是否为暗色模式（计算属性，考虑系统设置）
     */
    const isDark = computed(() => {
        if (currentTheme.value === 'system') {
            // 跟随系统时，直接检测当前系统主题
            return window.matchMedia('(prefers-color-scheme: dark)').matches;
        }
        return currentTheme.value === 'dark';
    });

    /**
     * 获取实际应用的主题名称（处理system类型）
     * @returns {string} 'light' | 'dark'
     */
    const getAppliedTheme = () => {
        if (currentTheme.value === 'system') {
            return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
        }
        return currentTheme.value;
    };

    /**
     * 应用主题到 DOM
     * @param {string} themeName - 主题名称 ('light' | 'dark' | 'system')
     */
    const applyTheme = (themeName) => {
        const actualTheme = themeName === 'system'
            ? getAppliedTheme()
            : themeName;
        document.documentElement.className = `theme-${actualTheme}`;
        document.body.setAttribute('theme', actualTheme)
    };

    /**
     * 加载主题并持久化
     * @param {string} themeName - 主题名称 ('light' | 'dark' | 'system')
     */
    const loadTheme = (themeName) => {
        try {
            // 验证主题名称合法性
            if (!['light', 'dark', 'system'].includes(themeName)) {
                throw new Error(`无效的主题名称: ${themeName}`);
            }

            currentTheme.value = themeName;
            applyTheme(themeName);
            localStorageUtil.set(commonKeys.THEME, themeName);
        } catch (error) {
            Message.error('主题切换失败: ' + error.message);
        }
    };

    /**
     * 切换主题（light ↔ dark，不包含system）
     */
    const toggleTheme = () => {
        loadTheme(currentTheme.value === 'dark' ? 'light' : 'dark');
    };

    /**
     * 切换到指定主题（包含system）
     * @param {string} themeName - 'light' | 'dark' | 'system'
     */
    const switchTheme = (themeName) => {
        loadTheme(themeName);
    };

    /**
     * 处理系统主题变化
     * @param {MediaQueryListEvent | MediaQueryList} e
     */
    const handleMediaQueryChange = (e) => {
        // 只有在跟随系统模式下才响应系统主题变化
        if (currentTheme.value === 'system') {
            applyTheme('system');
        }
    };

    /**
     * 移除媒体查询监听
     */
    const removeMediaQueryListener = () => {
        if (darkModeMediaQuery) {
            try {
                darkModeMediaQuery.removeEventListener('change', handleMediaQueryChange);
            } catch (e1) {
                try {
                    darkModeMediaQuery.removeListener(handleMediaQueryChange);
                } catch (e2) {
                    console.error('移除媒体查询监听失败:', e2);
                }
            }
        }
    };

    /**
     * 清除主题设置（恢复为跟随系统）
     */
    const clearTheme = () => {
        removeMediaQueryListener();
        loadTheme('system');
    };

    /**
     * 初始化主题
     */
    const initTheme = () => {
        // 先移除可能存在的旧监听
        removeMediaQueryListener();

        // 创建媒体查询实例
        darkModeMediaQuery = window.matchMedia('(prefers-color-scheme: dark)');

        // 添加系统主题变化监听
        try {
            darkModeMediaQuery.addEventListener('change', handleMediaQueryChange);
        } catch (e1) {
            try {
                darkModeMediaQuery.addListener(handleMediaQueryChange);
            } catch (e2) {
                console.error('添加媒体查询监听失败:', e2);
            }
        }

        // 初始化主题
        loadTheme(currentTheme.value || 'system');
    };

    /**
     * 卸载主题相关资源（组件卸载时调用）
     */
    const destroyTheme = () => {
        removeMediaQueryListener();
        darkModeMediaQuery = null;
    };

    return {
        // 响应式状态
        currentTheme,
        isDark,

        // 方法
        toggleTheme,       // 切换明暗主题（light/dark）
        switchTheme,       // 切换到指定主题（支持system）
        initTheme,         // 初始化主题
        clearTheme,        // 清除主题（恢复跟随系统）
        loadTheme,         // 加载主题
        destroyTheme,      // 销毁监听
        getAppliedTheme    // 获取实际应用的主题
    };
}
