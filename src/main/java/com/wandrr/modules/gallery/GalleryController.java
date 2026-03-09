package com.wandrr.modules.gallery;

import com.wandrr.modules.gallery.dto.GalleryPhotoDTO;
import com.wandrr.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/gallery")
@RequiredArgsConstructor
public class GalleryController {

    private final GalleryRepository galleryRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<GalleryPhotoDTO>>> getPhotos(
            @RequestParam(required = false) UUID tripId,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(defaultValue = "6") int limit) {

        PageRequest pageable = PageRequest.of(0, limit, Sort.by("createdAt").descending());
        Page<GalleryPhoto> photos;

        if (tripId != null) {
            photos = galleryRepository.findByTripId(tripId, pageable);
        } else if (Boolean.TRUE.equals(featured)) {
            photos = galleryRepository.findByIsFeaturedTrue(pageable);
        } else {
            photos = galleryRepository.findAll(pageable);
        }

        List<GalleryPhotoDTO> dtos = photos.getContent().stream()
                .map(GalleryPhotoDTO::from).toList();
        return ResponseEntity.ok(ApiResponse.success("Gallery photos retrieved.", dtos));
    }
}
