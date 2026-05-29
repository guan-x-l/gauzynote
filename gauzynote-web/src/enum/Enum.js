
export default class Enum {
    #code
    #info
    #sortIndex = 0
    constructor(code, info, sortIndex) {
        this.#code = code;
        this.#info = info;
        this.#sortIndex = sortIndex;
    }

    getCode() {
        return this.#code;
    }

    getInfo() {
        return this.#info;
    }
    getSortIndex() {
        return this.#sortIndex;
    }

    // 返回自身属性
    get props() {
        return { code: this.getCode(), info: this.getInfo() , sortIndex: this.getSortIndex() };
    }

    // 转为普通对象
    toObject() {
        return this.props
    }

    // 重写toJSON
    toJSON() {
        return this.toObject();
    }
}
