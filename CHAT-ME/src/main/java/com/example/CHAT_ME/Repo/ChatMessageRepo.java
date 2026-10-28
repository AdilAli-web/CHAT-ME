package com.example.CHAT_ME.Repo;

import com.example.CHAT_ME.Entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepo extends JpaRepository<ChatMessage,Long> {


    @Query("""
        SELECT m FROM ChatMessage m
        WHERE (
            (m.sender = :me AND m.recipient = :peer)
            OR
            (m.sender = :peer AND m.recipient = :me)
        )
        AND m.id < :before
        ORDER BY m.id DESC
        """)
    List<ChatMessage> history(String me, String peer, long before, Pageable page);


//    @Query("""
//    SELECT m FROM ChatMessage m
//    WHERE (m.sender = :me OR m.recipient = :me)
//    AND m.id IN (
//        SELECT MAX(c.id)
//        FROM ChatMessage c
//        WHERE c.sender = :me OR c.recipient = :me
//        GROUP BY c.sender, c.recipient
//    )
//    ORDER BY m.id DESC
//    """)
//    List<ChatMessage> conversations(@Param("me") String me);

    @Query("""
    SELECT m FROM ChatMessage m
    WHERE (m.sender = :me OR m.recipient = :me)
      AND m.id IN (
          SELECT MAX(c.id)
          FROM ChatMessage c
          WHERE (c.sender = :me OR c.recipient = :me)
          GROUP BY CASE
              WHEN c.sender = :me THEN c.recipient
              ELSE c.sender
          END
      )
    ORDER BY m.id DESC
    """)
    List<ChatMessage> conversations(@Param("me") String me);


}
