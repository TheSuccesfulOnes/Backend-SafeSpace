package com.experimentos.backend.mood.infrastructure;

import com.experimentos.backend.mood.domain.MoodEntry;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MoodEntryRepository extends JpaRepository<MoodEntry, Long> {
    Optional<MoodEntry> findByUserIdAndMoodDate(Long userId, LocalDate moodDate);

    List<MoodEntry> findByMoodDate(LocalDate moodDate);
}
