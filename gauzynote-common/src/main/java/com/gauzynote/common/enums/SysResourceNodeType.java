package com.gauzynote.common.enums;

public enum SysResourceNodeType {
    FOLDER("1", "文件夹"),
    NOTE("2", "markdown"),
    FILE("3", "文件");

    private final String code;
    private final String description;

    SysResourceNodeType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    // 根据code获取枚举
    public static SysResourceNodeType getByCode(String code) {
        for (SysResourceNodeType value : SysResourceNodeType.values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知的节点类型: " + code);
    }
}
