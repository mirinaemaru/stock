package com.stock.mapper;

import com.stock.model.Stock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * MyBatis Mapper 인터페이스
 * 쿼리는 StockMapper.xml에 정의되어 있습니다.
 */
@Mapper
public interface StockMapper {
    
    /**
     * 현재 시간 조회 (MariaDB)
     * @return 현재 시간 문자열
     */
    String getCurrentTime();
    
    /**
     * 주식 목록 조회
     * @return 주식 목록
     */
    List<Stock> findAll();
    
    /**
     * 주식 목록 조회 (페이징)
     * @param offset 시작 위치
     * @param limit 조회 개수
     * @return 주식 목록
     */
    List<Stock> findAllWithPaging(@Param("offset") int offset, @Param("limit") int limit);
    
    /**
     * 전체 주식 개수 조회
     * @return 총 개수
     */
    int countAll();
    
    /**
     * 주식 코드로 조회
     * @param code 주식 코드
     * @return 주식 정보
     */
    Stock findByCode(String code);
    
    /**
     * 주식 코드 또는 이름으로 검색
     * @param keyword 검색어 (코드 또는 이름)
     * @return 주식 목록
     */
    List<Stock> findByCodeOrName(String keyword);
    
    /**
     * 구분으로 필터링
     * @param gubunList 구분 목록
     * @return 주식 목록
     */
    List<Stock> findByGubun(@Param("gubunList") List<String> gubunList);
    
    /**
     * 주식 코드 또는 이름과 구분으로 검색
     * @param keyword 검색어 (코드 또는 이름)
     * @param gubunList 구분 목록
     * @return 주식 목록
     */
    List<Stock> findByCodeOrNameAndGubun(@Param("keyword") String keyword, @Param("gubunList") List<String> gubunList);
    
    /**
     * 구분으로 필터링 (페이징)
     * @param gubunList 구분 목록
     * @param offset 시작 위치
     * @param limit 조회 개수
     * @return 주식 목록
     */
    List<Stock> findByGubunWithPaging(@Param("gubunList") List<String> gubunList, @Param("offset") int offset, @Param("limit") int limit);
    
    /**
     * 주식 코드 또는 이름과 구분으로 검색 (페이징)
     * @param keyword 검색어 (코드 또는 이름)
     * @param gubunList 구분 목록
     * @param offset 시작 위치
     * @param limit 조회 개수
     * @return 주식 목록
     */
    List<Stock> findByCodeOrNameAndGubunWithPaging(@Param("keyword") String keyword, @Param("gubunList") List<String> gubunList, @Param("offset") int offset, @Param("limit") int limit);
    
    /**
     * 구분으로 필터링된 총 개수
     * @param gubunList 구분 목록
     * @return 총 개수
     */
    int countByGubun(@Param("gubunList") List<String> gubunList);
    
    /**
     * 주식 코드 또는 이름과 구분으로 검색된 총 개수
     * @param keyword 검색어 (코드 또는 이름)
     * @param gubunList 구분 목록
     * @return 총 개수
     */
    int countByCodeOrNameAndGubun(@Param("keyword") String keyword, @Param("gubunList") List<String> gubunList);
}

