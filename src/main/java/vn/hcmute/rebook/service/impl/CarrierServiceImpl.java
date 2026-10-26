package vn.hcmute.rebook.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hcmute.rebook.dto.CarrierForm;
import vn.hcmute.rebook.entity.Carrier;
import vn.hcmute.rebook.exception.BusinessException;
import vn.hcmute.rebook.exception.NotFoundException;
import vn.hcmute.rebook.repository.CarrierRepository;
import vn.hcmute.rebook.service.CarrierService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CarrierServiceImpl implements CarrierService {
    private final CarrierRepository carrierRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Carrier> findAll() {
        return carrierRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Carrier> findActive() {
        return carrierRepository.findByActiveTrueOrderByFeeAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public Carrier getById(Long id) {
        return carrierRepository.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy nhà vận chuyển"));
    }

    @Override
    public void save(CarrierForm form) {
        Carrier carrier = form.getId() == null ? new Carrier() : getById(form.getId());
        String name = form.getName().trim();
        if (!name.equalsIgnoreCase(carrier.getName()) && carrierRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessException("Nhà vận chuyển đã tồn tại");
        }
        carrier.setName(name);
        carrier.setFee(form.getFee());
        carrier.setEstimatedTime(form.getEstimatedTime());
        carrier.setActive(form.isActive());
        carrierRepository.save(carrier);
    }

    @Override
    public void delete(Long id) {
        try {
            carrierRepository.delete(getById(id));
            carrierRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Nhà vận chuyển đã có đơn hàng, hãy chuyển sang ngừng hoạt động thay vì xóa");
        }
    }
}
