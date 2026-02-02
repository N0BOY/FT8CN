# PSK Reporter Map Spots Implementation Plan

## Overview

This document outlines the plan for implementing PSK Reporter spots visualization on the Grid Tracker map. The feature will display real-time spots (stations that have heard your callsign) as markers on the map, allowing users to visualize their signal coverage geographically.

## Current State Analysis

### Existing PSK Reporter Implementation

**Location**: `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/psk/PskReporterMqtt.java`

- **Data Source**: Real-time MQTT connection to `mqtt.pskreporter.info:1883`
- **Spot Data Structure**:
  ```java
  public static class Spot {
      public final String receiverCallsign;  // Station that heard us
      public final String receiverLocator;   // Grid locator of receiver
      public final long frequency;           // Frequency in Hz
      public final long flowStartSeconds;     // Unix timestamp
  }
  ```
- **Current Display**: Text-based dialog (`PskReporterSpotsDialog.java`)
- **Spot Storage**: In-memory list with 1000 spot limit

### Existing Map Infrastructure

**Location**: `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/grid_tracker/GridOsmMapView.java`

- **Map Library**: OSMDroid (offline-capable OpenStreetMap)
- **Existing Marker System**: `GridMarker` class for CQ messages
- **Grid Conversion**: `MaidenheadGrid.gridToLatLng()` converts grid locators to coordinates
- **Marker Management**: 
  - `addGridMarker()` - Add markers
  - `clearMarkers()` - Remove all markers
  - `gridMarkers` - ArrayList tracking all markers
- **UI Controls**: Toggle switches for showing/hiding CQ markers and QSX polygons

### Integration Points

- **GridTrackerMainActivity**: Main activity hosting the map view
- **MainViewModel**: Shared view model with application state
- **GeneralVariables**: Contains user's grid locator (`getMyMaidenheadGrid()`)

---

## Architecture and Design

### Component Structure

```
GridTrackerMainActivity
    └── GridOsmMapView
        ├── gridMarkers (existing CQ markers)
        ├── spotMarkers (new PSK Reporter spot markers)
        └── spotPolylines (optional: lines from user to spots)
    └── PskReporterMqtt (existing MQTT client)
        └── Callback.onSpotReceived() → Add marker to map
```

### Data Flow

1. **MQTT Connection**: `PskReporterMqtt` connects and subscribes to spots
2. **Spot Reception**: MQTT callback receives spot via `onSpotReceived()`
3. **Marker Creation**: Convert grid locator to Lat/Lng, create marker
4. **Map Update**: Add marker to map overlay, refresh display
5. **Lifecycle**: Handle spot expiration, marker cleanup

### Marker Design

**New Marker Class**: `PskReporterSpotMarker` (extends OSMDroid `Marker`)

- **Icon**: Distinct from CQ markers (e.g., different color/shape)
- **Info Window**: Shows callsign, frequency, grid, time since spot
- **Position**: Based on `receiverLocator` grid square
- **Styling**: Different color scheme to distinguish from CQ markers

---

## Implementation Steps

### Phase 1: Basic Marker Display (MVP)

**Goal**: Display spots as markers on the map when received via MQTT

#### Step 1.1: Create Spot Marker Class

**File**: `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/grid_tracker/PskReporterSpotMarker.java`

**Implementation**:
- Extend OSMDroid `Marker` class
- Constructor takes `PskReporterMqtt.Spot` and context
- Convert `receiverLocator` to Lat/Lng using `MaidenheadGrid.gridToLatLng()`
- Set marker icon (create new drawable or reuse existing with color filter)
- Create info window layout and adapter
- Set title, snippet, and sub-description

**Key Code Structure**:
```java
public class PskReporterSpotMarker extends Marker {
    private final PskReporterMqtt.Spot spot;
    
    public PskReporterSpotMarker(Context context, MapView mapView, 
                                 PskReporterMqtt.Spot spot) {
        super(mapView);
        this.spot = spot;
        
        // Convert grid to coordinates
        LatLng latLng = MaidenheadGrid.gridToLatLng(spot.receiverLocator);
        if (latLng != null) {
            this.setPosition(new GeoPoint(latLng.latitude, latLng.longitude));
        }
        
        // Set icon, info window, etc.
    }
}
```

