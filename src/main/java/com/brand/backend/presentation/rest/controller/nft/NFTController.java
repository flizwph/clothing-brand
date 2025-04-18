package com.brand.backend.presentation.rest.controller.nft;

import com.brand.backend.presentation.dto.request.RevealRequest;
import com.brand.backend.presentation.dto.response.NFTDto;
import com.brand.backend.domain.nft.model.NFT;
import com.brand.backend.application.nft.service.NFTService;
import com.brand.backend.application.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/nfts")
@RequiredArgsConstructor
public class NFTController {

    private final NFTService nftService;
    private final UserService userService; // Предполагается, что через этот сервис получаем текущего пользователя

    /**
     * Эндпоинт для получения списка NFT для текущего пользователя.
     */
    @GetMapping("/me")
    public ResponseEntity<List<NFTDto>> getMyNFTs() {
        // Получаем текущего пользователя (например, через Spring Security)
        var currentUser = userService.getCurrentUser();
        List<NFT> nfts = nftService.getNFTsForUser(userService.getUserByUsername(currentUser.getUsername()));
        // Преобразуем в DTO для отдачи на фронтенд
        List<NFTDto> nftDtos = nfts.stream().map(nft -> new NFTDto(
                nft.getId(),
                nft.getPlaceholderUri(),
                nft.getRevealedUri(),
                nft.isRevealed(),
                nft.getRarity(),
                nft.getCreatedAt()
        )).collect(Collectors.toList());
        return ResponseEntity.ok(nftDtos);
    }

    /**
     * Эндпоинт для администратора для обновления (раскрытия) NFT.
     */
    @PutMapping("/{id}/reveal")
    public ResponseEntity<String> revealNFT(@PathVariable("id") Long nftId, @RequestBody RevealRequest request) {
        // Тут можно добавить проверку, что вызывающий – админ
        nftService.revealNFT(nftId, request.getRevealedUri());
        return ResponseEntity.ok("NFT успешно раскрыт");
    }
}