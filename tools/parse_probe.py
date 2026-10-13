import json
import sys

def parse_report(path):
    d = json.load(open(path, encoding="utf-8"))
    print("================== PROBE SUMMARY ==================")
    print("schemaVersion      :", d.get("schemaVersion"))
    print("phases completed   :", d.get("phasesCompleted"))
    print("errors             :", len(d.get("errors", [])))
    for e in d.get("errors", []):
        print("   ", e)

    device = d.get("device", {})
    print(f"Device: {device.get('manufacturer')} {device.get('model')} ({device.get('device')}), SDK {device.get('sdkInt')}, release {device.get('release')}")

    cameras = d.get("cameraIds", [])
    print("camera ids         :", len(cameras), [c["cameraId"] for c in cameras])
    print("concurrent groups  :", d.get("concurrentCameraIds"))

    for c in cameras:
        cid = c["cameraId"]
        caps = {x["name"] for x in c.get("requestAvailableCapabilities", [])}
        sc = c.get("streamConfig", {}) or {}
        gate = sc.get("openGateCandidates", {}) or {}
        vt = c.get("vendorTags", {}) or {}
        cls = vt.get("classification", {}) or {}
        cc = c.get("colorCorrection", {}) or {}
        print(f"\n--- camera {cid} ({c.get('lensFacing')}, {c.get('hardwareLevel')}) ---")
        print("   LOGICAL_MULTI_CAMERA      :", "LOGICAL_MULTI_CAMERA" in caps)
        print("   DYNAMIC_RANGE_TEN_BIT     :", "DYNAMIC_RANGE_TEN_BIT" in caps)
        print("   ULTRA_HIGH_RES_SENSOR     :", "ULTRA_HIGH_RESOLUTION_SENSOR" in caps)
        print("   active array aspect       :", c.get("sensor", {}).get("activeArrayAspectLabel"),
              c.get("sensor", {}).get("activeArrayAspectRatio"))
        print("   full-sensor video stream  :", gate.get("hasFullSensorAspectVideoStream"))
        print("   widest video-capable      :", (gate.get("widestVideoCapableStream") or {}).get("size"),
                                              (gate.get("widestVideoCapableStream") or {}).get("maxFps"))
        print("   HLG10 advertised          :", any(p.get("name") == "HLG10"
                                                    for p in c.get("dynamicRangeProfiles", {}).get("supported", [])))
        print("   WB supports CCT mode      :", cc.get("supportsCctMode"),
              "tempRange=", (cc.get("colorTemperatureRange") or {}))
        print("   vendor session keys       :", cls.get("sessionCount"),
              "| interesting:", cls.get("interestingSessionCount"))
        print("   namespaces present        :", cls.get("namespacesPresent"))

    for pre in d.get("sessionPrecheck", []):
        print(f"\n=== precheck camera {pre.get('cameraId')} ===")
        for r in pre.get("results", []):
            print(f"   {str(r.get('isSessionConfigurationSupported')):>6}  {r['label']:<28} "
                  f"{r.get('recordSize')} {r.get('dynamicRangeProfile')} {r.get('error') or ''}")

    for b in d.get("cameraxBindMatrix", []):
        print(f"\n=== phase B camera {b.get('cameraId')} (CameraX view) ===")
        if b.get("skipped"):
            print("   skipped:", b["skipped"]); continue
        info = b.get("cameraInfo", {}) or {}
        print("   zoom range            :", info.get("zoomRatioRange"))
        print("   logical multi / zsl   :", info.get("isLogicalMultiCameraSupported"),
              "/", info.get("isZslSupported"))
        print("   dynamic ranges ok     :", info.get("dynamicRangesSupported"))
        print("   frame rate ranges     :", info.get("supportedFrameRateRangesNoConfig"))
        caps = b.get("videoCapabilities", {}) or {}
        for src in caps.get("byCapabilitiesSource", []) or []:
            c = src.get("capabilities") or {}
            print(f"   caps via {src.get('source'):<17}: ranges={c.get('dynamicRanges')} "
                  f"stab={c.get('stabilizationSupported')}")
        print("   quality ladder        :", [(q.get('quality'), q.get('resolution'))
                                             for q in caps.get("qualityLadder", []) or []])
        for row in b.get("bindMatrix", []) or []:
            verdict = "BIND-OK" if row.get("bindError") is None else "BIND-FAIL"
            print(f"   {verdict:<10} sup={str(row.get('isSessionConfigSupported')):>5}  "
                  f"{row.get('label'):<26} {row.get('bindError') or row.get('constructionError') or ''}")
            for note in (row.get("negotiated") or [])[:5]:
                print("        ", note)

    for o in d.get("openGateAttempts", []):
        print(f"\n=== phase D camera {o.get('cameraId')} (open gate, recorded clips) ===")
        if o.get("skipped"):
            print("   skipped:", o["skipped"]); continue
        print("   candidates            :", o.get("openGateCandidates"))
        for a in o.get("attempts", []) or []:
            media = a.get("media") or {}
            dims = next((f"{t.get('width')}x{t.get('height')}" for t in (media.get("tracks") or [])
                         if t.get("width")), "-")
            req = (a.get("request") or {})
            print(f"   [{a.get('tier')}] {str(a.get('label')):<34} "
                  f"sup={str(a.get('isSessionConfigSupported')):>5} "
                  f"bind={'OK' if not a.get('bindError') else 'FAIL'} "
                  f"want={req.get('aspect')}/{req.get('quality')} "
                  f"clip={dims} bytes={media.get('bytes')} "
                  f"startGap={a.get('startGapMs')}ms finErr={a.get('finalizeError')}")
            if a.get("bindError"):
                print("        bind:", a["bindError"])
            if media.get("error") or media.get("extractError"):
                print("        media:", media.get("error") or media.get("extractError"))

if __name__ == "__main__":
    report_path = sys.argv[1] if len(sys.argv) > 1 else "docs/probe/probe-latest.json"
    parse_report(report_path)
