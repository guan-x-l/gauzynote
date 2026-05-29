import { h, render, reactive } from 'vue'
import MessageComponent from './Message.vue'
import AppContext from "@/context.js";

let messageInstance = null
// let appContext = null
/*Index.config({
    appContext: {v:1}
})*/
// 消息队列
const messageQueue = reactive([])

// 创建消息实例
const createMessage = () => {
    if (!messageInstance) {
        const container = document.createElement('div')
        container.className = 'message-container'
        document.body.appendChild(container)

        const vnode = h(MessageComponent, {
            messages: messageQueue,
            position: 'top-center',
        })
        vnode.appContext = AppContext.appContext
        render(vnode, container)
        messageInstance = vnode.component
    }
}

// 基础消息方法
const showMessage = (content, type = 'info', options = {}) => {
    createMessage()

    const {duration=3000, closable=false, position = 'top-center'} = options

    const message = {
        id: `message_${Date.now()}`,
        content,
        type,
        ...options,
        duration,
        position,
        closable: duration === 0 || closable,
        onClose: () => {
            const index = messageQueue.findIndex(item => item.id === message.id)
            if (index > -1) {
                messageQueue.splice(index, 1)
            }
            options.onClose?.()
        }
    }

    messageQueue.push(message)

    // 如果设置了显示时间，到期后自动关闭
    if (message.duration !== 0) {
        setTimeout(() => {
            message.onClose()
        }, message.duration)
    }

    return {
        close: message.onClose
    }
}

/**
 * MessageOptions
 * @typedef {Object} MessageOptions
 * @property {number} [duration]
 * @property {boolean} [closable]
 * @property {string} [position]
 */

// 四种消息类型的快捷方法
/**
 * @param {string} content
 * @param {MessageOptions} options
 */
const success = (content, options = {}) => showMessage(content, 'success', options)
/**
 * @param {string} content
 * @param {MessageOptions} options
 */
const error = (content, options = {}) => showMessage(content, 'error', options)
/**
 * @param {string} content
 * @param {MessageOptions} options
 */
const warning = (content, options = {}) => showMessage(content, 'warning', options)
/**
 * @param {string} content
 * @param {MessageOptions} options
 */
const info = (content, options = {}) => showMessage(content, 'info', options)

// 直接导入使用方式
export const Message = {
    show: showMessage,
    success,
    error,
    warning,
    info,
    /*config: (options) => {
        AppContext = options.appContext || AppContext.appContext
    }*/
}

// export default Message
