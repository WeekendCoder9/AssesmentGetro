package com.trackingnumber.repository;

import com.trackingnumber.domain.TrackingNumberEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrackingNumberRepository extends ReactiveCrudRepository<TrackingNumberEntity, String> {
}
