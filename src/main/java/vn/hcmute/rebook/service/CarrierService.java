package vn.hcmute.rebook.service;

import vn.hcmute.rebook.dto.CarrierForm;
import vn.hcmute.rebook.entity.Carrier;

import java.util.List;

public interface CarrierService {
    List<Carrier> findAll();

    List<Carrier> findActive();

    Carrier getById(Long id);

    void save(CarrierForm form);

    void delete(Long id);
}
