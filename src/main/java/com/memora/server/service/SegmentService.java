package com.memora.server.service;

import com.memora.server.dto.segment.PhotoMeta;
import com.memora.server.dto.segment.SegmentCreateRequest;
import com.memora.server.dto.segment.SegmentResponse;
import com.memora.server.dto.segment.SegmentUpdateRequest;
import com.memora.server.entity.Diary;
import com.memora.server.entity.DiarySegment;
import com.memora.server.entity.SegmentPhoto;
import com.memora.server.repository.DiaryRepository;
import com.memora.server.repository.DiarySegmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 중간 기록 관련 비즈니스 로직
 *
 * 중간 기록 추가/수정/삭제/순서 변경
 * 모든 작업에서 "본인 일기인지" 확인
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SegmentService {

    private final DiaryRepository diaryRepository;
    private final DiarySegmentRepository segmentRepository;

    /**
     * 중간 기록 추가
     *
     * 해당 일기에 속한 세그먼트 개수를 세서 다음 순서(stepOrder)를 자동 부여.
     * 사진은 두 가지 입력 경로를 모두 받는다:
     *   - photos 배열 (권장 / 다중) — 있으면 이쪽이 우선
     *   - 단일 photoUrl/takenAt/... (레거시) — photos가 비어있을 때만 단일 사진으로 처리
     */
    @Transactional
    public SegmentResponse createSegment(Integer userId, Integer diaryId, SegmentCreateRequest request) {
        Diary diary = findDiaryByIdAndUser(diaryId, userId);

        // 현재 세그먼트 개수로 다음 순서 계산
        List<DiarySegment> existing = segmentRepository.findByDiaryOrderByStepOrderAsc(diary);
        int nextOrder = existing.size() + 1;

        DiarySegment segment = DiarySegment.builder()
                .diary(diary)
                .stepOrder(nextOrder)
                .moodSnapshot(request.getMoodSnapshot())
                .userContent(request.getUserContent())
                .build();

        DiarySegment saved = segmentRepository.save(segment);

        // 사진 첨부 — photos 배열이 우선, 비어있으면 단일 photoUrl 경로 폴백
        List<PhotoMeta> photos = request.getPhotos();
        if (photos != null && !photos.isEmpty()) {
            int order = 1;
            for (PhotoMeta meta : photos) {
                if (meta.getPhotoUrl() == null || meta.getPhotoUrl().isBlank()) continue;
                SegmentPhoto photo = SegmentPhoto.builder()
                        .segment(saved)
                        .photoOrder(order++)
                        .photoUrl(meta.getPhotoUrl())
                        .takenAt(meta.getTakenAt())
                        .latitude(meta.getLatitude())
                        .longitude(meta.getLongitude())
                        .locationName(meta.getLocationName())
                        .build();
                saved.addPhoto(photo);
            }
        } else if (request.getPhotoUrl() != null && !request.getPhotoUrl().isBlank()) {
            SegmentPhoto photo = SegmentPhoto.builder()
                    .segment(saved)
                    .photoOrder(1)
                    .photoUrl(request.getPhotoUrl())
                    .takenAt(request.getTakenAt())
                    .latitude(request.getLatitude())
                    .longitude(request.getLongitude())
                    .locationName(request.getLocationName())
                    .build();
            saved.addPhoto(photo);
        }

        return SegmentResponse.from(saved);
    }

    /**
     * 특정 일기의 중간 기록 목록 조회 (순서대로)
     */
    public List<SegmentResponse> getSegments(Integer userId, Integer diaryId) {
        Diary diary = findDiaryByIdAndUser(diaryId, userId);

        return segmentRepository.findByDiaryOrderByStepOrderAsc(diary)
                .stream()
                .map(SegmentResponse::from)
                .toList();
    }

    /**
     * 중간 기록 수정
     *
     * 기분, 내용을 수정 가능
     * AI 초안을 확인 후 사용자가 수정할 때 사용
     */
    @Transactional
    public SegmentResponse updateSegment(Integer userId, Integer diaryId, Integer segmentId, SegmentUpdateRequest request) {
        DiarySegment segment = findSegmentByIdAndUser(segmentId, diaryId, userId);
        segment.update(request.getMoodSnapshot(), request.getUserContent());
        return SegmentResponse.from(segment);
    }

    /**
     * 중간 기록 삭제
     *
     * 삭제 후 남은 세그먼트들의 순서를 재정렬
     * 예: 1, 2, 3 중 2번 삭제 → 1, 2로 재정렬
     */
    @Transactional
    public void deleteSegment(Integer userId, Integer diaryId, Integer segmentId) {
        DiarySegment segment = findSegmentByIdAndUser(segmentId, diaryId, userId);
        Diary diary = segment.getDiary();

        segmentRepository.delete(segment);

        // 남은 세그먼트 순서 재정렬
        List<DiarySegment> remaining = segmentRepository.findByDiaryOrderByStepOrderAsc(diary);
        for (int i = 0; i < remaining.size(); i++) {
            remaining.get(i).updateStepOrder(i + 1);
        }
    }

    /**
     * 중간 기록 순서 변경
     *
     * 프론트에서 [3, 1, 2] 순서로 segmentId를 보내면
     * → 세그먼트3: stepOrder=1, 세그먼트1: stepOrder=2, 세그먼트2: stepOrder=3
     */
    @Transactional
    public List<SegmentResponse> reorderSegments(Integer userId, Integer diaryId, List<Integer> segmentIds) {
        Diary diary = findDiaryByIdAndUser(diaryId, userId);

        // 세그먼트 개수 검증
        List<DiarySegment> existing = segmentRepository.findByDiaryOrderByStepOrderAsc(diary);
        if (segmentIds.size() != existing.size()) {
            throw new IllegalArgumentException("순서 변경할 세그먼트 개수가 일치하지 않습니다.");
        }

        for (int i = 0; i < segmentIds.size(); i++) {
            DiarySegment segment = segmentRepository.findById(segmentIds.get(i))
                    .orElseThrow(() -> new IllegalArgumentException("중간 기록을 찾을 수 없습니다."));

            if (!segment.getDiary().getDiaryId().equals(diary.getDiaryId())) {
                throw new IllegalArgumentException("해당 일기의 중간 기록이 아닙니다.");
            }

            segment.updateStepOrder(i + 1);
        }

        return segmentRepository.findByDiaryOrderByStepOrderAsc(diary)
                .stream()
                .map(SegmentResponse::from)
                .toList();
    }

    /**
     * diaryId + userId로 일기 찾기 (본인 확인)
     */
    private Diary findDiaryByIdAndUser(Integer diaryId, Integer userId) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new IllegalArgumentException("일기를 찾을 수 없습니다."));

        if (!diary.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }
        return diary;
    }

    /**
     * segmentId + diaryId + userId로 세그먼트 찾기 (본인 확인)
     */
    private DiarySegment findSegmentByIdAndUser(Integer segmentId, Integer diaryId, Integer userId) {
        DiarySegment segment = segmentRepository.findById(segmentId)
                .orElseThrow(() -> new IllegalArgumentException("중간 기록을 찾을 수 없습니다."));

        if (!segment.getDiary().getDiaryId().equals(diaryId)) {
            throw new IllegalArgumentException("해당 일기의 중간 기록이 아닙니다.");
        }

        if (!segment.getDiary().getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }

        return segment;
    }
}
