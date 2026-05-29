<script setup>
defineOptions({name: 'TableColumn'})

defineProps({
  title: [String, Function],
  label: [String, Function],
  dataIndex: String,
  key: String,
  width: [String, Number],
  minWidth: [String, Number],
  align: String,
  format: Function,
})
</script>

<template>
  <!-- 仅用于声明列配置，不参与渲染 -->
</template>
