import {$t} from "@/locales/index.js";

export function getMessageByCode(code, msg) {
    if (msg) return msg;
    const errorKey = `error.${code}`;
    const translatedMsg = $t(errorKey);
    return translatedMsg === errorKey ? $t('error.unknown') : translatedMsg;
}
