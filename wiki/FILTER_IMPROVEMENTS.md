# QSO Log Filter Improvements

## Overview

Enhanced the QSO log filtering system to support QRZ upload status filtering, date range filtering, and comment text filtering. These filters work together with callsign search, and are applied to both the log display and ADIF export/share functionality.

---

## 1. QRZ Upload Status Filter

### Problem
Users needed to filter QSO logs based on whether they had been uploaded to QRZ.com or not, to identify which QSOs still needed to be uploaded.

### Solution
Added a filter option with three states:
- **All** (0): Show all QSOs regardless of QRZ upload status
- **QRZ Uploaded** (1): Show only QSOs that have been uploaded to QRZ.com (`isQRZ_uploaded = 1`)
- **QRZ Missing** (2): Show only QSOs that have not been uploaded (`isQRZ_uploaded = 0` or `NULL`)

### Implementation Details
- **Location**: `MainViewModel.queryQRZFilter` (int: 0, 1, or 2)
- **UI**: `FilterDialog` - Radio button group for QRZ upload status
- **Database Query**: Updated `DatabaseOpr.GetQSLByCallsign` and `GetQLSCallsignByCallsign` to include QRZ filter in SQL WHERE clause
- **SQL Filter Logic**:
  - Case 1: `AND (isQRZ_uploaded = 1)` - Only uploaded QSOs
  - Case 2: `AND ((isQRZ_uploaded = 0) OR (isQRZ_uploaded IS NULL))` - Only missing QSOs
  - Case 0: No filter applied

### Impact
- **Before**: Users could only filter by callsign search
- **After**: Users can filter by QRZ upload status, making it easy to identify QSOs that need to be uploaded

---

## 2. Date Range Filter

### Problem
Users needed to filter QSO logs by date range to export or view QSOs from specific time periods.

### Solution
Added date range filtering with start date and end date fields:
- **Start Date**: Filter QSOs with `qso_date_off >= startDate`
- **End Date**: Filter QSOs with `qso_date_off <= endDate`
- **Format**: YYYYMMDD (e.g., "20240101" for January 1, 2024)
- **Empty fields**: No date filtering applied if field is empty

### Implementation Details
- **Location**: 
  - `MainViewModel.queryStartDate` (String: YYYYMMDD format)
  - `MainViewModel.queryEndDate` (String: YYYYMMDD format)
- **UI**: `FilterDialog` - Two EditText fields for start and end dates
- **Database Query**: Updated database queries to include date range in SQL WHERE clause
- **SQL Filter Logic**:
  - Start date: `AND (SUBSTR(qso_date_off,1,8) >= "YYYYMMDD")`
  - End date: `AND (SUBSTR(qso_date_off,1,8) <= "YYYYMMDD")`
  - Uses `qso_date_off` field (QSO end date) for filtering

### Impact
- **Before**: Users could only filter by callsign search
- **After**: Users can filter by date range, enabling time-based log analysis and export

---

## 3. Comment Text Filter

### Problem
Users needed to search for QSOs based on text content in the comment field, such as searching for specific locations, distances, or other information stored in comments.

### Solution
Added comment text filtering:
- **Comment Text**: Filter QSOs where comment field contains the search text (case-insensitive LIKE search)
- **Empty field**: No comment filtering applied if field is empty
- **Search behavior**: Uses SQL `LIKE` with wildcards (`%text%`)

### Implementation Details
- **Location**: `MainViewModel.queryCommentFilter` (String: search text)
- **UI**: `FilterDialog` - EditText field for comment text search
- **Database Query**: Updated database queries to include comment filter in SQL WHERE clause
- **SQL Filter Logic**:
  - `AND (comment LIKE ?)` where parameter is `%searchText%`
  - Case-insensitive search (SQLite LIKE is case-insensitive for ASCII)

### Impact
- **Before**: Users could only filter by callsign search
- **After**: Users can search within comment fields, enabling location-based, distance-based, or other comment-based filtering

---

## 4. Filter Combination

### Problem
All filters needed to work together seamlessly, combining comment text search, QRZ upload status, callsign search, and date range.

### Solution
Implemented filter combination logic where all active filters are combined with SQL `AND` clauses:
```
WHERE ([call] LIKE ?)
  AND [Comment Text Filter]
  AND [QRZ Upload Status Filter]
  AND [Date Range Filters]
```

### Implementation Details
- **Filter Order**: Filters are applied in this order:
  1. Callsign search (always applied)
  2. Comment text filter (if provided)
  3. QRZ upload status filter (if not "All")
  4. Start date filter (if provided)
  5. End date filter (if provided)
