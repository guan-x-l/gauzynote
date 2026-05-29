package com.gauzynote.common.domain.model;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
public class PasswordUpdateBody {

    @NotBlank(message = "{password.current.not.blank}")
    private String currentPassword;

    @NotBlank(message = "{password.new.not.blank}")
    private String newPassword;

    @NotBlank(message = "{password.confirm.not.blank}")
    private String confirmPassword;
}
