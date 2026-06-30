# 운영 DB 수동 마이그레이션

현재 백엔드는 Hibernate `ddl-auto=update`를 사용하지만, MySQL 기존 `ENUM` 컬럼의 값 목록 변경은 안전하게 자동 반영되지 않을 수 있습니다.

따라서 `StudentLevel` enum을 5단계로 확장한 뒤에는 운영 DB에 아래 SQL을 직접 적용해야 합니다.

## 2026-06-30 user_analysis.student_level 5단계 확장

적용 파일:

```bash
deploy/sql/20260630_expand_user_analysis_student_level.sql
```

적용 SQL:

```sql
ALTER TABLE user_analysis
    MODIFY COLUMN student_level ENUM(
        'UPPER',
        'UPPER_MIDDLE',
        'MIDDLE',
        'LOWER_MIDDLE',
        'LOWER'
    ) NULL;
```

적용 확인:

```sql
SHOW COLUMNS
FROM user_analysis
LIKE 'student_level';
```

정상 결과의 `Type`에는 반드시 다섯 값이 보여야 합니다.

```text
enum('UPPER','UPPER_MIDDLE','MIDDLE','LOWER_MIDDLE','LOWER')
```

## 운영 서버에서 적용 예시

백엔드 DB 컨테이너 기준:

```bash
docker exec -i gao-mysql mysql --default-character-set=utf8mb4 -ugao_user -p1234 gao_db \
  < deploy/sql/20260630_expand_user_analysis_student_level.sql
```

서버에서 SQL 파일 위치가 다르면, 파일 경로만 실제 배포 경로에 맞게 바꿔 실행합니다.

## 기존 분석 결과 재저장

스키마 변경 후에도 기존 학생의 `user_analysis.student_level` 값이 `NULL`이면 백엔드/AI 분석을 다시 실행해 분석 결과를 재저장해야 합니다.

확인 SQL:

```sql
SELECT user_id, student_level
FROM user_analysis
WHERE student_level IS NULL;
```

`NULL` 학생이 남아 있으면 학생 설문 분석 또는 팀 생성 전 분석 흐름을 다시 태워 최신 5단계 등급을 저장합니다.
