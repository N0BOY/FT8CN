package com.bg7yoz.ft8cn.ft8transmit;

import com.bg7yoz.ft8cn.FT8Common;
import com.bg7yoz.ft8cn.Ft8Message;
import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.database.DatabaseOpr;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Unit tests for FT8TransmitSignal auto sequencing fixes.
 * Tests the fixes for:
 * 1. Processing messages with different sequences even if first message has same sequence
 * 2. Preventing sequence regression when receiving out-of-order messages
 */
public class FT8TransmitSignalTest {

    private FT8TransmitSignal transmitSignal;
    private MockDatabaseOpr mockDatabaseOpr;
    private MockOnDoTransmitted mockOnDoTransmitted;
    private MockOnTransmitSuccess mockOnTransmitSuccess;

    @Before
    public void setUp() {
        // Initialize GeneralVariables for testing
        GeneralVariables.myCallsign = "TEST";
        GeneralVariables.setMyMaidenheadGrid("OL50");
        GeneralVariables.band = 14074000;
        GeneralVariables.noReplyCount = 0;
        GeneralVariables.noReplyLimit = 3;
        GeneralVariables.autoCallFollow = false;
        GeneralVariables.transmitMessages = new ArrayList<>();
        
        // Create mock dependencies
        mockDatabaseOpr = new MockDatabaseOpr();
        mockOnDoTransmitted = new MockOnDoTransmitted();
        mockOnTransmitSuccess = new MockOnTransmitSuccess();
        
        // Create FT8TransmitSignal instance
        transmitSignal = new FT8TransmitSignal(
                mockDatabaseOpr,
                mockOnDoTransmitted,
                mockOnTransmitSuccess
        );
    }

    /**
     * Test fix 1: Process messages with different sequences even if first message has same sequence.
     * 
     * Scenario: We transmit in sequence 0, and receive messages:
     * - First message: sequence 0 (same as transmit) - should be skipped
     * - Second message: sequence 1 (different) - should be processed
     * 
     * Before fix: Would return early after checking first message
     * After fix: Should process the second message
     */
    @Test
    public void testProcessMessagesWithDifferentSequences_FirstMessageSameSequence() throws Exception {
        // Set up: We're transmitting in sequence 0, at functionOrder 2
        TransmitCallsign target = new TransmitCallsign(1, 0, "K1ABC", 14074000, 0, -10);
        transmitSignal.setTransmit(target, 2, "");
        assertEquals(1, transmitSignal.sequential); // We transmit in opposite sequence (0+1)%2 = 1
        assertEquals(2, getFunctionOrder(transmitSignal));

        // Create messages: first has same sequence as transmit (1), second has different (0)
        ArrayList<Ft8Message> messages = new ArrayList<>();
        
        // First message: same sequence as our transmit (1) - should be filtered out
        Ft8Message msg1 = createMessage("K1ABC", "TEST", "-10", 1);
        messages.add(msg1);
        
        // Second message: different sequence (0) - should be processed
        Ft8Message msg2 = createMessage("K1ABC", "TEST", "R-10", 0);
        messages.add(msg2);

        // Store initial function order
        int initialOrder = getFunctionOrder(transmitSignal);

        // Process messages
        transmitSignal.parseMessageToFunction(messages);

        // Verify: Should have advanced to functionOrder 4 (received order 3, so next is 4)
        // The message with sequence 0 should have been processed
        assertEquals(4, getFunctionOrder(transmitSignal));
    }

    /**
     * Test fix 1: All messages have same sequence - should skip processing.
     * 
     * Scenario: All received messages have the same sequence as our transmit sequence.
     * Should return early without processing.
     */
    @Test
    public void testSkipProcessingWhenAllMessagesSameSequence() throws Exception {
        // Set up: We're transmitting in sequence 1
        TransmitCallsign target = new TransmitCallsign(1, 0, "K1ABC", 14074000, 0, -10);
        transmitSignal.setTransmit(target, 2, "");
        assertEquals(1, transmitSignal.sequential);

        // Create messages: all have same sequence as transmit (1)
        ArrayList<Ft8Message> messages = new ArrayList<>();
        messages.add(createMessage("K1ABC", "TEST", "-10", 1));
        messages.add(createMessage("K1ABC", "TEST", "R-10", 1));
        messages.add(createMessage("OTHER", "TEST", "-15", 1));

        int initialOrder = getFunctionOrder(transmitSignal);

        // Process messages
        transmitSignal.parseMessageToFunction(messages);

        // Verify: Should not have changed function order (all messages skipped)
        assertEquals(initialOrder, getFunctionOrder(transmitSignal));
    }

