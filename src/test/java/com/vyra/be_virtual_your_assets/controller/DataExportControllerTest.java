package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.export.DataExportRequest;
import com.vyra.be_virtual_your_assets.dto.export.DataExportResponse;
import com.vyra.be_virtual_your_assets.dto.export.ExportFile;
import com.vyra.be_virtual_your_assets.security.model.CustomUserDetails;
import com.vyra.be_virtual_your_assets.service.DataExportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataExportControllerTest {

    @Mock private DataExportService dataExportService;

    @InjectMocks
    private DataExportController controller;

    private final UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(new CustomUserDetails("member-1", "token"), null);

    @Test
    void request_is202_andStatusCallsUseTheAuthenticatedMember() {
        DataExportRequest request = new DataExportRequest();
        request.setPin("123456");
        BaseResponse<DataExportResponse> queued = new BaseResponse<>("VYRA-EXP-REQ-000", "ok", new DataExportResponse());
        when(dataExportService.request("member-1", "123456")).thenReturn(queued);
        ResponseEntity<BaseResponse<DataExportResponse>> response = controller.request(request, authentication);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isSameAs(queued);

        BaseResponse<DataExportResponse> latest = new BaseResponse<>("VYRA-EXP-GET-000", "ok", null);
        when(dataExportService.getLatest("member-1")).thenReturn(latest);
        assertThat(controller.getLatest(authentication)).isSameAs(latest);
        when(dataExportService.get("member-1", "e-1")).thenReturn(latest);
        assertThat(controller.get("e-1", authentication)).isSameAs(latest);
    }

    @Test
    void downloads_areZipAttachments() {
        ExportFile zip = new ExportFile("vyra-data-2026-10-22.zip", "application/zip", new byte[]{7});
        when(dataExportService.download("member-1", "e-1")).thenReturn(zip);
        when(dataExportService.downloadWithToken("e-1", "secret")).thenReturn(zip);

        ResponseEntity<byte[]> fromApp = controller.download("e-1", authentication);
        ResponseEntity<byte[]> fromMail = controller.downloadWithToken("e-1", "secret");

        assertThat(fromApp.getHeaders().getContentDisposition().getFilename()).isEqualTo("vyra-data-2026-10-22.zip");
        assertThat(fromMail.getHeaders().getContentType()).hasToString("application/zip");
        assertThat(fromMail.getBody()).containsExactly(7);
    }
}
