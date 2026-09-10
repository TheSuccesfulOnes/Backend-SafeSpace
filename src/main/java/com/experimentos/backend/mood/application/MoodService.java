package com.experimentos.backend.mood.application;

import com.experimentos.backend.iam.domain.User;
import com.experimentos.backend.iam.infrastructure.UserRepository;
import com.experimentos.backend.mood.domain.MoodEntry;
import com.experimentos.backend.mood.infrastructure.MoodEntryRepository;
import com.experimentos.backend.mood.interfaces.MoodDtos;
import com.experimentos.backend.shared.security.CurrentUser;
import com.experimentos.backend.shared.security.Role;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MoodService {
    private final UserRepository users;
    private final MoodEntryRepository moods;

    public MoodService(UserRepository users, MoodEntryRepository moods) {
        this.users = users;
        this.moods = moods;
    }

    @Transactional
    public MoodDtos.MoodResponse submit(MoodDtos.SubmitMoodRequest request) {
        User user = currentUser();
        LocalDate today = LocalDate.now();
        if (moods.findByUserIdAndMoodDate(user.getId(), today).isPresent())
            throw new IllegalArgumentException("Mood has already been submitted for today");
        MoodEntry entry = moods.save(new MoodEntry(user, request.mood(), today));
        return new MoodDtos.MoodResponse(entry.getMood(), entry.getMoodDate());
    }

    @Transactional(readOnly = true)
    public MoodDtos.MoodResponse today() {
        User user = currentUser();
        return moods.findByUserIdAndMoodDate(user.getId(), LocalDate.now())
                .map(entry -> new MoodDtos.MoodResponse(entry.getMood(), entry.getMoodDate()))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public MoodDtos.MoodSummary summary(LocalDate date) {
        Map<com.experimentos.backend.mood.domain.Mood, Long> distribution =
                moods.findByMoodDate(date).stream()
                        .map(MoodEntry::getMood)
                        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        Arrays.stream(com.experimentos.backend.mood.domain.Mood.values())
                .forEach(mood -> distribution.putIfAbsent(mood, 0L));
        long totalResponses = distribution.values().stream().mapToLong(Long::longValue).sum();
        long activeEmployees = users.countByRoleAndEnabledTrue(Role.EMPLOYEE);
        int responseRate = calculateResponseRate(totalResponses, activeEmployees);
        return new MoodDtos.MoodSummary(
                date, totalResponses, distribution, activeEmployees, responseRate);
    }

    private int calculateResponseRate(long totalResponses, long activeEmployees) {
        if (activeEmployees == 0) return 0;
        return (int) Math.min(100L, Math.round(totalResponses * 100.0 / activeEmployees));
    }

    private User currentUser() {
        return users.findByUsernameIgnoreCaseOrEmailIgnoreCase(
                        CurrentUser.username(), CurrentUser.username())
                .orElseThrow(
                        () -> new IllegalArgumentException("Authenticated user was not found"));
    }
}
