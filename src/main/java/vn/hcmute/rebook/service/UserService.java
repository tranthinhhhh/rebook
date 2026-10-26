package vn.hcmute.rebook.service;

import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;
import vn.hcmute.rebook.dto.AddressForm;
import vn.hcmute.rebook.dto.ProfileForm;
import vn.hcmute.rebook.entity.Address;
import vn.hcmute.rebook.entity.User;
import vn.hcmute.rebook.entity.enums.Role;

import java.util.List;

public interface UserService {
    User getById(Long id);

    void updateProfile(Long userId, ProfileForm form, MultipartFile avatar);

    // ===== Địa chỉ nhận hàng =====
    List<Address> getAddresses(Long userId);

    Address getAddress(Long userId, Long addressId);

    void saveAddress(Long userId, AddressForm form);

    void deleteAddress(Long userId, Long addressId);

    void setDefaultAddress(Long userId, Long addressId);

    // ===== Quản trị =====
    Page<User> search(String keyword, Role role, int page);

    void setLocked(Long userId, boolean locked, Long actorId);

    void changeRole(Long userId, Role role, Long actorId);

    List<User> getActiveShippers();
}
