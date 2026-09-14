import { describe, expect, it } from "vitest";
import { formatCarbs } from "./format";

describe("formatCarbs", () => {
  it("muestra punto y rango como el diseño", () => {
    expect(formatCarbs(50, 39.6, 60.4)).toBe("≈50 g (40–60)");
  });
});
