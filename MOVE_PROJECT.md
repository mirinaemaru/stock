# 프로젝트 이동 가이드

## 프로젝트를 다른 디렉토리로 이동하는 방법

### 방법 1: 터미널 사용 (권장)

```bash
# 1. 현재 위치 확인
pwd
# 출력: /Users/changsupark/Stock

# 2. 목적지 디렉토리 생성 (필요시)
mkdir -p /원하는/경로

# 3. 프로젝트 이동
mv /Users/changsupark/Stock /원하는/경로/Stock

# 4. 이동 확인
cd /원하는/경로/Stock
pwd
```

### 방법 2: Finder 사용

1. Finder에서 `/Users/changsupark/Stock` 폴더를 찾습니다
2. 원하는 위치로 드래그 앤 드롭합니다
3. IDE에서 프로젝트 경로를 업데이트합니다

### 이동 후 확인 사항

1. **Git 저장소 확인**
   ```bash
   cd /새로운/경로/Stock
   git status
   ```

2. **IDE에서 프로젝트 다시 열기**
   - IntelliJ IDEA: File → Open → 새 경로 선택
   - VS Code: File → Open Folder → 새 경로 선택

3. **애플리케이션 실행 테스트**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

### 주의사항

- ✅ Git 저장소(.git)는 자동으로 함께 이동됩니다
- ✅ IDE 설정 파일(.idea, .vscode)도 함께 이동됩니다
- ✅ 프로젝트 내부 설정은 상대 경로를 사용하므로 문제없습니다
- ⚠️ IDE에서 프로젝트를 다시 열어야 할 수 있습니다
- ⚠️ 터미널에서 작업 중이었다면 새 경로로 이동해야 합니다

