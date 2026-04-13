#!/bin/bash
# .env 파일에서 환경변수를 읽어서 서버 실행
set -a
source .env
set +a
./gradlew bootRun
