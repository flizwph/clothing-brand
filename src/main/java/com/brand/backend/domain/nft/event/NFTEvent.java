package com.brand.backend.domain.nft.event;

import com.brand.backend.domain.nft.model.NFT;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class NFTEvent extends ApplicationEvent {

    private final NFT nft;
    private final NFTEventType eventType;

    public NFTEvent(Object source, NFT nft, NFTEventType eventType) {
        super(source);
        this.nft = nft;
        this.eventType = eventType;
    }

    public enum NFTEventType {
        CREATED,
        REVEALED,
        TRANSFERRED
    }
} 