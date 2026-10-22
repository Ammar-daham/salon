/** What an owner fills in to register their salon: their own account, then the salon. */
export interface RegistrationInput {
	firstName: string;
	lastName: string;
	email: string;
	password: string;
	salonName: string;
	description: string;
	image: string;
	currency: string;
	timezone: string;
	street: string;
	postalCode: string;
	city: string;
	country: string;
	/** Optional: the salon's phone, sent as its one contact. */
	phone: string;
}
