package mx.glucurvia.core.model;

/** De dónde salieron los macros de un ítem (diseño 3, meal_items.macro_source). */
public enum MacroSource {
  CATALOG,
  USER_ALIAS,
  LLM_FALLBACK,
  LABEL
}
