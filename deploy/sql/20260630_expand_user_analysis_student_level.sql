-- user_analysis.student_level을 3단계에서 5단계 등급으로 확장합니다.
-- 기존 컬럼:
--   enum('LOWER','MIDDLE','UPPER')
-- 변경 후 컬럼:
--   enum('UPPER','UPPER_MIDDLE','MIDDLE','LOWER_MIDDLE','LOWER')

ALTER TABLE user_analysis
    MODIFY COLUMN student_level ENUM(
        'UPPER',
        'UPPER_MIDDLE',
        'MIDDLE',
        'LOWER_MIDDLE',
        'LOWER'
    ) NULL;
