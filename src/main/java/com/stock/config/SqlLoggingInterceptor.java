package com.stock.config;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * MyBatis SQL 로깅 Interceptor
 * 파라미터가 바인딩된 쿼리를 포맷팅하여 출력
 */
@Component
@Intercepts({
    @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
    @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
public class SqlLoggingInterceptor implements Interceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(SqlLoggingInterceptor.class);
    
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs()[1];
        
        BoundSql boundSql = mappedStatement.getBoundSql(parameter);
        Configuration configuration = mappedStatement.getConfiguration();
        
        // 파라미터가 바인딩된 쿼리 생성
        String sql = getFormattedSql(configuration, boundSql);
        
        // 쿼리 출력
        logger.debug("\n" + sql);
        
        return invocation.proceed();
    }
    
    /**
     * 파라미터가 바인딩된 포맷팅된 SQL 생성
     */
    private String getFormattedSql(Configuration configuration, BoundSql boundSql) {
        String sql = boundSql.getSql();
        Object parameterObject = boundSql.getParameterObject();
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        
        if (parameterMappings != null && parameterMappings.size() > 0 && parameterObject != null) {
            TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
            
            if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                // 단일 파라미터
                sql = sql.replaceFirst("\\?", getParameterValue(parameterObject));
            } else {
                // 복합 파라미터
                MetaObject metaObject = configuration.newMetaObject(parameterObject);
                for (ParameterMapping parameterMapping : parameterMappings) {
                    String propertyName = parameterMapping.getProperty();
                    if (metaObject.hasGetter(propertyName)) {
                        Object value = metaObject.getValue(propertyName);
                        sql = sql.replaceFirst("\\?", getParameterValue(value));
                    } else if (boundSql.hasAdditionalParameter(propertyName)) {
                        Object value = boundSql.getAdditionalParameter(propertyName);
                        sql = sql.replaceFirst("\\?", getParameterValue(value));
                    }
                }
            }
        }
        
        // SQL 포맷팅 (줄바꿈 추가)
        return formatSql(sql);
    }
    
    /**
     * 파라미터 값을 문자열로 변환
     */
    private String getParameterValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            return "'" + value + "'";
        }
        if (value instanceof Date) {
            DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.KOREA);
            return "'" + formatter.format(value) + "'";
        }
        return value.toString();
    }
    
    /**
     * SQL을 포맷팅하여 Mapper.xml과 유사한 형태로 출력
     * Mapper.xml 스타일: SELECT 다음 컬럼들이 각각 새 줄에, 콤마는 앞에 위치
     * 주석은 대문자 변환하지 않고 한 줄로 표시
     */
    private String formatSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return sql;
        }
        
        // 주석 추출 및 제거
        String comment = null;
        String sqlWithoutComment = sql;
        
        // /* ... */ 형태의 주석 처리
        if (sql.contains("/*") && sql.contains("*/")) {
            int commentStart = sql.indexOf("/*");
            int commentEnd = sql.indexOf("*/", commentStart) + 2;
            if (commentEnd > commentStart) {
                // 주석 추출 (원본 그대로, 가공하지 않음)
                comment = sql.substring(commentStart, commentEnd);
                // 주석 제거한 SQL
                sqlWithoutComment = (sql.substring(0, commentStart) + sql.substring(commentEnd)).trim();
            }
        }
        
        // SQL 본문만 대문자로 변환 (주석은 제외)
        sqlWithoutComment = sqlWithoutComment.toUpperCase();
        
        // 연속된 공백을 하나로 정리
        sqlWithoutComment = sqlWithoutComment.replaceAll("\\s+", " ");
        sqlWithoutComment = sqlWithoutComment.trim();
        
        StringBuilder result = new StringBuilder();
        
        // 주석이 있으면 원본 그대로 표시 (쿼리와 분리)
        if (comment != null) {
            result.append(comment).append("\n");
        }
        
        // SELECT 문 처리: SELECT 다음 컬럼들을 각각 새 줄로 분리
        if (sqlWithoutComment.startsWith("SELECT")) {
            // SELECT 다음의 컬럼 리스트 추출
            int fromIndex = sqlWithoutComment.indexOf(" FROM ");
            if (fromIndex > 0) {
                String selectPart = sqlWithoutComment.substring(0, fromIndex);
                String restPart = sqlWithoutComment.substring(fromIndex);
                
                // SELECT 키워드 제거
                String columns = selectPart.substring(6).trim();
                
                // 컬럼들을 콤마로 분리
                String[] columnArray = columns.split(",");
                
                result.append("SELECT\n");
                
                for (int i = 0; i < columnArray.length; i++) {
                    String column = columnArray[i].trim();
                    if (i == 0) {
                        // 첫 번째 컬럼은 들여쓰기만
                        result.append("    ").append(column).append("\n");
                    } else {
                        // 두 번째 컬럼부터는 콤마를 앞에 위치 (Mapper.xml 스타일)
                        result.append("    , ").append(column).append("\n");
                    }
                }
                
                // 나머지 부분 처리 (FROM 이후)
                result.append(formatRestOfSql(restPart));
                return result.toString().trim();
            }
        }
        
        // SELECT가 아닌 경우 기본 포맷팅
        result.append(formatRestOfSql(sqlWithoutComment));
        return result.toString().trim();
    }
    
    /**
     * FROM 이후의 SQL 부분을 포맷팅
     */
    private String formatRestOfSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return sql;
        }
        
        // 주요 키워드 앞에 줄바꿈 추가
        sql = sql.replaceAll("\\s+FROM\\b", "\nFROM");
        sql = sql.replaceAll("\\s+WHERE\\b", "\nWHERE");
        sql = sql.replaceAll("\\s+ORDER BY\\b", "\nORDER BY");
        sql = sql.replaceAll("\\s+GROUP BY\\b", "\nGROUP BY");
        sql = sql.replaceAll("\\s+HAVING\\b", "\nHAVING");
        sql = sql.replaceAll("\\s+INNER JOIN\\b", "\nINNER JOIN");
        sql = sql.replaceAll("\\s+LEFT JOIN\\b", "\nLEFT JOIN");
        sql = sql.replaceAll("\\s+RIGHT JOIN\\b", "\nRIGHT JOIN");
        sql = sql.replaceAll("\\s+JOIN\\b", "\nJOIN");
        sql = sql.replaceAll("\\s+ON\\b", "\n    ON");
        sql = sql.replaceAll("\\s+AND\\b", "\n    AND");
        sql = sql.replaceAll("\\s+OR\\b", "\n    OR");
        sql = sql.replaceAll("\\s+INSERT INTO\\b", "\nINSERT INTO");
        sql = sql.replaceAll("\\s+UPDATE\\b", "\nUPDATE");
        sql = sql.replaceAll("\\s+SET\\b", "\nSET");
        sql = sql.replaceAll("\\s+VALUES\\b", "\nVALUES");
        sql = sql.replaceAll("\\s+DELETE FROM\\b", "\nDELETE FROM");
        sql = sql.replaceAll("\\s+LIMIT\\b", "\nLIMIT");
        sql = sql.replaceAll("\\s+OFFSET\\b", "\nOFFSET");
        
        // 각 줄을 파싱하여 Mapper.xml 스타일로 포맷팅
        String[] lines = sql.split("\n");
        StringBuilder formatted = new StringBuilder();
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            String upperLine = line.toUpperCase();
            
            // Mapper.xml 스타일에 맞춰 들여쓰기 적용
            if (upperLine.startsWith("FROM") || upperLine.startsWith("WHERE") || 
                upperLine.startsWith("ORDER BY") || upperLine.startsWith("GROUP BY") ||
                upperLine.startsWith("HAVING") || upperLine.startsWith("SET") ||
                upperLine.startsWith("VALUES") || upperLine.startsWith("INTO") ||
                upperLine.startsWith("LIMIT") || upperLine.startsWith("OFFSET")) {
                formatted.append("    ").append(line).append("\n");
            } else if (upperLine.startsWith("JOIN") || upperLine.startsWith("ON")) {
                formatted.append("        ").append(line).append("\n");
            } else if (upperLine.startsWith("AND") || upperLine.startsWith("OR")) {
                formatted.append("        ").append(line).append("\n");
            } else if (upperLine.startsWith("INSERT") || upperLine.startsWith("UPDATE") || 
                       upperLine.startsWith("DELETE")) {
                formatted.append(line).append("\n");
            } else {
                formatted.append(line).append("\n");
            }
        }
        
        return formatted.toString();
    }
    
    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }
    
    @Override
    public void setProperties(Properties properties) {
        // 설정 속성 처리
    }
}

