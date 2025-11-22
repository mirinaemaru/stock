package com.stock.controller;

import com.stock.mapper.StockMapper;
import com.stock.model.Stock;
import com.stock.service.StockDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class StockController {

    private static final Logger logger = LoggerFactory.getLogger(StockController.class);

    @Autowired
    private StockMapper stockMapper;
    
    @Autowired
    private StockDataService stockDataService;

    @GetMapping("/")
    public String index() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard/dashboard";
    }

    @GetMapping("/stock/list")
    public String stockList(@RequestParam(required = false) String keyword,
                           @RequestParam(required = false) List<String> gubun,
                           Model model) {
        logger.info("=== /stock/list 요청 수신, 검색어: {}, 구분: {} ===", keyword, gubun);
        try {
            List<Stock> stocks;
            List<String> selectedGubuns = gubun;
            int pageSize = 100; // 초기 로드 100건
            int offset = 0;
            int totalCount;
            
            // 구분이 선택되지 않았을 때만 기본값으로 KOSPI, KOSDAQ 설정
            // 사용자가 명시적으로 선택한 값이 있으면 그대로 사용
            if (selectedGubuns == null || selectedGubuns.isEmpty()) {
                selectedGubuns = new java.util.ArrayList<>();
                selectedGubuns.add("KOSPI");
                selectedGubuns.add("KOSDAQ");
            }
            
            if (keyword != null && !keyword.trim().isEmpty()) {
                // 검색어가 있으면 검색 수행
                totalCount = stockMapper.countByCodeOrNameAndGubun(keyword.trim(), selectedGubuns);
                stocks = stockMapper.findByCodeOrNameAndGubunWithPaging(keyword.trim(), selectedGubuns, offset, pageSize);
                logger.info("검색 결과 총 개수: {}, 초기 데이터: {}개", totalCount, stocks != null ? stocks.size() : 0);
            } else {
                // 검색어가 없으면 구분 필터를 적용하여 조회
                totalCount = stockMapper.countByGubun(selectedGubuns);
                stocks = stockMapper.findByGubunWithPaging(selectedGubuns, offset, pageSize);
                logger.info("구분 필터 조회 결과 총 개수: {}, 초기 데이터: {}개", totalCount, stocks != null ? stocks.size() : 0);
            }
            
            if (stocks == null) {
                stocks = new java.util.ArrayList<>();
            }
            
            model.addAttribute("stocks", stocks);
            model.addAttribute("totalCount", totalCount);
            model.addAttribute("displayCount", stocks.size());
            model.addAttribute("keyword", keyword != null ? keyword : "");
            model.addAttribute("selectedGubuns", selectedGubuns);
            logger.info("템플릿 반환: stock/stockList, 표시할 데이터: {}개, 총 개수: {}개", stocks.size(), totalCount);
            return "stock/stockList";
        } catch (Exception e) {
            logger.error("주식 목록 조회 중 오류 발생", e);
            model.addAttribute("stocks", new java.util.ArrayList<>());
            model.addAttribute("totalCount", 0);
            model.addAttribute("displayCount", 0);
            model.addAttribute("keyword", keyword != null ? keyword : "");
            List<String> defaultGubuns = new java.util.ArrayList<>();
            defaultGubuns.add("KOSPI");
            defaultGubuns.add("KOSDAQ");
            model.addAttribute("selectedGubuns", defaultGubuns);
            return "stock/stockList";
        }
    }
    
    /**
     * 무한 스크롤을 위한 추가 데이터 로드 API
     */
    @GetMapping("/stock/list/loadMore")
    @ResponseBody
    public Map<String, Object> loadMore(@RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) List<String> gubun,
                                                  @RequestParam(defaultValue = "0") int offset) {
        logger.info("=== /stock/list/loadMore 요청 수신, 검색어: {}, 구분: {}, offset: {} ===", keyword, gubun, offset);
        Map<String, Object> result = new HashMap<>();
        try {
            List<Stock> stocks;
            List<String> selectedGubuns = gubun;
            int pageSize = 100;
            
            // 구분이 선택되지 않았으면 기본값으로 KOSPI, KOSDAQ 설정
            if (selectedGubuns == null || selectedGubuns.isEmpty()) {
                selectedGubuns = new java.util.ArrayList<>();
                selectedGubuns.add("KOSPI");
                selectedGubuns.add("KOSDAQ");
            }
            
            if (keyword != null && !keyword.trim().isEmpty()) {
                // 검색어가 있으면 검색 수행
                stocks = stockMapper.findByCodeOrNameAndGubunWithPaging(keyword.trim(), selectedGubuns, offset, pageSize);
            } else {
                // 검색어가 없으면 구분 필터를 적용하여 조회
                stocks = stockMapper.findByGubunWithPaging(selectedGubuns, offset, pageSize);
            }
            
            if (stocks == null) {
                stocks = new java.util.ArrayList<>();
            }
            
            result.put("stocks", stocks);
            result.put("hasMore", stocks.size() == pageSize); // 다음 데이터가 있는지 확인
            logger.info("추가 데이터 로드: {}개, hasMore: {}", stocks.size(), stocks.size() == pageSize);
            return result;
        } catch (Exception e) {
            logger.error("추가 데이터 로드 중 오류 발생", e);
            result.put("stocks", new java.util.ArrayList<>());
            result.put("hasMore", false);
            return result;
        }
    }

    @GetMapping("/stock/add")
    public String stockAdd() {
        return "stock/stockAdd";
    }

    @GetMapping("/stock/analysis")
    public String analysis() {
        return "analysis/analysis";
    }
    
    /**
     * 주식 데이터를 가져오는 API 엔드포인트
     */
    @GetMapping("/api/stock/data")
    @ResponseBody
    public Map<String, Object> getStockData(@RequestParam String stockCode,
                                            @RequestParam(defaultValue = "KRX") String exchange) {
        logger.info("주식 데이터 요청: 종목코드={}, 거래소={}", stockCode, exchange);
        return stockDataService.getStockData(stockCode, exchange);
    }

    @GetMapping("/reports")
    public String reports() {
        return "reports/reports";
    }

    @GetMapping("/settings")
    public String settings() {
        return "settings/settings";
    }

    @GetMapping("/users")
    public String users() {
        return "users/users";
    }

    @GetMapping("/health")
    @ResponseBody
    public String health() {
        return "OK";
    }

    @GetMapping("/test/mybatis")
    @ResponseBody
    public String testMyBatis() {
        try {
            String currentTime = stockMapper.getCurrentTime();
            return "MyBatis 연결 성공! 현재 시간: " + currentTime;
        } catch (Exception e) {
            return "MyBatis 연결 실패: " + e.getMessage();
        }
    }

    @GetMapping("/test/stock-list-template")
    @ResponseBody
    public String testStockListTemplate(Model model) {
        logger.info("=== 템플릿 테스트 엔드포인트 호출 ===");
        try {
            List<Stock> stocks = stockMapper.findAll();
            logger.info("조회된 주식 개수: {}", stocks != null ? stocks.size() : 0);
            model.addAttribute("stocks", stocks != null ? stocks : new java.util.ArrayList<>());
            
            // 템플릿 경로 확인
            String templatePath = "stock/stockList";
            logger.info("템플릿 경로: {}", templatePath);
            
            return "템플릿 테스트 성공 - 주식 개수: " + (stocks != null ? stocks.size() : 0) + 
                   ", 템플릿 경로: " + templatePath;
        } catch (Exception e) {
            logger.error("템플릿 테스트 중 오류 발생", e);
            return "템플릿 테스트 실패: " + e.getMessage();
        }
    }

    @GetMapping("/test/simple")
    public String testSimple(Model model) {
        logger.info("=== 간단한 테스트 페이지 ===");
        model.addAttribute("message", "테스트 메시지");
        return "test/simple";
    }
}

