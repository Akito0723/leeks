import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class SettingsWindowTest {

    @Test
    public void keepsSingleStockPositionTogether() {
        assertEquals(List.of("sh600519,55.7,500"),
                SettingsWindow.parseInstrumentConfig("sh600519,55.7,500"));
    }

    @Test
    public void keepsSingleFundPositionTogether() {
        assertEquals(List.of("006250,3.66,1000"),
                SettingsWindow.parseInstrumentConfig("006250,3.66,1000"));
    }

    @Test
    public void preservesLegacyFundCodeList() {
        assertEquals(List.of("001632", "002340", "003095"),
                SettingsWindow.parseInstrumentConfig("001632,002340,003095"));
    }

    @Test
    public void preservesLegacyStockCodeList() {
        assertEquals(List.of("sh000001", "hk00700"),
                SettingsWindow.parseInstrumentConfig("sh000001,hk00700"));
    }

    @Test
    public void removesBlankAndDuplicateEntries() {
        assertEquals(List.of("sh000001", "hk00700"),
                SettingsWindow.parseInstrumentConfig(" sh000001 ; ; hk00700 ; sh000001 "));
    }
}
