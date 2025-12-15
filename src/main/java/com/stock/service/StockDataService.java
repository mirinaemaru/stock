package com.stock.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class StockDataService {

    private static final Logger logger = LoggerFactory.getLogger(StockDataService.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${public.data.api.key}")
    private String publicDataApiKey;

    public StockDataService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 주식 데이터를 가져옵니다
     * KOSPI/KOSDAQ의 경우 공공데이터포털 API 사용
     * 해외 주식의 경우 Yahoo Finance API 사용
     */
    public Map<String, Object> getStockData(String stockCode, String exchange) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if ("KRX".equals(exchange)) {
                // 한국 주식의 경우 공공데이터포털 API 사용
                result = fetchFromPublicData(stockCode);
            } else {
                // 해외 주식의 경우 Yahoo Finance API 사용
                String symbol = exchange + ":" + stockCode;
                result = fetchFromYahooFinance(symbol);
            }
        } catch (Exception e) {
            logger.error("주식 데이터 조회 중 오류 발생: {}", e.getMessage(), e);
            result.put("error", "데이터를 가져오는 중 오류가 발생했습니다: " + e.getMessage());
            result.put("success", false);
        }
        
        return result;
    }

    /**
     * Yahoo Finance API를 통해 주식 데이터 가져오기
     * 참고: 이는 비공식 API이며, 사용 제한이 있을 수 있습니다.
     */
    private Map<String, Object> fetchFromYahooFinance(String symbol) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Yahoo Finance API (비공식, 제한적)
            // 실제로는 더 안정적인 API를 사용하는 것이 좋습니다.
            String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + symbol + "?interval=1d&range=1mo";
            
            logger.info("Yahoo Finance API 호출: {}", url);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                JsonNode chart = jsonNode.path("chart");
                JsonNode resultNode = chart.path("result");
                
                if (resultNode.isArray() && resultNode.size() > 0) {
                    JsonNode data = resultNode.get(0);
                    JsonNode meta = data.path("meta");
                    JsonNode timestamps = data.path("timestamp");
                    JsonNode indicators = data.path("indicators");
                    JsonNode quote = indicators.path("quote").get(0);
                    
                    // 메타 정보
                    result.put("symbol", meta.path("symbol").asText());
                    result.put("currency", meta.path("currency").asText());
                    result.put("exchangeName", meta.path("exchangeName").asText());
                    result.put("regularMarketPrice", meta.path("regularMarketPrice").asDouble());
                    result.put("previousClose", meta.path("previousClose").asDouble());
                    
                    // 차트 데이터
                    List<Map<String, Object>> chartData = new ArrayList<>();
                    if (timestamps.isArray() && quote.path("close").isArray()) {
                        JsonNode closes = quote.path("close");
                        JsonNode opens = quote.path("open");
                        JsonNode highs = quote.path("high");
                        JsonNode lows = quote.path("low");
                        JsonNode volumes = quote.path("volume");
                        
                        for (int i = 0; i < timestamps.size(); i++) {
                            Map<String, Object> candle = new HashMap<>();
                            long timestamp = timestamps.get(i).asLong();
                            candle.put("time", timestamp * 1000); // JavaScript timestamp (milliseconds)
                            candle.put("open", opens.get(i).asDouble());
                            candle.put("high", highs.get(i).asDouble());
                            candle.put("low", lows.get(i).asDouble());
                            candle.put("close", closes.get(i).asDouble());
                            candle.put("volume", volumes.get(i).asLong());
                            chartData.add(candle);
                        }
                    }
                    
                    result.put("chartData", chartData);
                    result.put("success", true);
                } else {
                    result.put("error", "데이터를 찾을 수 없습니다.");
                    result.put("success", false);
                }
            } else {
                result.put("error", "API 호출 실패");
                result.put("success", false);
            }
        } catch (Exception e) {
            logger.error("Yahoo Finance API 호출 중 오류: {}", e.getMessage(), e);
            result.put("error", "데이터를 가져오는 중 오류가 발생했습니다: " + e.getMessage());
            result.put("success", false);
        }
        
        return result;
    }

    /**
     * 공공데이터포털 한국거래소 주식시세정보 API를 통해 데이터 가져오기
     * 참고: https://www.data.go.kr/data/15094808/openapi.do
     */
    private Map<String, Object> fetchFromPublicData(String stockCode) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 공공데이터포털 주식시세정보 API
            // 종목코드로 조회 (6자리 숫자)
            // 참고: https://www.data.go.kr/data/15094808/openapi.do
            // serviceKey는 URL 인코딩 필요 (공공데이터포털 API 요구사항)
            String encodedApiKey = URLEncoder.encode(publicDataApiKey, StandardCharsets.UTF_8.toString());
            // 파라미터: srtnCd (정확히 일치) 또는 likeSrtnCd (부분 일치)
            // 먼저 srtnCd로 시도, 없으면 likeSrtnCd로 시도
            // HTTPS 사용 (공공데이터포털 API는 HTTPS 권장)
            String url = "https://apis.data.go.kr/1160100/service/GetStockSecuritiesInfoService/getStockPriceInfo" +
                        "?serviceKey=" + encodedApiKey +
                        "&numOfRows=100" +
                        "&pageNo=1" +
                        "&resultType=json" +
                        "&srtnCd=" + stockCode;
            
            logger.info("공공데이터포털 API 호출: 종목코드={}, URL={}", stockCode, url.replace(encodedApiKey, "***"));
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            String responseBody = response.getBody();
            logger.info("API 응답 상태: {}, 본문 길이: {}", response.getStatusCode(), 
                        responseBody != null ? responseBody.length() : 0);
            
            // 전체 응답 데이터 로깅 (가독성 있게 포맷팅)
            JsonNode jsonNode = null;
            if (responseBody != null) {
                try {
                    // JSON을 파싱한 후 예쁘게 포맷팅
                    jsonNode = objectMapper.readTree(responseBody);
                    String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
                    
                    logger.info("=== 공공데이터포털 API 응답 전체 데이터 ===");
                    // 큰 로그를 여러 줄로 나누어 출력 (각 줄당 최대 1000자)
                    int maxLineLength = 1000;
                    if (prettyJson.length() > maxLineLength) {
                        String[] lines = prettyJson.split("\n");
                        for (String line : lines) {
                            if (line.length() > maxLineLength) {
                                // 한 줄이 너무 길면 여러 줄로 분할
                                for (int i = 0; i < line.length(); i += maxLineLength) {
                                    int end = Math.min(i + maxLineLength, line.length());
                                    logger.info("{}", line.substring(i, end));
                                }
                            } else {
                                logger.info("{}", line);
                            }
                        }
                    } else {
                        logger.info("응답 본문:\n{}", prettyJson);
                    }
                    logger.info("=== 응답 데이터 끝 ===");
                } catch (Exception e) {
                    // JSON 파싱 실패 시 원본 출력
                    logger.warn("JSON 포맷팅 실패, 원본 출력: {}", e.getMessage());
                    logger.info("=== 공공데이터포털 API 응답 전체 데이터 (원본) ===");
                    // 원본도 여러 줄로 나누어 출력
                    int maxLineLength = 1000;
                    if (responseBody.length() > maxLineLength) {
                        for (int i = 0; i < responseBody.length(); i += maxLineLength) {
                            int end = Math.min(i + maxLineLength, responseBody.length());
                            logger.info("{}", responseBody.substring(i, end));
                        }
                    } else {
                        logger.info("응답 본문:\n{}", responseBody);
                    }
                    logger.info("=== 응답 데이터 끝 ===");
                }
            } else {
                logger.warn("API 응답 본문이 null입니다.");
            }
            
            // 에러 응답도 로깅
            if (!response.getStatusCode().is2xxSuccessful() && responseBody != null) {
                logger.error("API 에러 응답: {}", responseBody);
            }
            
            if (response.getStatusCode().is2xxSuccessful() && jsonNode != null) {
                // 이미 위에서 파싱한 jsonNode 사용
                JsonNode responseNode = jsonNode.path("response");
                JsonNode body = responseNode.path("body");
                JsonNode items = body.path("items");
                JsonNode item = items.path("item");
                
                // item이 배열인지 단일 객체인지 확인
                JsonNode firstItem = null;
                if (item.isArray() && item.size() > 0) {
                    // 배열인 경우
                    firstItem = item.get(0);
                } else if (!item.isMissingNode() && !item.isNull()) {
                    // 단일 객체인 경우
                    firstItem = item;
                }
                
                if (firstItem != null && !firstItem.isMissingNode()) {
                    
                    // 메타 정보
                    result.put("symbol", firstItem.path("itmsNm").asText());
                    result.put("stockCode", firstItem.path("srtnCd").asText());
                    result.put("currency", "KRW");
                    result.put("exchangeName", "KRX");
                    
                    // 현재가 정보 (빈 문자열 체크 추가)
                    String clpr = firstItem.path("clpr").asText("0").replace(",", "").trim();
                    String vs = firstItem.path("vs").asText("0").replace(",", "").trim();
                    String prdyClpr = firstItem.path("prdyClpr").asText("0").replace(",", "").trim();
                    
                    double currentPrice = clpr.isEmpty() ? 0.0 : Double.parseDouble(clpr);
                    double change = vs.isEmpty() ? 0.0 : Double.parseDouble(vs);
                    double previousClose = prdyClpr.isEmpty() ? 0.0 : Double.parseDouble(prdyClpr);
                    
                    result.put("regularMarketPrice", currentPrice);
                    result.put("previousClose", previousClose);
                    result.put("change", change);
                    result.put("changePercent", previousClose != 0 ? (change / previousClose) * 100 : 0);
                    
                    // 추가 정보 (빈 문자열 체크 추가)
                    String hipr = firstItem.path("hipr").asText("0").replace(",", "").trim();
                    String lopr = firstItem.path("lopr").asText("0").replace(",", "").trim();
                    String mkp = firstItem.path("mkp").asText("0").replace(",", "").trim();
                    String trqu = firstItem.path("trqu").asText("0").replace(",", "").trim();
                    String trPrc = firstItem.path("trPrc").asText("0").replace(",", "").trim();
                    String mrktTotAmt = firstItem.path("mrktTotAmt").asText("0").replace(",", "").trim();
                    
                    result.put("highPrice", hipr.isEmpty() ? 0.0 : Double.parseDouble(hipr));
                    result.put("lowPrice", lopr.isEmpty() ? 0.0 : Double.parseDouble(lopr));
                    result.put("openingPrice", mkp.isEmpty() ? 0.0 : Double.parseDouble(mkp));
                    result.put("volume", trqu.isEmpty() ? 0L : Long.parseLong(trqu));
                    result.put("tradeValue", trPrc.isEmpty() ? 0L : Long.parseLong(trPrc));
                    result.put("marketCap", mrktTotAmt.isEmpty() ? 0L : Long.parseLong(mrktTotAmt));
                    
                    // 최근 일주일 데이터 가져오기 (차트용)
                    List<Map<String, Object>> chartData = fetchHistoricalData(stockCode);
                    result.put("chartData", chartData);
                    
                    result.put("success", true);
                } else {
                    result.put("error", "해당 종목코드의 데이터를 찾을 수 없습니다: " + stockCode);
                    result.put("success", false);
                }
            } else {
                // 에러 응답 확인
                String errorBody = responseBody;
                logger.error("API 호출 실패 - 상태: {}, 응답: {}", response.getStatusCode(), 
                            errorBody != null ? errorBody.substring(0, Math.min(500, errorBody.length())) : "null");
                
                if (errorBody != null && errorBody.contains("SERVICE_KEY")) {
                    result.put("error", "API 키 인증 실패. API 키를 확인해주세요.");
                } else if (errorBody != null && errorBody.contains("NODATA")) {
                    result.put("error", "해당 종목코드의 데이터를 찾을 수 없습니다: " + stockCode);
                } else {
                    result.put("error", "API 호출 실패: " + response.getStatusCode() + 
                              (errorBody != null ? " - " + errorBody.substring(0, Math.min(200, errorBody.length())) : ""));
                }
                result.put("success", false);
            }
        } catch (Exception e) {
            logger.error("공공데이터포털 API 호출 중 오류: {}", e.getMessage(), e);
            result.put("error", "데이터를 가져오는 중 오류가 발생했습니다: " + e.getMessage());
            result.put("success", false);
        }
        
        return result;
    }
    
    /**
     * 최근 일주일 주식 시세 데이터 가져오기 (차트 구성용)
     */
    private List<Map<String, Object>> fetchHistoricalData(String stockCode) {
        List<Map<String, Object>> chartData = new ArrayList<>();
        
        try {
            // 최근 7일 데이터 조회
            String encodedApiKey = URLEncoder.encode(publicDataApiKey, StandardCharsets.UTF_8.toString());
            String url = "https://apis.data.go.kr/1160100/service/GetStockSecuritiesInfoService/getStockPriceInfo" +
                        "?serviceKey=" + encodedApiKey +
                        "&numOfRows=7" +
                        "&pageNo=1" +
                        "&resultType=json" +
                        "&srtnCd=" + stockCode;
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            String historicalResponseBody = response.getBody();
            if (historicalResponseBody != null) {
                try {
                    // JSON을 파싱한 후 예쁘게 포맷팅
                    JsonNode jsonNode = objectMapper.readTree(historicalResponseBody);
                    String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
                    
                    logger.info("=== 과거 데이터 API 응답 전체 데이터 ===");
                    // 큰 로그를 여러 줄로 나누어 출력 (각 줄당 최대 1000자)
                    int maxLineLength = 1000;
                    if (prettyJson.length() > maxLineLength) {
                        String[] lines = prettyJson.split("\n");
                        for (String line : lines) {
                            if (line.length() > maxLineLength) {
                                // 한 줄이 너무 길면 여러 줄로 분할
                                for (int i = 0; i < line.length(); i += maxLineLength) {
                                    int end = Math.min(i + maxLineLength, line.length());
                                    logger.info("{}", line.substring(i, end));
                                }
                            } else {
                                logger.info("{}", line);
                            }
                        }
                    } else {
                        logger.info("응답 본문:\n{}", prettyJson);
                    }
                    logger.info("=== 과거 데이터 응답 끝 ===");
                } catch (Exception e) {
                    // JSON 파싱 실패 시 원본 출력
                    logger.warn("JSON 포맷팅 실패, 원본 출력: {}", e.getMessage());
                    logger.info("=== 과거 데이터 API 응답 전체 데이터 (원본) ===");
                    // 원본도 여러 줄로 나누어 출력
                    int maxLineLength = 1000;
                    if (historicalResponseBody.length() > maxLineLength) {
                        for (int i = 0; i < historicalResponseBody.length(); i += maxLineLength) {
                            int end = Math.min(i + maxLineLength, historicalResponseBody.length());
                            logger.info("{}", historicalResponseBody.substring(i, end));
                        }
                    } else {
                        logger.info("응답 본문:\n{}", historicalResponseBody);
                    }
                    logger.info("=== 과거 데이터 응답 끝 ===");
                }
            }
            
            if (response.getStatusCode().is2xxSuccessful() && historicalResponseBody != null) {
                JsonNode jsonNode = objectMapper.readTree(historicalResponseBody);
                JsonNode responseNode = jsonNode.path("response");
                JsonNode body = responseNode.path("body");
                JsonNode items = body.path("items");
                JsonNode item = items.path("item");
                
                // item이 배열인지 단일 객체인지 확인
                if (item.isArray()) {
                    for (JsonNode dayData : item) {
                        if (dayData.isNull() || dayData.isMissingNode()) continue;
                        Map<String, Object> candle = new HashMap<>();
                        
                        // 날짜 파싱 (YYYYMMDD 형식)
                        String basDt = dayData.path("basDt").asText("");
                        if (!basDt.isEmpty() && basDt.length() == 8) {
                            try {
                                int year = Integer.parseInt(basDt.substring(0, 4));
                                int month = Integer.parseInt(basDt.substring(4, 6));
                                int day = Integer.parseInt(basDt.substring(6, 8));
                                // JavaScript Date 객체를 위한 timestamp (milliseconds)
                                long timestamp = new java.util.GregorianCalendar(year, month - 1, day).getTimeInMillis();
                                candle.put("time", timestamp);
                            } catch (Exception e) {
                                logger.warn("날짜 파싱 오류: {}", basDt);
                            }
                        }
                        
                        // 숫자 필드 파싱 (빈 문자열 체크 추가)
                        String mkp = dayData.path("mkp").asText("0").replace(",", "").trim();
                        String hipr = dayData.path("hipr").asText("0").replace(",", "").trim();
                        String lopr = dayData.path("lopr").asText("0").replace(",", "").trim();
                        String clpr = dayData.path("clpr").asText("0").replace(",", "").trim();
                        String trqu = dayData.path("trqu").asText("0").replace(",", "").trim();
                        
                        candle.put("open", mkp.isEmpty() ? 0.0 : Double.parseDouble(mkp));
                        candle.put("high", hipr.isEmpty() ? 0.0 : Double.parseDouble(hipr));
                        candle.put("low", lopr.isEmpty() ? 0.0 : Double.parseDouble(lopr));
                        candle.put("close", clpr.isEmpty() ? 0.0 : Double.parseDouble(clpr));
                        candle.put("volume", trqu.isEmpty() ? 0L : Long.parseLong(trqu));
                        
                        chartData.add(candle);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("과거 데이터 조회 중 오류: {}", e.getMessage(), e);
        }
        
        return chartData;
    }
    
    /**
     * 최근 거래일의 전체 종목 정보 가져오기 (코스피, 코스닥 포함)
     * 코스피(mrktCtg=K)와 코스닥(mrktCtg=Q)를 각각 요청하여 합침
     * @param tradingDate 거래일 (YYYYMMDD 형식, null이면 최근 거래일)
     * @return 전체 종목 리스트 (코스피 + 코스닥)
     */
    public List<Map<String, Object>> getKospiDailyData(String tradingDate) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        try {
            // 코스피(mrktCtg=KOSPI) 조회
            logger.info("코스피 거래일 정보 조회 시작: 거래일={}", tradingDate);
            List<Map<String, Object>> kospiData = fetchMarketData(tradingDate, "KOSPI", "코스피");
            
            // 코스닥(mrktCtg=KOSDAQ) 조회
            logger.info("코스닥 거래일 정보 조회 시작: 거래일={}", tradingDate);
            List<Map<String, Object>> kosdaqData = fetchMarketData(tradingDate, "KOSDAQ", "코스닥");
            
            // 두 결과 합치기
            // 두 결과를 합칠 때 거래량이 없는 종목은 제외
            for (Map<String, Object> stock : kospiData) {
                Long volume = 0L;
                Object trquObj = stock.get("trqu");
                if (trquObj instanceof Number) {
                    volume = ((Number) trquObj).longValue();
                } else if (trquObj != null) {
                    try {
                        volume = Long.parseLong(trquObj.toString().replace(",", "").trim());
                    } catch (Exception ignored) {}
                }
                if (volume > 0) {
                    result.add(stock);
                }
            }
            for (Map<String, Object> stock : kosdaqData) {
                Long volume = 0L;
                Object trquObj = stock.get("trqu");
                if (trquObj instanceof Number) {
                    volume = ((Number) trquObj).longValue();
                } else if (trquObj != null) {
                    try {
                        volume = Long.parseLong(trquObj.toString().replace(",", "").trim());
                    } catch (Exception ignored) {}
                }
                if (volume > 0) {
                    result.add(stock);
                }
            }
            
            // 시가총액 기준으로 정렬 (내림차순)
            result.sort((a, b) -> {
                Long marketCapA = 0L;
                Long marketCapB = 0L;
                
                Object mrktTotAmtObjA = a.get("mrktTotAmt");
                if (mrktTotAmtObjA instanceof Number) {
                    marketCapA = ((Number) mrktTotAmtObjA).longValue();
                } else if (mrktTotAmtObjA != null) {
                    try {
                        marketCapA = Long.parseLong(mrktTotAmtObjA.toString().replace(",", "").trim());
                    } catch (Exception ignored) {}
                }
                
                Object mrktTotAmtObjB = b.get("mrktTotAmt");
                if (mrktTotAmtObjB instanceof Number) {
                    marketCapB = ((Number) mrktTotAmtObjB).longValue();
                } else if (mrktTotAmtObjB != null) {
                    try {
                        marketCapB = Long.parseLong(mrktTotAmtObjB.toString().replace(",", "").trim());
                    } catch (Exception ignored) {}
                }
                
                return marketCapB.compareTo(marketCapA);
            });
            
            logger.info("거래일 정보 조회 완료: 코스피 {}개, 코스닥 {}개, 총 {}개 종목", kospiData.size(), kosdaqData.size(), result.size());
        } catch (Exception e) {
            logger.error("거래일 정보 조회 중 오류: {}", e.getMessage(), e);
        }
        
        return result;
    }
    
    /**
     * 특정 시장(mrktCtg)의 거래일 정보 가져오기
     * @param tradingDate 거래일 (YYYYMMDD 형식, null이면 최근 거래일)
     * @param mrktCtg 시장분류 코드 (K: 코스피, Q: 코스닥)
     * @param marketName 시장명 (로깅용)
     * @return 종목 리스트
     */
    private List<Map<String, Object>> fetchMarketData(String tradingDate, String mrktCls, String marketName) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        try {
            String encodedApiKey = URLEncoder.encode(publicDataApiKey, StandardCharsets.UTF_8.toString());

            // numOfRows를 1000으로 하고, 전체 건수(page 단위) 순환 후 모두 합치는 방식
            int numOfRows = 1000;
            int pageNo = 1;
            int totalCount = Integer.MAX_VALUE; // 최초엔 무한대로 설정
            int loadedCount = 0;
            List<Map<String, Object>> mergedResults = new ArrayList<>();

            do {
                String url = "https://apis.data.go.kr/1160100/service/GetStockSecuritiesInfoService/getStockPriceInfo"
                        + "?serviceKey=" + encodedApiKey
                        + "&numOfRows=" + numOfRows
                        + "&pageNo=" + pageNo
                        + "&resultType=json"
                        + "&mrktCls=" + mrktCls;

                String resolvedTradingDate = tradingDate;
                if (resolvedTradingDate == null || resolvedTradingDate.trim().isEmpty()) {
                    // 오늘 날짜를 yyyyMMdd 형식으로 변환
                    java.time.LocalDate now = java.time.LocalDate.now();
                    java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd");
                    resolvedTradingDate = now.format(formatter);
                } else {
                    resolvedTradingDate = resolvedTradingDate.trim();
                }
                url += "&basDt=" + resolvedTradingDate;

                logger.info("{} API 호출: mrktCtg={}, 거래일={}, pageNo={}", marketName, mrktCls, tradingDate, pageNo);
                ResponseEntity<String> apiResponse = restTemplate.getForEntity(url, String.class);
                String responseBody = apiResponse.getBody();
                if (apiResponse.getStatusCode().is2xxSuccessful() && responseBody != null) {
                    JsonNode jsonNode = objectMapper.readTree(responseBody);
                    JsonNode responseNode = jsonNode.path("response");
                    JsonNode body = responseNode.path("body");
                    JsonNode items = body.path("items");
                    JsonNode item = items.path("item");

                    // totalCount를 첫 페이지에서만 파악
                    if (body.has("totalCount")) {
                        totalCount = body.path("totalCount").asInt(totalCount);
                    }

                    // item이 배열인지 단일 객체인지 확인
                    if (item.isArray()) {
                        for (JsonNode stockItem : item) {
                            if (stockItem.isNull() || stockItem.isMissingNode()) continue;
                            Map<String, Object> stockData = parseStockItem(stockItem);
                            if (stockData != null) {
                                mergedResults.add(stockData);
                            }
                        }
                    } else if (!item.isMissingNode() && !item.isNull()) {
                        Map<String, Object> stockData = parseStockItem(item);
                        if (stockData != null) {
                            mergedResults.add(stockData);
                        }
                    }
                } else {
                    logger.error("{} API 호출 실패 - 상태: {}", marketName, apiResponse.getStatusCode());
                    break;
                }

                loadedCount = mergedResults.size();
                pageNo++;
            } while (loadedCount < totalCount);

            result.addAll(mergedResults);

            logger.info("{} 조회 완료: {}개 종목", marketName, result.size());

        } catch (Exception e) {
            logger.error("{} 조회 중 오류: {}", marketName, e.getMessage(), e);
        }
        
        return result;
    }
    
    /**
     * 종목 데이터 파싱
     * @param stockItem JSON 노드
     * @return 종목 데이터 Map
     */
    private Map<String, Object> parseStockItem(JsonNode stockItem) {
        try {
            Map<String, Object> stockData = new HashMap<>();
            
            // 종목 기본 정보
            stockData.put("srtnCd", stockItem.path("srtnCd").asText(""));
            stockData.put("itmsNm", stockItem.path("itmsNm").asText(""));
            stockData.put("mrktCtg", stockItem.path("mrktCtg").asText(""));
            stockData.put("basDt", stockItem.path("basDt").asText(""));
            
            // 가격 정보 (빈 문자열 체크 추가)
            String clpr = stockItem.path("clpr").asText("0").replace(",", "").trim();
            String vs = stockItem.path("vs").asText("0").replace(",", "").trim();
            String prdyClpr = stockItem.path("prdyClpr").asText("0").replace(",", "").trim();
            String mkp = stockItem.path("mkp").asText("0").replace(",", "").trim();
            String hipr = stockItem.path("hipr").asText("0").replace(",", "").trim();
            String lopr = stockItem.path("lopr").asText("0").replace(",", "").trim();
            
            double currentPrice = clpr.isEmpty() ? 0.0 : Double.parseDouble(clpr);
            double change = vs.isEmpty() ? 0.0 : Double.parseDouble(vs);
            double previousClose = prdyClpr.isEmpty() ? 0.0 : Double.parseDouble(prdyClpr);
            double openingPrice = mkp.isEmpty() ? 0.0 : Double.parseDouble(mkp);
            double highPrice = hipr.isEmpty() ? 0.0 : Double.parseDouble(hipr);
            double lowPrice = lopr.isEmpty() ? 0.0 : Double.parseDouble(lopr);
            
            stockData.put("clpr", currentPrice);
            stockData.put("vs", change);
            stockData.put("vsPercent", previousClose != 0 ? (change / previousClose) * 100 : 0);
            stockData.put("prdyClpr", previousClose);
            stockData.put("mkp", openingPrice);
            stockData.put("hipr", highPrice);
            stockData.put("lopr", lowPrice);
            
            // 거래량 정보
            String trqu = stockItem.path("trqu").asText("0").replace(",", "").trim();
            String trPrc = stockItem.path("trPrc").asText("0").replace(",", "").trim();
            String mrktTotAmt = stockItem.path("mrktTotAmt").asText("0").replace(",", "").trim();
            
            stockData.put("trqu", trqu.isEmpty() ? 0L : Long.parseLong(trqu));
            stockData.put("trPrc", trPrc.isEmpty() ? 0L : Long.parseLong(trPrc));
            stockData.put("mrktTotAmt", mrktTotAmt.isEmpty() ? 0L : Long.parseLong(mrktTotAmt));
            
            return stockData;
        } catch (Exception e) {
            logger.error("종목 데이터 파싱 중 오류: {}", e.getMessage(), e);
            return null;
        }
    }
}

