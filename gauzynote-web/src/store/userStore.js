import {computed, ref} from "vue";
import {getInfo, logout} from "@/api/login.js";
import {removeToken} from "@/utils/auth.js";
import {useRouter} from "vue-router";
import {UserType} from "@/enum/index.js";
import localStorageUtil from "@/utils/lib/localStorageUtil.js";
import {userKeys} from "@/constants/cacheKeys.js";

/**
 *
 * @type {import('vue').Ref<Object>}
 */
const userinfo = ref({})
// 需要修改初始密码
const isDefaultModifyPwd = ref(false)
const userType = computed(()=> UserType.getByCode(userinfo.value?.userType)?.getCode())

export const useUserStore = () => {

    const router = useRouter()
    function updateUserinfo() {
        getInfo().then(res=>{
            userinfo.value = res.data
            localStorageUtil.set(userKeys.username, userinfo.value.username)
            isDefaultModifyPwd.value = res.isDefaultModifyPwd
        })
    }

    function signOut() {
        logout().then(res=>{
            removeToken()
            userinfo.value = {}
            router.push('/login')
            // location.href = '/login'
        })
    }

    return {
        userinfo,
        userType,
        isDefaultModifyPwd,
        updateUserinfo,
        signOut,
    }
}
