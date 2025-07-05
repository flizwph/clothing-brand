package com.brand.backend.presentation.rest.controller.nft;

import com.brand.backend.presentation.dto.request.RevealRequest;
import com.brand.backend.presentation.dto.response.NFTDto;
import com.brand.backend.domain.nft.model.NFT;
import com.brand.backend.application.nft.service.NFTService;
import com.brand.backend.application.user.service.UserService;
import com.brand.backend.domain.user.model.User;
import com.brand.backend.presentation.dto.response.NFTCollectionDTO;
import com.brand.backend.presentation.dto.response.NFTCollectionStatsDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/nfts")
@RequiredArgsConstructor
@Tag(name = "NFT Коллекция", description = "API для управления NFT коллекцией")
public class NFTController {

    private final NFTService nftService;
    private final UserService userService; // Предполагается, что через этот сервис получаем текущего пользователя

    /**
     * Эндпоинт для получения списка NFT для текущего пользователя.
     */
    @Operation(summary = "Получение своих NFT", description = "Возвращает список NFT текущего пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список NFT успешно получен"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован")
    })
    @GetMapping("/me")
    public ResponseEntity<List<NFTDto>> getMyNFTs(@AuthenticationPrincipal User user) {
        if (user == null) {
            log.error("Пользователь не найден в контексте безопасности");
            return ResponseEntity.notFound().build();
        }
        
        List<NFT> nfts = nftService.getNFTsForUser(user);
        // Преобразуем в DTO для отдачи на фронтенд
        List<NFTDto> nftDtos = nfts.stream().map(nft -> new NFTDto(
                nft.getId(),
                nft.getPlaceholderUri(),
                nft.getRevealedUri(),
                nft.isRevealed(),
                nft.getRarity() != null ? nft.getRarity().name() : null,
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

    @Operation(summary = "Получение NFT коллекции", description = "Возвращает все NFT пользователя с возможностью фильтрации")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Коллекция NFT успешно получена"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован")
    })
    @GetMapping("/collection")
    public ResponseEntity<List<NFTCollectionDTO>> getNFTCollection(
            @AuthenticationPrincipal User user,
            @Parameter(description = "Фильтр по редкости") @RequestParam(required = false) String rarity,
            @Parameter(description = "Фильтр по блокчейну") @RequestParam(required = false) String blockchain,
            @Parameter(description = "Фильтр по статусу") @RequestParam(required = false) String status,
            @Parameter(description = "Поиск по названию или коллекции") @RequestParam(required = false) String search
    ) {
        if (user == null) {
            log.error("Пользователь не найден в контексте безопасности");
            return ResponseEntity.notFound().build();
        }
        
        try {
            log.debug("Запрос NFT коллекции для пользователя: {}", user.getUsername());
        List<NFTCollectionDTO> collection = nftService.getUserNFTCollection(
                user.getId(), rarity, blockchain, status, search);
            log.debug("Найдено {} NFT для пользователя {}", collection.size(), user.getUsername());
        return ResponseEntity.ok(collection);
        } catch (Exception e) {
            log.error("Ошибка при получении NFT коллекции для пользователя {}: {}", user.getUsername(), e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Статистика NFT коллекции", description = "Возвращает статистику по NFT коллекции пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Статистика успешно получена"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован")
    })
    @GetMapping("/stats")
    public ResponseEntity<NFTCollectionStatsDTO> getNFTStats(@AuthenticationPrincipal User user) {
        if (user == null) {
            log.error("Пользователь не найден в контексте безопасности");
            return ResponseEntity.notFound().build();
        }
        
        try {
            log.debug("Запрос статистики NFT для пользователя: {}", user.getUsername());
        NFTCollectionStatsDTO stats = nftService.getUserNFTStats(user.getId());
        return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Ошибка при получении статистики NFT для пользователя {}: {}", user.getUsername(), e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Детали NFT", description = "Получение подробной информации об NFT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Детали NFT получены"),
            @ApiResponse(responseCode = "404", description = "NFT не найден"),
            @ApiResponse(responseCode = "403", description = "Нет доступа к NFT")
    })
    @GetMapping("/{id}")
    public ResponseEntity<NFTCollectionDTO> getNFTDetails(
            @AuthenticationPrincipal User user,
            @Parameter(description = "ID NFT") @PathVariable Long id
    ) {
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        
        NFTCollectionDTO nft = nftService.getNFTDetails(user.getId(), id);
        return ResponseEntity.ok(nft);
    }

    @Operation(summary = "Выставить NFT на продажу", description = "Выставляет NFT на продажу")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "NFT выставлен на продажу"),
            @ApiResponse(responseCode = "404", description = "NFT не найден"),
            @ApiResponse(responseCode = "403", description = "Нет доступа к NFT")
    })
    @PostMapping("/{id}/sell")
    public ResponseEntity<Map<String, Object>> sellNFT(
            @AuthenticationPrincipal User user,
            @Parameter(description = "ID NFT") @PathVariable Long id
    ) {
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> result = nftService.sellNFT(user.getId(), id);
        return ResponseEntity.ok(result);
    }
}