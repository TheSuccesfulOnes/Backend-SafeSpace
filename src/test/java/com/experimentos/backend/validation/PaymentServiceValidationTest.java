package com.experimentos.backend.validation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.experimentos.backend.audit.application.*;
import com.experimentos.backend.iam.domain.*;
import com.experimentos.backend.iam.infrastructure.*;
import com.experimentos.backend.payment.application.*;
import com.experimentos.backend.payment.domain.*;
import com.experimentos.backend.payment.infrastructure.*;
import com.experimentos.backend.payment.interfaces.*;
import com.experimentos.backend.shared.interfaces.ApiExceptionHandler;
import com.experimentos.backend.shared.security.*;
import java.util.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

class PaymentServiceValidationTest extends ScenarioContract {
    static final byte[] PDF = "%PDF-1.7".getBytes(java.nio.charset.StandardCharsets.US_ASCII);

    static class Fixture {
        final PaymentRepository payments = mock(PaymentRepository.class);
        final UserRepository users = mock(UserRepository.class);
        final AuditService audit = mock(AuditService.class);
        final User actor = user(1, Role.SYSTEM_ADMIN);
        final PaymentService service = new PaymentService(payments, users, audit, 700000, CLOCK);
        final PaymentController controller = new PaymentController(service);

        Fixture() {
            authenticate(actor);
            when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("actor", "actor"))
                    .thenReturn(Optional.of(actor));
            when(payments.save(any())).thenAnswer(i -> id(i.getArgument(0), 1));
        }

        MockMultipartFile pdf() {
            return new MockMultipartFile("voucher", "x.pdf", "application/pdf", PDF);
        }

        PaymentDtos.PaymentResponse pay(String name, PaymentPlan plan, MultipartFile file) {
            return service.recordPayment(name, plan, file);
        }

