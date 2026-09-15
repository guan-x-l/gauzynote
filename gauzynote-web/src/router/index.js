import {createRouter, createWebHistory} from "vue-router";
import { basicRoutes } from "./routes";
import {getToken} from "@/utils/auth.js";

export const router = createRouter({
    history: createWebHistory('/gauzynote/'),
    routes: basicRoutes,
    strict: true,
    // 当切换页面，滚动到最顶部
    // scrollBehavior: () => ({ left: 0, top: 0 }),
});

// Injection Progress
router.beforeEach(async (to, from, next) => {
    const isMobileRoute = to.path === '/m' || to.path.startsWith('/m/')
    const isLoginRoute = to.path === '/login' || to.path === '/m/login'
    if (getToken()) {
        // to.meta.title && store.dispatch('settings/setTitle', to.meta.title)
        /* has token*/
        if (isLoginRoute) {
            next({ path: to.path === '/m/login' ? '/m' : '/' })
        } else {
            next()
        }
    } else {
        // 没有token
        if (isLoginRoute) {
            next()
        } else {
            next({
                path: isMobileRoute ? '/m/login' : '/login',
                query: {redirect: to.fullPath}
            }) // 否则全部重定向到登录页
        }
    }
});

router.afterEach(async (to, from, failure) => {
    // console.log(to)
    // console.log(from)
    // console.log(failure)
});
