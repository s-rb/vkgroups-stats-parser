package ru.list.surkovr;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface GroupStatsRepository extends JpaRepository<GroupStats, Integer> {

    GroupStats findFirstByGroupIdAndUpdatedTimeIsLessThan(int groupId, LocalDateTime time);
}
