package com.ap.sts.approvals;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {

    List<ApprovalRequest> findByTransactionIdOrderByLevelAsc(String transactionId);

    Optional<ApprovalRequest> findFirstByTransactionIdAndDecisionOrderByLevelAsc(
            String transactionId, ApprovalRequest.Decision decision);
}