    /**
     * Test fix 2: Prevent sequence regression when receiving out-of-order messages.
     * 
     * Scenario: We're at functionOrder 4, but receive a message with order 1 (delayed/out-of-order).
     * Should not regress to functionOrder 2.
     */
    @Test
    public void testPreventSequenceRegression_OutOfOrderMessage() throws Exception {
        // Set up: We're at functionOrder 4
        TransmitCallsign target = new TransmitCallsign(1, 0, "K1ABC", 14074000, 0, -10);
        transmitSignal.setTransmit(target, 4, "");
        assertEquals(4, getFunctionOrder(transmitSignal));

        // Receive an out-of-order message: order 1 (would set us to 2, but we're already at 4)
        ArrayList<Ft8Message> messages = new ArrayList<>();
        Ft8Message outOfOrderMsg = createMessage("K1ABC", "TEST", "OL50", 0); // Order 1
        messages.add(outOfOrderMsg);

        // Process messages
        transmitSignal.parseMessageToFunction(messages);

        // Verify: Should NOT have regressed to 2, should stay at 4
        assertEquals(4, getFunctionOrder(transmitSignal));
    }

    /**
     * Test fix 2: Normal sequence advancement - should work correctly.
     * 
     * Scenario: We're at functionOrder 2, receive order 2, should advance to 3.
     */
    @Test
    public void testNormalSequenceAdvancement_ForwardProgress() throws Exception {
        // Set up: We're at functionOrder 2
        TransmitCallsign target = new TransmitCallsign(1, 0, "K1ABC", 14074000, 0, -10);
        transmitSignal.setTransmit(target, 2, "");
        assertEquals(2, getFunctionOrder(transmitSignal));

        // Receive message with order 2
        ArrayList<Ft8Message> messages = new ArrayList<>();
        Ft8Message msg = createMessage("K1ABC", "TEST", "-10", 0); // Order 2
        messages.add(msg);

        // Process messages
        transmitSignal.parseMessageToFunction(messages);

        // Verify: Should have advanced to 3
        assertEquals(3, getFunctionOrder(transmitSignal));
    }

    /**
     * Test fix 2: Receive message that advances us forward correctly.
     * 
     * Scenario: We're at functionOrder 1, receive order 3, should advance to 4.
     */
    @Test
    public void testSequenceAdvancement_JumpForward() throws Exception {
        // Set up: We're at functionOrder 1
        TransmitCallsign target = new TransmitCallsign(1, 0, "K1ABC", 14074000, 0, -10);
        transmitSignal.setTransmit(target, 1, "");
        assertEquals(1, getFunctionOrder(transmitSignal));

        // Receive message with order 3 (maybe we missed order 2)
        ArrayList<Ft8Message> messages = new ArrayList<>();
        Ft8Message msg = createMessage("K1ABC", "TEST", "R-10", 0); // Order 3
        messages.add(msg);

        // Process messages
        transmitSignal.parseMessageToFunction(messages);

        // Verify: Should have advanced to 4 (3+1)
        assertEquals(4, getFunctionOrder(transmitSignal));
    }

    /**
     * Test fix 2: Receive message at same order level - should advance.
     * 
     * Scenario: We're at functionOrder 3, receive order 2, should advance to 3 (2+1=3, but we're already at 3, so stays at 3).
     * Actually, wait - if we receive order 2, next should be 3, and we're at 3, so it should stay at 3.
     * But the condition is nextOrder >= functionOrder, so 3 >= 3 is true, so it would set to 3.
     * Actually, that's fine - it means we're already at the right place.
     */
    @Test
    public void testSequenceAdvancement_SameLevel() throws Exception {
        // Set up: We're at functionOrder 3
        TransmitCallsign target = new TransmitCallsign(1, 0, "K1ABC", 14074000, 0, -10);
        transmitSignal.setTransmit(target, 3, "");
        assertEquals(3, getFunctionOrder(transmitSignal));

        // Receive message with order 2 (next would be 3, which equals current)
        ArrayList<Ft8Message> messages = new ArrayList<>();
        Ft8Message msg = createMessage("K1ABC", "TEST", "-10", 0); // Order 2
        messages.add(msg);

        // Process messages
        transmitSignal.parseMessageToFunction(messages);

        // Verify: Should be at 3 (2+1=3, and 3 >= 3, so sets to 3)
        assertEquals(3, getFunctionOrder(transmitSignal));
    }

