package com.qfc.feedback;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SiteFeedbackService {

    private static final String DEFAULT_STATUS = "OPEN";

    private final SiteFeedbackMapper siteFeedbackMapper;

    public SiteFeedbackService(SiteFeedbackMapper siteFeedbackMapper) {
        this.siteFeedbackMapper = siteFeedbackMapper;
    }

    public SiteFeedbackView createFeedback(Long authorUserId, SiteFeedbackCreateRequest request) {
        LocalDateTime now = LocalDateTime.now();
        SiteFeedback feedback = new SiteFeedback();
        feedback.setAuthorUserId(authorUserId);
        feedback.setTitle(request.getTitle());
        feedback.setContent(request.getContent());
        feedback.setStatus(DEFAULT_STATUS);
        feedback.setCreatedAt(now);
        feedback.setUpdatedAt(now);
        siteFeedbackMapper.insert(feedback);
        return siteFeedbackMapper.selectViewById(feedback.getId());
    }

    public List<SiteFeedbackView> listFeedback() {
        return siteFeedbackMapper.selectAllViews();
    }
}
