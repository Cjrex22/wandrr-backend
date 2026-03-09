package com.wandrr.modules.gallery;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GalleryRepository extends JpaRepository<GalleryPhoto, UUID> {
    Page<GalleryPhoto> findByTripId(UUID tripId, Pageable pageable);

    Page<GalleryPhoto> findByIsFeaturedTrue(Pageable pageable);
}
