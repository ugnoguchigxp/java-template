import { describe, expect, it } from "vitest";
import {
	defaultShowcaseTableSearch,
	parseShowcaseTableSearch,
	showcaseTablePageSizes,
	showcaseTableSortFields,
} from "./showcase-table-search";

describe("showcase table search", () => {
	it("uses defaults for invalid values", () => {
		expect(parseShowcaseTableSearch({})).toEqual(defaultShowcaseTableSearch);
		expect(
			parseShowcaseTableSearch({ page: "0", pageSize: "99", sortBy: "bad" }),
		).toEqual(defaultShowcaseTableSearch);
	});

	it("normalizes valid pagination and sorting", () => {
		expect(
			parseShowcaseTableSearch({
				page: "2",
				pageSize: "20",
				sortBy: showcaseTableSortFields[0],
				sortDir: "desc",
			}),
		).toEqual({ page: 2, pageSize: 20, sortBy: "component", sortDir: "desc" });
		expect(showcaseTablePageSizes).toContain(10);
	});
});