#### Step 1.2: Add Marker Management to GridOsmMapView

**File**: `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/grid_tracker/GridOsmMapView.java`

**Add**:
- `private final ArrayList<PskReporterSpotMarker> spotMarkers = new ArrayList<>();`
- `public synchronized PskReporterSpotMarker addSpotMarker(PskReporterMqtt.Spot spot)`
- `public synchronized void clearSpotMarkers()`
- `public synchronized void removeSpotMarker(PskReporterSpotMarker marker)`

**Implementation Notes**:
- Follow existing pattern from `addGridMarker()` and `clearMarkers()`
- Validate grid locator before creating marker
- Handle null/invalid grid locators gracefully

#### Step 1.3: Integrate MQTT Callback in GridTrackerMainActivity

**File**: `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/grid_tracker/GridTrackerMainActivity.java`

**Add**:
- `private PskReporterMqtt pskReporterMqtt;` field
- Initialize MQTT client in `onCreate()` or `doAfterCreate()`
- Implement `PskReporterMqtt.Callback` to add markers when spots received
- Handle connection state (connected/disconnected/error)

**Key Code Structure**:
```java
pskReporterMqtt = new PskReporterMqtt();
pskReporterMqtt.start(new PskReporterMqtt.Callback() {
    @Override
    public void onSpotReceived(PskReporterMqtt.Spot spot) {
        runOnUiThread(() -> {
            if (!spot.receiverLocator.isEmpty()) {
                gridOsmMapView.addSpotMarker(spot);
            }
        });
    }
    // ... other callbacks
});
```

#### Step 1.4: Create Info Window Layout

**File**: `ft8cn/app/src/main/res/layout/tracker_psk_spot_info_win.xml`

**Content**:
- Callsign (title)
- Grid locator
- Frequency (formatted)
- Time since spot (e.g., "5 min ago")
- Distance from user's location (optional)

#### Step 1.5: Create Marker Icon

**Options**:
- Create new drawable: `ic_baseline_psk_spot_24.xml`
- Or reuse existing icon with color filter (different color from CQ markers)
- Consider using a distinct shape (e.g., circle vs. square)

**File**: `ft8cn/app/src/main/res/drawable/ic_baseline_psk_spot_24.xml`

#### Step 1.6: Handle Lifecycle

**In GridTrackerMainActivity**:
- Start MQTT in `onResume()` or `doAfterCreate()`
- Stop MQTT in `onPause()` or `onDestroy()`
- Clear spot markers when appropriate

---

### Phase 2: UI Controls and Filtering

**Goal**: Add user controls to show/hide spots and manage display

#### Step 2.1: Add Toggle Switch

**File**: `ft8cn/app/src/main/res/layout/activity_grid_tracker_main.xml`

**Add**:
- Toggle switch for "Show PSK Reporter Spots" (similar to existing CQ/QSX switches)
- Place near existing toggle controls

#### Step 2.2: Implement Toggle Functionality

**File**: `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/grid_tracker/GridTrackerMainActivity.java`

**Add**:
- `setShowPskSpotsSwitchClickListener()` method
- Save preference: `DataConfigShowPskSpots = "tracker_show_psk_spots"`
- Load preference in `readConfig()`
- Call `gridOsmMapView.setShowPskSpots(boolean)` to show/hide markers

**File**: `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/grid_tracker/GridOsmMapView.java`

**Add**:
- `private boolean showPskSpots = true;` field
- `public void setShowPskSpots(boolean show)` method
- Hide/show markers based on flag

#### Step 2.3: Add Clear Spots Button (Optional)

**Location**: Near existing clear/refresh buttons

**Functionality**:
- Clear all spot markers from map
- Optionally clear MQTT spot list

---

### Phase 3: Polylines and Enhanced Display

**Goal**: Draw lines from user's location to spot locations

#### Step 3.1: Create Spot Polyline Class

**File**: `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/grid_tracker/PskReporterSpotPolyline.java`

**Implementation**:
- Extend OSMDroid `Polyline` class
- Draw line from user's grid to spot's grid
- Use distinct color/style from QSO lines
- Optional: Fade based on spot age

