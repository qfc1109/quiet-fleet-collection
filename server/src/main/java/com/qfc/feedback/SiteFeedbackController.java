package com.qfc.feedback;

import com.qfc.auth.LoginUser;
import com.qfc.auth.CurrentUserResolver;
import com.qfc.common.ApiResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SiteFeedbackController {

    private final SiteFeedbackService siteFeedbackService;
    private final CurrentUserResolver currentUserResolver;

    public SiteFeedbackController(SiteFeedbackService siteFeedbackService, CurrentUserResolver currentUserResolver) {
        this.siteFeedbackService = siteFeedbackService;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping("/api/public/feedback")
    public ApiResponse<SiteFeedbackView> createFeedback(
        @Valid @RequestBody SiteFeedbackCreateRequest request,
        HttpServletRequest servletRequest
    ) {
        LoginUser current = currentSiteUser(servletRequest);
        return ApiResponse.success(siteFeedbackService.createFeedback(current.getId(), request));
    }

    @GetMapping("/api/admin/feedback")
    public ApiResponse<List<SiteFeedbackView>> listFeedback() {
        return ApiResponse.success(siteFeedbackService.listFeedback());
    }

    private LoginUser currentSiteUser(HttpServletRequest request) {
        return currentUserResolver.resolveSite(request);
    }
}
