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
}

