ALTER TABLE user_preferences
    DROP FOREIGN KEY fk_preferences_user,
    ADD CONSTRAINT fk_preferences_user_delete
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE mood_entries
    DROP FOREIGN KEY fk_mood_user,
    ADD CONSTRAINT fk_mood_user_delete
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE survey_answers
    DROP FOREIGN KEY fk_answer_survey,
    ADD CONSTRAINT fk_answer_survey_delete
        FOREIGN KEY (survey_id) REFERENCES surveys(id) ON DELETE CASCADE,
    DROP FOREIGN KEY fk_answer_user,
    ADD CONSTRAINT fk_answer_user_delete
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE comments
    DROP FOREIGN KEY fk_comment_survey,
    ADD CONSTRAINT fk_comment_survey_delete
        FOREIGN KEY (survey_id) REFERENCES surveys(id) ON DELETE CASCADE,
    DROP FOREIGN KEY fk_comment_user,
    ADD CONSTRAINT fk_comment_user_delete
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    DROP FOREIGN KEY fk_comment_parent,
    ADD CONSTRAINT fk_comment_parent_delete
        FOREIGN KEY (parent_id) REFERENCES comments(id) ON DELETE CASCADE;

ALTER TABLE comment_likes
    DROP FOREIGN KEY fk_like_comment,
    ADD CONSTRAINT fk_like_comment_delete
        FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE,
    DROP FOREIGN KEY fk_like_user,
    ADD CONSTRAINT fk_like_user_delete
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE activity_options
    DROP FOREIGN KEY fk_option_activity,
    ADD CONSTRAINT fk_option_activity_delete
        FOREIGN KEY (activity_id) REFERENCES weekly_activities(id) ON DELETE CASCADE;

ALTER TABLE activity_votes
    DROP FOREIGN KEY fk_vote_activity,
    ADD CONSTRAINT fk_vote_activity_delete
        FOREIGN KEY (activity_id) REFERENCES weekly_activities(id) ON DELETE CASCADE,
    DROP FOREIGN KEY fk_vote_user,
    ADD CONSTRAINT fk_vote_user_delete
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE ai_conversations
    DROP FOREIGN KEY fk_conversation_user,
    ADD CONSTRAINT fk_conversation_user_delete
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE ai_messages
    DROP FOREIGN KEY fk_message_conversation,
    ADD CONSTRAINT fk_message_conversation_delete
        FOREIGN KEY (conversation_id) REFERENCES ai_conversations(id) ON DELETE CASCADE;

ALTER TABLE reports
    DROP FOREIGN KEY fk_report_user,
    ADD CONSTRAINT fk_report_user_delete
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE audit_logs
    DROP FOREIGN KEY fk_audit_actor,
    ADD CONSTRAINT fk_audit_actor_delete
        FOREIGN KEY (actor_user_id) REFERENCES users(id) ON DELETE SET NULL;
