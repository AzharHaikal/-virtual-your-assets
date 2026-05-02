package com.vyra.virtual_your_assets.controller;

import com.vyra.virtual_your_assets.constant.ApiPath;
import com.vyra.virtual_your_assets.dto.BaseResponse;
import com.vyra.virtual_your_assets.dto.member.GetMemberResponse;
import com.vyra.virtual_your_assets.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import static com.vyra.virtual_your_assets.constant.ApiPath.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1_MEMBER)
public class MemberController {
    private final MemberService memberService;

    @GetMapping(GET_MEMBER)
    public BaseResponse<GetMemberResponse> getMember(@PathVariable String phoneNumber) {
        return memberService.getMember(phoneNumber);
    }
}
