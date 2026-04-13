package com.memora.server.dto.segment;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 중간 기록 순서 변경 요청 DTO
 *
 * segmentId 목록을 원하는 순서대로 보내면
 * 순서(stepOrder)가 1, 2, 3... 으로 재설정됨
 *
 * 예시: { "segmentIds": [3, 1, 2] }
 * → 세그먼트3이 1번, 세그먼트1이 2번, 세그먼트2가 3번
 */
@Getter
@NoArgsConstructor
public class SegmentOrderRequest {

    private List<Long> segmentIds;
}
