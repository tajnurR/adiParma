package com.adipharma.common.enums;

import java.util.Arrays;

public enum PaymentMethod {
    CASH(1, "Cash"),
    CARD(2, "Card"),
    MOBILE(3, "Mobile"),
    OTHER(4, "Other");

    private final int code;
    private final String label;

    PaymentMethod(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static PaymentMethod fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
            .filter(method -> method.code == code)
            .findFirst()
            .orElse(null);
    }
}