#### Step 3.2: Add Polyline Management

**File**: `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/grid_tracker/GridOsmMapView.java`

**Add**:
- `private final ArrayList<PskReporterSpotPolyline> spotPolylines = new ArrayList<>();`
- `public synchronized void addSpotPolyline(PskReporterMqtt.Spot spot)`
- `public synchronized void clearSpotPolylines()`
- Toggle for showing/hiding polylines

**Implementation Notes**:
- Get user's grid from `GeneralVariables.getMyMaidenheadGrid()`
- Convert both grids to Lat/Lng
- Create polyline between points
- Use dashed or different color to distinguish from QSO lines

---

### Phase 4: Spot Expiration and Cleanup

**Goal**: Automatically remove old spots from map

#### Step 4.1: Implement Spot Expiration

**File**: `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/grid_tracker/GridOsmMapView.java`

**Add**:
- `private static final long SPOT_EXPIRATION_SEC = 3600;` // 1 hour
- Method to check spot age: `isSpotExpired(PskReporterMqtt.Spot spot)`
- Periodic cleanup task (e.g., every 5 minutes)

**Implementation**:
```java
private void cleanupExpiredSpots() {
    long now = System.currentTimeMillis() / 1000;
    synchronized (spotMarkers) {
        Iterator<PskReporterSpotMarker> it = spotMarkers.iterator();
        while (it.hasNext()) {
            PskReporterSpotMarker marker = it.next();
            if (now - marker.getSpot().flowStartSeconds > SPOT_EXPIRATION_SEC) {
                gridMapView.getOverlays().remove(marker);
                it.remove();
            }
        }
    }
    gridMapView.invalidate();
}
```

#### Step 4.2: Limit Marker Count

**Add**:
- Maximum marker limit (e.g., 100 markers)
- When limit reached, remove oldest markers
- Sort by timestamp, keep most recent

---

### Phase 5: Performance and Polish

**Goal**: Optimize performance and improve user experience

#### Step 5.1: Marker Deduplication

**Implementation**:
- Track markers by key: `receiverCallsign:frequency`
- Update existing marker instead of creating duplicate
- Update timestamp and refresh display

#### Step 5.2: Batch Updates

**Implementation**:
- Collect multiple spots over short period (e.g., 1 second)
- Add all markers in single batch
- Reduces map refresh overhead

#### Step 5.3: Info Window Enhancements

**Enhancements**:
- Show distance from user's location
- Format frequency nicely (e.g., "14.074 MHz")
- Show time since spot in human-readable format
- Optional: Show SNR if available in future

#### Step 5.4: Visual Improvements

**Considerations**:
- Marker clustering for dense areas (future enhancement)
- Different colors for different bands
- Animation when marker appears
- Fade out when spot expires

---

## Technical Details

### Grid Locator Validation

**Location**: `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/maidenhead/MaidenheadGrid.java`

**Validation**:
- Check if `receiverLocator` is not empty
- Validate format (2, 4, or 6 characters)
- Handle invalid grids gracefully (skip marker creation)

**Code**:
```java
LatLng latLng = MaidenheadGrid.gridToLatLng(spot.receiverLocator);
if (latLng == null) {
    Log.w(TAG, "Invalid grid locator: " + spot.receiverLocator);
    return null; // Skip marker creation
}
```

### Thread Safety

**Considerations**:
- MQTT callbacks run on background thread
- Map updates must run on UI thread
- Use `runOnUiThread()` or `Handler` for UI updates
- Synchronize marker list access

**Pattern**:
```java
@Override
public void onSpotReceived(PskReporterMqtt.Spot spot) {
    activity.runOnUiThread(() -> {
        gridOsmMapView.addSpotMarker(spot);
    });
}
```

### Memory Management

**Considerations**:
- Limit total markers (e.g., 100-200 max)
- Remove expired markers regularly
- Clear markers when activity paused/destroyed
- Consider weak references if needed

### Configuration Persistence

**Location**: `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/database/DatabaseOpr.java`

