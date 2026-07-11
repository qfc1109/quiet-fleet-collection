package com.qfc.feedback;

import com.qfc.auth.LoginUser;
import com.qfc.auth.LoginSessionProperties;
import com.qfc.auth.LoginSessionService;
import com.qfc.common.ApiException;
import com.qfc.common.ApiResponse;
import com.qfc.config.SessionKeys;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SiteFeedbackControllerTest {

    @Mock
    private SiteFeedbackService siteFeedbackService;

    private SiteFeedbackController controller;

    @BeforeEach
    void setUp() {
        LoginSessionProperties properties = new LoginSessionProperties();
        properties.setSessionLifetimeSeconds(86400);
        controller = new SiteFeedbackController(siteFeedbackService, new LoginSessionService(properties));
    }

    @Test
    void createFeedbackRequiresSiteLogin() {
        SiteFeedbackCreateRequest request = new SiteFeedbackCreateRequest();
        request.setTitle("主站体验不好");
        request.setContent("导航入口不好找。");

        ApiException exception = assertThrows(
            ApiException.class,
            () -> controller.createFeedback(request, requestWithSession(new MockHttpSession()))
        );

        assertEquals("UNAUTHORIZED", exception.getCode());
    }

    @Test
    void createFeedbackUsesSiteSessionUserId() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
            SessionKeys.SITE_LOGIN_USER,
            new LoginUser(5L, "player01", "主站用户", "SITE_USER", "ENABLED", Arrays.asList(), Arrays.asList())
        );
        SiteFeedbackCreateRequest request = new SiteFeedbackCreateRequest();
        request.setTitle("主站体验不好");
        request.setContent("导航入口不好找。");
        SiteFeedbackView view = new SiteFeedbackView();
        view.setId(81L);
        when(siteFeedbackService.createFeedback(5L, request)).thenReturn(view);

        ApiResponse<SiteFeedbackView> response = controller.createFeedback(request, requestWithSession(session));

        assertEquals(81L, response.getData().getId());
        verify(siteFeedbackService).createFeedback(5L, request);
    }

    @Test
    void listFeedbackReturnsAdminVisibleFeedback() {
        SiteFeedbackView view = new SiteFeedbackView();
        view.setId(82L);
        view.setTitle("体验反馈");
        when(siteFeedbackService.listFeedback()).thenReturn(Arrays.asList(view));

        ApiResponse<List<SiteFeedbackView>> response = controller.listFeedback();

        assertEquals(1, response.getData().size());
        assertEquals("体验反馈", response.getData().get(0).getTitle());
        verify(siteFeedbackService).listFeedback();
    }

    private MockHttpServletRequest requestWithSession(MockHttpSession session) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        return request;
    }
}
