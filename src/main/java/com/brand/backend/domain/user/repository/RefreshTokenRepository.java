package com.brand.backend.domain.user.repository;

import com.brand.backend.domain.user.model.RefreshToken;
import com.brand.backend.domain.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    /**
     * Находит токен обновления по значению токена
     * @param token значение токена
     * @return Optional с токеном обновления или пустой Optional
     */
    Optional<RefreshToken> findByToken(String token);
    
    /**
     * Находит токен обновления по пользователю
     * @param user объект пользователя
     * @return Optional с токеном обновления или пустой Optional
     */
    Optional<RefreshToken> findByUser(User user);
    
    /**
     * Удаляет токен обновления по пользователю
     * @param user объект пользователя
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken r WHERE r.user = :user")
    void deleteByUser(User user);
    
    /**
     * Удаляет токен обновления по идентификатору пользователя
     * @param userId идентификатор пользователя
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken r WHERE r.user.id = :userId")
    void deleteByUserId(Long userId);
    
    /**
     * Атомарно обновляет токен и срок его действия
     * Этот метод гарантирует отсутствие race condition при высокой конкурентности
     * @param token новое значение токена
     * @param expiryDate новая дата истечения срока
     * @param userId идентификатор пользователя
     * @return количество обновленных записей
     */
    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken r SET r.token = :token, r.expiryDate = :expiryDate WHERE r.user.id = :userId")
    int updateTokenForUser(@Param("token") String token, @Param("expiryDate") Instant expiryDate, @Param("userId") Long userId);
    
    @Modifying
    @Query(value = "INSERT INTO refresh_tokens(user_id, token, expiry_date) VALUES (:userId, :token, :expiryDate)", 
           nativeQuery = true)
    void insertToken(@Param("userId") Long userId, @Param("token") String token, @Param("expiryDate") Instant expiryDate);
}

