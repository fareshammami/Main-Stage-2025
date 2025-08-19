package cqrses.projection;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserInduStateRepository extends MongoRepository<UserInduState, String> {

}
