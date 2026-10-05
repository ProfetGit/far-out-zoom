#!/usr/bin/env python3
"""Far Out Zoom with Distant Horizons on a real dedicated server, off-screen.

dh_mp.py [--mc 26.2] [--case dh_server,client_only] [--frames]

dh_server    a Fabric server with Distant Horizons (+ Fabric API): DH sends its LODs to the client. The client (Far
             Out + DH + Fabric API + Sodium) aims past the view distance at a red concrete wall, zooms, and the
             rangefinder must read the wall from DH's terrain ("~ 300 m . Red Concrete").
client_only  a vanilla server: DH on the client only has LODs of chunks it has seen, so the player visits the wall first
             and comes back, then the same check.
no_visit     the control: a vanilla server, no visit. No LOD of the wall exists, so the rangefinder must not read it.

The server builds the wall (x 300, z 20..80) and a gold pillar (x 400, z 40) in a flat world (surface y -10), then
unloads them, so only DH can show them. The client's director is demo/MpDirector.java; its checks land in
<work>/out/results.json. Work dirs: dev/server/.work/dh-<case>-<mc>. With --frames, <work>/out/<case>/ holds every
frame and <work>/<case>.mp4 a preview.
"""
import argparse
import json
import re
import shutil
import socket
import subprocess
import sys
import threading
import time
import importlib.util
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
WS = ROOT.parents[1]
sys.path.insert(0, str(WS / "tools/ModTest"))
sys.path.insert(0, str(WS / "tools/ModJar"))
import client  # noqa: E402
import slots  # noqa: E402

def _current(libs, mod_id, build):
    """The jar of the version in gradle.properties; build/libs keeps older builds (and 0.2.8 sorts after 0.2.13)."""
    version = re.search(r"^mod\.version=(.+)$", (ROOT / "gradle.properties").read_text(), re.M).group(1).strip()
    jar = libs / f"{mod_id}-{version}+{build}.jar"
    if not jar.is_file():
        sys.exit(f"no {jar.name} in {libs}: build it first")
    return jar


_spec = importlib.util.spec_from_file_location("modjar_smoke", WS / "tools/ModJar" / "smoke.py")
ms = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(ms)

PLAYER = ("NestCam", "5e1dcaa0-0000-4000-8000-00000000c001")
PROFILE = {"26.2": "DH 26.2"}
G = -10
FLAT = ('{"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:stone","height":50},'
        '{"block":"minecraft:dirt","height":3},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains"}')


def profile_mod(mc: str, glob: str) -> Path:
    hits = sorted((client.profile_dir(PROFILE[mc]) / "mods").glob(glob))
    if not hits:
        sys.exit(f"no {glob} in the {PROFILE[mc]} profile")
    return hits[-1]


def our_jar(mc: str) -> Path:
    return _current(ROOT / "versions" / f"{mc}-fabric" / "build/libs", "far_out_zoom", f"{mc}-fabric")


def free_port() -> int:
    with socket.socket() as s:
        s.bind(("127.0.0.1", 0))
        return s.getsockname()[1]


def vanilla_cmd(mc: str, work: Path) -> list[str]:
    meta = client.META
    vdir = sorted(meta.glob(f"versions/{mc}-*"))[-1]
    cp = subprocess.check_output(["python3", str(WS / "tools/ClientCapture/classpath.py"), str(vdir / f"{vdir.name}.json"), str(meta / "libraries")], text=True).strip()
    return [ms.java(), "-Xmx2G", f"-Djava.io.tmpdir={work / 'tmp'}", "-cp", cp + ":" + str(vdir / f"{vdir.name}.jar"), "net.minecraft.server.Main", "--nogui"]


class Server:
    def __init__(self, work: Path, cmd: list[str], port: int, mods: list[Path]):
        (work / "mods").mkdir(parents=True)
        (work / "tmp").mkdir()
        for m in mods:
            shutil.copy(m, work / "mods")
        (work / "eula.txt").write_text("eula=true\n")
        (work / "server.properties").write_text(
            f"online-mode=false\nserver-ip=127.0.0.1\nserver-port={port}\nlevel-type=minecraft\\:flat\n"
            f"generator-settings={FLAT}\ngenerate-structures=false\nview-distance=8\nsimulation-distance=6\n"
            "spawn-protection=0\ndifficulty=peaceful\npause-when-empty-seconds=0\nwhite-list=false\nenforce-whitelist=false\n")
        self.log = work / "server.log"
        self.out = open(self.log, "w")
        self.proc = subprocess.Popen(cmd, cwd=work, stdin=subprocess.PIPE, stdout=self.out, stderr=subprocess.STDOUT, text=True)
        self.lock = threading.Lock()

    def send(self, *lines):
        with self.lock:
            for line in lines:
                self.proc.stdin.write(line + "\n")
            self.proc.stdin.flush()

    def text(self) -> str:
        return self.log.read_text(errors="replace")

    def wait(self, pattern: str, timeout: float, after: int = 0) -> re.Match | None:
        end = time.time() + timeout
        while time.time() < end:
            m = re.search(pattern, self.text()[after:])
            if m:
                return m
            if self.proc.poll() is not None:
                return None
            time.sleep(0.25)
        return None

    def stop(self):
        try:
            self.send("stop")
            self.proc.wait(90)
        except Exception:
            self.proc.kill()
        self.out.close()


