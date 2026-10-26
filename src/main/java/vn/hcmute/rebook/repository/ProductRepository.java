package vn.hcmute.rebook.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.hcmute.rebook.entity.Product;
import vn.hcmute.rebook.entity.enums.ProductStatus;
import vn.hcmute.rebook.entity.enums.StoreStatus;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    /** Sách đang bán của các shop đang hoạt động */
    @Query("select p from Product p where p.status = :ps and p.store.status = :ss")
    Page<Product> findVisible(@Param("ps") ProductStatus ps, @Param("ss") StoreStatus ss, Pageable pageable);

    /** Trang chủ Guest: sách đã bán trên :minSold cuốn, lượng bán giảm dần */
    @Query("select p from Product p where p.status = :ps and p.store.status = :ss and p.soldCount > :minSold order by p.soldCount desc")
    List<Product> findBestSellersAbove(@Param("ps") ProductStatus ps, @Param("ss") StoreStatus ss,
                                       @Param("minSold") int minSold, Pageable pageable);

    Page<Product> findByStoreId(Long storeId, Pageable pageable);

    @Query("""
            select p from Product p
            where p.store.id = :storeId
              and (:kw is null or lower(p.title) like lower(concat('%', :kw, '%')) or lower(p.author) like lower(concat('%', :kw, '%')))
              and (:status is null or p.status = :status)
            """)
    Page<Product> searchInStore(@Param("storeId") Long storeId, @Param("kw") String kw,
                                @Param("status") ProductStatus status, Pageable pageable);

    @Query("""
            select p from Product p
            where (:storeId is null or p.store.id = :storeId)
              and (:kw is null or lower(p.title) like lower(concat('%', :kw, '%')) or lower(p.author) like lower(concat('%', :kw, '%')))
              and (:status is null or p.status = :status)
            """)
    Page<Product> adminSearch(@Param("storeId") Long storeId, @Param("kw") String kw,
                              @Param("status") ProductStatus status, Pageable pageable);

    Optional<Product> findByIdAndStoreId(Long id, Long storeId);

    long countByStoreId(Long storeId);

    long countByCategoryId(Long categoryId);

    @Query("select p from Product p where p.category.id = :catId and p.id <> :id and p.status = vn.hcmute.rebook.entity.enums.ProductStatus.ACTIVE and p.store.status = vn.hcmute.rebook.entity.enums.StoreStatus.ACTIVE order by p.soldCount desc")
    List<Product> findRelated(@Param("catId") Long categoryId, @Param("id") Long id, Pageable pageable);

    @Modifying
    @Query("update Product p set p.viewCount = p.viewCount + 1 where p.id = :id")
    void increaseView(@Param("id") Long id);
}
