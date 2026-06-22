package com.capteam.gaobackend.repository;

import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.enums.AccountRole;
import com.capteam.gaobackend.enums.Grade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUserId(String userId);
    long countByAccountRole(AccountRole accountRole);
    List<User> findByAccountRole(AccountRole accountRole);
    List<User> findByAccountRoleAndGrade(AccountRole accountRole, Grade grade);
    Optional<User> findByNameAndGrade(String name, Grade grade);
    List<User> findByGrade(Grade grade);
}
