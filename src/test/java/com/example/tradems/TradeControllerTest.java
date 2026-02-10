package com.example.tradems;
import com.example.tradems.controller.TradeController;
import com.example.tradems.service.TradeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TradeController.class)
class TradeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TradeService tradeService;


    @Test
    void getActiveTrades_ShouldReturnList() throws Exception {
        mockMvc.perform(get("/active/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void closeTrade_ShouldReturnSuccessMessage() throws Exception {
        UUID tradeId = UUID.randomUUID();

        mockMvc.perform(delete("/close/" + tradeId))
                .andExpect(status().isOk())
                .andExpect(content().string("Pozisiya bazar qiyməti ilə bağlandı və mənfəət/zərər balansa köçürüldü."));
    }
}
