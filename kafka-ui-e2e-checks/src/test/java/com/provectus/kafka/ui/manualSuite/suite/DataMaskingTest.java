package com.provectus.kafka.ui.manualSuite.suite;

import com.provectus.kafka.ui.manualSuite.BaseManualTest;
import com.provectus.kafka.ui.utilities.qaseUtils.annotations.Automation;
import io.qase.api.annotation.QaseId;
import org.testng.annotations.Test;

import static com.provectus.kafka.ui.utilities.qaseUtils.enums.State.NOT_AUTOMATED;

public class DataMaskingTest extends BaseManualTest {

    @Automation(state = NOT_AUTOMATED)
    @QaseId(262)
    @Test
    public void testCaseA() {
    }

    @Automation(state = NOT_AUTOMATED)
    @QaseId(264)
    @Test
    public void testCaseB() {
    }

    @Automation(state = NOT_AUTOMATED)
    @QaseId(265)
    @Test
    public void testCaseC() {
    }
}
