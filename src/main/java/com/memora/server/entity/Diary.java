package com.memora.server.entity;

import com.memora.server.entity.enums.DiaryStatus;
import com.memora.server.entity.enums.MoodType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "diaries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Diary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long diaryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    private MoodType finalMood;

    @Column(columnDefinition = "TEXT")
    private String aiDraft;

    @Column(columnDefinition = "TEXT")
    private String finalContent;

    @Builder.Default
    private Boolean isEdited = false;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiaryStatus status = DiaryStatus.IN_PROGRESS;

    @Builder.Default
    @Column(updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Builder.Default
    @Column(nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    /**
     * 최종 일기 수정
     * 최종 기분, 내용을 업데이트하고 수정 시간 갱신
     */
    public void updateFinal(MoodType finalMood, String finalContent) {
        if (finalMood != null) this.finalMood = finalMood;
        if (finalContent != null) {
            this.finalContent = finalContent;
            this.isEdited = true;  // 사용자가 내용을 수정했다는 표시
        }
        this.updatedAt = OffsetDateTime.now();
    }

    /**
     * AI 초안 저장
     */
    public void updateAiDraft(String aiDraft) {
        this.aiDraft = aiDraft;
        this.updatedAt = OffsetDateTime.now();
    }

    /**
     * 일기 완료 처리
     * IN_PROGRESS → COMPLETED
     */
    public void complete() {
        this.status = DiaryStatus.COMPLETED;
        this.updatedAt = OffsetDateTime.now();
    }
}
