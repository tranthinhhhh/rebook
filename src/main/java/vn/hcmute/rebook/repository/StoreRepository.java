package vn.hcmute.rebook.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.hcmute.rebook.entity.Store;
import vn.hcmute.rebook.entity.enums.StoreStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {
    Optional<Store> findByOwnerId(Long ownerId);

    boolean existsByNameIgnoreCase(String name);

    List<Store> findByStatusOrderByNameAsc(StoreStatus status);

    long countByStatus(StoreStatus status);

    long countByCreatedAtAfter(LocalDateTime time);

    @Query("""
            select s from Store s
            where (:kw is null or lower(s.name) like lower(concat('%', :kw, '%')))
              and (:status is null or s.status = :status)
            """)
    Page<Store> search(@Param("kw") String keyword, @Param("status") StoreStatus status, Pageable pageable);
}
