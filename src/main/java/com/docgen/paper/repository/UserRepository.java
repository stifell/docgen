package com.docgen.paper.repository;

import com.docgen.paper.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author Денис on 05.06.2024
 */
public interface UserRepository extends JpaRepository<User, Long> {
}
