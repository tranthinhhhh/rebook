package vn.hcmute.rebook.service;

import vn.hcmute.rebook.dto.VoucherForm;
import vn.hcmute.rebook.entity.Voucher;

import java.util.Collection;
import java.util.List;

public interface VoucherService {
    List<Voucher> findSystemVouchers();

    List<Voucher> findStoreVouchers(Long storeId);

    List<Voucher> findApplicable(Collection<Long> storeIds);

    Voucher getById(Long id);

    /** storeId = null: mã toàn sàn */
    void save(VoucherForm form, Long storeId);

    void delete(Long id, Long storeId);

    VoucherForm toForm(Voucher v);
}
