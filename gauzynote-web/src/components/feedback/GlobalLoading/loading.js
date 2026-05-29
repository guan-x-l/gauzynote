import {useLoading} from "./useLoading.js";

const {addLoadingInstances, removeLoadingInstances, clearAllLoadingInstances} = useLoading();

export const MLoading = {
    show: (config={})=>{
        const id = addLoadingInstances(config);
        return {
            // 关闭当前加载实例
            close() {
                removeLoadingInstances(id);
            },
            id
        };
    },
    close: removeLoadingInstances,
    closeAll: clearAllLoadingInstances,
};
// 导出完整的加载服务
// export default MLoading



// 显示基本加载状态
// const showLoading = () => {
//     const load = MLoading.show({text: '加载中...'});
//     console.log(load)
//     // 模拟操作完成
//     setTimeout(() => {
//         load.close();
//     }, 2000);
// };
//
// // 显示带文字的加载状态
// const showLoadingWithText = () => {
//     const load = MLoading.show({ text: '处理中，请稍候...' });
//
//     // 模拟操作完成
//     setTimeout(() => {
//         load.close();
//     }, 3000);
// };
//
// // 清空所有加载状态
// const clearAllLoadings = () => {
//     MLoading.closeAll();
// };
//
// // 模拟多次调用场景
// const simulateMultipleCalls = () => {
//     // 第一次调用
//     const load1 = MLoading.show({ text: '第一次调用' });
//     let load2 = null
//     let load3 = null
//     // 1秒后第二次调用
//     setTimeout(() => {
//         load2 = MLoading.show({ text: '第二次调用' });
//     }, 1000);
//
//     // 2秒后第三次调用
//     setTimeout(() => {
//         load3 = MLoading.show({ text: '第三次调用' });
//     }, 2000);
//
//     // 3秒后第一次隐藏
//     setTimeout(() => {
//         load1.close();
//     }, 3000);
//
//     // 4秒后第二次隐藏
//     setTimeout(() => {
//         load2.close();
//     }, 4000);
//
//     // 5秒后第三次隐藏
//     setTimeout(() => {
//         load3.close();
//     }, 5000);
// };