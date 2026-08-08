package com.capteam.gaobackend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import javax.sql.DataSource;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserAnalysisSchemaMigration implements ApplicationRunner {

    private static final String EXPECTED_STUDENT_LEVEL_COLUMN_TYPE =
            "enum('UPPER','UPPER_MIDDLE','MIDDLE','LOWER_MIDDLE','LOWER')";

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        if (!isMySql()) {
            return;
        }

        migrateUserExperienceColumn();
        String columnType = findStudentLevelColumnType();
        if (columnType == null || isExpandedStudentLevelColumn(columnType)) {
            return;
        }

        jdbcTemplate.execute("""
                ALTER TABLE user_analysis
                MODIFY COLUMN student_level ENUM(
                    'UPPER',
                    'UPPER_MIDDLE',
                    'MIDDLE',
                    'LOWER_MIDDLE',
                    'LOWER'
                ) NULL
                """);

        log.info("user_analysis.student_level 컬럼을 5단계 enum으로 확장했습니다.");
    }

    private void migrateUserExperienceColumn() {
        findVarcharCollectionColumns("experience").forEach(column -> {
            jdbcTemplate.execute("ALTER TABLE " + column.tableName() + " MODIFY COLUMN " + column.columnName() + " TEXT");
            log.info("{}.{} 컬럼을 TEXT로 확장했습니다.", column.tableName(), column.columnName());
        });
    }

    private boolean isMySql() {
        try (var connection = dataSource.getConnection()) {
            String databaseProductName = connection.getMetaData().getDatabaseProductName();
            return databaseProductName != null && databaseProductName.toLowerCase().contains("mysql");
        } catch (Exception e) {
            log.warn("DB 종류를 확인하지 못해 user_analysis.student_level 자동 마이그레이션을 건너뜁니다.", e);
            return false;
        }
    }

    private String findStudentLevelColumnType() {
        return jdbcTemplate.query("""
                        SELECT COLUMN_TYPE
                        FROM INFORMATION_SCHEMA.COLUMNS
                        WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME = 'user_analysis'
                          AND COLUMN_NAME = 'student_level'
                        """,
                resultSet -> resultSet.next() ? resultSet.getString("COLUMN_TYPE") : null
        );
    }

    private boolean isExpandedStudentLevelColumn(String columnType) {
        String normalizedColumnType = columnType.replace(" ", "").toUpperCase();
        String normalizedExpectedType = EXPECTED_STUDENT_LEVEL_COLUMN_TYPE.replace(" ", "").toUpperCase();
        return normalizedColumnType.equals(normalizedExpectedType)
                || !normalizedColumnType.startsWith("ENUM(");
    }

    private List<TableColumn> findVarcharCollectionColumns(String columnName) {
        return jdbcTemplate.query("""
                        SELECT TABLE_NAME, COLUMN_NAME
                        FROM INFORMATION_SCHEMA.COLUMNS
                        WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME IN ('user_experience', 'users_experience')
                          AND COLUMN_NAME = ?
                          AND DATA_TYPE IN ('varchar', 'char')
                        """,
                (resultSet, rowNumber) -> new TableColumn(
                        resultSet.getString("TABLE_NAME"),
                        resultSet.getString("COLUMN_NAME")
                ),
                columnName
        );
    }

    private record TableColumn(String tableName, String columnName) {
    }
}
