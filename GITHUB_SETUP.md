# GitHub 연동 가이드

이 문서는 Stock 프로젝트를 GitHub에 업로드하고 연동하는 방법을 안내합니다.

## 사전 준비사항

1. **GitHub 계정 생성** (없는 경우)
   - https://github.com 에서 계정 생성

2. **Git 설치 확인**
   ```bash
   git --version
   ```
   설치되어 있지 않다면: https://git-scm.com/downloads

## 단계별 가이드

### 1단계: Git 저장소 초기화

프로젝트 디렉토리에서 다음 명령어를 실행합니다:

```bash
cd /Users/changsupark/Stock
git init
```

### 2단계: Git 사용자 정보 설정 (최초 1회만)

```bash
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"
```

이미 설정되어 있다면 생략 가능합니다.

### 3단계: 파일 추가 및 첫 커밋

```bash
# 모든 파일을 스테이징 영역에 추가
git add .

# 첫 커밋 생성
git commit -m "Initial commit: Stock Management Application"
```

### 4단계: GitHub에서 새 저장소 생성

1. GitHub 웹사이트에 로그인
2. 우측 상단의 **"+"** 버튼 클릭 → **"New repository"** 선택
3. 저장소 정보 입력:
   - **Repository name**: `Stock` (또는 원하는 이름)
   - **Description**: "Stock Management Web Application" (선택사항)
   - **Public** 또는 **Private** 선택
   - ⚠️ **"Initialize this repository with a README"** 체크하지 않기
   - ⚠️ **"Add .gitignore"** 체크하지 않기 (이미 있음)
4. **"Create repository"** 버튼 클릭

### 5단계: 원격 저장소 연결

GitHub에서 생성한 저장소의 URL을 복사합니다 (예: `https://github.com/yourusername/Stock.git`)

```bash
# 원격 저장소 추가
git remote add origin https://github.com/yourusername/Stock.git

# 원격 저장소 확인
git remote -v
```

### 6단계: 코드 업로드 (Push)

```bash
# 메인 브랜치 이름 확인 및 설정 (필요시)
git branch -M main

# GitHub에 코드 업로드
git push -u origin main
```

**인증 방법:**
- **Personal Access Token (PAT) 사용** (권장)
  - GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
  - "Generate new token" 클릭
  - `repo` 권한 체크
  - 토큰 생성 후 비밀번호 대신 사용
- 또는 **SSH 키 사용**
  - SSH 키가 설정되어 있다면 `git@github.com:yourusername/Stock.git` 형식 사용

### 7단계: 업로드 확인

GitHub 웹사이트에서 저장소 페이지를 새로고침하여 파일들이 업로드되었는지 확인합니다.

## 이후 작업 흐름

### 변경사항 업로드

```bash
# 변경된 파일 확인
git status

# 변경사항 스테이징
git add .

# 커밋 생성
git commit -m "커밋 메시지"

# GitHub에 업로드
git push
```

### 최신 코드 가져오기

```bash
git pull
```

## 문제 해결

### 인증 오류 발생 시

```bash
# Personal Access Token 사용
git remote set-url origin https://YOUR_TOKEN@github.com/yourusername/Stock.git

# 또는 SSH 사용
git remote set-url origin git@github.com:yourusername/Stock.git
```

### 이미 원격 저장소가 있는 경우

```bash
# 기존 원격 저장소 제거
git remote remove origin

# 새 원격 저장소 추가
git remote add origin https://github.com/yourusername/Stock.git
```

### 커밋 이력 초기화가 필요한 경우

```bash
# 기존 .git 폴더 삭제 후 재초기화
rm -rf .git
git init
git add .
git commit -m "Initial commit"
```

## 주의사항

1. **민감한 정보는 커밋하지 않기**
   - `application.properties`에 데이터베이스 비밀번호 등이 있다면 환경변수로 분리 권장
   - `.gitignore`에 이미 설정되어 있지만 확인 필요

2. **대용량 파일 주의**
   - `target/` 폴더는 `.gitignore`에 포함되어 있어 자동 제외됨

3. **브랜치 전략**
   - `main` 또는 `master` 브랜치를 메인 브랜치로 사용
   - 기능 개발 시 별도 브랜치 생성 권장

## 추가 리소스

- [Git 공식 문서](https://git-scm.com/doc)
- [GitHub 가이드](https://guides.github.com/)
- [Git 명령어 치트시트](https://education.github.com/git-cheat-sheet-education.pdf)

