package vn.hcmute.rebook.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hcmute.rebook.dto.VoucherForm;
import vn.hcmute.rebook.entity.Voucher;
import vn.hcmute.rebook.exception.BusinessException;
import vn.hcmute.rebook.exception.NotFoundException;
import vn.hcmute.rebook.repository.StoreRepository;
import vn.hcmute.rebook.repository.VoucherRepository;
import vn.hcmute.rebook.service.VoucherService;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class VoucherServiceImpl implements VoucherService {
    private final VoucherRepository voucherRepository;
    private final StoreRepository storeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Voucher> findSystemVouchers() {
        return voucherRepository.findByStoreIsNullOrderByIdDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Voucher> findStoreVouchers(Long storeId) {
        return voucherRepository.findByStoreIdOrderByIdDesc(storeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Voucher> findApplicable(Collection<Long> storeIds) {
        if (storeIds.isEmpty()) storeIds = List.of(-1L);
        return voucherRepository.findApplicable(storeIds).stream().filter(Voucher::isUsable).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Voucher getById(Long id) {
        return voucherRepository.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy mã giảm giá"));
    }

    @Override
    public void save(VoucherForm form, Long storeId) {
        Voucher v;
        if (form.getId() != null) {
            v = storeId == null ? getById(form.getId())
                    : voucherRepository.findByIdAndStoreId(form.getId(), storeId)
                        .orElseThrow(() -> new NotFoundException("Không tìm thấy mã giảm giá"));
            if (storeId == null && v.getStore() != null) throw new BusinessException("Đây là mã của shop");
        } else {
            v = new Voucher();
            if (storeId != null) v.setStore(storeRepository.getReferenceById(storeId));
        }
        String code = form.getCode().trim().toUpperCase();
        if (!code.equalsIgnoreCase(v.getCode()) && voucherRepository.existsByCodeIgnoreCase(code)) {
            throw new BusinessException("Mã " + code + " đã tồn tại");
        }
        if (form.getQuantity() < v.getUsedCount()) {
            throw new BusinessException("Số lượng không được nhỏ hơn số lượt đã dùng (" + v.getUsedCount() + ")");
        }
        v.setCode(code);
        v.setName(form.getName().trim());
        v.setType(form.getType());
        v.setPercent(form.getPercent());
        v.setMaxDiscount(form.getMaxDiscount());
        v.setMinOrder(form.getMinOrder());
        v.setStartAt(form.getStartAt());
        v.setEndAt(form.getEndAt());
        v.setQuantity(form.getQuantity());
        v.setActive(form.isActive());
        voucherRepository.save(v);
    }

    @Override
    public void delete(Long id, Long storeId) {
        Voucher v = storeId == null ? getById(id)
                : voucherRepository.findByIdAndStoreId(id, storeId).orElseThrow(() -> new NotFoundException("Không tìm thấy mã"));
        if (v.getUsedCount() > 0) {
            // Mã đã được dùng: chỉ tắt để giữ lịch sử đơn hàng
            v.setActive(false);
        } else {
            voucherRepository.delete(v);
        }
    }

    @Override
    public VoucherForm toForm(Voucher v) {
        VoucherForm f = new VoucherForm();
        f.setId(v.getId());
        f.setCode(v.getCode());
        f.setName(v.getName());
        f.setType(v.getType());
        f.setPercent(v.getPercent());
        f.setMaxDiscount(v.getMaxDiscount());
        f.setMinOrder(v.getMinOrder());
        f.setStartAt(v.getStartAt());
        f.setEndAt(v.getEndAt());
        f.setQuantity(v.getQuantity());
        f.setActive(v.isActive());
        return f;
    }
}
