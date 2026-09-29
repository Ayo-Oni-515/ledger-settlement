import http from "k6/http";

// Constant arrival rate: fixes requests/sec regardless of how fast the
// server responds, which is what "steady load" for a GC test actually
// needs — a constant-VU test lets latency growth silently drop the real
// request rate, which would hide GC-caused pauses instead of exposing them.
export const options = {
  scenarios: {
    steady: {
      executor: "constant-arrival-rate",
      rate: 150, // requests per second — tune to your service's real headroom
      timeUnit: "1s",
      duration: "10m",
      preAllocatedVUs: 200,
      maxVUs: 400,
    },
  },
};

export default function () {
  http.get("http://localhost:8080/payments/settlement?merchantId=MR-4471");
}
