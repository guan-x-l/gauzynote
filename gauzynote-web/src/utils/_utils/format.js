import dayjs from "../lib/dayjs.js";


/**
 * @param date
 * @param {string} template
 * @returns {string}
 */
export function dateFormat(date, template = 'YYYY/MM/DD'){
    return dayjs(date).format(template)
}
export function formatBytes(bytes, decimals = 2) {
    if (bytes === 0) return '0 Bytes';

    const k = 1024;
    const dm = decimals < 0 ? 0 : decimals;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];

    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
}