**Add**:
- Save preference: `tracker_show_psk_spots` (boolean)
- Load in `readConfig()` method
- Similar to existing `DataConfigShowCQ` and `DataConfigShowQsx`

---

## UI/UX Considerations

### Visual Design

**Marker Appearance**:
- **Color**: Distinct from CQ markers (e.g., green/blue vs. red/yellow)
- **Icon**: Different shape or style
- **Size**: Similar to CQ markers for consistency
- **Info Window**: Clean, readable layout

**Color Scheme Options**:
- Green: For spots (signal received)
- Blue: Alternative option
- Purple: Another distinct option

### User Controls

**Toggle Switch**:
- Label: "Show PSK Reporter Spots" or similar
- Location: Near existing CQ/QSX toggles
- State: Persisted across app sessions

**Info Window Content**:
- Callsign (prominent)
- Grid locator
- Frequency (formatted)
- Time since spot
- Distance (optional)

### Interaction

**Marker Click**:
- Show info window with spot details
- Optional: Center map on marker
- Optional: Show polyline to spot

**Map Behavior**:
- Markers appear automatically when spots received
- Markers update if same station spotted again
- Old markers fade or disappear after expiration

---

## Testing Approach

### Unit Tests

**Test Cases**:
1. Grid locator validation (valid/invalid grids)
2. Marker creation with valid spot
3. Marker deduplication (same callsign/frequency)
4. Spot expiration logic
5. Marker limit enforcement

**Location**: `ft8cn/app/src/test/java/com/bg7yoz/ft8cn/grid_tracker/`

### Integration Tests

**Test Cases**:
1. MQTT connection and spot reception
2. Marker appears on map when spot received
3. Toggle switch shows/hides markers
4. Marker cleanup on expiration
5. Multiple spots handled correctly

### Manual Testing

**Scenarios**:
1. Start Grid Tracker, verify MQTT connects
2. Receive spot, verify marker appears
3. Click marker, verify info window shows
4. Toggle switch off, verify markers hidden
5. Wait for expiration, verify markers removed
6. Receive many spots, verify performance
7. Close and reopen activity, verify state restored

---

## Potential Issues and Solutions

### Issue 1: Invalid Grid Locators

**Problem**: Some spots may have empty or invalid grid locators

**Solution**:
- Validate grid before creating marker
- Skip marker creation if grid invalid
- Log warning for debugging

### Issue 2: Performance with Many Spots

**Problem**: Too many markers could slow map rendering

**Solution**:
- Limit total markers (e.g., 100-200)
- Remove oldest markers when limit reached
- Consider marker clustering (future enhancement)
- Batch marker updates

### Issue 3: MQTT Connection Lifecycle

**Problem**: MQTT connection needs proper start/stop with activity lifecycle

**Solution**:
- Start MQTT in `onResume()` or `doAfterCreate()`
- Stop MQTT in `onPause()` or `onDestroy()`
- Handle reconnection on resume
- Clear markers on disconnect

### Issue 4: Thread Safety

**Problem**: MQTT callbacks run on background thread, map updates need UI thread

**Solution**:
- Always use `runOnUiThread()` for map updates
- Synchronize marker list access
- Use `Handler` if needed for delayed updates

### Issue 5: Marker Deduplication

**Problem**: Same station may be spotted multiple times

**Solution**:
- Track markers by `receiverCallsign:frequency` key
- Update existing marker instead of creating duplicate
- Refresh timestamp and info window

### Issue 6: Memory Leaks

**Problem**: Markers not cleaned up properly

**Solution**:
- Clear markers in `onPause()` or `onDestroy()`
- Remove from overlay manager
- Clear references
- Implement proper cleanup in `clearSpotMarkers()`

---

## Future Enhancements

### Phase 6: Advanced Features (Future)

1. **Marker Clustering**: Group nearby markers when zoomed out
2. **Band Filtering**: Show/hide spots by frequency band
3. **Time Range Filter**: Show spots from last N minutes/hours
4. **Statistics**: Show count of spots per region/country
5. **Export**: Export spot data to file
6. **Heat Map**: Show signal strength as heat map overlay
7. **Animation**: Animate marker appearance/disappearance
8. **Search**: Search for specific callsign in spots
9. **History**: Keep spot history beyond expiration for viewing
10. **Notifications**: Alert when new spot received

