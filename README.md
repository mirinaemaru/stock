# Stock Management System

Spring Boot 기반 주식 관리 웹 애플리케이션입니다.

## 기술 스택

- **Java**: 11
- **Spring Boot**: 2.7.18
- **템플릿 엔진**: Thymeleaf
- **데이터베이스**: MariaDB
- **ORM**: JPA, MyBatis
- **빌드 도구**: Maven

## 주요 기능

- 웹 기반 주식 관리 시스템
- RESTful API 지원
- 데이터베이스 연동 (JPA, MyBatis)
- MyBatis Mapper 지원

## 프로젝트 구조

```
stock/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/stock/
│   │   │       ├── StockApplication.java
│   │   │       ├── config/
│   │   │       │   └── MyBatisConfig.java
│   │   │       ├── controller/
│   │   │       │   └── StockController.java
│   │   │       └── mapper/
│   │   │           └── StockMapper.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── mapper/
│   │       │   └── StockMapper.xml
│   │       └── templates/
│   │           └── index.html
│   └── test/
│       └── java/
│           └── com/stock/
│               └── StockApplicationTests.java
├── pom.xml
└── README.md
```

## 실행 방법

### 1. Maven을 사용한 실행

```bash
# 프로젝트 빌드
mvn clean install

# 애플리케이션 실행
mvn spring-boot:run
```

### 2. IDE에서 실행

`StockApplication.java` 파일을 실행합니다.

## 접속 주소

- **메인 페이지**: http://localhost:8080
- **MyBatis 테스트**: http://localhost:8080/test/mybatis

## 데이터베이스 설정

### MariaDB 연결 정보
- **호스트**: localhost
- **포트**: 3306
- **데이터베이스명**: stockdb
- **사용자명**: root (application.properties에서 수정 가능)
- **비밀번호**: (application.properties에서 설정)

### 데이터베이스 생성
애플리케이션 실행 전에 MariaDB에 데이터베이스를 생성해야 합니다:

```sql
CREATE DATABASE IF NOT EXISTS stockdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 설정 변경
1. `src/main/resources/application.properties.example` 파일을 복사하여 `application.properties` 파일을 생성합니다:
   ```bash
   cp src/main/resources/application.properties.example src/main/resources/application.properties
   ```

2. `application.properties` 파일에서 데이터베이스 연결 정보를 수정합니다:
   ```properties
   spring.datasource.url=jdbc:mariadb://localhost:3306/stock?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Seoul
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   ```

**주의**: `application.properties` 파일은 Git에 포함되지 않습니다. 각 개발자는 자신의 환경에 맞게 설정 파일을 생성해야 합니다.

## MyBatis 사용법

### Mapper 인터페이스 생성
```java
@Mapper
public interface StockMapper {
    @Select("SELECT * FROM stock WHERE id = #{id}")
    Stock findById(Long id);
}
```

### Mapper XML 파일 사용
- 위치: `src/main/resources/mapper/**/*.xml`
- 네임스페이스: Mapper 인터페이스의 전체 경로와 일치해야 함

### 설정
- Mapper 스캔: `@MapperScan(basePackages = "com.stock.mapper")` (MyBatisConfig.java)
- XML 위치: `mybatis.mapper-locations=classpath:mapper/**/*.xml`
- Type Aliases: `mybatis.type-aliases-package=com.stock.model`

## 개발 환경 설정

1. Java 11 이상 설치
2. Maven 3.6 이상 설치
3. MariaDB 설치 및 실행
   ```bash
   # macOS (Homebrew)
   brew install mariadb
   brew services start mariadb
   
   # 데이터베이스 생성
   mysql -u root -p
   CREATE DATABASE stockdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
4. IDE (IntelliJ IDEA, Eclipse, VS Code 등) 설정

## 다음 단계

- [ ] 주식 데이터 모델 설계
- [ ] CRUD 기능 구현
- [ ] REST API 엔드포인트 추가
- [ ] 데이터베이스 스키마 설계
- [ ] 프론트엔드 UI 개선

## 라이선스

이 프로젝트는 개인 프로젝트입니다.

