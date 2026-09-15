package com.experimentos.backend.payment.infrastructure;

import com.experimentos.backend.payment.domain.Payment;
import com.experimentos.backend.shared.infrastructure.firebase.repositories.AbstractFirestoreRepository;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentRepository extends AbstractFirestoreRepository<Payment, Long> {
    public PaymentRepository(Firestore firestore) {
        super(firestore, Payment.class, "payments");
    }
}
