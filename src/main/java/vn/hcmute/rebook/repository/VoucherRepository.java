package vn.hcmute.rebook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.hcmute.rebook.entity.Voucher;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    Optional<Voucher> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<Voucher> findByStoreIsNullOrderByIdDesc();

    List<Voucher> findByStoreIdOrderByIdDesc(Long storeId);

    Optional<Voucher> findByIdAndStoreId(Long id, Long storeId);

    /** Mã còn hiệu lực áp dụng được cho giỏ hàng: mã toàn sàn + mã của các shop có trong giỏ */
    @Query("select v from Voucher v where v.active = true and v.usedCount < v.quantity and (v.store is null or v.store.id in :storeIds) order by v.percent desc")
    List<Voucher> findApplicable(@Param("storeIds") Collection<Long> storeIds);
}
