package com.qfc.feedback;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SiteFeedbackServiceTest {

    @Mock
    private SiteFeedbackMapper siteFeedbackMapper;

    private SiteFeedbackService siteFeedbackService;

    @BeforeEach
    void setUp() {
        siteFeedbackService = new SiteFeedbackService(siteFeedbackMapper);
    }

    @Test
    void createFeedbackUsesCurrentSiteUserAndOpenStatus() {
        when(siteFeedbackMapper.insert(any(SiteFeedback.class))).thenAnswer(invocation -> {
            SiteFeedback feedback = invocation.getArgument(0);
            feedback.setId(71L);
            return 1;
        });
        SiteFeedbackView view = new SiteFeedbackView();
        view.setId(71L);
        view.setAuthorUserId(5L);
        view.setTitle("主站体验反人类");
        view.setContent("反馈入口太隐蔽，用户不知道哪里可以吐槽。");
        view.setStatus("OPEN");
        when(siteFeedbackMapper.selectViewById(71L)).thenReturn(view);
        SiteFeedbackCreateRequest request = new SiteFeedbackCreateRequest();
        request.setTitle("主站体验反人类");
        request.setContent("反馈入口太隐蔽，用户不知道哪里可以吐槽。");

        SiteFeedbackView created = siteFeedbackService.createFeedback(5L, request);

        ArgumentCaptor<SiteFeedback> captor = ArgumentCaptor.forClass(SiteFeedback.class);
        verify(siteFeedbackMapper).insert(captor.capture());
        SiteFeedback saved = captor.getValue();
        assertEquals(5L, saved.getAuthorUserId());
        assertEquals("主站体验反人类", saved.getTitle());
        assertEquals("反馈入口太隐蔽，用户不知道哪里可以吐槽。", saved.getContent());
        assertEquals("OPEN", saved.getStatus());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertEquals(71L, created.getId());
        assertEquals("OPEN", created.getStatus());
    }

    @Test
    void listFeedbackReturnsNewestFeedbackViews() {
        SiteFeedbackView feedback = new SiteFeedbackView();
        feedback.setId(72L);
        feedback.setAuthorDisplayName("体验官");
        feedback.setTitle("导航不好找");
        when(siteFeedbackMapper.selectAllViews()).thenReturn(Arrays.asList(feedback));

        List<SiteFeedbackView> views = siteFeedbackService.listFeedback();

        assertEquals(1, views.size());
        assertEquals("体验官", views.get(0).getAuthorDisplayName());
        assertEquals("导航不好找", views.get(0).getTitle());
    }
}
