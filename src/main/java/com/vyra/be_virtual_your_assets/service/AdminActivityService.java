package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.AdminActivityEvent;
import com.vyra.be_virtual_your_assets.entity.Admin;
import com.vyra.be_virtual_your_assets.entity.AdminActivity;
import com.vyra.be_virtual_your_assets.repository.AdminActivityRepository;
import com.vyra.be_virtual_your_assets.repository.AdminRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * Persists audit trail entries for actions performed by portal administrators.
 * Kept fully separate from {@link MemberActivityService}, which tracks
 * mobile app member activity.
 */
@Service
@RequiredArgsConstructor
public class AdminActivityService {

    /** Failed logins for an unknown e-mail have no admin id; the column is NOT NULL. */
    static final String UNKNOWN_ADMIN_ID = "UNKNOWN";
    private static final int ADMIN_NAME_MAX = 50;   // matches AdminActivity.adminName length
    private static final int TEXT_MAX = 255;        // default varchar length

    private final AdminActivityRepository adminActivityRepository;
    private final AdminRepository adminRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createAdminActivity(String adminId, String adminName, AdminActivityEvent event, String ipAddress, String device) {
        createAdminActivity(adminId, adminName, event, null, ipAddress, device);
    }

    /** Same as above, with the target of the action appended to the description (e.g. "member 123"). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createAdminActivity(String adminId, String adminName, AdminActivityEvent event, String detail,
                                    String ipAddress, String device) {
        HttpServletRequest request = currentRequest();

        AdminActivity activity = new AdminActivity();
        activity.setAdminId(StringUtils.defaultIfBlank(adminId, UNKNOWN_ADMIN_ID));
        activity.setAdminName(StringUtils.abbreviate(resolveAdminName(adminId, adminName), ADMIN_NAME_MAX));
        activity.setEvent(event.name());
        activity.setDescription(StringUtils.truncate(
                StringUtils.isBlank(detail) ? event.getDescription() : event.getDescription() + ": " + detail, TEXT_MAX));
        activity.setIpAddress(StringUtils.truncate(
                StringUtils.defaultIfBlank(ipAddress, request != null ? clientIp(request) : null), TEXT_MAX));
        activity.setDevice(StringUtils.truncate(
                StringUtils.defaultIfBlank(device, request != null ? request.getHeader("User-Agent") : null), TEXT_MAX));
        activity.setCreatedAt(LocalDateTime.now());
        adminActivityRepository.save(activity);
    }

    /**
     * Several callers pass the admin id where a display name is expected.
     * Resolve the real name so the audit trail never shows a raw UUID.
     */
    private String resolveAdminName(String adminId, String adminName) {
        if (StringUtils.isNotBlank(adminId)
                && (StringUtils.isBlank(adminName) || adminName.equals(adminId))) {
            return adminRepository.findById(adminId)
                    .map(Admin::getName)
                    .orElse(StringUtils.defaultIfBlank(adminName, adminId));
        }
        return StringUtils.defaultIfBlank(adminName, UNKNOWN_ADMIN_ID);
    }

    private static HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        return attributes instanceof ServletRequestAttributes servlet ? servlet.getRequest() : null;
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.isNotBlank(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
