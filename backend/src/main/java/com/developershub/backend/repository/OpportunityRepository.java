package com.developershub.backend.repository;

import com.developershub.backend.entity.Opportunity;
import com.developershub.backend.entity.OpportunityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OpportunityRepository extends JpaRepository<Opportunity, Long>, JpaSpecificationExecutor<Opportunity> {

    Optional<Opportunity> findByIdAndStatus(Long id, OpportunityStatus status);
}
