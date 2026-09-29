package com.runway.engagement.reviews;

import com.runway.engagement.reviews.controller.RatingController;
import com.runway.engagement.reviews.dto.RatingRequest;
import com.runway.engagement.reviews.model.Rating;
import com.runway.engagement.reviews.service.RatingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RatingControllerTest {

    @Mock
    private RatingService ratingService;

    @InjectMocks
    private RatingController controller;

    // -----------------------------------------
    // POST /products/{productId}/ratings
    // -----------------------------------------
    @Test
    void createRating_returnsCreated() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(10L);

        RatingRequest request = new RatingRequest();
        request.setRating(5);

        Rating rating = new Rating();
        rating.setProductId(99L);
        rating.setUserId(10L);
        rating.setRating(5);

        when(ratingService.createRating(99L, 10L, 5))
                .thenReturn(rating);

        var response = controller.createRating(99L, request, auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getRating()).isEqualTo(5);

        verify(ratingService).createRating(99L, 10L, 5);
    }

    // -----------------------------------------
    // PUT /products/{productId}/ratings
    // -----------------------------------------
    @Test
    void updateRating_returnsUpdated() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(10L);

        RatingRequest request = new RatingRequest();
        request.setRating(4);

        Rating rating = new Rating();
        rating.setRating(4);

        when(ratingService.updateRating(99L, 10L, 4))
                .thenReturn(rating);

        var response = controller.updateRating(99L, request, auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getRating()).isEqualTo(4);

        verify(ratingService).updateRating(99L, 10L, 4);
    }

    // -----------------------------------------
    // DELETE /products/{productId}/ratings (ADMIN)
    // -----------------------------------------
    @Test
    void deleteRating_admin() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(10L);

        Collection<GrantedAuthority> authorities =
                List.of((GrantedAuthority) () -> "ROLE_ADMIN");
        doReturn(authorities).when(auth).getAuthorities();

        var response = controller.deleteRating(99L, auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(ratingService).deleteRating(99L, 10L, true);
    }

    // -----------------------------------------
    // DELETE /products/{productId}/ratings (CUSTOMER)
    // -----------------------------------------
    @Test
    void deleteRating_customer() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(10L);

        Collection<GrantedAuthority> authorities =
                List.of((GrantedAuthority) () -> "ROLE_CUSTOMER");
        doReturn(authorities).when(auth).getAuthorities();

        var response = controller.deleteRating(99L, auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(ratingService).deleteRating(99L, 10L, false);
    }

    // -----------------------------------------
    // GET /products/{productId}/ratings?mine=true
    // -----------------------------------------
    @Test
    void getRatingsForProduct_mineTrue() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(10L);

        Rating rating = new Rating();
        rating.setRating(4);

        when(ratingService.getUserRatingForProduct(99L, 10L))
                .thenReturn(rating);

        var response = controller.getRatingsForProduct(99L, true, auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getRating()).isEqualTo(4);

        verify(ratingService).getUserRatingForProduct(99L, 10L);
    }

    // -----------------------------------------
    // GET /products/{productId}/ratings (PUBLIC)
    // -----------------------------------------
    @Test
    void getRatingsForProduct_all() {
        when(ratingService.getRatingsByProductId(99L))
                .thenReturn(List.of(new Rating()));

        var response = controller.getRatingsForProduct(99L, false, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);

        verify(ratingService).getRatingsByProductId(99L);
    }

    // -----------------------------------------
    // GET /products/{productId}/ratings/average
    // -----------------------------------------
    @Test
    void getAverageRating_returnsValue() {
        when(ratingService.getAverageRatingByProductId(99L))
                .thenReturn(4.5);

        var response = controller.getAverageRating(99L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(4.5);

        verify(ratingService).getAverageRatingByProductId(99L);
    }

    // -----------------------------------------
    // GET /ratings?mine=true
    // -----------------------------------------
    @Test
    void getRatings_mineTrue() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(10L);

        when(ratingService.getUserRatings(10L))
                .thenReturn(List.of(new Rating()));

        var response = controller.getRatings(true, auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);

        verify(ratingService).getUserRatings(10L);
    }

    // -----------------------------------------
    // GET /ratings (ADMIN)
    // -----------------------------------------
    @Test
    void getRatings_all() {
        when(ratingService.getAllRatings())
                .thenReturn(List.of(new Rating(), new Rating()));

        var response = controller.getRatings(false, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);

        verify(ratingService).getAllRatings();
    }
}