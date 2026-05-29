<script>
import {h, ref, nextTick, cloneVNode, defineExpose, useTemplateRef, provide, onMounted} from 'vue'
import { Teleport, Transition } from 'vue'
import { useEventListener } from "@/hooks/index.js";

export default {
  props: {
    /**
     * 触发方式
     * @type {import('vue').PropType.<'hover' | 'click' | 'focus' | 'contextmenu'>}
     * @default click
     */
    trigger: {
      type: String,
      default: 'click'
    },
    position: {
      type: String,
    },
    /**
     * 弹出框的挂载容器
     * @type {import('vue').PropType.<String | HTMLElement>}
     */
    popupContainer: {
      type: [String, HTMLElement],
      default: 'body'
    }
  },
  emits: ['select', 'show', 'hide'],
  setup(props, { emit, slots, attrs }) {
    const open = ref(false)
    const x = ref(0)
    const y = ref(0)
    const menuRef = ref(null)
    const triggerRef = ref(null)
    
    const contextmenuEv = useEventListener(document, 'contextmenu', handleGlobalEvent)
    const clickEv = useEventListener(document, 'click', handleGlobalEvent)
    
    onMounted(()=>{
      contextmenuEv.stop()
      clickEv.stop()
    })
    
  
    
    // 显示菜单处理函数
    const showMenu = (event) => {
      if (open.value) {
        // 看需求可以关闭或者重新展示
        if (props.trigger === 'contextmenu'){
          event.preventDefault()
        }
        hideMenu()
        return
      }
      if (props.trigger !== event.type) return
      emit('show')
      contextmenuEv.restart()
      clickEv.restart()
      
      if (event.type === 'contextmenu') {
        handleContextmenu(event)
      } else if (event.type === 'click') {
        handleClick(event)
      }
    }
    
    // 右键菜单处理
    function handleContextmenu(event) {
      event.preventDefault()
      x.value = event.clientX
      y.value = event.clientY
      open.value = true
      
      nextTick(() => adjustPosition(event))
    }
    
    // 点击处理
    function handleClick(event) {
      x.value = event.clientX
      y.value = event.clientY
      open.value = true
      
      nextTick(() => adjustPosition(event))
    }
    
    // 调整位置避免溢出
    const adjustPosition = (event) => {
      if (!menuRef.value) return
      
      let {right, left, bottom, width, height} = menuRef.value.getBoundingClientRect()
      const viewportWidth = window.innerWidth
      const viewportHeight = window.innerHeight
      if (props.position === 'bl'){
        x.value = left - width
        right -= width
      }
      // 右侧溢出检测
      if (right > viewportWidth) {
        x.value = Math.max(0, event.clientX - width)
      }
      // 底部溢出检测
      if (bottom > viewportHeight) {
        y.value = Math.max(0, event.clientY - height)
      }
    }
    
    // 隐藏菜单
    const hideMenu = () => {
      if (!open.value) return
      emit('hide')
      open.value = false
      contextmenuEv.stop()
      clickEv.stop()
    }
    
    // 菜单项点击处理
    const handleMenuItemClick = (item) => {
      emit('select', item)
      hideMenu()
    }
    
    function handleGlobalEvent(event) {
      // 如果菜单没打开，不处理
      if (!open.value) return
      // 判断点击是否在触发元素内部
      const isClickInTrigger = triggerRef.value?.contains(event.target)
      // 判断点击是否在菜单内部
      const isClickInMenu = menuRef.value?.contains(event.target)
      // 点击外部才关闭
      if (!isClickInTrigger && !isClickInMenu) {
        hideMenu()
      }
    }
    
    
    
    
    provide('mDropdown', {
      onOptionClick: handleMenuItemClick,
    })
    // console.log(attrs)
    // 渲染函数
    return () =>{
      const children = slots.default?.() ?? [];
      return children.map(child => {
        if (!child) return null;
        return [cloneVNode(child, {
          // ref: (el) => { triggerRef.value = el; },
          // ref: triggerRef,
          // 对于组件，使用 onVnodeMounted 来获取实例
          onVnodeMounted: (vnode) => {
            triggerRef.value = vnode.el
          },
          onContextmenu: showMenu,
          onClick:showMenu
        }),
          //  teleport组件用于将菜单渲染到指定容器
          h(Teleport, {
            defer: true,
            to: props.popupContainer
          }, [
            // h(Transition, {name: 'fade-in-out'}, {
            //   default: () => open.value ? h('div', {
              open.value ? h('div', {
                class: 'm-dropdown',
                style: {
                  left: `${x.value}px`,
                  top: `${y.value}px`
                },
                ref: menuRef
              }, [
                h('ul', { class: 'm-dropdown-menu' },
                  slots?.content?.() || h('li', {class: 'm-dropdown-option'}, '-')
                )
              ]) : null
            // })
          ])
        ]
      })
    }
  }
}
</script>
<style scoped lang="scss">
@import '@/assets/style/animation.css';
.m-dropdown {
  position: absolute;
  z-index: 1009;
}

.m-dropdown-menu {
  width: max-content;
  height: max-content;
  margin: 0;
  padding: 4px;
  border-radius: 8px;

  background-color: var(--color-bg-2);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  border: 1px solid var(--color-border-2);
}
</style>