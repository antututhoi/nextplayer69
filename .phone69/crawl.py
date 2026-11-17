#!/usr/bin/env python3
"""69 virtual phone: walks one app (main screen, menus, drawer, every settings page) and, for a
player / file manager, the extra screens. Saves screenshots + a text list of everything seen.
Usage: crawl.py OUT_DIR APP_NAME KIND PKG     KIND = player | files | other"""
import subprocess, sys, os, re, time, hashlib, xml.etree.ElementTree as ET

OUT, APP, KIND, PKG = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4]
os.makedirs(OUT, exist_ok=True)
T0 = time.time(); BUDGET = int(os.environ.get("BUDGET_MIN", "70")) * 60
shots = 0; log = open(os.path.join(OUT, "crawl.txt"), "w"); seen_txt = open(os.path.join(OUT, "texts.txt"), "w")
SKIP = re.compile(r"save|export|import|share|choose|pick|directory|location|installer|clear|delete|reset|log ?out|sign ?out|remove|wipe|uninstall|factory|biometric|app lock|check for update|install|crash|backup|restore|erase|purge|empty|quit|exit|donate|rate|github|website|privacy policy|licen[sc]e|translate|telegram|discord|source code|format|unmount|eject", re.I)
OKS = ("allow", "while using the app", "ok", "got it", "continue", "skip", "not now", "accept", "agree", "close", "later", "no thanks", "done", "next")

def L(*a):
    s = " ".join(str(x) for x in a); print(s, flush=True); log.write(s + "\n"); log.flush()
def adb(*a, out=False, t=60):
    try: r = subprocess.run(["adb", *a], stdin=subprocess.DEVNULL, capture_output=True, timeout=t)
    except Exception: return b"" if out else None
    return r.stdout if out else None
def size():
    m = re.search(rb"(\d+)x(\d+)", adb("shell", "wm", "size", out=True) or b"")
    return (int(m.group(1)), int(m.group(2))) if m else (1080, 2400)
W, H = size()
def resize():
    """screen size as it is now (the player turns the phone sideways)"""
    global W, H
    d = (adb("shell", "dumpsys", "window", "displays", out=True) or b"").decode(errors="ignore")
    m = re.search(r"cur=(\d+)x(\d+)", d)
    if m: W, H = int(m.group(1)), int(m.group(2))
def over(limit=None): return time.time() - T0 > (limit or BUDGET)
def tap(x, y, wait=1.3): adb("shell", "input", "tap", str(int(x)), str(int(y))); time.sleep(wait)
def hold(x, y): adb("shell", "input", "swipe", str(int(x)), str(int(y)), str(int(x)), str(int(y)), "900"); time.sleep(1.3)
try:
    import uiautomator2 as u2
    D = u2.connect()
except Exception as e:
    D = None; L("uiautomator2 not available:", repr(e))
def focus():
    """package that is in front (menus and pop-ups of the app count as the app)"""
    if D is not None:
        try: return D.app_current().get("package", "")
        except Exception: pass
    f = (adb("shell", "dumpsys", "window", out=True) or b"").decode(errors="ignore")
    m = re.search(r"mCurrentFocus=.*", f); return m.group(0) if m else ""
def launch(): adb("shell", "monkey", "-p", PKG, "-c", "android.intent.category.LAUNCHER", "1"); time.sleep(5)
def ensure_app():
    f = focus()
    if PKG not in f and "permissioncontroller" not in f:
        L("left the app (", f[:90], ") - relaunching"); launch()
