package cqrses.repository;

import cqrses.entity.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface EventEntityRepository extends JpaRepository<EventEntity, UUID> {

}
