/** Formato de un rango de hidratos como lo muestra el diseño (10.5): "≈50 g (40–60)". */
export function formatCarbs(point: number, low: number, high: number): string {
  return `≈${Math.round(point)} g (${Math.round(low)}–${Math.round(high)})`;
}
