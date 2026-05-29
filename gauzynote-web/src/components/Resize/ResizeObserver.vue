<script>
import {
  defineComponent,
  onMounted,
  onUnmounted,
  ref,
  computed,
  cloneVNode,
  watch,
} from 'vue';
import ResizeObserver from 'resize-observer-polyfill';
import { getFirstComponent, isComponentInstance } from '@/utils/index.js';

// https://arco.design/vue
export default defineComponent({
  name: 'ResizeObserver',
  emits: [
    /**
     * resize 事件
     * @property {ResizeObserverEntry} entry 触发 resize 的 dom 元素
     */
    'resize',
  ],
  setup(props, { emit, slots }) {
    /**
     * @type {ResizeObserver | null}
     */
    let resizeObserver;

    /**
     *
     * @type {import('vue').Ref<HTMLElement | import('vue').ComponentPublicInstance>}
     */
    const componentRef = ref();

    /**
     * @type {import('vue').Component<HTMLElement>}
     */
    const element = computed(() =>
            isComponentInstance(componentRef.value)
                ? componentRef.value.$el
                : componentRef.value
    );

    /**
     *
     * @param {HTMLElement} target
     */
    const createResizeObserver = (target) => {
      if (!target) return;
      resizeObserver = new ResizeObserver((entries) => {
        const entry = entries[0];
        emit('resize', entry);
      });
      resizeObserver.observe(target);
    };

    const destroyResizeObserver = () => {
      if (resizeObserver) {
        resizeObserver.disconnect();
        resizeObserver = null;
      }
    };

    watch(element, (_element) => {
      if (resizeObserver) destroyResizeObserver();
      if (_element) createResizeObserver(_element);
    });

    onMounted(() => {
      if (element.value) {
        createResizeObserver(element.value);
      }
    });

    onUnmounted(() => {
      destroyResizeObserver();
    });

    return () => {
      const firstChild = getFirstComponent(slots.default?.() ?? []);

      if (firstChild) {
        return cloneVNode(
            firstChild,
            {
              ref: componentRef,
            },
            true
        );
      }

      return null;
    };
  },
});
</script>