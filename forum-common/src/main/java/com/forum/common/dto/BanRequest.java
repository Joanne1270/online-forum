package com.forum.common.dto;

import lombok.Data;

@Data
public class BanRequest {

    /** 封禁天数；-1 或 null 表示永久封禁 */
    private Integer banDays;
}