    /**
     * Test: Mixed sequence messages - some same, some different.
     * Should process the different ones.
     */
    @Test
    public void testMixedSequenceMessages_ProcessDifferentOnes() throws Exception {
        // Set up: We're transmitting in sequence 1
        TransmitCallsign target = new TransmitCallsign(1, 0, "K1ABC", 14074000, 0, -10);
        transmitSignal.setTransmit(target, 2, "");
        assertEquals(1, transmitSignal.sequential);

        // Create mixed messages
        ArrayList<Ft8Message> messages = new ArrayList<>();
        messages.add(createMessage("OTHER1", "TEST", "-10", 1)); // Same sequence - skip
        messages.add(createMessage("K1ABC", "TEST", "R-10", 0)); // Different sequence, order 3 - process
        messages.add(createMessage("OTHER2", "TEST", "-15", 1)); // Same sequence - skip

        int initialOrder = getFunctionOrder(transmitSignal);

        // Process messages
        transmitSignal.parseMessageToFunction(messages);

        // Verify: Should have advanced based on the message with different sequence
        assertEquals(4, getFunctionOrder(transmitSignal)); // Received order 3, so next is 4
    }

    /**
     * Test: Empty message list - should return early.
     */
    @Test
    public void testEmptyMessageList_ReturnsEarly() throws Exception {
        TransmitCallsign target = new TransmitCallsign(1, 0, "K1ABC", 14074000, 0, -10);
        transmitSignal.setTransmit(target, 2, "");
        int initialOrder = getFunctionOrder(transmitSignal);

        ArrayList<Ft8Message> messages = new ArrayList<>();

        // Process empty list
        transmitSignal.parseMessageToFunction(messages);

        // Verify: Should not have changed
        assertEquals(initialOrder, getFunctionOrder(transmitSignal));
    }

    /**
     * Test: No matching callsign in messages - should not advance.
     */
    @Test
    public void testNoMatchingCallsign_DoesNotAdvance() throws Exception {
        TransmitCallsign target = new TransmitCallsign(1, 0, "K1ABC", 14074000, 0, -10);
        transmitSignal.setTransmit(target, 2, "");
        int initialOrder = getFunctionOrder(transmitSignal);

        // Messages from different station
        ArrayList<Ft8Message> messages = new ArrayList<>();
        messages.add(createMessage("K2XYZ", "TEST", "-10", 0)); // Different callsign

        transmitSignal.parseMessageToFunction(messages);

        // Verify: Should not have advanced (no matching callsign)
        assertEquals(initialOrder, getFunctionOrder(transmitSignal));
    }

    /**
     * Test Fix #1: Compound callsign matching - should recognize compound callsigns correctly.
     * 
     * Scenario: Target has compound callsign "K1ABC/P", message from "K1ABC" should match.
     */
    @Test
    public void testCompoundCallsignMatching_RecognizesCompoundCallsigns() throws Exception {
        // Set up: Target has compound callsign
        TransmitCallsign target = new TransmitCallsign(1, 0, "K1ABC/P", 14074000, 0, -10);
        transmitSignal.setTransmit(target, 2, "");
        assertEquals(2, getFunctionOrder(transmitSignal));

        // Message from base callsign (without /P suffix) - should match due to compound callsign handling
        ArrayList<Ft8Message> messages = new ArrayList<>();
        Ft8Message msg = createMessage("K1ABC", "TEST", "-10", 0); // Order 2
        messages.add(msg);

        // Process messages
        transmitSignal.parseMessageToFunction(messages);

        // Verify: Should have advanced to 3 (compound callsign matching works)
        assertEquals(3, getFunctionOrder(transmitSignal));
    }

    /**
     * Test Fix #1: Compound callsign matching - should handle /MM, /P, etc.
     */
    @Test
    public void testCompoundCallsignMatching_VariousSuffixes() throws Exception {
        // Test with /MM suffix
        TransmitCallsign target1 = new TransmitCallsign(1, 0, "W1ABC/MM", 14074000, 0, -10);
        transmitSignal.setTransmit(target1, 1, "");
        
        ArrayList<Ft8Message> messages = new ArrayList<>();
        messages.add(createMessage("W1ABC", "TEST", "OL50", 0)); // Order 1
        
        transmitSignal.parseMessageToFunction(messages);
        assertEquals(2, getFunctionOrder(transmitSignal));
    }

