package com.memora.server.repository;

import com.memora.server.entity.DiarySegment;
import com.memora.server.entity.SegmentPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SegmentPhotoRepository extends JpaRepository<SegmentPhoto, Integer> {

    List<SegmentPhoto> findBySegmentOrderByPhotoOrderAsc(DiarySegment segment);
}
