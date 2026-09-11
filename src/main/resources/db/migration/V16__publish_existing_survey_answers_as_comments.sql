INSERT INTO comments (survey_id, user_id, parent_id, content, created_at)
SELECT answer.survey_id,
       answer.user_id,
       NULL,
       answer.answer_text,
       answer.created_at
FROM survey_answers answer
WHERE NOT EXISTS (
    SELECT 1
    FROM comments comment
    WHERE comment.survey_id = answer.survey_id
      AND comment.user_id = answer.user_id
      AND comment.parent_id IS NULL
      AND comment.content = answer.answer_text
      AND comment.created_at = answer.created_at
);
