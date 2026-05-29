package com.company.imticket.service.domain.knowledge;

import com.company.imticket.common.exception.BizErrorCode;
import com.company.imticket.common.exception.BizException;
import com.company.imticket.dao.entity.KnowledgeFaq;
import com.company.imticket.dao.mapper.KnowledgeFaqMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for KnowledgeSearchService.
 * <p>
 * Uses manual stubs for DocumentSearchService and TicketArchiveSearchService
 * because Mockito cannot mock concrete classes on Java 26 (ByteBuddy limitation).
 * KnowledgeFaqMapper is an interface so @Mock works fine.
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeSearchServiceTest {

    @Mock
    private KnowledgeFaqMapper faqMapper;

    // Controllable return values for document/archive stubs
    private KnowledgeSearchResult documentResult;
    private KnowledgeSearchResult archiveResult;

    private KnowledgeSearchService knowledgeSearchService;

    private static final String QUESTION = "审批流程是什么？";

    @BeforeEach
    void setUp() {
        documentResult = null;
        archiveResult = null;

        // Manual stubs for concrete classes (cannot use @Mock on Java 26)
        DocumentSearchService documentSearchService = new DocumentSearchService() {
            @Override
            public KnowledgeSearchResult search(String question) {
                return documentResult;
            }
        };

        TicketArchiveSearchService archiveSearchService = new TicketArchiveSearchService() {
            @Override
            public KnowledgeSearchResult search(String question) {
                return archiveResult;
            }
        };

        knowledgeSearchService = new KnowledgeSearchService(faqMapper, documentSearchService, archiveSearchService);
    }

    private KnowledgeFaq createFaq(Long id, String question, String answer) {
        KnowledgeFaq faq = new KnowledgeFaq();
        faq.setId(id);
        faq.setQuestion(question);
        faq.setAnswer(answer);
        faq.setHitCount(10L);
        return faq;
    }

    // ======================== Null input validation ========================

    @Test
    void search_NullQuestion_shouldThrowBizException() {
        BizException ex = assertThrows(BizException.class, () ->
                knowledgeSearchService.search(null));
        assertEquals(BizErrorCode.PARAM_INVALID, ex.getErrorCode());
    }

    // ======================== L1 FAQ tests ========================

    @Test
    void search_L1FaqHit_shouldReturnL1ResultAndIncrementHitCount() {
        KnowledgeFaq faq = createFaq(1L, QUESTION, "审批流程包含三个步骤：提交、审核、通过。");
        when(faqMapper.searchByQuestion(QUESTION)).thenReturn(Collections.singletonList(faq));

        KnowledgeSearchResult result = knowledgeSearchService.search(QUESTION);

        assertNotNull(result);
        assertEquals("审批流程包含三个步骤：提交、审核、通过。", result.getAnswer());
        assertEquals(KnowledgeSearchResult.SOURCE_L1_FAQ, result.getSource());
        assertEquals(1.0, result.getConfidence(), 0.001);
        assertEquals(1, result.getReferences().size());
        assertEquals(QUESTION, result.getReferences().get(0));

        verify(faqMapper).incrementHitCount(1L);
    }

    @Test
    void search_L1FaqHit_shouldUseFirstResultWhenMultipleHits() {
        KnowledgeFaq faq1 = createFaq(1L, "问题A", "答案A");
        KnowledgeFaq faq2 = createFaq(2L, "问题B", "答案B");
        when(faqMapper.searchByQuestion(QUESTION)).thenReturn(List.of(faq1, faq2));

        KnowledgeSearchResult result = knowledgeSearchService.search(QUESTION);

        assertNotNull(result);
        assertEquals("答案A", result.getAnswer());
        verify(faqMapper).incrementHitCount(1L);
        verify(faqMapper, never()).incrementHitCount(2L);
    }

    @Test
    void search_L1FaqEmptyList_shouldFallThroughToL2() {
        when(faqMapper.searchByQuestion(QUESTION)).thenReturn(Collections.emptyList());
        documentResult = new KnowledgeSearchResult(
                "文档答案", KnowledgeSearchResult.SOURCE_L2_DOC,
                Collections.singletonList("参考文档1"), 0.85
        );

        KnowledgeSearchResult result = knowledgeSearchService.search(QUESTION);

        assertNotNull(result);
        assertEquals(KnowledgeSearchResult.SOURCE_L2_DOC, result.getSource());
        assertEquals(0.85, result.getConfidence(), 0.001);
        assertEquals("文档答案", result.getAnswer());

        verify(faqMapper, never()).incrementHitCount(anyLong());
    }

    @Test
    void search_L1FaqNull_shouldFallThroughToL2() {
        when(faqMapper.searchByQuestion(QUESTION)).thenReturn(null);
        documentResult = new KnowledgeSearchResult(
                "文档答案", KnowledgeSearchResult.SOURCE_L2_DOC,
                Collections.singletonList("参考文档1"), 0.85
        );

        KnowledgeSearchResult result = knowledgeSearchService.search(QUESTION);

        assertNotNull(result);
        assertEquals(KnowledgeSearchResult.SOURCE_L2_DOC, result.getSource());

        verify(faqMapper, never()).incrementHitCount(anyLong());
    }

    // ======================== L1 miss -> L2 miss -> L3 hit ========================

    @Test
    void search_L1Miss_L2Miss_L3Hit_shouldReturnL3Result() {
        when(faqMapper.searchByQuestion(QUESTION)).thenReturn(Collections.emptyList());
        documentResult = null;
        archiveResult = new KnowledgeSearchResult(
                "历史工单答案", KnowledgeSearchResult.SOURCE_L3_ARCHIVE,
                Collections.singletonList("IM-20260501-0001"), 0.72
        );

        KnowledgeSearchResult result = knowledgeSearchService.search(QUESTION);

        assertNotNull(result);
        assertEquals(KnowledgeSearchResult.SOURCE_L3_ARCHIVE, result.getSource());
        assertEquals(0.72, result.getConfidence(), 0.001);
        assertEquals("历史工单答案", result.getAnswer());
        assertEquals(1, result.getReferences().size());
        assertEquals("IM-20260501-0001", result.getReferences().get(0));
    }

    // ======================== All layers miss ========================

    @Test
    void search_AllLayersMiss_shouldReturnNull() {
        when(faqMapper.searchByQuestion(QUESTION)).thenReturn(Collections.emptyList());
        documentResult = null;
        archiveResult = null;

        KnowledgeSearchResult result = knowledgeSearchService.search(QUESTION);

        assertNull(result);
    }

    // ======================== L1 hit -> no L2/L3 calls ========================

    @Test
    void search_L1Hit_shouldNotCallL2OrL3() {
        KnowledgeFaq faq = createFaq(1L, QUESTION, "答案");
        when(faqMapper.searchByQuestion(QUESTION)).thenReturn(Collections.singletonList(faq));
        // Set documentResult to non-null to verify it's NOT called
        documentResult = new KnowledgeSearchResult(
                "不应该被调用", KnowledgeSearchResult.SOURCE_L2_DOC,
                Collections.emptyList(), 0.5
        );
        archiveResult = new KnowledgeSearchResult(
                "不应该被调用", KnowledgeSearchResult.SOURCE_L3_ARCHIVE,
                Collections.emptyList(), 0.5
        );

        KnowledgeSearchResult result = knowledgeSearchService.search(QUESTION);

        assertNotNull(result);
        assertEquals(KnowledgeSearchResult.SOURCE_L1_FAQ, result.getSource());
        assertEquals("答案", result.getAnswer());
    }

    // ======================== L2 null vs L2 non-null ========================

    @Test
    void search_L2ReturnsNull_shouldFallThroughToL3() {
        when(faqMapper.searchByQuestion(QUESTION)).thenReturn(Collections.emptyList());
        documentResult = null;
        archiveResult = new KnowledgeSearchResult(
                "工单答案", KnowledgeSearchResult.SOURCE_L3_ARCHIVE,
                Collections.emptyList(), 0.65
        );

        KnowledgeSearchResult result = knowledgeSearchService.search(QUESTION);

        assertNotNull(result);
        assertEquals(KnowledgeSearchResult.SOURCE_L3_ARCHIVE, result.getSource());
        assertEquals(0.65, result.getConfidence(), 0.001);
    }
}
