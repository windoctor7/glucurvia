package mx.glucurvia.core.model;

/** Familia única de lecturas con la que se calcula una respuesta glucémica (diseño 6.1, paso 0). */
public enum SeriesSource {
  REALTIME,
  HISTORIC;

  public ReadingType readingType() {
    return this == REALTIME ? ReadingType.REALTIME : ReadingType.HISTORIC;
  }
}
