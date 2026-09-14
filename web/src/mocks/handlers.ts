import { http, HttpResponse } from "msw";
import type { CgmStatus, ChatResponse } from "../api/client";

/** Mocks que respetan el contrato. El agente de web añade aquí lo que necesite cada pantalla. */
export const handlers = [
  http.get("/api/v1/cgm/status", () => {
    const status: CgmStatus = {
      source: "NIGHTSCOUT",
      lastPullAt: new Date().toISOString(),
      lastReadingTs: new Date(Date.now() - 3 * 60_000).toISOString(),
      minutesSinceLastReading: 3,
      lastStatus: "OK",
      lastError: null,
      consecutiveFailures: 0,
    };
    return HttpResponse.json(status);
  }),
  http.post("/api/v1/chat/messages", async () => {
    const reply: ChatResponse = {
      conversationId: "00000000-0000-0000-0000-000000000010",
      reply: "Anotado: cena 21:52, ≈50 g de hidratos (40–60). Glucosa antes de cenar: 98, estable.",
      createdEvents: [],
      updatedEvents: [],
      clarification: null,
      dataQualityNotes: [],
    };
    return HttpResponse.json(reply);
  }),
];
