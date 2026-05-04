# Memora - AI 감정 일기 서비스

노년층(65세 이상)을 위한 AI 기반 감정 일기 & 건강 리포트 서비스입니다.
하루 동안의 감정을 기록하면 AI가 따뜻한 일기를 작성해주고, 주간/월간 감정 변화를 분석합니다.

## 기술 스택

| 구분 | 기술 |
|------|------|
| Backend | Spring Boot 4.0.5 / Java 21 |
| Database | PostgreSQL 14 |
| Authentication | JWT + 카카오 소셜 로그인 |
| AI | OpenRouter (Gemini 2.5 Flash) |
| Push | Firebase Cloud Messaging |
| Email | Gmail SMTP |

## 주요 기능

- 회원가입 / 로그인 (이메일 + 카카오)
- 중간 일기 작성 (기분 선택 + 사진 + 한줄 메모)
- AI 일기 초안 생성 (중간기록 → 최종 일기)
- 자정 자동 일기 완료 (스케줄러)
- 주간/월간 감정 분석 리포트
- 푸시 알림 (일기 미작성 리마인드)
- 이메일 인증 비밀번호 재설정

## 실행 방법

### 1. PostgreSQL DB 생성
```sql
CREATE DATABASE memora_db;
CREATE USER memora_admin WITH PASSWORD '비밀번호';
GRANT ALL PRIVILEGES ON DATABASE memora_db TO memora_admin;
```

### 2. 환경변수 설정
`.env.example`을 복사하여 `.env` 파일 생성 후 값 채우기

### 3. Firebase 설정
`firebase-service-account.json` 파일을 `src/main/resources/`에 추가

### 4. 서버 실행
```bash
./gradlew bootRun
```

서버 실행 후 Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## 팀 구성

| 역할 | 인원 |
|------|------|
| Backend | 2명 |
| Frontend (React Native) | 2명 |
