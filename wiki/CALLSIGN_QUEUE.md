# Callsign Queue Feature

## Overview

The Callsign Queue feature provides intelligent management of multiple callsigns responding to your callsign during FT8 operations. When multiple stations respond to your CQ or call, the system automatically queues them and processes them in order, ensuring you don't miss contacts and can efficiently manage multiple QSOs.

## Features

### Automatic Queue Management

- **Automatic Queuing**: When multiple callsigns respond to your callsign, they are automatically added to a queue
- **Default Processing Order**: By default, the system processes callsigns in order of oldest first, then newest
- **Queue Display**: The queue is displayed in the sequence details section on the Calling tab, showing all queued callsigns with their position numbers

### Manual Queue Control

- **Reorder Items**: Long-press and drag queue items up or down to manually reorder them
- **Delete Items**: Swipe left or right on any queue item to remove it from the queue
- **Visual Feedback**: 
  - Drag handle (⋮⋮) indicates items are draggable
  - Delete icon appears while swiping
  - Position numbers update automatically after reordering

### Integration with Auto Sequencing

- **Seamless Processing**: When a QSO completes, the system automatically moves to the next callsign in the queue
- **Active QSO Priority**: If you're already in a QSO with a callsign and they call again, the system responds immediately without adding them to the queue
- **Queue Updates**: The queue updates in real-time as callsigns are added, removed, or processed

## User Interface

### Location

The callsign queue is displayed in the **Call Sequence Details** section on the Calling tab:
- **Portrait Mode**: Below the message header, above the message list
- **Landscape Mode**: Below the spectrum view on the left side

### Layout

The sequence details section is split into two halves:
- **Left Side**: Sequence information (current order, next message, status, progress, last received)
- **Right Side**: Callsign queue (RecyclerView showing queued callsigns with position numbers)

### Queue Item Display

Each queue item shows:
- **Position Number**: The order in which the callsign will be processed (1-based)
- **Callsign**: The callsign waiting in the queue
- **Drag Handle**: Visual indicator (⋮⋮) that the item can be reordered

## How It Works

### Adding to Queue

1. When a callsign responds to your CQ or call, the system checks:
   - If you're already in a QSO with that callsign → Respond immediately
   - If you're in a QSO with a different callsign → Add to queue
   - If you're in CQ mode → Process immediately (oldest first)

2. Each queued callsign includes:
   - The callsign
   - Timestamp of when it was added
   - The initial message that triggered the queue entry

### Processing Queue

1. When a QSO completes or times out, the system:
   - Removes the completed/timed-out callsign from the queue
   - Automatically starts the next QSO with the first callsign in the queue
   - Updates the queue display

2. Queue processing respects the **No Reply Limit** configuration:
   - When a QSO times out due to no response (reaches the configured retry limit), the system automatically moves to the next queued callsign
   - The retry limit is configured in Settings → No Response Count
   - If set to 0 (ignore), the system will retry indefinitely until manually stopped
   - If set to a number (e.g., 3), the system will retry that many times before moving to the next queued callsign
   - Each queued callsign gets a fresh retry count when its QSO starts

3. The queue maintains order:
   - By default: Oldest first, then newest (based on timestamp)
   - After manual reordering: Follows the user-defined order

### Manual Operations

#### Reordering

1. Long-press on any queue item
2. Drag up or down to the desired position
3. Release to drop the item in the new position
4. The queue order updates immediately and persists

#### Deleting

1. Swipe left or right on any queue item
2. A delete icon appears while swiping
3. Release to delete the item from the queue
4. The item is removed from both the display and the processing queue

## Technical Implementation

### Components

- **`CallsignQueue.java`**: Core queue management class with thread-safe operations
- **`CallsignQueueAdapter.java`**: RecyclerView adapter for displaying the queue
- **`MyCallingFragment.java`**: UI integration and gesture handling
- **`FT8TransmitSignal.java`**: Integration with auto sequencing logic

### Key Methods

#### CallsignQueue
- `addCallsign(String callsign, Ft8Message initialMessage)`: Add a callsign to the queue
- `removeCallsign(String callsign)`: Remove a callsign by name
- `removeByIndex(int index)`: Remove a callsign by position
- `moveItem(int fromPosition, int toPosition)`: Reorder queue items
- `getNext()`: Get the next callsign to process (oldest first)
- `getAll()`: Get all queued callsigns in order
- `setOrder(ArrayList<QueuedCallsign> newOrder)`: Set queue order directly

#### CallsignQueueAdapter
- `updateQueue(List<QueuedCallsign> newQueue)`: Update the displayed queue
- `removeItem(int position)`: Remove an item and notify listeners
- `moveItem(int fromPosition, int toPosition)`: Move an item and notify listeners
- `getQueue()`: Get the current queue order

### Data Flow

1. **Message Received** → `checkCQMeOrFollowCQMessage()` in `FT8TransmitSignal`
2. **Queue Decision** → Add to queue or respond immediately
3. **Queue Update** → `mutableCallsignQueue.postValue()` notifies observers
4. **UI Update** → `MyCallingFragment` observer updates the adapter
5. **QSO Complete/Timeout** → Process next item in queue automatically
   - On successful completion: Removes callsign and moves to next
   - On timeout (no reply limit reached): Removes callsign and moves to next
   - Each new QSO starts with `noReplyCount = 0` (fresh retry count)

## Benefits

1. **No Missed Contacts**: Ensures all responding callsigns are queued and processed
2. **Efficient QSO Management**: Automatically moves to the next contact when one completes or times out
3. **Respects Retry Configuration**: Works seamlessly with the No Reply Limit setting - each queued callsign gets proper retry attempts before moving to the next
4. **User Control**: Manual reordering and deletion for priority management
5. **Visual Feedback**: Clear indication of queue status and position
6. **Seamless Integration**: Works automatically with existing auto sequencing and retry logic

## Usage Tips

1. **Monitor the Queue**: Keep an eye on the queue display to see how many callsigns are waiting
2. **Prioritize Important Contacts**: Use drag-and-drop to move priority callsigns to the top
3. **Remove Unwanted Contacts**: Swipe to delete callsigns you don't want to contact
4. **Let It Run**: The system automatically processes the queue, so you can focus on operating

## Future Enhancements

Potential improvements for future versions:
- Clear queue when switching frequencies
- Queue persistence across app restarts
- Queue statistics (total processed, average wait time)
- Priority levels for callsigns
- Queue export/import functionality
- Sound notifications when queue items are added

---

**Related Documentation**:
- [Auto Sequencing Improvements](AUTO_SEQUENCING_IMPROVEMENTS.md) - Details on the auto sequencing system
- [Decode Improvements](DECODE_IMPROVEMENTS.md) - Message decoding enhancements
