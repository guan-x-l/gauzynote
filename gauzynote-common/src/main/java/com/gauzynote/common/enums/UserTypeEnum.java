package com.gauzynote.common.enums;

public enum UserTypeEnum {
    ADMIN("0", "管理员"),
    NORMAL("1", "普通用户"),
    READ_ONLY("2", "只读用户");

    private final String code;
    private final String description;

    UserTypeEnum(String code, String description) {
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
    public static UserTypeEnum getByCode(String code) {
        for (UserTypeEnum value : UserTypeEnum.values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知的用户类型: " + code);
    }
}
