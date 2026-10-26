package vn.hcmute.rebook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.hcmute.rebook.entity.Carrier;

import java.util.List;

public interface CarrierRepository extends JpaRepository<Carrier, Long> {
    List<Carrier> findByActiveTrueOrderByFeeAsc();

    boolean existsByNameIgnoreCase(String name);
}
