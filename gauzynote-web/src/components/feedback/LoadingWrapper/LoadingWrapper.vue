<script setup>
defineProps({
  loading: Boolean,
  inline: Boolean,
  maskTransparency: {
    type: Number,
    default: 0.1
  },
  text: String
})
</script>

<template>
<div class="loading-wrapper" :class="{'loading-wrapper-inline': inline}">
  <slot></slot>
  <transition name="fade-out">
    <div class="loading-mask" v-if="loading" :style="{'--loading-mask-transparency': maskTransparency}">
      <icon-loading size="20" spin/>
      <p v-if="text">{{ text }}</p>
    </div>
  </transition>
</div>
</template>

<style scoped lang="scss">
@import '@/assets/style/animation.css';
.loading-wrapper{
  position: relative;
  //width: 100%;
  //height: 100%;
  .loading-mask{
    --loading-mask-transparency: 0.1;
    position: absolute;
    top: 0;
    right: 0;
    bottom: 0;
    left: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-direction: column;
    z-index: 1;
    text-align: center;
    background-color: rgba(255, 255, 255, .6);
    user-select: none;
    border-radius: inherit;
    color: rgb(var(--primary-6));
  }
}
.loading-wrapper-inline{
  //width: max-content;
  //height: max-content;
  //width: unset;
  //height: unset;
  display: inline-block;
}
</style>