# Task 3: Leave Service + Controller

## Status
DONE

## Commits
(none)

## Test summary
N/A

## Concerns
- The `LeaveDao` uses MySQL `LIMIT` syntax in `selectByNumberPage`, which is MySQL-specific. If the project ever switches databases, this will need adjustment.
- `Leave.setDept_name()` and `Leave.setStart_date()` rely on Lombok `@Data` generating setters from the field names. Verify that Lombok annotation processing is enabled in the IDE/build.
- Session attribute key `"emp"` must match the key set during login in the authentication controller.
