package org.ki.cloud.poc.user.data;

import java.util.Optional;

import javax.transaction.Transactional;

import org.ki.cloud.poc.user.data.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 * 
 * @author Karthik Iyer
 *
 */
@Transactional
public interface UserRepository extends JpaRepository<UserEntity, Long> {

	Optional<UserEntity> findByUsername(String u);

}