package com.b301.knpl.repository;

import com.b301.knpl.dto.TeamDto;
import com.b301.knpl.entity.Team;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamRepository extends MongoRepository<Team, String> {
}