---

## File Structure

### New Files to Create

```
ft8cn/app/src/main/java/com/bg7yoz/ft8cn/grid_tracker/
    └── PskReporterSpotMarker.java          (Phase 1)
    └── PskReporterSpotPolyline.java       (Phase 3, optional)

ft8cn/app/src/main/res/layout/
    └── tracker_psk_spot_info_win.xml       (Phase 1)

ft8cn/app/src/main/res/drawable/
    └── ic_baseline_psk_spot_24.xml         (Phase 1)
```

### Files to Modify

```
ft8cn/app/src/main/java/com/bg7yoz/ft8cn/grid_tracker/
    └── GridOsmMapView.java                 (Phases 1, 2, 3, 4)
    └── GridTrackerMainActivity.java        (Phases 1, 2, 4)

ft8cn/app/src/main/res/layout/
    └── activity_grid_tracker_main.xml      (Phase 2)

ft8cn/app/src/main/res/values/
    └── strings.xml                         (Phase 2, UI strings)

ft8cn/app/src/main/java/com/bg7yoz/ft8cn/database/
    └── DatabaseOpr.java                    (Phase 2, config persistence)
```

---

## Implementation Checklist

### Phase 1: Basic Marker Display
- [ ] Create `PskReporterSpotMarker` class
- [ ] Add marker management methods to `GridOsmMapView`
- [ ] Integrate MQTT callback in `GridTrackerMainActivity`
- [ ] Create info window layout
- [ ] Create marker icon/drawable
- [ ] Test basic marker display

### Phase 2: UI Controls
- [ ] Add toggle switch to layout
- [ ] Implement toggle functionality
- [ ] Add configuration persistence
- [ ] Test show/hide functionality

### Phase 3: Polylines (Optional)
- [ ] Create `PskReporterSpotPolyline` class
- [ ] Add polyline management
- [ ] Test polyline display

### Phase 4: Expiration and Cleanup
- [ ] Implement spot expiration logic
- [ ] Add periodic cleanup task
- [ ] Implement marker limit
- [ ] Test expiration and cleanup

### Phase 5: Performance and Polish
- [ ] Implement marker deduplication
- [ ] Add batch updates
- [ ] Enhance info window
- [ ] Performance testing
- [ ] Final polish and testing

---

## Estimated Effort

### Phase 1: Basic Marker Display
- **Time**: 4-6 hours
- **Complexity**: Medium
- **Dependencies**: None

### Phase 2: UI Controls
- **Time**: 2-3 hours
- **Complexity**: Low
- **Dependencies**: Phase 1

### Phase 3: Polylines (Optional)
- **Time**: 2-3 hours
- **Complexity**: Medium
- **Dependencies**: Phase 1

### Phase 4: Expiration and Cleanup
- **Time**: 2-3 hours
- **Complexity**: Medium
- **Dependencies**: Phase 1

### Phase 5: Performance and Polish
- **Time**: 3-4 hours
- **Complexity**: Medium-High
- **Dependencies**: Phases 1-4

### Total Estimated Effort
- **Minimum (Phases 1-2)**: 6-9 hours
- **Full Implementation (Phases 1-5)**: 13-19 hours

---

## Notes

- Start with Phase 1 (MVP) to validate approach
- Test with real MQTT spots before proceeding
- Consider user feedback before implementing Phase 3 (polylines)
- Performance testing important before Phase 5
- Follow existing code patterns and conventions
- Maintain consistency with existing CQ marker implementation

---

## References

- **PSK Reporter MQTT**: `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/psk/PskReporterMqtt.java`
- **Grid Tracker Activity**: `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/grid_tracker/GridTrackerMainActivity.java`
- **Map View**: `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/grid_tracker/GridOsmMapView.java`
- **Grid Conversion**: `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/maidenhead/MaidenheadGrid.java`
- **Existing Marker**: `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/grid_tracker/GridOsmMapView.java` (GridMarker class)
- **PSK Reporter Documentation**: `wiki/PSK_REPORTER_IMPLEMENTATION.md`
