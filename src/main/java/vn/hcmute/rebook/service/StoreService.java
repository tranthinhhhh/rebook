package vn.hcmute.rebook.service;

import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;
import vn.hcmute.rebook.dto.StoreForm;
import vn.hcmute.rebook.entity.Store;
import vn.hcmute.rebook.entity.enums.StoreStatus;

import java.util.List;
import java.util.Optional;

public interface StoreService {
    Store getById(Long id);

    Optional<Store> findByOwner(Long ownerId);

    Store getActiveStore(Long id);

    List<Store> findActiveStores();

    void register(Long userId, StoreForm form);

    void update(Long storeId, StoreForm form, MultipartFile logo, MultipartFile banner);

    Page<Store> search(String keyword, StoreStatus status, int page);

    void changeStatus(Long storeId, StoreStatus status);

    void updateCommission(Long storeId, double rate);
}