def run_case(case: str, mc: str, frames: bool) -> tuple[bool, list[str]]:
    work = HERE / ".work" / f"dh-{case}-{mc}"
    shutil.rmtree(work, ignore_errors=True)
    port = free_port()
    dh, fapi, sodium = profile_mod(mc, "DistantHorizons-*.jar"), profile_mod(mc, "fabric-api-*.jar"), profile_mod(mc, "sodium-fabric-*.jar")
    if case == "dh_server":
        scmd, smods = ms.command(mc, "fabric", work / "server"), [dh, fapi]
    else:
        scmd, smods = vanilla_cmd(mc, work / "server"), []
    notes, ok = [], True
    with slots.slot("server", f"FarOut dh_mp {case} {mc}"):
        (work / "server").mkdir(parents=True)
        srv = Server(work / "server", scmd, port, smods)
        try:
            if not srv.wait(r"Done \(\d", 240):
                return False, ["server did not start"] + ms.tail(srv.log)
            result = {}

            def run_client():
                props = {"far_out_zoom.demo": str(work / "out"), "far_out_zoom.demo.mp": case, "far_out_zoom.demo.frames": str(frames).lower(),
                         "modtest.audit": "1"}
                result["r"] = client.run(mc=mc, loader="fabric", out=work / "out", game=work / "game", mod_jars=[our_jar(mc)],
                                         mods=[dh, fapi, sodium], props=props, join=f"127.0.0.1:{port}",
                                         options={"fps": 60, "volume": 0.0001, "render_distance": 8}, timeout=420,
                                         user=PLAYER[0], uuid=PLAYER[1], label=f"FarOut dh_mp {case}")

            t = threading.Thread(target=run_client)
            t.start()
            if not srv.wait(rf"{PLAYER[0]} joined the game", 300):
                notes.append("FAIL join  the client never joined")
                ok = False
            else:
                srv.send("op " + PLAYER[0], "gamerule send_command_feedback false", "gamerule log_admin_commands false", "gamerule advance_time false", "time set 6000", "gamerule advance_weather false", "weather clear",
                         "gamerule spawn_mobs false", "gamerule spawn_monsters false", "gamemode creative " + PLAYER[0],
                         "forceload add 280 0 420 90")
                time.sleep(3)
                srv.send(f"fill 300 {G + 1} 20 300 {G + 12} 80 minecraft:red_concrete", f"fill 400 {G + 1} 38 402 {G + 60} 42 minecraft:gold_block")
                time.sleep(2)
                # unloaded and saved: past the view distance only DH's LODs can show them
                srv.send("save-all flush", "forceload remove all")
                time.sleep(1)
                built = len(re.findall(r"Successfully filled \d+ block", srv.text())) >= 2
                notes.append(("PASS " if built else "FAIL ") + "server/landmarks  wall and pillar built")
                ok &= built
            t.join()
            r = result.get("r", {})
            res = work / "out" / "results.json"
            if res.exists():
                for c in json.loads(res.read_text())["results"]:
                    notes.append(("PASS " if c["pass"] else "FAIL ") + c["name"] + "  " + c["detail"])
                    ok &= c["pass"]
                if not json.loads(res.read_text())["results"]:
                    notes.append("FAIL client  no checks ran")
                    ok = False
            else:
                notes.append(f"FAIL client  no results.json ({'; '.join(r.get('notes', []))[:300]})")
                ok = False
            log = (work / "game" / "client.log").read_text(errors="replace") if (work / "game" / "client.log").exists() else ""
            if "Distant Horizons raycast failed" in log:
                notes.append("FAIL client/raycast  " + re.search(r"Distant Horizons raycast failed.*", log).group(0)[:200])
                ok = False
            errs = [l for l in srv.text().splitlines() if re.search(r"/(ERROR|FATAL)\]", l)]
            notes.append(("PASS " if not errs else "FAIL ") + f"server/log  {len(errs)} error lines" + (": " + errs[0][:200] if errs else ""))
            ok &= not errs
        finally:
            srv.stop()
    if frames and (work / "out" / case).exists():
        mp4 = work / f"{case}.mp4"
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-framerate", "60", "-i", str(work / "out" / case / "f%05d.png"),
                        "-vf", "scale=960:540:flags=lanczos,format=yuv420p", "-c:v", "libx264", "-crf", "20", str(mp4)])
        notes.append(f"info preview {mp4}")
    return ok, notes


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--mc", default="26.2")
    ap.add_argument("--case", default="dh_server,client_only,no_visit")
    ap.add_argument("--frames", action="store_true")
    a = ap.parse_args()
    all_ok = True
    for case in a.case.split(","):
        t0 = time.time()
        ok, notes = run_case(case, a.mc, a.frames)
        all_ok &= ok
        print(f"{case} {a.mc}: {'PASS' if ok else 'FAIL'} ({time.time() - t0:.0f} s)")
        for n in notes:
            print("   " + n)
    sys.exit(0 if all_ok else 1)


if __name__ == "__main__":
    main()
