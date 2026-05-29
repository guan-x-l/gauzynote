<template>
  <transition
    @enter="onEnter"
    @after-enter="onAfterEnter"
    @before-leave="onBeforeLeave"
  >
    <slot/>
  </transition>
</template>

<script setup>
const props = defineProps({
  expanded: Boolean,
});

function onEnter(el) {
  const rectHeight = `${el.getBoundingClientRect().height}px`;
  el.style.height = props.expanded ? 0 : rectHeight;
  el.offsetHeight;
  el.style.height = props.expanded ? rectHeight : 0
}

function onAfterEnter(el) {
  el.style.height = props.expanded ? '' : '0';
}

// 在 leave 钩子之前调用
// 大多数时候，你应该只会用到 leave 钩子
function onBeforeLeave(el) {
  el.style.height = `${el.getBoundingClientRect().height}px`
  el.offsetHeight;
  el.style.height = 0;
}

</script>