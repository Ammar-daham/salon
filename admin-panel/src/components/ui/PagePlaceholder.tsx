import React from "react";
import PageHeader from "./PageHeader";
import EmptyState from "./EmptyState";
import { BoxCubeIcon } from "@/icons";

interface PagePlaceholderProps {
	title: string;
	description: string;
	/** Which delivery phase fills this page in. Shown so a half-built panel is
	 *  never mistaken for a broken one. */
	phase: string;
	detail: string;
}

/**
 * Temporary landing for a route that exists in the navigation but whose feature
 * has not been built yet. Every nav item must lead somewhere — a dead link
 * reads as a bug, an honest placeholder reads as a roadmap.
 */
export default function PagePlaceholder({
	title,
	description,
	phase,
	detail,
}: PagePlaceholderProps) {
	return (
		<>
			<PageHeader title={title} description={description} />
			<EmptyState
				icon={<BoxCubeIcon className="size-6" />}
				title={`Coming in ${phase}`}
				description={detail}
			/>
		</>
	);
}
