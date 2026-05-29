<script setup>
import {NodeType} from "@/enum/index.js";
import {useTabsStore} from "@/store/index.js";
import {useRoute, useRouter} from "vue-router";
import {getFileNameWithoutExtension} from "@/utils/index.js";
import Tooltip from "@/components/base/Tooltip/Tooltip.vue";
import {useI18n} from "vue-i18n";

const emit = defineEmits(["initView", "handleTabClick", "removeDynamicComponent"]);

const route = useRoute()
const router = useRouter()
const {t} = useI18n()

const {tabsList, activeTab, removeTab, changeTabFixed} = useTabsStore()

/**
 * 删除标签页
 * @param {Tab} tab - 标签页对象
 */
function deleteTab(tab) {
  if (tab.isPin) return;
  if (window.isFormDirty.has(tab.relatedId)) {
    if(confirm("您有未保存的更改，确定要离开吗？")){
      window.isFormDirty.delete(tab.relatedId)
    } else {
      return
    }
  }
  removeTab(tab).then(res => {
    if (activeTab.value?.fullPath) {
      router.push(activeTab.value.fullPath)
    } else if (route.path !== '/newtab') {
      router.push('/newtab')
    } else {
      emit('initView')
    }
  })
}

function handleTabClick(tab) {
  emit('handleTabClick', tab)
}
</script>

<template>
  <div class="tabs-pages min-w-0 self-end flex items-center">
    <transition-group>
      <div v-for="item in tabsList" class="tabs-pages__container min-w-0 flex" :key="item.tabId">
        <dropdown trigger="contextmenu">
          <div
              class="tabs-pages__item min-w-0 flex items-center"
              :class="{'tabs-pages__item--active': item.tabId === activeTab.tabId, 'tabs-pages__item-pinned': item.isPin}"
              @click="handleTabClick(item)"
              @click.middle="deleteTab(item)"
              :title="item.title"
          >
            <div class="tabs-pages__item-title flex-1 text-ellipsis" v-if="NodeType.isImage(item.nodeType)">
              <icon-image style="margin-right: 4px" />{{ getFileNameWithoutExtension(item.title) }}
            </div>
            <div class="tabs-pages__item-title flex-1 text-ellipsis" v-else>{{ item.title }}</div>
            <Tooltip v-if="item.isPin" :content="t('action.unpin')" position="bottom" :mouseEnterDelay="300" mini>
              <m-button class="tabs-pages__item-pin" type="base" size="mini" shape="circle" @click="changeTabFixed(item)" :aria-label="t('action.unpin')">
                <template #icon>
                  <icon-pin size="16"/>
                </template>
              </m-button>
            </Tooltip>
            <Tooltip v-else :content="t('action.close')" position="bottom" :mouseEnterDelay="300" mini>
              <m-button class="tabs-pages__item-close" type="base" size="mini" shape="circle"
                        @click.stop="deleteTab(item)" :aria-label="t('action.close')">
                <template #icon>
                  <icon-close size="16"/>
                </template>
              </m-button>
            </Tooltip>
            <span class="tabs-pages__item-border-mask tabs-pages__item-mask"></span>
            <svg class="tabs-pages__item-left-svg-mask tabs-pages__item-mask svg-icon" xmlns="http://www.w3.org/2000/svg"
                 viewBox="0 0 16 36">
              <path d="M7 36 L8 28 Q8 36 16 36Z" stroke="none"></path>
              <path d="M0 0 C0 0 8 0 8 8 L8,28 C8,36 16,36 16,36" stroke-width="1" fill="none"></path>
            </svg>
            <svg class="tabs-pages__item-right-svg-mask tabs-pages__item-mask svg-icon" xmlns="http://www.w3.org/2000/svg"
                 viewBox="0 0 16 36">
              <path d="M7 36 L8 28 Q8 36 16 36Z" stroke="none"></path>
              <path d="M0 0 C0 0 8 0 8 8 L8,28 C8,36 16,36 16,36" stroke-width="1" fill="none"></path>
            </svg>
          </div>
          <template #content>
            <dropdown-option @click="deleteTab(item)">{{t('action.close')}}</dropdown-option>
            <divider/>
            <dropdown-option @click="changeTabFixed(item)">{{ item.isPin ? t('action.unpin') : t('action.pin') }}</dropdown-option>
          </template>
        </dropdown>
      </div>
    </transition-group>
  </div>
</template>

<style scoped lang="scss">

.tabs-pages {
  margin-bottom: -1px;

  .tabs-pages__item {
    width: 200px;
    position: relative;
    max-width: 200px;
    height: 36px;
    margin-right: 8px;
    padding-bottom: 4px;
    border-radius: 8px 8px 0 0;
    //cursor: pointer;
  }

  .tabs-pages__item::after {
    content: "";
    position: absolute;
    right: -5px;
    height: 12px;
    width: 2px;
    background-color: var(--color-fill-3);
  }

  .tabs-pages__item-border-mask {
    position: absolute;
    top: 0;
    left: 5px;
    right: 5px;
    height: 24px;
    border-top: 1px solid var(--color-fill-4);
    pointer-events: none;
  }

  .tabs-pages__item-right-svg-mask, .tabs-pages__item-left-svg-mask {
    position: absolute;
    top: 0;
    right: -8px;
    width: 16px;
    height: 36px;
    color: var(--color-fill-4);
    fill: var(--color-bg-1);
    pointer-events: none;
  }

  .tabs-pages__item-left-svg-mask {
    right: unset;
    left: -8px;
    transform: rotateY(180deg);
  }

  .tabs-pages__item-mask {
    opacity: 0;
  }

  .tabs-pages__item-title {
    height: 32px;
    line-height: 32px;
    padding: 0 8px 0 16px;
    border-radius: 4px;
    user-select: none;
  }

  .tabs-pages__item-close {
    position: absolute;
    right: 8px;
    opacity: 0;
  }

  .tabs-pages__item-pin {
    position: absolute;
    right: 8px;
    color: rgb(var(--primary-6));

    &:hover {
      color: rgb(var(--primary-6));
    }
  }

  .tabs-pages__item.tabs-pages__item-pinned .tabs-pages__item-title {
    padding-right: 32px;
  }

  .tabs-pages__item:hover .tabs-pages__item-title {
    background-color: var(--color-fill-3);
    padding-right: 32px;
  }

  .tabs-pages__item:hover .tabs-pages__item-close,
  .tabs-pages__item.tabs-pages__item--active .tabs-pages__item-close,
  .tabs-pages__item.tabs-pages__item--active .tabs-pages__item-mask {
    opacity: 1;
  }

  .tabs-pages__item.tabs-pages__item--active .tabs-pages__item-title {
    background-color: var(--color-bg-1);
    padding-right: 32px;
  }

  .tabs-pages__item.tabs-pages__item--active {
    background-color: var(--color-bg-1);
  }

  .tabs-pages__item.tabs-pages__item--active::after,
  .tabs-pages__container:has(+ .tabs-pages__container .tabs-pages__item.tabs-pages__item--active) .tabs-pages__item::after {
    display: none;
  }
}

.tabs-pages__container {
  position: relative;
  max-width: 200px;
  //min-width: 20px;
}

.v-move,
.v-enter-active,
.v-leave-active {
  transition: opacity .16s ease-out, max-width .16s ease-out;
}

.tabs-pages .tabs-pages__container.v-enter-from,
.tabs-pages .tabs-pages__container.v-leave-to {
  opacity: 0;
  max-width: 0;
  margin-right: 0;
}
</style>
