<template>
  <div
    class="message-wrapper"
    :class="`message-wrapper--${position}`"
  >
    <TransitionGroup class="message__list" name="message-fade" tag="ul" :aria-label="t('text.textList', [t('message.title'), t('text.list')])">
      <li
        v-for="message in messages"
        :key="message.id"
        class="message"
        :class="[
          `message--${message.type}`,
        ]"
        :aria-label="t('message.message',{type: t(`message.${message.type}`)})"
      >
        <span class="message__icon">
          <component :is="iconMap[message.type]" />
        </span>
        <span class="message__content">
          <slot :message="message">{{ message.content }}</slot>
        </span>
        <m-button v-if="message.closable" class="message__close flex-shrink-0" type="base" shape="circle" size="mini" @click="message.onClose">
          <template #icon>
            <icon-close size="14"/>
          </template>
        </m-button>
      </li>
    </TransitionGroup>
  </div>
</template>

<script setup>
import {useI18n} from "vue-i18n";

const props = defineProps({
  messages: {
    type: Array,
    required: true
  },
  appContext: {
    type: Object,
    default: null
  },
  position: {
    type: String,
    default: 'top-center'
  }
})
const {t} = useI18n()
const iconMap = {
  success: 'IconCheckCircle',
  error: 'IconXCircle',
  warning: 'IconAlertCircle',
  info: 'IconInfo'
}
/*
const iconMap = {
  success: IconCheckCircle,
  error: IconXCircle,
  warning: IconAlertCircle,
  info: IconInfo
}*/
</script>

<style scoped>
.message-wrapper {
  position: fixed;
  width: 100%;
  z-index: 1010;
  pointer-events: none;
}

.message-wrapper--top-center {
  top: 48px;
}

.message-wrapper--bottom-center {
  bottom: 48px;
}

.message__list{
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-block-start: 0;
  margin-block-end: 0;
  padding-inline-start: 0;
  pointer-events: none;
}

.message {
  display: flex;
  align-items: center;
  width: max-content;
  max-width: 62%;
  padding: 8px 16px;
  margin-bottom: 16px;
  border-radius: 4px;
  word-break: break-all;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  background-color: var(--color-bg-2);
  pointer-events: all;
  overflow: hidden;
}

.message__content {
  color: var(--color-text-1);
}

.message__icon {
  margin-right: 8px;
}

.message__close {
  margin-left: 8px;
  border-radius: 50%;
  padding: 4px;
/*  cursor: pointer;*/
}

.message--success .message__icon {
  color: rgb(var(--success-6));
}

.message--error .message__icon {
  color: rgb(var(--danger-6));
}

.message--warning .message__icon {
  color: rgb(var(--warning-6));
}

.message--info .message__icon {
  color: rgb(var(--primary-6));
}

.message-fade-move,
.message-fade-enter-active,
.message-fade-leave-active {
  transition: all .3s ease-out;
}

.message-fade-enter-from,
.message-fade-leave-to
{
  opacity: 0;
  transform: translateY(-100%);
}


.message-fade-leave-active {
  position: absolute;
}
</style>