    /**
     * Test Fix #2: Unparseable message handling - should log and continue searching.
     * 
     * Scenario: Receive message from target callsign but order can't be parsed.
     * Should log it and continue searching for other parseable messages.
     */
    @Test
    public void testUnparseableMessage_LogsAndContinues() throws Exception {
        TransmitCallsign target = new TransmitCallsign(1, 0, "K1ABC", 14074000, 0, -10);
        transmitSignal.setTransmit(target, 2, "");
        int initialOrder = getFunctionOrder(transmitSignal);

        // Create message with unparseable extraInfo (invalid format)
        ArrayList<Ft8Message> messages = new ArrayList<>();
        Ft8Message unparseableMsg = createMessage("K1ABC", "TEST", "INVALID_FORMAT", 0);
        messages.add(unparseableMsg);
        
        // Add a parseable message after it
        Ft8Message parseableMsg = createMessage("K1ABC", "TEST", "-10", 0); // Order 2
        messages.add(parseableMsg);

        // Process messages
        transmitSignal.parseMessageToFunction(messages);

        // Verify: Should have advanced based on the parseable message
        // (unparseable message is logged but doesn't block processing)
        assertEquals(3, getFunctionOrder(transmitSignal));
    }

    /**
     * Test Fix #2: Unparseable message - only unparseable message, should not crash.
     */
    @Test
    public void testUnparseableMessage_OnlyUnparseable() throws Exception {
        TransmitCallsign target = new TransmitCallsign(1, 0, "K1ABC", 14074000, 0, -10);
        transmitSignal.setTransmit(target, 2, "");
        int initialOrder = getFunctionOrder(transmitSignal);

        // Only unparseable message
        ArrayList<Ft8Message> messages = new ArrayList<>();
        Ft8Message unparseableMsg = createMessage("K1ABC", "TEST", "CORRUPTED", 0);
        messages.add(unparseableMsg);

        // Process messages - should not crash
        transmitSignal.parseMessageToFunction(messages);

        // Verify: Should not have advanced (no parseable message), but shouldn't crash
        assertEquals(initialOrder, getFunctionOrder(transmitSignal));
    }

    /**
     * Test Fix #3: Bounds checking - empty message list should not crash.
     */
    @Test
    public void testBoundsChecking_EmptyListAfterFiltering() throws Exception {
        TransmitCallsign target = new TransmitCallsign(1, 0, "K1ABC", 14074000, 0, -10);
        transmitSignal.setTransmit(target, 2, "");
        int initialOrder = getFunctionOrder(transmitSignal);

        // Create list with only messages from same sequence (will be filtered out)
        ArrayList<Ft8Message> messages = new ArrayList<>();
        messages.add(createMessage("K1ABC", "TEST", "-10", 1)); // Same sequence as transmit

        // Process messages - should not crash even if all messages filtered
        transmitSignal.parseMessageToFunction(messages);

        // Verify: Should not have changed (all messages filtered), but shouldn't crash
        assertEquals(initialOrder, getFunctionOrder(transmitSignal));
    }

    /**
     * Test Fix #4: Null safety - toCallsign becoming null should not crash.
     * 
     * This tests the defensive null check when setting toCQ.
     */
    @Test
    public void testNullSafety_ToCallsignNullCheck() throws Exception {
        TransmitCallsign target = new TransmitCallsign(1, 0, "K1ABC", 14074000, 0, -10);
        transmitSignal.setTransmit(target, 4, "");
        
        // Set noReplyCount high to trigger reset to CQ
        GeneralVariables.noReplyCount = 10;
        GeneralVariables.noReplyLimit = 3;

        // Messages that don't match
        ArrayList<Ft8Message> messages = new ArrayList<>();
        messages.add(createMessage("K2XYZ", "TEST", "-10", 0)); // Different callsign

        // Process messages - should not crash even if toCallsign becomes null
        transmitSignal.parseMessageToFunction(messages);

        // Verify: Should have reset to CQ (functionOrder 6) without crashing
        assertEquals(6, getFunctionOrder(transmitSignal));
    }

    /**
     * Test Fix #6: Null safety in checkTargetCallMe - should handle null toCallsign safely.
     */
    @Test
    public void testNullSafety_CheckTargetCallMe() throws Exception {
        // Set up without target callsign (toCallsign will be null)
        // This simulates the scenario where toCallsign might become null
        TransmitCallsign target = new TransmitCallsign(1, 0, "CQ", 14074000, 0, -10);
        transmitSignal.setTransmit(target, 6, ""); // CQ state
        
        // Use reflection to set toCallsign to null to test defensive check
        Field toCallsignField = FT8TransmitSignal.class.getDeclaredField("toCallsign");
        toCallsignField.setAccessible(true);
        toCallsignField.set(transmitSignal, null);

        // Messages that would normally trigger checkTargetCallMe
        ArrayList<Ft8Message> messages = new ArrayList<>();
        messages.add(createMessage("K1ABC", "TEST", "-10", 0));

        // Process messages - should not crash with null toCallsign
        transmitSignal.parseMessageToFunction(messages);

        // Verify: Should not crash, functionOrder should remain at 6 (CQ)
        assertEquals(6, getFunctionOrder(transmitSignal));
    }

