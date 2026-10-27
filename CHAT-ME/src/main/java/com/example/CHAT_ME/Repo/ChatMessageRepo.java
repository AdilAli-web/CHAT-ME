package com.example.CHAT_ME.Repo;

import com.example.CHAT_ME.Entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.awt.print.Pageable;
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

    @Query(
            "select m from ChatMessage m where m.id in (" +
                    " select max(c.id) from ChatMessage c " +
                    " where (c.sender = :me or c.recipient = :me) " +
                    " group by c.sender, c.recipient" +
                    ") order by m.id desc"
    )
    List<ChatMessage> conversations( String me);
}
