import { useEffect, useState } from "react";
import { getCgmStatus, postChatMessage, type CgmStatus } from "./api/client";

export default function App() {
  const [status, setStatus] = useState<CgmStatus | null>(null);
  const [text, setText] = useState("");
  const [log, setLog] = useState<string[]>([]);

  useEffect(() => {
    getCgmStatus()
      .then(setStatus)
      .catch(() => setStatus(null));
  }, []);

  async function send() {
    if (!text.trim()) return;
    const sent = text;
    setText("");
    setLog((l) => [...l, `Tú: ${sent}`]);
    try {
      const r = await postChatMessage({
        clientMessageId: crypto.randomUUID(),
        text: sent,
        clientTime: new Date().toISOString(),
        timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
      });
      setLog((l) => [...l, `Glucurvia: ${r.reply}`]);
    } catch (e) {
      setLog((l) => [...l, `Sin respuesta (${(e as Error).message})`]);
    }
  }

  return (
    <div className="app">
      <header>
        <h1>Glucurvia</h1>
        <p>{status ? `Última lectura hace ${status.minutesSinceLastReading ?? "?"} min` : "Sin conexión con la API"}</p>
      </header>
      <main>{log.length === 0 ? <p>Cuéntame qué comiste.</p> : log.map((line, i) => <p key={i}>{line}</p>)}</main>
      <footer>
        <form
          className="composer"
          onSubmit={(e) => {
            e.preventDefault();
            void send();
          }}
        >
          <input value={text} onChange={(e) => setText(e.target.value)} placeholder="Estoy comiendo…" autoFocus />
          <button type="submit">Enviar</button>
        </form>
      </footer>
    </div>
  );
}