- **Location**: `DatabaseOpr.GetQSLByCallsign` and `GetQLSCallsignByCallsign` classes
- **Method**: Uses `StringBuilder` to build filter SQL string dynamically
- **Query Parameters**: Dynamically builds parameter array based on which filters are active

### Impact
- **Before**: Only callsign filter was available
- **After**: All four filter types can be combined for precise log filtering

---

## 5. Share ADIF Filter Integration

### Problem
The share ADIF functionality needed to respect all filter settings, not just callsign search.

### Solution
Updated `ShareLogs` class to accept and apply all filter parameters:
- Comment text filter
- QRZ upload status filter
- Date range filter (start and end dates)
- Existing callsign filter

### Implementation Details
- **Location**: `ShareLogs.makeSQL()`, `getCount()`, `getData()`, `downQSLTableToFile()`, `doShareLogs()`
- **Changes**:
  - Updated `makeSQL()` to accept `commentFilter`, `qrzFilter`, `startDate`, `endDate` parameters
  - Removed `queryFilter` parameter (QSL status filter removed)
  - Updated method signatures to pass new filter parameters
  - Updated `LogFragment.buildShareLogs()` to pass all filter values from `MainViewModel`
- **Filter Application**: Share ADIF uses the same SQL filter logic as log display queries
- **Query Parameters**: Dynamically builds parameter array based on active filters

### Impact
- **Before**: Share ADIF only respected callsign filter
- **After**: Share ADIF respects all filters (comment text, QRZ upload status, date range, callsign), ensuring exported ADIF matches the filtered view

---

## 6. Filter Visual Indicator

### Problem
Users needed a visual indication when filters are active on the QSO logs page, so they know the displayed list is filtered.

### Solution
Added a filter indicator TextView below the action bar that displays which filters are currently active. The indicator:
- Shows only when at least one filter is active
- Lists all active filters in a comma-separated format
- Updates automatically when filters change
- Uses a styled background to make it visually distinct

### Implementation Details
- **Location**: `fragment_log.xml` - TextView below action bar
- **Background**: `filter_indicator_background.xml` - Semi-transparent blue background with rounded corners
- **Update Method**: `LogFragment.updateFilterIndicator()` - Builds filter text and shows/hides indicator
- **Observers**: All filter LiveData observers call `updateFilterIndicator()` when filters change
- **Display Format**: "Filters active: Comment, QRZ Missing, Date Range" (shows only active filters)

### Filter Detection Logic
- **Comment Filter**: Active if `queryCommentFilter` is not null and not empty
- **QRZ Filter**: Active if `queryQRZFilter` is 1 (uploaded) or 2 (missing)
- **Date Range Filter**: Active if `queryStartDate` or `queryEndDate` is not empty

### Impact
- **Before**: No visual indication when filters were active
- **After**: Clear visual indicator shows which filters are applied, making it obvious when the list is filtered

---

## 7. UI Updates

### Filter Dialog Enhancements

#### Layout Changes
- **File**: `filter_dialog_layout.xml`
- **Changes**:
  - Removed QSL Status Filter section (confirmed/unconfirmed)
  - Added Comment Text Filter section with EditText field
  - Kept QRZ Upload Status Filter section
  - Kept Date Range Filter section
  - Uses ScrollView to accommodate all filter options
  - Increased dialog height to 70% of screen height

#### Filter Dialog Code
- **File**: `FilterDialog.java`
- **Changes**:
  - Removed QSL status filter radio button handlers
  - Added TextWatcher for comment filter EditText
  - Updated `show()` method to restore comment filter state from `MainViewModel`
  - Real-time filter updates via LiveData observers

### String Resources
- **File**: `strings.xml`
- **Removed Strings**:
  - `filter_is_qsl`: "Show confirmed" (removed)
  - `filter_none_qsl`: "Show unconfirmed" (removed)
- **New Strings**:
  - `filter_comment`: "Comment Text"
  - `filter_comment_hint`: "Search in comments"
- **Existing Strings** (kept):
  - `filter_all`: "Show all"
  - `filter_qrz_uploaded`: "QRZ uploaded"
  - `filter_qrz_missing`: "QRZ missing"
  - `filter_date_range`: "Date Range"
  - `filter_start_date`: "Start Date"
  - `filter_end_date`: "End Date"
  - `filter_date_format_hint`: "YYYYMMDD"

---

## 7. Database Query Updates

### Method Signatures
Updated database query methods to support new filters and remove QSL status filter:

**Before**:
```java
getQSLRecordByCallsign(boolean showAll, int offset, String callsign, int filter, ...)
getQSLCallsignsByCallsign(boolean showAll, int offset, String callsign, int filter, ...)
```

**After**:
```java
getQSLRecordByCallsign(boolean showAll, int offset, String callsign, 
                       int qrzFilter, String commentFilter, String startDate, String endDate, ...)
getQSLCallsignsByCallsign(boolean showAll, int offset, String callsign,
                          int qrzFilter, String commentFilter, String startDate, String endDate, ...)
```

### Query Parameter Handling
- **Comment Filter**: When comment filter is provided, adds `?` parameter for `LIKE` clause
- **Dynamic Parameters**: Query parameter array is built based on which filters are active
- **Example**: If comment filter is active, query uses 3 parameters: `[callsign, callsign, comment]`

### SQL Query Structure
```sql
SELECT * FROM QSLTable 
WHERE ([call] LIKE ?)
  AND [Comment Text Filter - if provided]
  AND [QRZ Upload Status Filter - if not "All"]
  AND [Start Date Filter - if provided]
  AND [End Date Filter - if provided]
ORDER BY qso_date DESC, time_off DESC
LIMIT 100 OFFSET [offset]
```

---

## 8. LiveData Observers

### Filter State Management
Added LiveData observers in `LogFragment` to automatically refresh log display when filters change:

- `mutableQueryCommentFilter`: Observes comment filter changes
- `mutableQueryQRZFilter`: Observes QRZ filter changes
- `mutableQueryStartDate`: Observes start date changes
- `mutableQueryEndDate`: Observes end date changes

**Removed**: `mutableQueryFilter` observer (QSL status filter removed)

All observers call `queryByCallsign()` to refresh the log display with updated filters.

---

## Summary

### Files Modified
1. **MainViewModel.java**: Removed `queryFilter`, added `queryCommentFilter` and LiveData
2. **FilterDialog.java**: Removed QSL status UI, added comment text field
3. **filter_dialog_layout.xml**: Removed QSL filter section, added comment filter section
4. **DatabaseOpr.java**: Updated query methods to remove QSL filter, add comment filter
5. **ShareLogs.java**: Updated to remove QSL filter, add comment filter
6. **LogFragment.java**: Updated to use new filters, observe comment filter changes, and show filter indicator
7. **fragment_log.xml**: Added filter indicator TextView
8. **filter_indicator_background.xml**: Created drawable background for filter indicator
9. **GridTrackerMainActivity.java**: Updated to use new method signature
10. **strings.xml**: Removed QSL filter strings, added comment filter strings and filter indicator strings

### Filter Types Supported
1. **Callsign Search**: Text search in callsign fields (existing)
2. **Comment Text**: Text search in comment field (new)
3. **QRZ Upload Status**: All / Uploaded / Missing (existing)
4. **Date Range**: Start date and/or end date (existing)

### Removed Filters
- **QSL Status**: Confirmed / Unconfirmed filter removed

### Benefits
- **Better Search**: Users can search within comment fields for location, distance, or other information
- **Simplified UI**: Removed QSL status filter reduces complexity
- **Consistent Filtering**: Share ADIF respects all filter settings
- **Flexible Combinations**: All filters work together seamlessly

---

## Usage Examples

### Filter for QSOs with Specific Location in Comments
1. Open Filter dialog
2. Enter location name in Comment Text field (e.g., "Japan")
3. Log display shows only QSOs with "Japan" in the comment field
4. Share ADIF will export only those QSOs

### Filter for Unuploaded QSOs from Last Month with Distance Info
1. Open Filter dialog
2. Set QRZ filter to "QRZ missing"
3. Set Start Date to first day of last month (e.g., "20240101")
4. Set End Date to last day of last month (e.g., "20240131")
5. Enter "Distance" in Comment Text field
6. Share ADIF will export only unuploaded QSOs from that month with distance information

### Filter for QSOs with Specific Callsign and Comment Text
1. Enter callsign in main search field (e.g., "W1ABC")
2. Open Filter dialog
3. Enter text in Comment Text field (e.g., "contest")
4. Log display shows only QSOs with that callsign containing "contest" in comments

---

## Technical Notes

- Comment filter uses SQL `LIKE` with wildcards for partial matching
- Date format is YYYYMMDD (8 digits, no separators)
- Date filtering uses `qso_date_off` field (QSO end date)
- QRZ filter handles NULL values (treats as not uploaded)
- All filters are applied at database query level for efficiency
- Filter state persists during app session via MainViewModel
- Share ADIF uses same filter logic as log display queries
- Query parameters are built dynamically based on active filters
