package com.brand.backend.domain.promotion.repository;

import com.brand.backend.domain.promotion.model.PromoCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromoCodeRepository extends JpaRepository<PromoCode, Long> {
    
    Optional<PromoCode> findByCode(String code);
    
    List<PromoCode> findByActive(boolean active);
    
    List<PromoCode> findByEndDateAfter(LocalDateTime date);
    
    List<PromoCode> findByEndDateBefore(LocalDateTime date);
    
    List<PromoCode> findByStartDateBefore(LocalDateTime date);
    
    List<PromoCode> findByActiveAndStartDateBeforeAndEndDateAfter(
            boolean active, LocalDateTime startDate, LocalDateTime endDate);
} 