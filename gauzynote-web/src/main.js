import {createApp} from 'vue'
import App from './App.vue';
import 'normalize.css/normalize.css'
import './assets/style/base.css'
import './assets/style/flex.css'
import {router} from "./router";
import icons from "./components/icons/index.js";
import GlobalComponents from "./components/index.js";
import AppContext from './context.js'
import {i18n} from "./locales";
import dayjs from "@/utils/lib/dayjs.js";
import localStorageUtil from "@/utils/lib/localStorageUtil.js";
import {commonKeys} from "@/constants/cacheKeys.js";

// 监听 beforeunload 事件
window.isFormDirty = new Set()
window.addEventListener("beforeunload", (event) => {
    if (isFormDirty.size) {
        // 自定义提示信息
        const message = "您有未保存的更改，确定要离开吗？";
        // 标准化事件的返回值
        event.returnValue = message; // 对某些浏览器有效
        return message; // 对于其他浏览器有效
    }
});
// import setupStorageCompatibility from "@/storage-compatibility.js";
// setupStorageCompatibility()

// 监听存储错误
// window.addEventListener('storage-local-error', (event) => {
//     console.error('本地存储错误:', event.detail);
// });



/**
 * @type {import('vue').App<Element>}
 */
const Vue = createApp(App)

// 注册全局自定义指令 v-img-error
Vue.directive('img-error', {
    mounted(el) {
        el.onerror = function() {
            if (el.alt==='') {
                el.removeAttribute('alt')
            }
            el.classList.add('img-error')
        }
    }
})

// 循环注册组件
Object.keys(icons).forEach(componentName => {
    Vue.component(componentName, icons[componentName]);
});

// 循环注册组件
Object.keys(GlobalComponents).forEach(componentName => {
    Vue.component(componentName, GlobalComponents[componentName]);
});

const locale = localStorageUtil.get(commonKeys.LANGUAGE)
dayjs.locale(locale === 'zh-CN' ? 'zh-cn' : 'en')
Vue.use(router)
Vue.use(i18n)
AppContext.appContext = Vue._context
Vue.mount('#app')
