import Enum from "@/enum/Enum.js";
import {enumGetByCode, enumGetByInfo, enumValues} from "@/enum/enumUtils.js";

export default class UserType{
    static ADMIN = new Enum('0', '管理员');
    static NORMAL = new Enum('1', '普通用户');
    static READ_ONLY = new Enum('2', '只读用户');
    static isAdmin(code){
        return this.ADMIN.getCode() === code
    }
    static values = enumValues
    static getByCode = enumGetByCode
    static getByInfo =enumGetByInfo
}