        MvcResult multipart(String name, String plan, MockMultipartFile voucher, int status)
                throws Exception {
            var mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper()
                            .findAndRegisterModules()
                            .setPropertyNamingStrategy(
                                    com.fasterxml.jackson.databind.PropertyNamingStrategies
                                            .SNAKE_CASE)
                            .disable(
                                    com.fasterxml.jackson.databind.SerializationFeature
                                            .WRITE_DATES_AS_TIMESTAMPS);
            var mvc =
                    MockMvcBuilders.standaloneSetup(controller)
                            .setControllerAdvice(new ApiExceptionHandler())
                            .setMessageConverters(
                                    new org.springframework.http.converter.json
                                            .MappingJackson2HttpMessageConverter(mapper))
                            .build();
            var r =
                    mvc.perform(
                                    org.springframework.test.web.servlet.request
                                            .MockMvcRequestBuilders.multipart(
                                                    "/api/v1/admin/payments")
                                            .file(voucher)
                                            .param("beneficiary_name", name)
                                            .param("plan", plan)
                                            .accept(
                                                    org.springframework.http.MediaType
                                                            .APPLICATION_JSON))
                            .andReturn();
            assertThat(r.getResponse().getStatus()).isEqualTo(status);
            return r;
        }
    }

    @Override
    protected List<Scenario> scenarios() {
        return List.of(
                unit(
                        "beneficiary null",
                        () -> {
                            var f = new Fixture();
                            rejected(
                                    () -> f.pay(null, PaymentPlan.MONTHLY, f.pdf()), "beneficiary");
                        }),
                unit(
                        "beneficiary overflow",
                        () -> {
                            var f = new Fixture();
                            rejected(
                                    () -> f.pay("x".repeat(101), PaymentPlan.MONTHLY, f.pdf()),
                                    "too long");
                        }),
                unit(
                        "beneficiary hundred accepted trimmed",
                        () -> {
                            var f = new Fixture();
                            var r =
                                    f.pay(
                                            " " + "x".repeat(100) + " ",
                                            PaymentPlan.MONTHLY,
                                            f.pdf());
                            assertThat(r.beneficiaryName()).hasSize(100);
                        }),
                unit(
                        "plan absent",
                        () -> {
                            var f = new Fixture();
                            rejected(() -> f.pay("Person", null, f.pdf()), "plan");
                        }),
                unit(
                        "voucher absent",
                        () -> {
                            var f = new Fixture();
                            rejected(() -> f.pay("Person", PaymentPlan.MONTHLY, null), "voucher");
                        }),
                unit(
                        "voucher empty",
                        () -> {
                            var f = new Fixture();
                            rejected(
                                    () ->
                                            f.pay(
                                                    "Person",
                                                    PaymentPlan.MONTHLY,
                                                    new MockMultipartFile(
                                                            "voucher",
                                                            "x.pdf",
                                                            "application/pdf",
                                                            new byte[0])),
                                    "voucher");
                        }),
                unit(
                        "missing filename",
                        () -> {
                            var f = new Fixture();
                            rejected(
                                    () ->
                                            f.pay(
                                                    "Person",
                                                    PaymentPlan.MONTHLY,
                                                    new MockMultipartFile(
                                                            "voucher", "", "application/pdf", PDF)),
                                    "filename");
                        }),
                unit(
                        "filename traversal marker",
                        () -> {
                            var f = new Fixture();
                            rejected(
                                    () ->
                                            f.pay(
                                                    "Person",
                                                    PaymentPlan.MONTHLY,
                                                    new MockMultipartFile(
                                                            "voucher",
                                                            "evil..pdf",
                                                            "application/pdf",
                                                            PDF)),
                                    "filename");
                        }),
                unit(
                        "filename oversized",
                        () -> {
                            var f = new Fixture();
                            rejected(
                                    () ->
                                            f.pay(
                                                    "Person",
                                                    PaymentPlan.MONTHLY,
                                                    new MockMultipartFile(
                                                            "voucher",
                                                            "x".repeat(252) + ".pdf",
                                                            "application/pdf",
                                                            PDF)),
                                    "filename");
                        }),
                unit(
                        "wrong extension",
                        () -> {
                            var f = new Fixture();
                            rejected(
                                    () ->
                                            f.pay(
                                                    "Person",
                                                    PaymentPlan.MONTHLY,
                                                    new MockMultipartFile(
                                                            "voucher",
                                                            "x.exe",
                                                            "application/pdf",
                                                            PDF)),
                                    "PDF file");
                        }),
                unit(
                        "wrong mime",
                        () -> {
                            var f = new Fixture();
                            rejected(
                                    () ->
                                            f.pay(
                                                    "Person",
                                                    PaymentPlan.MONTHLY,
                                                    new MockMultipartFile(
                                                            "voucher", "x.pdf", "text/plain", PDF)),
                                    "PDF file");
                        }),
                unit(
                        "short signature",
                        () -> {
                            var f = new Fixture();
                            rejected(
                                    () ->
                                            f.pay(
                                                    "Person",
                                                    PaymentPlan.MONTHLY,
                                                    new MockMultipartFile(
                                                            "voucher",
                                                            "x.pdf",
                                                            "application/pdf",
                                                            "%PD".getBytes())),
                                    "valid PDF");
                        }),
                unit(
                        "unreadable voucher",
                        () -> {
                            var f = new Fixture();
                            MultipartFile v = mock(MultipartFile.class);
                            when(v.getOriginalFilename()).thenReturn("x.pdf");
                            when(v.getContentType()).thenReturn("application/pdf");
                            when(v.getSize()).thenReturn(8L);
                            when(v.getInputStream())
                                    .thenThrow(new java.io.IOException("synthetic"));
                            rejected(
                                    () -> f.pay("Person", PaymentPlan.MONTHLY, v),
                                    "could not be read");
                        }),
                unit(
                        "reported size cannot bypass actual length",
                        () -> {
                            var f = new Fixture();
                            MultipartFile v = mock(MultipartFile.class);
                            when(v.getOriginalFilename()).thenReturn("x.pdf");
                            when(v.getSize()).thenReturn(1L);
                            when(v.getInputStream())
                                    .thenReturn(new java.io.ByteArrayInputStream(new byte[700001]));
                            rejected(() -> f.pay("Person", PaymentPlan.MONTHLY, v), "valid PDF");
                        }),
                integration(
                        "monthly end of month renewal HTTP",
                        () -> {
                            var f = new Fixture();
                            var r = f.multipart("Person", "MONTHLY", f.pdf(), 201);
                            assertThat(r.getResponse().getContentAsString()).contains("2026-02-28");
                            verify(f.audit).record(f.actor, "RECORD_PAYMENT", "PAYMENT", "1");
                        }),
                integration(
                        "annual renewal HTTP",
                        () -> {
                            var f = new Fixture();
                            var r = f.multipart("Person", "ANNUAL", f.pdf(), 201);
                            assertThat(r.getResponse().getContentAsString()).contains("2027-01-31");
                        }),
                integration(
                        "Windows directory stripped HTTP",
                        () -> {
                            var f = new Fixture();
                            f.multipart(
                                    "Person",
                                    "MONTHLY",
                                    new MockMultipartFile(
                                            "voucher",
                                            "C:\\fakepath\\RECEIPT.PDF",
                                            "application/octet-stream",
                                            PDF),
                                    201);
                            verify(f.payments)
                                    .save(
                                            argThat(
                                                    p ->
                                                            p.getVoucherFilename()
                                                                    .equals("RECEIPT.PDF")));
                        }),
                integration(
                        "forged PDF blocked HTTP",
                        () -> {
                            var f = new Fixture();
                            f.multipart(
                                    "Person",
                                    "MONTHLY",
                                    new MockMultipartFile(
                                            "voucher",
                                            "x.pdf",
                                            "application/pdf",
                                            "MZ-executable".getBytes()),
                                    400);
                            verify(f.payments, never()).save(any());
                        }),
                integration(
                        "unknown plan blocked by binding HTTP",
                        () -> {
                            var f = new Fixture();
                            f.multipart("Person", "WEEKLY", f.pdf(), 400);
                            verifyNoInteractions(f.payments, f.audit);
                        }),
                integration(
                        "700000 byte boundary HTTP",
                        () -> {
                            var f = new Fixture();
                            byte[] data = new byte[700000];
                            System.arraycopy(PDF, 0, data, 0, PDF.length);
                            f.multipart(
                                    "Person",
                                    "MONTHLY",
                                    new MockMultipartFile(
                                            "voucher", "x.pdf", "application/pdf", data),
                                    201);
                            verify(f.payments).save(argThat(p -> p.getVoucherSize() == 700000));
                        }));
    }
}
