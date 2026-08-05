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
