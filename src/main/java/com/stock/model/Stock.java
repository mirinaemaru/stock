package com.stock.model;

import lombok.Data;

@Data
public class Stock {
    private String code;      // 주식 코드
    private String name;      // 주식 이름
    private String gubun;     // 구분 (KOSPI, KOSDAQ 등)
    private String useYn;     // 사용 여부 (Y/N)
}

