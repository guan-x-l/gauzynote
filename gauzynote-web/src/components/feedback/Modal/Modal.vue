<script setup>
import {watch} from "vue";
import {useI18n} from "vue-i18n";
const {t} = useI18n()
const props = defineProps({
  show: Boolean,
  title: {
    type: String,
    default: ''
  },
  content: {
    type: [String, null],
    default: ''
  },
  maskClosable:{
    type: Boolean,
    default: true
  },
  loading: {
    type: Boolean,
    default: false
  },
  width: {
    type: String,
    default: '500px'
  },
  okText: {
    type: String,
    default: ''
  },
  cancelText: {
    type: String,
    default: ''
  },
  hideCancel: {
    type: Boolean,
    default: false
  },
  hideCancelIcon: {
    type: Boolean,
    default: false
  },
  header: {
    type: Boolean,
    default: true
  },
  footer: {
    type: Boolean,
    default: true
  },
  bodyStyle: {
    type: Object,
  }
})
const emit = defineEmits(['ok', 'cancel', 'close', 'open'])

const resetOverflow = () => {
  document.body.style.overflow = ''
  document.body.style.width = ''
};
const setOverflowHidden = () => {
  if (document.body?.clientWidth) {
    document.body.style.width = document.body.clientWidth + 'px'
  }
  document.body.style.overflow = 'hidden'
};

function handleClose() {
  setTimeout(()=>{
    resetOverflow();
  }, 0.25 * 1000 / 2)
  emit('close')
}

function maskEvent() {
  if (props.maskClosable && !props.loading) {
    emit('cancel')
    handleClose()
  }
}
function onOk() {
  if (!props.loading) {
    emit('ok')
    handleClose()
  }
}
function cancel() {
  if (!props.loading) {
    emit('cancel')
    handleClose()
  }
}

watch(()=>props.show,(value)=>{
  if (value) {
    emit('open')
    setOverflowHidden();
  }
})
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div class="modal-container" v-if="show">
        <div class="modal-mask theme-transition"></div>
        <div class="modal-wrapper" @click="maskEvent">
          <loading-wrapper :loading="loading" :style="{width: width}"  inline class="m-modal box-shadow-intact-m theme-transition" @click.stop>
            <m-button type="base" size="medium" shape="circle" v-if="!hideCancelIcon" aria-label="Close" class="modal-header-icon-close-btn" @click="cancel">
              <template #icon>
                <icon-close/>
              </template>
            </m-button>
            <div class="modal-header theme-transition" v-if="header">
              <slot name="header">
                <h4>{{title}}</h4>
              </slot>
            </div>
            <div class="modal-body" :style="bodyStyle">
              <slot>
                <p>{{content}}</p>
              </slot>
            </div>
            <div class="modal-footer" v-if="footer">
              <slot name="footer">
                <m-button type="base" style="margin-right: 8px" @click="cancel" v-if="!hideCancel">{{cancelText || t('cancelText')}}</m-button>
                <m-button type="primary" @click="onOk">{{okText || t('okText')}}</m-button>
              </slot>
            </div>
          </loading-wrapper>
        </div>
      </div>
  </Transition>
  </Teleport>
</template>

<style scoped lang="scss">

.modal-container{
  position: fixed;
  inset: 0;
  white-space: nowrap;
  z-index: 1001;
  transition: opacity 150ms ease;
}
.modal-mask{
  position: absolute;
  inset: 0;
  background-color: rgba(29, 33, 41, .6);
}
.modal-wrapper {
  position: absolute;
  inset: 0;
  padding: 5vh 0;
  white-space: nowrap;
  text-align: center;
  overflow: auto;
  transition: transform 250ms cubic-bezier(0.21, 1.4, 0.35, 1.1);
  &::after{
    display: inline-block;
    width: 0;
    height: 100%;
    vertical-align: middle;
    content: "";
  }
}

.m-modal {
  position: relative;
  display: inline-block;
  margin: 0 auto;
  background-color: var(--color-bg-1);
  border-radius: var(--border-radius-medium);
  //transition: var(--transition-theme);
  text-align: left;
  vertical-align: middle;
  overflow: hidden;
}
.m-modal .modal-header-icon-close-btn {
  position: absolute;
  top: var(--spacing-4);
  right: var(--spacing-8);
}

.modal-header {
  //position: relative;
  color: var(--color-text-1);
  padding: var(--spacing-12) var(--spacing-12);
}
.modal-header h4{
  margin: 0;
}


.modal-body {
  padding: 0 var(--spacing-12) var(--spacing-12) var(--spacing-12);
  color: var(--color-text-1);
}


.modal-footer {
  display: flex;
  justify-content: flex-end;
  padding: var(--spacing-8) var(--spacing-12);
  color: var(--color-text-1);
}


.modal-enter-from {
  opacity: 0;
}

.modal-leave-from .modal-wrapper, .modal-leave-active .modal-wrapper, .modal-leave-to .modal-wrapper {
  transition: transform 250ms ease-out;
}
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .modal-wrapper,
.modal-leave-to .modal-wrapper {
  transform: scale(0.6);
}
</style>