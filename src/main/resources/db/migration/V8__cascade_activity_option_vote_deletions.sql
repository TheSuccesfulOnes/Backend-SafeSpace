ALTER TABLE activity_votes
    DROP FOREIGN KEY fk_vote_option,
    ADD CONSTRAINT fk_vote_option_delete
        FOREIGN KEY (option_id) REFERENCES activity_options(id) ON DELETE CASCADE;
