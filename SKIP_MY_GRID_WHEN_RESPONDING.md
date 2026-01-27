# Skip My Grid When Responding Feature

## Overview
The "Skip my grid when responding" setting allows users to omit their maidenhead grid square from FT8 messages when responding to stations that have called them, while always including it when calling CQ or initiating contact.

## How It Works

The setting is implemented in `FT8TransmitSignal.getFunctionCommand()` with the following logic:

1. **CQ Messages (case 6)**: 
   - **Always includes grid square** - regardless of setting value
   - This ensures CQ calls always provide location information

2. **When I Initiate Contact**:
   - **Always includes grid square** - regardless of setting value
   - When `isResponseToCall = false`, the grid is always included

3. **When Responding to Someone Who Called Me**:
   - **Conditionally includes grid square** - based on setting value
   - If `skip_my_grid_when_responding = true` AND `isResponseToCall = true`: grid is omitted
   - If `skip_my_grid_when_responding = false` OR `isResponseToCall = false`: grid is included

## Implementation Details

The logic in `getFunctionCommand()`:

```java
// Determine if we should include my grid square
// Skip my grid if: setting is enabled AND this is a response to someone calling me
// BUT: Always include grid for CQ (case 6) and when I initiate contact (not a response)
boolean shouldIncludeMyGrid;
if (order == 6) {
    // CQ always includes grid square
    shouldIncludeMyGrid = true;
} else {
    // For other messages: skip grid only if responding to someone who called me AND setting is enabled
    shouldIncludeMyGrid = !(GeneralVariables.skip_my_grid_when_responding && isResponseToCall);
}
String myGrid = shouldIncludeMyGrid ? GeneralVariables.getMyMaidenhead4Grid() : "";
```

## Response Detection

The system tracks whether a transmission is a response through the `isResponseToCall` flag:

- **Set to `true`** when:
  - `checkCQMeOrFollowCQMessage()` detects someone calling us and calls `setTransmit(..., true)`
  - Processing from the callsign queue (someone previously called us)

- **Set to `false`** (default) when:
  - Manually calling a station via `doCallNow()`
  - Auto-calling a CQ station
  - Any other initiated contact

## Message Types Affected

The setting only affects **message type 1** (first contact message with grid):
- `BG7YOY BG7YOZ OL50` - includes grid if setting allows

Other message types are not affected:
- Message 2: `BG7YOY BG7YOZ -10` - signal report only
- Message 3: `BG7YOY BG7YOZ R-10` - roger with signal report
- Message 4: `BG7YOY BG7YOZ RRR` - roger roger roger
- Message 5: `BG7YOY BG7YOZ 73` - 73 only
- Message 6: `CQ BG7YOZ OL50` - **always includes grid**

## Use Cases

**When enabled:**
- Useful for operators who want to reduce message length when responding
- May be preferred in crowded conditions where shorter messages are beneficial
- Grid square is still included in CQ and when initiating contact

**When disabled (default):**
- Grid square is always included in all messages
- Provides location information in all contacts
- Standard FT8 behavior

## Testing the Feature

To verify the feature works correctly:

1. **CQ Test**: 
   - Enable the setting
   - Call CQ
   - Verify grid square is present in CQ message ✓

2. **Initiate Contact Test**:
   - Enable the setting
   - Manually call a station (not responding)
   - Verify grid square is present in message 1 ✓

3. **Response Test**:
   - Enable the setting
   - Wait for someone to call you
   - Verify grid square is **omitted** from response message 1 ✓

4. **Response Test (Disabled)**:
   - Disable the setting
   - Wait for someone to call you
   - Verify grid square is **included** in response message 1 ✓

## Related Files

- **Implementation**: `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ft8transmit/FT8TransmitSignal.java`
- **Setting Variable**: `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/GeneralVariables.java`
- **UI Toggle**: `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ui/ConfigFragment.java`
- **Layout**: `ft8cn/app/src/main/res/layout/fragment_config.xml`
- **String Resources**: `ft8cn/app/src/main/res/values/strings.xml`
- **Database Persistence**: `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/database/DatabaseOpr.java`
