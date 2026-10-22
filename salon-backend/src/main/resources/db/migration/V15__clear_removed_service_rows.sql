/*
  =======================
   Removing a service clears who performs it
  =======================
 */
-- SalonServiceDataAccessService.deleteServiceById now deletes these as it stamps deleted_at: no read reaches a
-- removed service, and it can't be given to anyone again, so they'd never be seen again. Its appointments
-- stay, as the salon's history. This clears what earlier removals left behind, as V14 does for staff.
DELETE FROM staff_services WHERE service_id IN (SELECT id FROM services WHERE deleted_at IS NOT NULL);
