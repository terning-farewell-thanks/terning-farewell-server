package com.terning.farewell_server.event.dto.response;

import lombok.Builder;

@Builder
public record SetStockResponse(String message, int stock) {
    public static SetStockResponse from(int stock) {
        return SetStockResponse.builder()
                .message("이벤트 재고가 " + stock + "개로 설정되었습니다.")
                .stock(stock)
                .build();
    }
}
