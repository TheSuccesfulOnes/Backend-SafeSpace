package com.experimentos.backend.mood.infrastructure;

import com.experimentos.backend.mood.domain.MoodEntry;
import com.experimentos.backend.shared.infrastructure.firebase.repositories.AbstractFirestoreRepository;
import com.google.cloud.firestore.Firestore;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MoodEntryRepository extends AbstractFirestoreRepository<MoodEntry, Long> {
    public MoodEntryRepository(Firestore firestore) {
        super(firestore, MoodEntry.class, "mood_entries");
    }

    public Optional<MoodEntry> findByUserIdAndMoodDate(Long userId, LocalDate moodDate) {
        return readAll().stream()
                .filter(
                        entry ->
                                entry.getUser() != null
                                        && userId.equals(entry.getUser().getId())
                                        && moodDate.equals(entry.getMoodDate()))
                .findFirst();
    }

    public List<MoodEntry> findByMoodDate(LocalDate moodDate) {
        return readAll().stream().filter(entry -> moodDate.equals(entry.getMoodDate())).toList();
    }
}
