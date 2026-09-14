package mx.glucurvia.core.model;

/** Tipo de lectura; el código es el smallint de glucose_readings.reading_type (diseño 3). */
public enum ReadingType {
  HISTORIC(0),
  SCAN(1),
  REALTIME(2),
  MANUAL(3),
  STRIP(4);

  private final int code;

  ReadingType(int code) {
    this.code = code;
  }

  public int code() {
    return code;
  }

  public static ReadingType fromCode(int code) {
    for (ReadingType t : values()) {
      if (t.code == code) {
        return t;
      }
    }
    throw new IllegalArgumentException("reading_type desconocido: " + code);
  }
}
