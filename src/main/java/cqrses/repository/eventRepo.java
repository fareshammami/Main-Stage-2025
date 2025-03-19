package cqrses.repository;

import cqrses.entity.event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface eventRepo extends JpaRepository<event, UUID> {

}
