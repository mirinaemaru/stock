-- 기존 PRIMARY KEY 제거
ALTER TABLE krx_stock_price_info DROP PRIMARY KEY;

-- mrktCtg를 NOT NULL로 변경 (PRIMARY KEY에 포함되므로 필수)
ALTER TABLE krx_stock_price_info MODIFY mrktCtg VARCHAR(40) NOT NULL COMMENT '시장구분 (KOSPI/KOSDAQ/KONEX)';

-- 새로운 PRIMARY KEY 추가 (basDt, srtnCd, mrktCtg)
ALTER TABLE krx_stock_price_info ADD PRIMARY KEY (basDt, srtnCd, mrktCtg);

-- 테이블명 변경
ALTER TABLE krx_stock_price_info RENAME TO stock_price_info;

