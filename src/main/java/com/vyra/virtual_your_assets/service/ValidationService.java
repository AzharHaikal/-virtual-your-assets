//package com.vyra.virtual_your_assets.service;
//
//import com.vyra.virtual_your_assets.constant.ErrorConstant;
//import com.vyra.virtual_your_assets.dto.register.RegisterRequest;
//import com.vyra.virtual_your_assets.exception.BadRequestMessageException;
//import com.vyra.virtual_your_assets.exception.BaseException;
//import com.vyra.virtual_your_assets.repository.MemberRepository;
//import io.micrometer.common.util.StringUtils;
//import lombok.RequiredArgsConstructor;
//import org.apache.coyote.BadRequestException;
//import org.springframework.stereotype.Service;
//
//@Service
//@RequiredArgsConstructor
//public class ValidationService {
//    private final MemberRepository memberRepository;
//
//    public RegisterRequest validateRegisterRequest(RegisterRequest request) {
//        if (StringUtils.isBlank(request.getFirstName()) ||
//                StringUtils.isBlank(request.getLastName()) ||
//                StringUtils.isBlank(request.getEmail()) ||
//                StringUtils.isBlank(request.getPhoneNumber()) ||
//                StringUtils.isBlank(request.getPin())
//        ) {
//            throw new BadRequestMessageException(ErrorConstant.BAD_REQUEST);
//        }
//
//        if (memberRepository.existsByPhoneNumber(request.getPhoneNumber())) {
//            throw new BaseException(ErrorConstant.PHONE_NUMBER_ALREADY_EXIST);
//        }
//
//        if (memberRepository.existsByEmail(request.getPhoneNumber())) {
//            throw new BaseException(ErrorConstant.EMAIL_ALREADY_EXIST);
//        }
//
//        String phoneNumber = request.getPhoneNumber().trim().replaceAll("\\s", "");
//        if (phoneNumber.startsWith("62")) {
//            phoneNumber = "+62" + phoneNumber.substring(2);
//        } else if (phoneNumber.startsWith("0")) {
//            phoneNumber = "+62" + phoneNumber.substring(1);
//        } else {
//            throw new BadRequestMessageException(ErrorConstant.BAD_REQUEST);
//        }
//        request.setPhoneNumber(phoneNumber);
//
//        return request;
//    }
//}
