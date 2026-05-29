<template>
  <component :is="AsyncIconComponent"/>
</template>

<script setup>
import {defineAsyncComponent, resolveComponent} from "vue";
import {capitalize, isObject} from "@/utils/index.js";

const props = defineProps({
  icon: {
    type: String,
    required: true
  }
});

const modules = import.meta.glob("./**.vue")

const name = props.icon.startsWith('Icon') ? props.icon : `Icon${capitalize(props.icon)}`

const AsyncIconComponent = isObject(resolveComponent(name)) ? resolveComponent(name) :
  modules[`./${props.icon}.vue`] ? defineAsyncComponent(modules[`./${props.icon}.vue`]) : () => "⊠"

</script>