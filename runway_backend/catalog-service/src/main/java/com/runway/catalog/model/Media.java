package com.runway.catalog.model;

import com.runway.catalog.model.enums.MediaType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name ="media")
@Getter @Setter @NoArgsConstructor
public class Media {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "url_path", nullable = false)
    private String urlPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type")
    private MediaType mediaType = MediaType.IMAGE;

    @Column(name = "is_primary")
    private boolean isPrimary = false;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;
}

