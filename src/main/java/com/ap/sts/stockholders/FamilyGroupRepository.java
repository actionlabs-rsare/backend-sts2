package com.ap.sts.stockholders;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FamilyGroupRepository extends JpaRepository<FamilyGroup, String> {

    @Query("select g from FamilyGroup g order by g.name")
    List<FamilyGroup> findAllOrdered();

    @Query(value = "select nextval('family_group_id_seq')", nativeQuery = true)
    long nextFamilyGroupIdSeq();
}
