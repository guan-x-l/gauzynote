
export const basicRoutes = [
    {
        path: "/login",
        name: "Login",
        meta: {},
        component: () => import("@/views/login.vue")
    },
    {
        path: "/",
        name: "",
        redirect: 'newtab',
        component: () => import("@/layout/index.vue"),
        children: [
            {
                path: "/index",
                name: "index",
                meta: {
                    title: '首页',
                    isOnly: true,
                },
                component: () => import("@/views/index.vue")
            },
            {
                path: "/iconpages",
                name: "iconpages",
                meta: {
                    title: 'iconpages'
                },
                component: () => import("@/views/iconPages.vue")
            },
            {
                path: "/newtab",
                name: "newTab",
                meta: {
                    title: '新标签页'
                },
                component: () => import("@/views/newTab.vue")
            },
            {
                path: "/note/:id",
                name: "note",
                meta: {
                    // 路由动态组件
                    dynamicComponent: import("@/views/note/note.vue")
                },
                // component: () => import("@/views/note/note.vue")
                component: {
                    render(e) {
                        return null;
                    }
                }
            },
            {
                path: "/file/:id",
                name: "file",
                meta: {
                    dynamicComponent: import("@/views/file/file.vue")
                },
                component: {
                    render(e) {
                        return null;
                    }
                }
            },
            {
                path: "/task",
                name: "task",
                meta: {
                    title: '任务列表',
                    isOnly: true,
                },
                component: () => import("@/views/task/task.vue"),
            },
            {
                path: "/dict/type",
                name: "dictType",
                meta: {
                    title: '字典管理',
                    isOnly: true,
                },
                component: () => import("@/views/dict/dictType.vue"),
            },
            {
                path: "/dict/data",
                name: "dictData",
                meta: {
                    title: '字典数据',
                    isOnly: true,
                },
                component: () => import("@/views/dict/dictData.vue"),
            },
            {
                path: "/system/user",
                name: "userManagement",
                meta: {
                    title: '用户管理',
                    isOnly: true,
                },
                component: () => import("@/views/user/userManagement.vue"),
            },
            {
                path: "/system/account",
                name: "account",
                meta: {
                    title: '帐号管理',
                    isOnly: true,
                },
                component: () => import("@/views/user/account.vue"),
            },
        ]
    },
    {
        path: "/500",
        name: "500",
        meta: {
            title: "ErrorPage"
        },
        component: () => import("@/views/errorPages/error500.vue")
    },
    {
        path: "/:path(.*)*",
        name: "404",
        meta: {
            title: "NotFound",
        },
        component: () => import("@/views/errorPages/error404.vue")
    }
];
