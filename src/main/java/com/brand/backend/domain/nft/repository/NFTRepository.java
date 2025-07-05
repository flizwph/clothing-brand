package com.brand.backend.domain.nft.repository;

import com.brand.backend.domain.nft.model.NFT;
import com.brand.backend.domain.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NFTRepository extends JpaRepository<NFT, Long> {
    List<NFT> findByUser(User user);
    
    /**
     * Подсчитать количество NFT пользователя
     * 
     * @param user пользователь
     * @return количество NFT
     */
    Long countByUser(User user);
}
