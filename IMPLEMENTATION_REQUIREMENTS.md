# Health Tracker MVP Requirements

## Product goal
Deliver an offline-first Android application that converts physical activity into game progress through story chapters, XP levels, achievements, and weekly challenges.

## MVP scope
- Track daily steps and derive estimated distance.
- Show daily and weekly activity progress.
- Let user configure a daily step goal.
- Calculate streak of completed goal days.
- Provide gamification:
  - XP and levels,
  - achievements,
  - weekly challenge,
  - story chapters unlocked by activity.
- Provide 4 main screens:
  - Today,
  - Story Map,
  - Achievements,
  - Statistics.

## Non-functional requirements
- Works fully offline after installation.
- Data persists locally and survives app restarts.
- UI updates reactively when activity data changes.
- Architecture supports future cloud sync via repository interfaces.

## User stories
- As a user, I want to see my steps today so I can monitor progress.
- As a user, I want a daily goal so I can stay motivated.
- As a user, I want rewards and levels so activity feels engaging.
- As a user, I want a story path that unlocks with walking.
- As a user, I want achievements so I can track milestones.
- As a user, I want weekly challenge progress for medium-term motivation.

## Acceptance criteria
- Daily steps can be increased and saved.
- Today screen shows: steps, distance, goal progress, level, XP, streak.
- Story screen shows chapter list with lock/unlock state.
- Achievements screen shows unlocked/locked badges and progress notes.
- Statistics screen shows last 7 days and total metrics.
- Weekly challenge recalculates automatically at least once per day.
- Unit tests verify XP/level and achievement unlock logic.

## Out-of-scope for this MVP
- Real-time multiplayer.
- Mandatory cloud account.
- Complex anti-cheat logic.
