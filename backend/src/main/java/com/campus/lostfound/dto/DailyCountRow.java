package com.campus.lostfound.dto;

import lombok.Data;

import java.time.LocalDate;

/** 近 N 天每日发布量。 */
@Data
public class DailyCountRow {

    private LocalDate day;
    private Long total;
}
