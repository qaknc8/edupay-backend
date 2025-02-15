package com.eliceteam8.edupay.payment.service;

import com.eliceteam8.edupay.bill.domain.Bill;
import com.eliceteam8.edupay.bill.domain.Status;
import com.eliceteam8.edupay.bill.repository.BillRepository;
import com.eliceteam8.edupay.global.exception.BillAlreadyPaidException;
import com.eliceteam8.edupay.global.exception.PaymentIncompleteException;
import com.eliceteam8.edupay.global.exception.PaymentMismatchException;
import com.eliceteam8.edupay.payment.dto.CallbackRequestDTO;
import com.eliceteam8.edupay.payment.dto.PaymentInfoDTO;
import com.eliceteam8.edupay.payment.entity.PaymentHistory;
import com.eliceteam8.edupay.payment.repository.PaymentHistoryRepository;
import com.eliceteam8.edupay.receipt.dto.ReceiptDto;
import com.eliceteam8.edupay.receipt.service.ReceiptService;
import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.exception.IamportResponseException;
import com.siot.IamportRestClient.response.IamportResponse;
import com.siot.IamportRestClient.response.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentHistoryRepository paymentHistoryRepository;
    private final BillRepository billRepository;
    private final IamportClient iamportClient;
    private final ReceiptService receiptService;

    public PaymentInfoDTO createPaymentInfoDTO(Long billId) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(NoSuchElementException::new);

        PaymentInfoDTO paymentInfoDTO = PaymentInfoDTO.builder()
                .billId(bill.getId())
                .billName(bill.getAcademy().getAcademyName())
                .amount(bill.getTotalPrice())
                .build();

        return paymentInfoDTO;
    }

    public void validatePayment(CallbackRequestDTO request) throws IamportResponseException, IOException {
        IamportResponse<Payment> iamportResponse = getIamportResponse(request);
        Bill bill = billRepository.findById(request.getBillId()).orElseThrow(NoSuchElementException::new);
        validatePaymentStatusAndPay(iamportResponse, bill);

        if (!Status.PAID.equals(bill.getStatus())) {
            bill.setStatusToPaid();
            billRepository.save(bill);
        } else {
            throw new BillAlreadyPaidException("결제 완료된 청구서입니다.");
        }

        savePaymentHistory(request.getImpUid(), request.getBillId());
        receiptService.createReceipt(bill);
    }

    private IamportResponse<Payment> getIamportResponse(CallbackRequestDTO request) throws IamportResponseException, IOException {
        IamportResponse<Payment> iamportResponse = iamportClient.paymentByImpUid(request.getImpUid());

        return iamportResponse;
    }

    private void validatePaymentStatusAndPay(IamportResponse<Payment> iamportResponse, Bill bill) {
        if (!iamportResponse.getResponse().getStatus().equals("paid")) {
            throw new PaymentIncompleteException("결제가 미완료 상태입니다.");
        }

        Long totalPrice = bill.getTotalPrice();
        Long portOnePrice = iamportResponse.getResponse().getAmount().longValue();

        if (!Objects.equals(portOnePrice, totalPrice)) {
            throw new PaymentMismatchException("결제금액이 불일치합니다.");
        }
    }

    public void savePaymentHistory(String paymentUid, Long billId) {
        PaymentHistory paymentHistory = PaymentHistory.builder()
                .paymentUid(paymentUid)
                .billId(billId)
                .createdAt(Instant.now())
                .build();
        paymentHistoryRepository.save(paymentHistory);
    }
}
