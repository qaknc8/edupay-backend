package com.eliceteam8.edupay.bill.service;

import com.eliceteam8.edupay.academy_management.entity.Academy;
import com.eliceteam8.edupay.academy_management.entity.AcademyStudent;
import com.eliceteam8.edupay.academy_management.entity.Lecture;
import com.eliceteam8.edupay.academy_management.repository.AcademyStudentRepository;
import com.eliceteam8.edupay.bill.domain.Bill;
import com.eliceteam8.edupay.bill.domain.BillLog;
import com.eliceteam8.edupay.bill.domain.Status;
import com.eliceteam8.edupay.bill.dto.request.CreateMultipleBillsRequest;
import com.eliceteam8.edupay.bill.dto.request.CreateSingleBillRequest;
import com.eliceteam8.edupay.bill.dto.response.BillDetailResponse;
import com.eliceteam8.edupay.bill.dto.response.BillInfoResponse;
import com.eliceteam8.edupay.bill.dto.response.BillLogResponse;
import com.eliceteam8.edupay.bill.repository.BillLogRepository;
import com.eliceteam8.edupay.bill.repository.BillRepository;
import com.eliceteam8.edupay.global.exception.EntityNotFoundException;
import com.eliceteam8.edupay.global.exception.MessageTooLongException;
import com.eliceteam8.edupay.global.exception.NotEnoughPointsException;
import com.eliceteam8.edupay.user.dto.UserDTO;
import com.eliceteam8.edupay.user.entity.User;
import com.eliceteam8.edupay.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BillService {
    private static final int MAX_MESSAGE_LENGTH = 255;
    private static final String DEFAULT_MESSAGE = "청구서를 기한 내에 납부해 주세요.";

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private BillLogRepository billLogRepository;

    @Autowired
    private AcademyStudentRepository academyStudentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Transactional
    public BillInfoResponse createBill(CreateSingleBillRequest request) {
        User currentUser = getCurrentUser();

        if (currentUser.getPoint() <= 0) {
            throw new NotEnoughPointsException("포인트가 부족합니다.", "NOT_ENOUGH_POINTS");
        }

        if (request.getMessage().length() > MAX_MESSAGE_LENGTH) {
            throw new MessageTooLongException("메시지가 허용된 최대 길이를 초과했습니다.", "MESSAGE_TOO_LONG");
        }

        AcademyStudent student = academyStudentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new EntityNotFoundException("ID가 " + request.getStudentId() + "인 학생을 찾을 수 없습니다."));

        Academy academy = student.getAcademy();
        if (academy == null) {
            throw new EntityNotFoundException("ID가 " + request.getStudentId() + "인 학생의 학원을 찾을 수 없습니다.");
        }

        List<String> lectureNames = student.getLectures().stream()
                .map(Lecture::getLectureName)
                .collect(Collectors.toList());

        if (lectureNames.isEmpty()) {
            throw new EntityNotFoundException("ID가 " + request.getStudentId() + "인 학생의 강의를 찾을 수 없습니다.");
        }

        long totalPrice = student.getLectures().stream()
                .mapToLong(Lecture::getPrice)
                .sum();

        LocalDateTime dueDate = LocalDateTime.now().plusWeeks(2);

        Bill bill = new Bill();
        bill.setDueDate(dueDate);
        bill.setMessage(request.getMessage());
        bill.setStatus(Status.BEFORE);
        bill.setStudent(student);
        bill.setAcademy(academy);
        bill.setTotalPrice(totalPrice);
        billRepository.save(bill);

        // 포인트 차감
        currentUser.usePoint(1L);
        userRepository.save(currentUser);

        // 새로운 BillLog를 생성하여 저장합니다.
        BillLog newBillLog = new BillLog();
        newBillLog.setBill(bill);
        newBillLog.setRemainingBills(currentUser.getPoint());
        newBillLog.setCreatedAt(LocalDateTime.now());
        billLogRepository.save(newBillLog);

        BillInfoResponse billInfoResponse = new BillInfoResponse();
        billInfoResponse.setAcademyName(academy.getAcademyName());
        billInfoResponse.setStudentName(student.getStudentName());
        billInfoResponse.setGrade(student.getGrade());
        billInfoResponse.setContact(student.getPhoneNumber());
        billInfoResponse.setLectureNames(lectureNames);
        billInfoResponse.setTotalPrice(totalPrice);
        billInfoResponse.setDueDate(dueDate);
        billInfoResponse.setMessage(request.getMessage());

        // 이메일 보내기
        String subject = "청구서 안내";
        String text = "청구서가 생성되었습니다. 결제 URL: http://34.47.70.191/bill/" + bill.getId();
        emailService.sendSimpleMessage(student.getEmail(), subject, text);

        return billInfoResponse;
    }

    @Transactional(readOnly = true)
    public BillDetailResponse getBillDetail(Long id) {
        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ID가 " + id + "인 청구서를 찾을 수 없습니다."));

        BillDetailResponse response = new BillDetailResponse();
        response.setAcademyName(bill.getAcademy().getAcademyName());
        response.setReason("학원비"); // 발급 사유 고정
        response.setTotalPrice(bill.getTotalPrice());

        return response;
    }

    @Transactional
    public List<BillInfoResponse> createMultipleBills(CreateMultipleBillsRequest request) {
        User currentUser = getCurrentUser();
        if (currentUser.getPoint() < request.getStudentIds().size()) {
            throw new NotEnoughPointsException("포인트가 부족합니다.", "NOT_ENOUGH_POINTS");
        }

        List<BillInfoResponse> responses = new ArrayList<>();
        for (Long studentId : request.getStudentIds()) {
            BillInfoResponse response = createBillForStudent(studentId);

            // 포인트 차감 및 업데이트를 각 청구서 생성 후 바로 반영
            currentUser.usePoint(1L);
            userRepository.save(currentUser);

            // 새로운 BillLog를 생성하여 저장
            Bill bill = billRepository.findById(response.getBillId())
                    .orElseThrow(() -> new EntityNotFoundException("ID가 " + response.getBillId() + "인 청구서를 찾을 수 없습니다."));

            BillLog newBillLog = new BillLog();
            newBillLog.setBill(bill);
            newBillLog.setRemainingBills(currentUser.getPoint());
            newBillLog.setCreatedAt(LocalDateTime.now());
            billLogRepository.save(newBillLog);

            responses.add(response);
        }

        return responses;
    }

    private BillInfoResponse createBillForStudent(Long studentId) {
        User currentUser = getCurrentUser();

        AcademyStudent student = academyStudentRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException("ID가 " + studentId + "인 학생을 찾을 수 없습니다."));

        Academy academy = student.getAcademy();
        if (academy == null) {
            throw new EntityNotFoundException("ID가 " + studentId + "인 학생의 학원을 찾을 수 없습니다.");
        }

        List<String> lectureNames = student.getLectures().stream()
                .map(Lecture::getLectureName)
                .collect(Collectors.toList());

        if (lectureNames.isEmpty()) {
            throw new EntityNotFoundException("ID가 " + studentId + "인 학생의 강의를 찾을 수 없습니다.");
        }

        long totalPrice = student.getLectures().stream()
                .mapToLong(Lecture::getPrice)
                .sum();

        LocalDateTime dueDate = LocalDateTime.now().plusWeeks(2);

        Bill bill = new Bill();
        bill.setDueDate(dueDate);
        bill.setMessage(DEFAULT_MESSAGE);
        bill.setStatus(Status.BEFORE);
        bill.setStudent(student);
        bill.setAcademy(academy);
        bill.setTotalPrice(totalPrice);
        billRepository.save(bill);

        BillInfoResponse billInfoResponse = new BillInfoResponse();
        billInfoResponse.setAcademyName(academy.getAcademyName());
        billInfoResponse.setStudentName(student.getStudentName());
        billInfoResponse.setGrade(student.getGrade());
        billInfoResponse.setContact(student.getPhoneNumber());
        billInfoResponse.setLectureNames(lectureNames);
        billInfoResponse.setTotalPrice(totalPrice);
        billInfoResponse.setDueDate(dueDate);
        billInfoResponse.setMessage(DEFAULT_MESSAGE);
        billInfoResponse.setBillId(bill.getId()); // Bill ID 설정

        // 이메일 보내기
        String subject = "청구서 안내";
        String text = "청구서가 생성되었습니다. 결제 URL: http://34.47.70.191/bill/" + bill.getId();
        emailService.sendSimpleMessage(student.getEmail(), subject, text);

        return billInfoResponse;
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDTO) {
            String email = ((UserDTO) principal).getEmail();
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new EntityNotFoundException("로그인한 사용자를 찾을 수 없습니다."));
        } else {
            throw new IllegalStateException("인증된 사용자를 찾을 수 없습니다.");
        }
    }

    @Transactional(readOnly = true)
    public Page<BillLogResponse> getBillLogs(Pageable pageable) {
        Page<BillLog> billLogs = billLogRepository.findAll(pageable);
        if (billLogs.isEmpty()) {
            throw new EntityNotFoundException("청구서 내역이 존재하지 않습니다.");
        }
        return billLogs.map(log -> {
            BillLogResponse response = new BillLogResponse();
            response.setId(log.getId());
            response.setRemainingBills(log.getRemainingBills());
            response.setCreatedAt(log.getCreatedAt());
            response.setBillId(log.getBill() != null ? log.getBill().getId() : null);
            return response;
        });
    }
}
