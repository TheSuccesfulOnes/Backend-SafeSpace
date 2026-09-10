package com.experimentos.backend.payment.interfaces;

import com.experimentos.backend.payment.application.PaymentService;
import com.experimentos.backend.payment.domain.PaymentPlan;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/payments")
public class PaymentController {
    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @GetMapping("/plans")
    public List<PaymentDtos.PlanSummary> plans() {
        return service.listPlans();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentDtos.PaymentResponse recordPayment(
            @RequestParam("beneficiary_name") String beneficiaryName,
            @RequestParam("plan") PaymentPlan plan,
            @RequestPart("voucher") MultipartFile voucher) {
        return service.recordPayment(beneficiaryName, plan, voucher);
    }
}
