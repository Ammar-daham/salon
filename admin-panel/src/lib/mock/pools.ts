/** Name and detail pools for fixture data. Nordic-leaning to match the seeded
 *  salons, with enough variety that a 6-salon platform doesn't look cloned. */

export const FIRST_NAMES = [
	"Aino", "Elias", "Sofia", "Onni", "Venla", "Leevi", "Emma", "Niilo",
	"Iida", "Eetu", "Helmi", "Väinö", "Aada", "Oliver", "Ellen", "Joel",
	"Lotta", "Miro", "Sanni", "Rasmus", "Nea", "Aleksi", "Pihla", "Samu",
	"Linnea", "Otto", "Vilma", "Kasper", "Eevi", "Daniel", "Anni", "Leo",
] as const;

export const LAST_NAMES = [
	"Virtanen", "Korhonen", "Mäkinen", "Nieminen", "Mäkelä", "Hämäläinen",
	"Laine", "Heikkinen", "Koskinen", "Järvinen", "Lehtonen", "Lehtinen",
	"Saarinen", "Salminen", "Heinonen", "Niemi", "Heikkilä", "Kinnunen",
	"Salonen", "Turunen", "Salo", "Laitinen", "Tuominen", "Rantanen",
] as const;

export const STAFF_TITLES = [
	"Senior stylist",
	"Stylist",
	"Junior stylist",
	"Colour specialist",
	"Barber",
	"Beauty therapist",
	"Massage therapist",
	"Nail technician",
	"Salon manager",
	"Apprentice",
] as const;

export const CUSTOMER_TAGS = [
	"Regular",
	"New client",
	"Prefers mornings",
	"Sensitive scalp",
	"Allergy: PPD",
	"Referral",
	"Loyalty member",
] as const;

export const CUSTOMER_NOTES = [
	"Prefers a quieter appointment slot when possible.",
	"Books every six weeks, usually with the same stylist.",
	"Allergic to PPD — patch test before any colour service.",
	"Happy with the current shade; wants to go slightly cooler next time.",
	"Usually runs about ten minutes late.",
	"Asked about a package price for cut and colour together.",
	"Prefers minimal product; sensitive to strong fragrance.",
] as const;

export const WEEKDAYS = [
	"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday",
] as const;

export type Weekday = (typeof WEEKDAYS)[number];
