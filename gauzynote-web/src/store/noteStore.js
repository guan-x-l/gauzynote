import {computed, isRef, nextTick, ref} from "vue";
import {getAssetNote} from "@/api/assetNote.js";
import {Message} from "@/components/index.js";
import {useTabsStore} from "@/store/tabsStore.js";
import {useRouter} from "vue-router";
import {$t} from "@/locales";

/**@type {import('vue').Ref<Object.<number, Object>>} */
const noteMap = ref({})
/**@type {import('vue').Ref<Object.<number, number>>} */
const noteVersionMap = ref({})

export const useNoteStore = (id) => {
    const _id = isRef(id) ? id : ref(id)
    const {activeTab, removeTab} = useTabsStore()
    const router = useRouter()

    const noteName = ref()
    const noteVersion = computed(() => noteVersionMap.value[_id.value])
    const noteData = computed({
        get() {
            return noteMap.value[_id.value]
        },
        set(newValue) {
            noteMap.value[_id.value] = newValue
        },
    })

    async function getNote(loading) {
        const loadTimer = setTimeout(() => {
            if (loading) loading.value = true
        }, 50)
        return getAssetNote(_id.value).then(async res => {
            if (res.data) {
                res.data.content = res.data.content || ''
                noteMap.value[_id.value] = res.data
                noteName.value = res.data.noteName || ''
            } else if (res.code === 200) {
                Message.error($t('message.noAsset'))
                toNewTab()
            }
            return res
        }).finally(async () => {
            clearTimeout(loadTimer)
            if (loading) loading.value = false
        })

    }

    function toNewTab() {
        removeTab(activeTab.value)
        router.push('/newtab')


    }

    function handNoteUpdate(newValue) {
        noteMap.value[_id.value] = newValue // 显式更新原数据
        noteName.value = newValue.noteName || ''

    }

    function updateNoteVersion() {
        noteVersionMap.value[_id.value] = noteVersion.value ? noteVersion.value + 1 : 1;
    }

    return {
        noteData,
        noteName,
        noteVersion,
        getNote,
        updateNoteVersion,
    }
}
