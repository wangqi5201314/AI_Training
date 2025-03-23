package com.wangqi.openaitest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OpenAiTestApplicationTests {
    @Autowired
    private ComplaintSimulation complaintSimulation;
    @Test
    void contextLoads() {

    }

}
