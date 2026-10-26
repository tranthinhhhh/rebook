package vn.hcmute.rebook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.hcmute.rebook.entity.ChatMessage;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByStoreIdAndCustomerIdOrderByCreatedAtAsc(Long storeId, Long customerId);

    /** Tin nhắn mới nhất của mỗi cuộc hội thoại của shop */
    @Query("""
            select m from ChatMessage m where m.store.id = :storeId and m.id in (
                select max(m2.id) from ChatMessage m2 where m2.store.id = :storeId group by m2.customer.id)
            order by m.id desc
            """)
    List<ChatMessage> findLatestPerCustomer(@Param("storeId") Long storeId);

    /** Tin nhắn mới nhất của mỗi cuộc hội thoại của khách */
    @Query("""
            select m from ChatMessage m where m.customer.id = :customerId and m.id in (
                select max(m2.id) from ChatMessage m2 where m2.customer.id = :customerId group by m2.store.id)
            order by m.id desc
            """)
    List<ChatMessage> findLatestPerStore(@Param("customerId") Long customerId);

    @Modifying
    @Query("update ChatMessage m set m.readFlag = true where m.store.id = :storeId and m.customer.id = :customerId and m.sender.id <> :readerId")
    void markRead(@Param("storeId") Long storeId, @Param("customerId") Long customerId, @Param("readerId") Long readerId);
}
