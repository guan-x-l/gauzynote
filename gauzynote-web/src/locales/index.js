import { createI18n } from 'vue-i18n';
import zhCN from './zh-cn.js';
import enUS from './en-us.js';
import localStorageUtil from "@/utils/lib/localStorageUtil.js";
import {commonKeys} from "@/constants/cacheKeys.js";

// 定义支持的语言列表
const messages = {
    'zh-CN': zhCN,
    'en-US': enUS
};

// 获取浏览器默认语言（可选，用于自动设置初始语言）
const getDefaultLanguage = () => {
    const browserLang = localStorageUtil.get(commonKeys.LANGUAGE) || navigator.language || (navigator).userLanguage;
    return messages[browserLang] ? browserLang : 'zh-CN'; // 默认中文
};

// 创建 i18n 实例
export const i18n = createI18n({
    legacy: false, // 使用 Composition API 模式（必须设置为 false）
    // locale: 'en-US', // 初始语言
    locale: getDefaultLanguage(), // 初始语言
    fallbackLocale: 'zh-CN', // 回退语言（当当前语言缺少翻译时使用）
    messages // 翻译资源
});

export const  $t = i18n.global.t
