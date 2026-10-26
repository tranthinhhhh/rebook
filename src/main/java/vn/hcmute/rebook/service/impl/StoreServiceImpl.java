package vn.hcmute.rebook.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.hcmute.rebook.dto.StoreForm;
import vn.hcmute.rebook.entity.Store;
import vn.hcmute.rebook.entity.User;
import vn.hcmute.rebook.entity.enums.Role;
import vn.hcmute.rebook.entity.enums.StoreStatus;
import vn.hcmute.rebook.exception.BusinessException;
import vn.hcmute.rebook.exception.NotFoundException;
import vn.hcmute.rebook.repository.StoreRepository;
import vn.hcmute.rebook.repository.UserRepository;
import vn.hcmute.rebook.service.NotificationService;
import vn.hcmute.rebook.service.StorageService;
import vn.hcmute.rebook.service.StoreService;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class StoreServiceImpl implements StoreService {
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final NotificationService notificationService;

    @Value("${app.default-commission-rate}")
    private double defaultCommission;

    @Override
    @Transactional(readOnly = true)
    public Store getById(Long id) {
        return storeRepository.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy cửa hàng"));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Store> findByOwner(Long ownerId) {
        return storeRepository.findByOwnerId(ownerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Store getActiveStore(Long id) {
        Store s = getById(id);
        if (s.getStatus() != StoreStatus.ACTIVE) throw new NotFoundException("Cửa hàng không tồn tại hoặc đã ngừng hoạt động");
        return s;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Store> findActiveStores() {
        return storeRepository.findByStatusOrderByNameAsc(StoreStatus.ACTIVE);
    }

    @Override
    public void register(Long userId, StoreForm form) {
        User user = userRepository.findById(userId).orElseThrow();
        if (user.getRole() != Role.USER && user.getRole() != Role.VENDOR) {
            throw new BusinessException("Tài khoản quản trị/shipper không thể mở cửa hàng");
        }
        if (storeRepository.findByOwnerId(userId).isPresent()) {
            throw new BusinessException("Bạn đã đăng ký cửa hàng rồi");
        }
        if (storeRepository.existsByNameIgnoreCase(form.getName().trim())) {
            throw new BusinessException("Tên cửa hàng đã được sử dụng");
        }
        storeRepository.save(Store.builder()
                .owner(user)
                .name(form.getName().trim())
                .description(form.getDescription())
                .address(form.getAddress().trim())
                .phone(form.getPhone().trim())
                .status(StoreStatus.PENDING)
                .commissionRate(defaultCommission)
                .build());
        userRepository.findByRoleAndLockedFalse(Role.ADMIN).forEach(admin ->
                notificationService.notify(admin, "Cửa hàng mới \"" + form.getName().trim() + "\" đang chờ duyệt", "/admin/stores?status=PENDING"));
    }

    @Override
    public void update(Long storeId, StoreForm form, MultipartFile logo, MultipartFile banner) {
        Store store = getById(storeId);
        String name = form.getName().trim();
        if (!name.equalsIgnoreCase(store.getName()) && storeRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessException("Tên cửa hàng đã được sử dụng");
        }
        store.setName(name);
        store.setDescription(form.getDescription());
        store.setAddress(form.getAddress().trim());
        store.setPhone(form.getPhone().trim());
        String logoUrl = storageService.upload(logo, "stores");
        if (logoUrl != null) store.setLogoUrl(logoUrl);
        String bannerUrl = storageService.upload(banner, "stores");
        if (bannerUrl != null) store.setBannerUrl(bannerUrl);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Store> search(String keyword, StoreStatus status, int page) {
        String kw = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return storeRepository.search(kw, status, PageRequest.of(Math.max(page, 0), 15, Sort.by(Sort.Direction.DESC, "id")));
    }

    @Override
    public void changeStatus(Long storeId, StoreStatus status) {
        Store store = getById(storeId);
        store.setStatus(status);
        User owner = store.getOwner();
        if (status == StoreStatus.ACTIVE && owner.getRole() == Role.USER) {
            owner.setRole(Role.VENDOR);
        }
        String msg = switch (status) {
            case ACTIVE -> "Cửa hàng \"" + store.getName() + "\" đã được duyệt. Bạn có thể bắt đầu bán sách ngay!";
            case BLOCKED -> "Cửa hàng \"" + store.getName() + "\" đã bị khóa. Liên hệ quản trị viên để biết thêm.";
            case PENDING -> "Cửa hàng \"" + store.getName() + "\" đang chờ duyệt lại.";
        };
        notificationService.notify(owner, msg, "/vendor");
    }

    @Override
    public void updateCommission(Long storeId, double rate) {
        if (rate < 0 || rate > 50) throw new BusinessException("Chiết khấu từ 0% đến 50%");
        getById(storeId).setCommissionRate(rate);
    }
}
