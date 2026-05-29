import {isArray} from "@/utils/index.js";

/**
 * Quoted from vue-next
 * https://github.com/vuejs/vue-next/blob/master/packages/shared/src/shapeFlags.ts
 */
export const ShapeFlags = Object.freeze({
    ELEMENT: 1,
    FUNCTIONAL_COMPONENT: 1 << 1,
    STATEFUL_COMPONENT: 1 << 2,
    COMPONENT: 1 << 2 | 1 << 1,
    TEXT_CHILDREN: 1 << 3,
    ARRAY_CHILDREN: 1 << 4,
    SLOTS_CHILDREN: 1 << 5,
    TELEPORT: 1 << 6,
    SUSPENSE: 1 << 7,
    COMPONENT_SHOULD_KEEP_ALIVE: 1 << 8,
    COMPONENT_KEPT_ALIVE: 1 << 9,
})
/**
 * @param {*} value
 * @returns {boolean} - value is import('vue').ComponentPublicInstance
 */
export function isComponentInstance (value) {
    return value?.$ !== undefined;
}
/**
 * @param {import('vue').VNode} vn
 */
export const isElement = (vn) => {
    return Boolean(vn && vn.shapeFlag & ShapeFlags.ELEMENT);
}
/**
 * @param {import('vue').VNode} vn
 * @param {import('vue').VNodeTypes} [type]
 * @returns {boolean} - type is Component
 */
export const isComponent = (vn, type) => {
    return Boolean(vn && vn.shapeFlag & ShapeFlags.COMPONENT);
}
/**
 * @param {import('vue').VNode} vn
 * @param {import('vue').VNode['children']} children
 * @returns {boolean} - children is string
 */
export const isText = (vn, children) => {
    return Boolean(vn && vn.shapeFlag & ShapeFlags.TEXT_CHILDREN);
}
/**
 * @param {import('vue').VNode} child
 * @param {string} name
 * @returns {boolean} - children is string
 */
export const isNamedComponent = (child, name) => {
    return isComponent(child, child.type) && child.type.name === name;
}
/**
 * @param {import('vue').VNode} child
 * @param {import('vue').VNode['children']} children
 * @returns {boolean} - children is string
 */
export const isTextChildren = (child, children) => {
    return Boolean(child && child.shapeFlag & 8);
}
/**
 * @param {import('vue').VNode} vn
 * @param {import('vue').VNode['children']} children
 * @returns {boolean} - children is VNode[]
 */
export const isArrayChildren = (vn, children) => {
    return Boolean(vn && vn.shapeFlag & ShapeFlags.ARRAY_CHILDREN);
}
/**
 * @param {import('vue').VNode} vn
 * @param {import('vue').VNode['children']} children
 * @returns {boolean} - children is Slots
 */
export const isSlotsChildren = (vn) => {
    return Boolean(vn && vn.shapeFlag & ShapeFlags.SLOTS_CHILDREN);
}
/**
 *
 * @param {import('vue').VNode[]} children
 * @returns {import('vue').VNode | undefined}
 */
export function getFirstComponent (children){
    if (!children) {
        return undefined;
    }

    for (const child of children) {
        if (isElement(child) || isComponent(child)) {
            return child;
        }
        // If the current node is not a component, continue to find subcomponents
        if (isArrayChildren(child, child.children)) {
            const result = getFirstComponent(child.children);
            if (result) return result;
        } else if (isSlotsChildren(child, child.children)) {
            const children = child.children.default?.();
            if (children) {
                const result = getFirstComponent(children);
                if (result) return result;
            }
        } else if (isArray(child)) {
            const result = getFirstComponent(child);
            if (result) return result;
        }
    }

    return undefined;
}