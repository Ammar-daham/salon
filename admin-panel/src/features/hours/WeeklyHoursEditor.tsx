"use client";

import { useId, useMemo, useRef, useState, type FormEvent } from "react";
import Button from "@/components/ui/button/Button";
import { TextInput } from "@/components/ui/form/Field";
import { PlusIcon, TrashBinIcon } from "@/icons";
import {
	DAYS_OF_WEEK,
	DAY_LABELS,
	type DayOfWeek,
	type WeeklyInterval,
} from "@/lib/resources/hours/hours.types";
import { addMinutes, weekErrors } from "@/lib/resources/hours/hours.validation";

interface Draft extends WeeklyInterval {
	/** A stable React key while the interval's times are being edited. */
	key: number;
}

/** Day, then start, then end: the order the backend returns, so an untouched week compares equal. */
function normalised(intervals: WeeklyInterval[]): string {
	const sorted = intervals
		.map(({ dayOfWeek, start, end }) => ({ dayOfWeek, start, end }))
		.sort(
			(a, b) =>
				DAYS_OF_WEEK.indexOf(a.dayOfWeek) - DAYS_OF_WEEK.indexOf(b.dayOfWeek) ||
				a.start.localeCompare(b.start) ||
				a.end.localeCompare(b.end),
		);
	return JSON.stringify(sorted);
}

interface WeeklyHoursEditorProps {
	initial: WeeklyInterval[];
	/** What a day without intervals is: "Closed" for a salon, "Day off" for a person. */
	emptyLabel: string;
	/** Names one interval for the add button, e.g. "Add hours" or "Add shift". */
	addLabel: string;
	saving: boolean;
	onSave: (intervals: WeeklyInterval[]) => void | Promise<void>;
}

/**
 * The whole week, saved in one PUT that replaces what is stored. Mount it once the week has
 * loaded; give it a new key to start again from what is stored, e.g. after a save.
 */
export default function WeeklyHoursEditor({
	initial,
	emptyLabel,
	addLabel,
	saving,
	onSave,
}: WeeklyHoursEditorProps) {
	const baseId = useId();
	const [drafts, setDrafts] = useState<Draft[]>(() =>
		initial.map((interval, key) => ({ ...interval, key })),
	);
	// Only read and bumped in event handlers; the initial drafts took keys below it.
	const nextKey = useRef(initial.length);

	const errors = useMemo(() => weekErrors(drafts), [drafts]);
	const errorFor = new Map(drafts.map((draft, i) => [draft.key, errors[i]]));
	const dirty = normalised(drafts) !== normalised(initial);
	const invalid = errors.some(Boolean);

	function add(day: DayOfWeek) {
		const last = drafts.filter((d) => d.dayOfWeek === day).at(-1);
		// A first interval gets a usual working day; another follows straight on from the last.
		const start = last ? last.end : "09:00";
		const end = last ? addMinutes(last.end, 60) : "17:00";
		setDrafts((current) => [...current, { key: nextKey.current++, dayOfWeek: day, start, end }]);
	}

	function change(key: number, field: "start" | "end", value: string) {
		setDrafts((current) => current.map((d) => (d.key === key ? { ...d, [field]: value } : d)));
	}

	function remove(key: number) {
		setDrafts((current) => current.filter((d) => d.key !== key));
	}

	async function handleSubmit(e: FormEvent<HTMLFormElement>) {
		e.preventDefault();
		if (invalid) return;
		await onSave(drafts.map(({ dayOfWeek, start, end }) => ({ dayOfWeek, start, end })));
	}

	return (
		<form onSubmit={handleSubmit}>
			<ul className="divide-y divide-border-default">
				{DAYS_OF_WEEK.map((day) => {
					const intervals = drafts.filter((d) => d.dayOfWeek === day);
					return (
						<li key={day} className="flex flex-col gap-2 py-4 first:pt-0 sm:flex-row sm:items-start sm:gap-4">
							<span className="w-28 shrink-0 text-sm font-medium text-ink sm:pt-3">
								{DAY_LABELS[day]}
							</span>

							<div className="flex flex-1 flex-col gap-2">
								{intervals.length === 0 && (
									<p className="text-sm text-ink-subtle sm:pt-3">{emptyLabel}</p>
								)}
								{intervals.map((interval) => {
									const error = errorFor.get(interval.key);
									const errorId = error ? `${baseId}-${interval.key}-error` : undefined;
									return (
										<div key={interval.key} className="flex flex-col gap-1">
											<div className="flex items-center gap-2">
												<TextInput
													type="time"
													aria-label={`${DAY_LABELS[day]} from`}
													aria-invalid={error ? true : undefined}
													aria-describedby={errorId}
													value={interval.start}
													onChange={(e) => change(interval.key, "start", e.target.value)}
													className="w-32"
												/>
												<span className="text-ink-subtle" aria-hidden="true">
													–
												</span>
												<TextInput
													type="time"
													aria-label={`${DAY_LABELS[day]} until`}
													aria-invalid={error ? true : undefined}
													aria-describedby={errorId}
													value={interval.end}
													onChange={(e) => change(interval.key, "end", e.target.value)}
													className="w-32"
												/>
												<Button
													size="sm"
													variant="ghost"
													aria-label={`Remove ${DAY_LABELS[day]} ${interval.start}–${interval.end}`}
													onClick={() => remove(interval.key)}
													startIcon={<TrashBinIcon className="size-4" />}
												/>
											</div>
											{error && (
												<p id={errorId} className="text-sm text-error-600 dark:text-error-300">
													{error}
												</p>
											)}
										</div>
									);
								})}
							</div>

							<Button
								size="sm"
								variant="ghost"
								aria-label={`${addLabel} on ${DAY_LABELS[day]}`}
								onClick={() => add(day)}
								startIcon={<PlusIcon className="size-4" />}
								className="self-start sm:mt-1"
							>
								{addLabel}
							</Button>
						</li>
					);
				})}
			</ul>

			<div className="mt-4 flex justify-end gap-3 border-t border-border-default pt-4">
				<Button
					type="button"
					variant="outline"
					onClick={() =>
						setDrafts(initial.map((interval) => ({ ...interval, key: nextKey.current++ })))
					}
					disabled={!dirty || saving}
				>
					Discard changes
				</Button>
				<Button type="submit" loading={saving} disabled={!dirty || invalid}>
					Save week
				</Button>
			</div>
		</form>
	);
}

/** The same week, read-only, for someone who may see it but not change it. */
export function WeeklyHoursSummary({
	intervals,
	emptyLabel,
}: {
	intervals: WeeklyInterval[];
	emptyLabel: string;
}) {
	return (
		<dl className="divide-y divide-border-default">
			{DAYS_OF_WEEK.map((day) => {
				const times = intervals
					.filter((i) => i.dayOfWeek === day)
					.map((i) => `${i.start}–${i.end}`);
				return (
					<div key={day} className="flex gap-4 py-3 first:pt-0 last:pb-0">
						<dt className="w-28 shrink-0 text-sm font-medium text-ink">{DAY_LABELS[day]}</dt>
						<dd className={times.length ? "text-sm tabular-nums text-ink" : "text-sm text-ink-subtle"}>
							{times.length ? times.join(", ") : emptyLabel}
						</dd>
					</div>
				);
			})}
		</dl>
	);
}
