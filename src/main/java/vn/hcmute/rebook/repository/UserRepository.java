package vn.hcmute.rebook.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.hcmute.rebook.entity.User;
import vn.hcmute.rebook.entity.enums.Role;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRoleAndLockedFalse(Role role);

    long countByRole(Role role);

    long countByCreatedAtAfter(LocalDateTime time);

    @Query("""
            select u from User u
            where (:kw is null or lower(u.fullName) like lower(concat('%', :kw, '%'))
                   or lower(u.email) like lower(concat('%', :kw, '%')) or u.phone like concat('%', :kw, '%'))
              and (:role is null or u.role = :role)
            """)
    Page<User> search(@Param("kw") String keyword, @Param("role") Role role, Pageable pageable);
}
