package vn.hcmute.rebook.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.hcmute.rebook.dto.AddressForm;
import vn.hcmute.rebook.dto.ProfileForm;
import vn.hcmute.rebook.entity.Address;
import vn.hcmute.rebook.entity.User;
import vn.hcmute.rebook.entity.enums.Role;
import vn.hcmute.rebook.exception.BusinessException;
import vn.hcmute.rebook.exception.NotFoundException;
import vn.hcmute.rebook.repository.AddressRepository;
import vn.hcmute.rebook.repository.StoreRepository;
import vn.hcmute.rebook.repository.UserRepository;
import vn.hcmute.rebook.service.StorageService;
import vn.hcmute.rebook.service.UserService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {
    private static final int MAX_ADDRESSES = 10;

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final StoreRepository storeRepository;
    private final StorageService storageService;

    @Override
    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
    }

    @Override
    public void updateProfile(Long userId, ProfileForm form, MultipartFile avatar) {
        User user = getById(userId);
        user.setFullName(form.getFullName().trim());
        user.setPhone(form.getPhone().trim());
        String url = storageService.upload(avatar, "avatars");
        if (url != null) user.setAvatarUrl(url);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Address> getAddresses(Long userId) {
        return addressRepository.findByUserIdOrderByDefaultAddressDescIdDesc(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Address getAddress(Long userId, Long addressId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy địa chỉ"));
    }

    @Override
    public void saveAddress(Long userId, AddressForm form) {
        Address address;
        if (form.getId() != null) {
            address = getAddress(userId, form.getId());
        } else {
            if (addressRepository.countByUserId(userId) >= MAX_ADDRESSES) {
                throw new BusinessException("Bạn chỉ được lưu tối đa " + MAX_ADDRESSES + " địa chỉ");
            }
            address = new Address();
            address.setUser(getById(userId));
        }
        address.setReceiverName(form.getReceiverName().trim());
        address.setPhone(form.getPhone().trim());
        address.setDetail(form.getDetail().trim());
        address.setWard(form.getWard().trim());
        address.setDistrict(form.getDistrict().trim());
        address.setProvince(form.getProvince().trim());
        boolean first = addressRepository.countByUserId(userId) == 0;
        addressRepository.save(address);
        if (form.isDefaultAddress() || first) setDefaultAddress(userId, address.getId());
    }

    @Override
    public void deleteAddress(Long userId, Long addressId) {
        Address address = getAddress(userId, addressId);
        boolean wasDefault = address.isDefaultAddress();
        addressRepository.delete(address);
        addressRepository.flush();
        if (wasDefault) {
            getAddresses(userId).stream().findFirst().ifPresent(a -> a.setDefaultAddress(true));
        }
    }

    @Override
    public void setDefaultAddress(Long userId, Long addressId) {
        getAddress(userId, addressId);
        for (Address a : getAddresses(userId)) {
            a.setDefaultAddress(a.getId().equals(addressId));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> search(String keyword, Role role, int page) {
        String kw = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return userRepository.search(kw, role, PageRequest.of(Math.max(page, 0), 15, Sort.by(Sort.Direction.DESC, "id")));
    }

    @Override
    public void setLocked(Long userId, boolean locked, Long actorId) {
        if (userId.equals(actorId)) throw new BusinessException("Không thể tự khóa tài khoản của mình");
        User user = getById(userId);
        if (user.getRole() == Role.ADMIN) throw new BusinessException("Không thể khóa tài khoản quản trị viên");
        user.setLocked(locked);
    }

    @Override
    public void changeRole(Long userId, Role role, Long actorId) {
        if (userId.equals(actorId)) throw new BusinessException("Không thể tự đổi quyền của mình");
        User user = getById(userId);
        boolean ownsStore = storeRepository.findByOwnerId(userId).isPresent();
        if (role == Role.VENDOR && !ownsStore) {
            throw new BusinessException("Người dùng chưa có cửa hàng, không thể cấp quyền Vendor");
        }
        if (ownsStore && role != Role.VENDOR && user.getRole() == Role.VENDOR) {
            throw new BusinessException("Người dùng đang sở hữu cửa hàng, hãy khóa cửa hàng trước");
        }
        user.setRole(role);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getActiveShippers() {
        return userRepository.findByRoleAndLockedFalse(Role.SHIPPER);
    }
}
