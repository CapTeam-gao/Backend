package com.capteam.gaobackend.repository;

import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.enums.AccountRole;
import com.capteam.gaobackend.enums.Grade;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUserId(String userId);
    long countByAccountRole(AccountRole accountRole);
    List<User> findByAccountRole(AccountRole accountRole);
    List<User> findByAccountRoleAndGrade(AccountRole accountRole, Grade grade);
    Optional<User> findByNameAndGrade(String name, Grade grade);
    List<User> findByGrade(Grade grade);

    @Query("""
            select u
            from User u
            where u.accountRole = :accountRole
              and u.grade = :grade
              and (
                    lower(u.name) like lower(concat('%', :keyword, '%'))
                 or lower(u.userId) like lower(concat('%', :keyword, '%'))
              )
            order by u.userId asc
            """)
    // 이 프로젝트는 별도 학번 컬럼 없이 student userId(stu2107 등)에 학번이 포함되어 있으므로
    // 학번 부분 검색도 userId like 검색으로 함께 처리합니다.
    List<User> searchStudentsByKeyword(@Param("accountRole") AccountRole accountRole,
                                        @Param("grade") Grade grade,
                                        @Param("keyword") String keyword,
                                        Pageable pageable);

    @Query("""
            select u
            from User u
            where u.accountRole = :accountRole
              and u.grade = :grade
              and (
                    lower(u.name) like lower(concat('%', :keyword, '%'))
                 or lower(u.userId) like lower(concat('%', :keyword, '%'))
                 or (:studentRole is not null and u.studentRole = :studentRole)
              )
            order by u.userId asc
            """)
    List<User> searchStudentsForManualTeam(@Param("accountRole") AccountRole accountRole,
                                            @Param("grade") Grade grade,
                                            @Param("keyword") String keyword,
                                            @Param("studentRole") com.capteam.gaobackend.enums.StudentRole studentRole,
                                            Pageable pageable);
}
