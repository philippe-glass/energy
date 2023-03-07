package com.sapereapi.api;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.sapereapi.model.Config;

@Repository
public interface ConfigRepository extends MongoRepository<Config, String>
{
	  Config findConfigByname(String name);

}
