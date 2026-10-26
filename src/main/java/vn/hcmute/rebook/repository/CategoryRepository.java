package vn.hcmute.rebook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.hcmute.rebook.entity.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findBySlug(String slug);

    boolean existsByNameIgnoreCase(String name);

    List<Category> findAllByOrderByNameAsc();
}
