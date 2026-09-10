package com.qfc.rag;
import java.util.List; import org.springframework.stereotype.Component;
@Component public class MockRagModelAdapter { public String answer(String question,List<String> context){ return context.isEmpty()?"资料中没有找到":"根据已纳入资料找到相关内容"; } }
