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
    private User user; // 작성자 연결

    @Column(nullable = false)
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    private MoodType finalMood;

    @Column(columnDefinition = "TEXT")
    private String finalContent;

    @Builder.Default
    private Boolean isEdited = false;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiaryStatus status = DiaryStatus.IN_PROGRESS;

    @Builder.Default // 빌더를 사용할 때 기본값(now)을 쓰도록 강제합니다.
    @Column(nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}