    /**
     * Test: Compound callsign in message calling us - should be recognized.
     */
    @Test
    public void testCompoundCallsign_CallingUs() throws Exception {
        // Set up: We're calling compound callsign
        TransmitCallsign target = new TransmitCallsign(1, 0, "K1ABC/P", 14074000, 0, -10);
        transmitSignal.setTransmit(target, 1, "");
        assertEquals(1, getFunctionOrder(transmitSignal));

        // Message from compound callsign calling us
        ArrayList<Ft8Message> messages = new ArrayList<>();
        Ft8Message msg = createMessage("K1ABC/P", "TEST", "OL50", 0); // Order 1, calling us
        messages.add(msg);

        // Process messages
        transmitSignal.parseMessageToFunction(messages);

        // Verify: Should recognize compound callsign and advance
        assertEquals(2, getFunctionOrder(transmitSignal));
    }

    /**
     * Test: Multiple messages with one unparseable - should process parseable one.
     */
    @Test
    public void testMultipleMessages_OneUnparseable() throws Exception {
        TransmitCallsign target = new TransmitCallsign(1, 0, "K1ABC", 14074000, 0, -10);
        transmitSignal.setTransmit(target, 2, "");

        ArrayList<Ft8Message> messages = new ArrayList<>();
        // First message: unparseable
        messages.add(createMessage("K1ABC", "TEST", "UNPARSEABLE", 0));
        // Second message: parseable (order 2)
        messages.add(createMessage("K1ABC", "TEST", "-10", 0));

        transmitSignal.parseMessageToFunction(messages);

        // Verify: Should process the parseable message and advance
        assertEquals(3, getFunctionOrder(transmitSignal));
    }

    /**
     * Helper method to get functionOrder using reflection (since it's private)
     */
    private int getFunctionOrder(FT8TransmitSignal signal) throws Exception {
        Field field = FT8TransmitSignal.class.getDeclaredField("functionOrder");
        field.setAccessible(true);
        return field.getInt(signal);
    }

    // Helper method to create test messages
    private Ft8Message createMessage(String from, String to, String extraInfo, int sequence) {
        Ft8Message msg = new Ft8Message(FT8Common.FT8_MODE);
        msg.callsignFrom = from;
        msg.callsignTo = to;
        msg.extraInfo = extraInfo;
        // Set utcTime to get desired sequence (sequence is calculated from utcTime)
        // For FT8: sequence = (((utcTime + 750) / 1000) / 15) % 2
        // To get sequence 0: utcTime = 0 (or any multiple of 30000)
        // To get sequence 1: utcTime = 15000 (or 15000 + multiple of 30000)
        if (sequence == 0) {
            msg.utcTime = 0;
        } else {
            msg.utcTime = 15000;
        }
        msg.band = GeneralVariables.band;
        msg.snr = -10;
        return msg;
    }

    // Mock classes for dependencies
    private static class MockDatabaseOpr extends DatabaseOpr {
        public MockDatabaseOpr() {
            super(null, null, null, 1); // DatabaseOpr constructor requires context, name, factory, version
        }
    }

    private static class MockOnDoTransmitted implements OnDoTransmitted {
        @Override
        public void onBeforeTransmit(Ft8Message message, int functionOder) {
            // Mock implementation (note: interface has typo "functionOder")
        }

        @Override
        public void onAfterTransmit(Ft8Message message, int functionOder) {
            // Mock implementation (note: interface has typo "functionOder")
        }

        @Override
        public void onTransmitByWifi(Ft8Message message) {
            // Mock implementation
        }

        @Override
        public void onTransmitOverCAT(Ft8Message message) {
            // Mock implementation
        }

        @Override
        public boolean supportTransmitOverCAT() {
            return false;
        }
    }

    private static class MockOnTransmitSuccess implements OnTransmitSuccess {
        @Override
        public void doAfterTransmit(com.bg7yoz.ft8cn.log.QSLRecord record) {
            // Mock implementation
        }
    }
}
