/*
  =======================
   Removing a staff member clears their shifts, services and time off
  =======================
 */
-- StaffDataAccessService.deleteStaffById now deletes these as it stamps deleted_at: no read reaches a removed
-- staff member, and one taken on again is a new staff record (V12), so they'd never be seen again. Their
-- appointments stay, as the salon's history. This clears what earlier removals left behind.
DELETE FROM staff_schedules WHERE staff_id IN (SELECT id FROM staff WHERE deleted_at IS NOT NULL);
DELETE FROM staff_services WHERE staff_id IN (SELECT id FROM staff WHERE deleted_at IS NOT NULL);
DELETE FROM staff_time_off WHERE staff_id IN (SELECT id FROM staff WHERE deleted_at IS NOT NULL);
