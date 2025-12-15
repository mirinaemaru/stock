CREATE TABLE krx_stock_price_info (
    basDt DATE NOT NULL COMMENT '기준 일자 (YYYY-MM-DD)',
    srtnCd VARCHAR(9) NOT NULL COMMENT '단축코드 (종목코드)',
    isinCd VARCHAR(12) COMMENT 'ISIN코드',
    itmsNm VARCHAR(120) COMMENT '종목명',
    mrktCtg VARCHAR(40) COMMENT '시장구분 (KOSPI/KOSDAQ/KONEX)',
    clpr BIGINT COMMENT '종가',
    vs INT COMMENT '대비 (전일 대비 등락)',
    fltRt DECIMAL(11, 2) COMMENT '등락률 (전일 대비 등락 비율)',
    mkp BIGINT COMMENT '시가',
    hipr BIGINT COMMENT '고가',
    lopr BIGINT COMMENT '저가',
    trqu BIGINT COMMENT '거래량 (체결수량의 누적 합계)',
    trPrc BIGINT COMMENT '거래대금 (체결가격 * 체결수량의 누적 합계)',
    lstgStCnt BIGINT COMMENT '상장주식수',
    mrktTotAmt BIGINT COMMENT '시가총액 (종가 * 상장주식수)',
    PRIMARY KEY (basDt, srtnCd)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

