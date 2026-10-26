package vn.hcmute.rebook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.hcmute.rebook.entity.OtpToken;
import vn.hcmute.rebook.entity.enums.OtpPurpose;

import java.util.Optional;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {
    Optional<OtpToken> findFirstByEmailAndPurposeAndUsedFalseOrderByIdDesc(String email, OtpPurpose purpose);
}