def back(): adb("shell", "input", "keyevent", "4"); time.sleep(1.2); ensure_app()
def up():   adb("shell", "input", "swipe", str(W // 2), str(int(H * .70)), str(W // 2), str(int(H * .30)), "250"); time.sleep(0.6)
def down(): adb("shell", "input", "swipe", str(W // 2), str(int(H * .30)), str(W // 2), str(int(H * .80)), "250"); time.sleep(0.6)
def shot(name):
    global shots
    if shots > 500: return
    shots += 1
    png = adb("exec-out", "screencap", "-p", out=True)
    fn = re.sub(r"[^A-Za-z0-9_.-]+", "_", name)[:95]
    open(os.path.join(OUT, fn + ".png"), "wb").write(png or b"")
    r = dump()
    if r is not None: seen_txt.write("## " + fn + "\n" + "\n".join(dict.fromkeys(texts(r))) + "\n\n"); seen_txt.flush()

def bounds(e):
    m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", e.get("bounds", "")); return tuple(map(int, m.groups())) if m else (0, 0, 0, 0)
def center(e): x1, y1, x2, y2 = bounds(e); return (x1 + x2) // 2, (y1 + y2) // 2
def texts(e):
    r = []
    for n in e.iter("node"):
        t = (n.get("text") or n.get("content-desc") or "").strip()
        if t: r.append(t)
    return r
def dump():
    for k in range(4):
        root = None
        if D is not None:
            try: root = ET.fromstring(D.dump_hierarchy(compressed=False))
            except Exception as e:
                if k == 3: L("   (screen could not be read:", repr(e)[:80], ")")
        if root is None:
            adb("shell", "rm", "-f", "/sdcard/u.xml")
            adb("shell", "uiautomator", "dump", "/sdcard/u.xml"); x = adb("exec-out", "cat", "/sdcard/u.xml", out=True)
            try: root = ET.fromstring(x)
            except Exception: time.sleep(1); continue
        t = texts(root)
        if any("isn't responding" in v or "keeps stopping" in v for v in t):
            L("!! ANR / crash dialog:", [v for v in t if "respond" in v or "stopping" in v])
            for n in root.iter("node"):
                if (n.get("text") or "").strip() in ("Wait", "Close app", "OK"):
                    tap(*center(n)); break
            continue
        return root
    return None
def sig(root): return "" if root is None else hashlib.md5("|".join(texts(root)).encode()).hexdigest()
def find(rx, root=None, clickable=False):
    root = root if root is not None else dump()
    if root is None: return None
    for n in root.iter("node"):
        t = (n.get("text") or n.get("content-desc") or "").strip()
        if t and re.fullmatch(rx, t, re.I) and (not clickable or n.get("clickable") == "true"): return n
    return None
def tap_text(rx, root=None):
    n = find(rx, root)
    if n is None: return False
    tap(*center(n)); return True
def dismiss(maxn=6):
    """first-run dialogs and permission prompts"""
    for _ in range(maxn):
        r = dump()
        if r is None: return
        hit = None
        for n in r.iter("node"):
            t = (n.get("text") or "").strip().lower()
            if t in OKS and (n.get("clickable") == "true" or n.get("class", "").endswith("Button")): hit = n; break
        if hit is None: return
        L("dismiss:", hit.get("text")); tap(*center(hit))
def rows(root, top=None, bottom=None):
    top = H * .10 if top is None else top; bottom = H * .93 if bottom is None else bottom
    res = []
    if root is None: return res
    for n in root.iter("node"):
        if n.get("clickable") != "true": continue
        x1, y1, x2, y2 = bounds(n)
        if y2 < top or y1 > bottom or x2 - x1 < W * .2: continue
        t = texts(n)
        if not t: continue
        tog = None
        for k in n.iter("node"):
            c = k.get("class", "")
            kx1, ky1, kx2, ky2 = bounds(k)
            # a real switch / tick box is a small thing inside the row (Compose marks whole rows as "checkable")
            if (re.search(r"Switch|CheckBox|RadioButton|ToggleButton", c) or k.get("checked") == "true") and (kx2 - kx1) < W * .3:
                tog = "on" if k.get("checked") == "true" else "off"; break
        res.append(dict(title=t[0], all=t, b=(x1, y1, x2, y2), checkable=tog is not None, state=tog))
    return res
def icons(root, top=0, bottom=None):
    """small clickable things (toolbar / bottom bar icons)"""
    bottom = H if bottom is None else bottom; res = []
    if root is None: return res
    for n in root.iter("node"):
        if n.get("clickable") != "true": continue
        x1, y1, x2, y2 = bounds(n)
        if y1 >= top and y2 <= bottom and 20 < x2 - x1 < W * .35 and y2 - y1 < H * .12:
            rid = ""
            for k in n.iter("node"):
                if k.get("resource-id"): rid = k.get("resource-id").split("/")[-1]; break
            nm = n.get("content-desc") or " ".join(texts(n)) or rid or f"icon_at_{(x1 + x2) // 2}_{(y1 + y2) // 2}"
            res.append(((x1 + x2) // 2, (y1 + y2) // 2, nm))
    return res
def is_dialog(root):
    t = [x.lower() for x in texts(root)] if root is not None else []
    return ("cancel" in t or "ok" in t or "dismiss" in t or "close" in t) and len(t) < 45

def page_scroll(prefix, maxn=8):
    last = None; n = 0
    for i in range(maxn):
        s = sig(dump())
        if s == last: break
        shot(f"{prefix}_p{i}"); last = s; n += 1; up()
    for _ in range(n + 1): down()
def settle_top():
    for _ in range(4): down()
def _scan(title):
    last = None
    for _ in range(12):
        r = dump()
        for row in rows(r):
            if row["title"] == title:
                x1, y1, x2, y2 = row["b"]; tap((x1 + x2) // 2, (y1 + y2) // 2); return True
        sg = sig(r)
        if sg == last: return False
        last = sg; up()
    return False
def find_and_tap(title):
    if _scan(title): return True
    settle_top(); return _scan(title)
def list_entries():
    settle_top(); seen = []; last = None
    for _ in range(12):
        r = dump()
        for row in rows(r):
            if row["title"] not in [e["title"] for e in seen]: seen.append(row)
        s = sig(r)
        if s == last: break
        last = s; up()
    settle_top(); return seen
def on_page(titles):
    r = dump(); vis = set(texts(r) if r is not None else []); first = titles[:4]
    if sum(1 for t in first if t in vis) >= max(1, len(first) // 2): return True
    settle_top(); r = dump(); vis = set(texts(r) if r is not None else [])
    return sum(1 for t in first if t in vis) >= max(1, len(first) // 2)

def open_settings():
    if tap_text("settings"): return True
    for rx in ("account", "me", "more options", "more", "menu", "open navigation drawer", "navigate up", "open drawer"):
        if tap_text(rx):
            time.sleep(.8)
            if tap_text("settings"): return True
            for _ in range(3):
                up()
                if tap_text("settings"): return True
            back()
    adb("shell", "input", "swipe", "5", str(H // 2), str(int(W * .7)), str(H // 2), "200"); time.sleep(1)
    for _ in range(3):
        if tap_text("settings"): return True
        up()
    return False
def goto(path):
    adb("shell", "input", "keyevent", "3")
    for pk in ("com.google.android.documentsui", "com.android.documentsui", "com.android.settings"): adb("shell", "am", "force-stop", pk)
    adb("shell", "am", "force-stop", PKG); time.sleep(1); launch(); dismiss(3)
    if not open_settings(): return False
    time.sleep(1.5)
    for t in path:
        if not find_and_tap(t): return False
        time.sleep(1.5)
    return True
def crawl_page(path, depth=0, maxdepth=2):
    if not goto(path): L("cannot reach", path); shot("lost_" + "_".join(path)); return
    if depth == 0: page_scroll("settings_main")
    ents = list_entries(); titles = [e["title"] for e in ents]
    L("  " * depth, "PAGE", " > ".join(path) or "settings")
    for e in ents: L("  " * depth, "  -", ("[switch " + e["state"] + "] " if e["checkable"] else "") + " | ".join(e["all"][:4]))
    for i, e in enumerate(ents):
        if over(BUDGET * .7): return
        if e["checkable"]: continue
        if SKIP.search(e["title"]): L("  " * depth, "skip", e["title"]); continue
        if i and not on_page(titles) and not goto(path): L("cannot reach", path); return
        if not find_and_tap(e["title"]): L("  " * depth, "cannot find", e["title"]); continue
        name = "settings_" + "_".join(path + [f"{i:02d}", e["title"]])
        time.sleep(1); r = dump()
        L("  " * depth, "opened", e["title"], "->", (texts(r)[:6] if r is not None else "unreadable"))
        if PKG not in focus(): shot(name + "_left_app"); ensure_app(); goto(path); continue
        if is_dialog(r): shot(name + "_dialog"); back(); continue
        if on_page(titles): continue
        page_scroll(name)
        if depth < maxdepth and rows(dump()):
            crawl_page(path + [e["title"]], depth + 1, maxdepth)
            goto(path)
        else:
            back()

def menus(prefix, top=0, bottom=None, maxitems=14):
    """tap every small icon in the given band; if it opens a menu, open each item too"""
    r = dump(); base = sig(r); base_titles = set(texts(r))
    for i, (x, y, d) in enumerate(icons(r, top, bottom)):
        if over() or re.search(r"navigate up|^back$", d, re.I) or SKIP.search(d): continue
        tap(x, y); rr = dump()
        if PKG not in focus(): ensure_app(); continue
        if sig(rr) == base: continue
        shot(f"{prefix}_icon{i}_{d}")
        items = [row for row in rows(rr, top=0, bottom=H) if row["title"] not in base_titles][:maxitems]
        back()
        if sig(dump()) != base: back()
        for j, it in enumerate(items):
            if over() or SKIP.search(it["title"]) or it["checkable"]: continue
            tap(x, y); rr = dump(); hit = None
            for row in rows(rr, top=0, bottom=H):
                if row["title"] == it["title"]: hit = row
            if not hit: back(); continue
            x1, y1, x2, y2 = hit["b"]; tap((x1 + x2) // 2, (y1 + y2) // 2)
            if PKG not in focus(): shot(f"{prefix}_icon{i}_{d}_item{j}_{it['title']}_left_app"); ensure_app(); continue
            if sig(dump()) != base:
                page_scroll(f"{prefix}_icon{i}_{d}_item{j}_{it['title']}", 3)
                back()
                if sig(dump()) != base: back()
                if sig(dump()) != base: goto_main()
def goto_main():
    adb("shell", "am", "force-stop", PKG); time.sleep(1); launch(); dismiss(3)

def main_screen():
    goto_main(); page_scroll("main", 3)
    menus("main", 0, H * .16); menus("main_bottom", H * .86, H)
    # side drawer
    adb("shell", "input", "swipe", "5", str(H // 2), str(int(W * .7)), str(H // 2), "200"); time.sleep(1.2)
    page_scroll("drawer", 4); back()
    # long-press the first rows (selection mode + its menus)
    goto_main(); r = dump(); rs = rows(r, H * .16, H * .86)
    for k, row in enumerate(rs[:2]):
        x1, y1, x2, y2 = row["b"]; hold((x1 + x2) // 2, (y1 + y2) // 2)
        shot(f"select{k}_{row['title']}"); menus(f"select{k}", 0, H * .16); menus(f"select{k}_bottom", H * .86, H)
        goto_main()

def files_extra():
    """open a folder, the file menu, properties, the open-with of sample files"""
    goto_main()
    for name in ("Download", "Movies", "Pictures"):
        if not find_and_tap(name): continue
        page_scroll("folder_" + name, 2); menus("folder_" + name, 0, H * .16, 10)
        r = dump(); rs = [x for x in rows(r, H * .16, H * .9)]
        for k, row in enumerate(rs[:4]):
            if over(): return
            x1, y1, x2, y2 = row["b"]
            tap(x2 - 60, (y1 + y2) // 2); shot(f"filemenu_{name}_{k}_{row['title']}"); back()
            tap((x1 + x2) // 2, (y1 + y2) // 2); time.sleep(2); shot(f"open_{name}_{k}_{row['title']}"); ensure_app()
            if sig(dump()) != sig(r): back()
        goto_main()

def player_extra():
    """play the sample videos; every button, panel and gesture overlay of the player screen"""
    for k, f in enumerate(("sample_h264.mp4", "sample_subs.mkv")):
        if over(): return
        adb("shell", "am", "start", "-a", "android.intent.action.VIEW", "-d", "file:///sdcard/Movies/" + f, "-t", "video/*", "-p", PKG); time.sleep(6); dismiss(3)
        p = f"player{k}"; resize(); shot(p + "_playing")
        cx, cy = W // 2, H // 2
        def show():
            # a playing video never lets the screen be read, so pause first
            adb("shell", "input", "keyevent", "127"); time.sleep(.5)
            r = dump()
            if find("got it|ok|skip|close", r) is not None: dismiss(3); r = dump()
            if len(icons(r)) < 4: tap(cx, cy, .8); r = dump()
            if len(icons(r)) < 4: tap(cx, cy, .8); r = dump()
            return r
        r = show(); shot(p + "_controls"); ics = icons(r); L(p, "buttons:", [d for _, _, d in ics])
        def replay():
            adb("shell", "am", "start", "-a", "android.intent.action.VIEW", "-d", "file:///sdcard/Movies/" + f, "-t", "video/*", "-p", PKG); time.sleep(5); resize()
        for i, (x, y, d) in enumerate(ics):
            if over(): break
            if re.search(r"navigate up|^back$|close|exit", d, re.I) or SKIP.search(d): continue
            if x < W * .09 and y < H * .2: continue   # the back arrow
            r = show(); base = sig(r); tap(x, y); time.sleep(.6)
            if PKG not in focus(): shot(f"{p}_btn{i}_{d}_left_app"); replay(); continue
            if W < H: shot(f"{p}_btn{i}_{d}_closed_player"); replay(); continue
            r2 = dump(); shot(f"{p}_btn{i}_{d}")
            if is_dialog(r2) or len(rows(r2, 0, H)) > len(rows(r, 0, H)) + 1:
                page_scroll(f"{p}_btn{i}_{d}_panel", 3)
                for j, row in enumerate([q for q in rows(r2, 0, H) if q["title"] not in set(texts(r))][:8]):
                    if SKIP.search(row["title"]) or row["checkable"]: continue
                    x1, y1, x2, y2 = row["b"]; tap((x1 + x2) // 2, (y1 + y2) // 2); shot(f"{p}_btn{i}_{d}_sub{j}_{row['title']}"); back()
                    if find(re.escape(row["title"])) is None: tap(x, y)
                back()
            if "player" not in focus().lower() and PKG not in focus(): ensure_app(); break
        # gestures: long-press, double-tap sides, swipes
        show(); tap(cx, cy, .5)
        hold(cx, cy); shot(p + "_longpress")
        for nm, x in (("dtap_left", W * .2), ("dtap_right", W * .8)):
            adb("shell", "input", "tap", str(int(x)), str(cy)); adb("shell", "input", "tap", str(int(x)), str(cy)); time.sleep(.4); shot(p + "_" + nm)
        adb("shell", "input", "swipe", str(int(W * .85)), str(int(H * .6)), str(int(W * .85)), str(int(H * .35)), "400"); shot(p + "_swipe_right_side")
        adb("shell", "input", "swipe", str(int(W * .15)), str(int(H * .6)), str(int(W * .15)), str(int(H * .35)), "400"); shot(p + "_swipe_left_side")
        adb("shell", "input", "swipe", str(int(W * .3)), str(cy), str(int(W * .7)), str(cy), "400"); shot(p + "_swipe_seek")
        adb("shell", "input", "keyevent", "4"); time.sleep(1); adb("shell", "input", "keyevent", "4"); time.sleep(1.5); resize()

L("start", APP, KIND, PKG, W, H)
launch(); dismiss(8); shot("first_run")
for part in ([main_screen] + ([files_extra] if KIND == "files" else []) + ([player_extra] if KIND == "player" else []) + [lambda: crawl_page([], 0)]):
    try: part()
    except Exception as e: L("part failed", repr(e))
L("done", shots, "screenshots", int(time.time() - T0), "s")
