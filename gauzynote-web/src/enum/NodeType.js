import Enum from "@/enum/Enum.js";
import {enumGetByCode, enumGetByInfo, enumValues} from "@/enum/enumUtils.js";

export default class NodeType{
    static FOLDER = new Enum('1', 'folder');
    static NOTE = new Enum('2', 'note');
    static FILE = new Enum('3', 'file');


    static isFolder(code){
        return this.FOLDER.getCode() === code
    }
    static isNote(code){
        return this.NOTE.getCode() === code
    }
    static isFile(code){
        return this.FILE.getCode() === code
    }

    static values = enumValues
    static getByCode = enumGetByCode
    /**
     * @returns {Enum}
     */
    static getByInfo =enumGetByInfo
}
