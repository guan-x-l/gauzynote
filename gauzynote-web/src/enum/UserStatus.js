import Enum from "@/enum/Enum.js";
import {enumGetByCode, enumGetByInfo, enumValues} from "@/enum/enumUtils.js";

export default class UserStatus{
    static NORMAL = new Enum('0', '正常');
    static DISABLED = new Enum('1', '停用');
    static DELETED = new Enum('2', '删除');

    static isNormal(code){
        return this.NORMAL.getCode() === code
    }
    static isDisabled(code){
        return this.DISABLED.getCode() === code
    }

    static isDeleted(code){
        return this.DELETED.getCode() === code
    }

    static values = enumValues
    static getByCode = enumGetByCode
    static getByInfo =enumGetByInfo
}
