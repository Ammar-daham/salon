"""Builds the design canvas's files from the screen templates.

Expands every @@macro(arg|arg)@@ in templates/*.html into out/project/<name>.dc.html and copies
canvas.json next to them, so out/ can be published to the canvas as it is.

    python3 client-app/design/build.py [out-dir] [brand-dir]
"""
import pathlib
import re
import shutil
import sys

HERE = pathlib.Path(__file__).resolve().parent
TPL = HERE / "templates"
OUT = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else HERE / "out" / "project"
BRAND = pathlib.Path(sys.argv[2]) if len(sys.argv) > 2 else HERE.parents[1] / "brand"

FONT = "'Outfit', ui-sans-serif, system-ui, -apple-system, 'Segoe UI', sans-serif"

ICONS = {
    "search": '<circle cx="11" cy="11" r="7"/><path d="m20 20-4-4"/>',
    "pin": '<path d="M12 21.5s-7-6.1-7-11.6a7 7 0 0 1 14 0c0 5.5-7 11.6-7 11.6z"/><circle cx="12" cy="9.8" r="2.6"/>',
    "calendar": '<rect x="3.5" y="5" width="17" height="15.5" rx="2.5"/><path d="M3.5 10h17M8 3v4M16 3v4"/>',
    "calplus": '<rect x="3.5" y="5" width="17" height="15.5" rx="2.5"/><path d="M3.5 10h17M8 3v4M16 3v4M12 12.8v5M9.5 15.3h5"/>',
    "heart": '<path d="M12 20.2S3.8 15.4 3.8 9.4A4.4 4.4 0 0 1 12 7.2a4.4 4.4 0 0 1 8.2 2.2c0 6-8.2 10.8-8.2 10.8z"/>',
    "heartf": '<path d="M12 20.2S3.8 15.4 3.8 9.4A4.4 4.4 0 0 1 12 7.2a4.4 4.4 0 0 1 8.2 2.2c0 6-8.2 10.8-8.2 10.8z" fill="currentColor"/>',
    "user": '<circle cx="12" cy="8" r="4"/><path d="M4.5 20.5a7.5 7.5 0 0 1 15 0"/>',
    "users": '<circle cx="9" cy="8.5" r="3.5"/><path d="M3 20a6 6 0 0 1 12 0"/><path d="M15.5 5.2a3.5 3.5 0 0 1 0 6.6M17.5 14.3A6 6 0 0 1 21 20"/>',
    "discover": '<circle cx="12" cy="12" r="9"/><path d="m15.6 8.4-2.1 5.1-5.1 2.1 2.1-5.1z"/>',
    "bell": '<path d="M6 16.5V11a6 6 0 0 1 12 0v5.5l1.5 2h-15z"/><path d="M10 21a2.2 2.2 0 0 0 4 0"/>',
    "star": '<path d="M12 3.6l2.55 5.2 5.75.84-4.15 4.05.98 5.72L12 16.7l-5.13 2.7.98-5.72L3.7 9.64l5.75-.84z" fill="currentColor" stroke="none"/>',
    "staro": '<path d="M12 3.6l2.55 5.2 5.75.84-4.15 4.05.98 5.72L12 16.7l-5.13 2.7.98-5.72L3.7 9.64l5.75-.84z"/>',
    "clock": '<circle cx="12" cy="12" r="9"/><path d="M12 7.5V12l3 2"/>',
    "left": '<path d="m14.5 5.5-6.5 6.5 6.5 6.5"/>',
    "right": '<path d="m9.5 5.5 6.5 6.5-6.5 6.5"/>',
    "down": '<path d="m5.5 9.5 6.5 6.5 6.5-6.5"/>',
    "sliders": '<path d="M4 7h9M17 7h3M4 17h3M11 17h9"/><circle cx="15" cy="7" r="2"/><circle cx="9" cy="17" r="2"/>',
    "close": '<path d="M6 6l12 12M18 6 6 18"/>',
    "check": '<path d="m5 12.5 4.5 4.5L19 7.5"/>',
    "share": '<path d="M12 15V3.5M7.5 8 12 3.5 16.5 8"/><path d="M5 12.5v6A2.5 2.5 0 0 0 7.5 21h9a2.5 2.5 0 0 0 2.5-2.5v-6"/>',
    "phone": '<path d="M5.5 3.5h3l1.6 4.4-2.2 1.5a11.5 11.5 0 0 0 6.7 6.7l1.5-2.2 4.4 1.6v3a2 2 0 0 1-2.2 2A17 17 0 0 1 3.5 5.7a2 2 0 0 1 2-2.2z"/>',
    "nav": '<path d="M20.5 3.5 3.5 10.6l7.1 2.8 2.8 7.1z"/>',
    "map": '<path d="M9 4.5 3.5 6.8v12.7L9 17.2l6 2.3 5.5-2.3V4.5L15 6.8z"/><path d="M9 4.5v12.7M15 6.8v12.7"/>',
    "list": '<path d="M9 6h11M9 12h11M9 18h11"/><path d="M4.5 6h.01M4.5 12h.01M4.5 18h.01" stroke-width="2.6"/>',
    "scissors": '<circle cx="6.5" cy="6.5" r="2.5"/><circle cx="6.5" cy="17.5" r="2.5"/><path d="M8.6 8 20 17.5M8.6 16 20 6.5"/>',
    "comb": '<rect x="3.5" y="7.5" width="17" height="4" rx="1"/><path d="M5.5 11.5v5M8.5 11.5v5M11.5 11.5v5M14.5 11.5v5M17.5 11.5v5"/>',
    "polish": '<rect x="7" y="10.5" width="10" height="10.5" rx="2.5"/><path d="M9.5 10.5V7h5v3.5M10.5 7V3h3v4"/>',
    "leaf": '<path d="M5 19c0-8 5-13.5 14-14-.5 9-6 14-14 14z"/><path d="M5 19 13 11"/>',
    "sparkle": '<path d="M11 3.5l1.7 4.8 4.8 1.7-4.8 1.7L11 16.5l-1.7-4.8L4.5 10l4.8-1.7z"/><path d="M18 14.5l.8 2.2 2.2.8-2.2.8-.8 2.2-.8-2.2-2.2-.8 2.2-.8z"/>',
    "eye": '<path d="M2.5 12S6 5.8 12 5.8 21.5 12 21.5 12 18 18.2 12 18.2 2.5 12 2.5 12z"/><circle cx="12" cy="12" r="2.8"/>',
    "info": '<circle cx="12" cy="12" r="9"/><path d="M12 11v5.5M12 7.8v.01"/>',
    "pencil": '<path d="M4 20h4L19 9a2.8 2.8 0 0 0-4-4L4 16z"/><path d="m13.5 6.5 4 4"/>',
    "logout": '<path d="M14 4h3.5A2.5 2.5 0 0 1 20 6.5v11a2.5 2.5 0 0 1-2.5 2.5H14"/><path d="M10 16.5 5.5 12 10 7.5M5.5 12H15"/>',
    "globe": '<circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3c2.6 2.6 3.8 5.6 3.8 9s-1.2 6.4-3.8 9c-2.6-2.6-3.8-5.6-3.8-9S9.4 5.6 12 3z"/>',
    "lock": '<rect x="5" y="10.5" width="14" height="10" rx="2.5"/><path d="M8 10.5V8a4 4 0 0 1 8 0v2.5"/>',
    "help": '<circle cx="12" cy="12" r="9"/><path d="M9.6 9.3a2.5 2.5 0 0 1 4.8.9c0 1.7-2.4 2.2-2.4 3.8M12 17v.01"/>',
    "shield": '<path d="M12 3.5 5 6v5.5c0 4.6 3 7.8 7 9 4-1.2 7-4.4 7-9V6z"/>',
    "store": '<path d="M4 9.5 5.5 4h13L20 9.5"/><path d="M4 9.5a2.7 2.7 0 0 0 5.3 0 2.7 2.7 0 0 0 5.4 0 2.7 2.7 0 0 0 5.3 0"/><path d="M5.5 12v8.5h13V12M10 20.5v-5h4v5"/>',
    "mail": '<rect x="3.5" y="5.5" width="17" height="13" rx="2.5"/><path d="m4 7 8 6 8-6"/>',
    "arrow": '<path d="M5 12h14M13 6l6 6-6 6"/>',
    "rebook": '<path d="M20 11a8 8 0 0 0-14.3-4.9L4 8"/><path d="M4 3.5V8h4.5"/><path d="M4 13a8 8 0 0 0 14.3 4.9L20 16"/><path d="M20 20.5V16h-4.5"/>',
    "xcircle": '<circle cx="12" cy="12" r="9"/><path d="m9 9 6 6M15 9l-6 6"/>',
    "okcircle": '<circle cx="12" cy="12" r="9"/><path d="m8 12.3 2.8 2.8L16 9.8"/>',
    "note": '<path d="M4 5.5h16v11H9l-5 4z"/>',
    "image": '<rect x="3.5" y="4.5" width="17" height="15" rx="2.5"/><circle cx="9" cy="10" r="1.8"/><path d="m4 17.5 5-4.5 3.5 3 3-2.5 4.5 4"/>',
    "locate": '<circle cx="12" cy="12" r="3.2"/><circle cx="12" cy="12" r="7.5"/><path d="M12 2.5v2M12 19.5v2M2.5 12h2M19.5 12h2"/>',
    "trash": '<path d="M4.5 7h15M9.5 7V4.5h5V7M6.5 7l1 13h9l1-13"/>',
    "plus": '<path d="M12 5v14M5 12h14"/>',
    "more": '<path d="M5.5 12h.01M12 12h.01M18.5 12h.01" stroke-width="3"/>',
    "gift": '<rect x="4" y="9" width="16" height="11.5" rx="2"/><path d="M3 9h18M12 9v11.5M12 9c-1.5-3.5-5.5-4.5-5.5-2S10 9 12 9zm0 0c1.5-3.5 5.5-4.5 5.5-2S14 9 12 9z"/>',
}

TONES = {
    "rose": ("#F0D3CD", "#9C5A65", "#7D4751"),
    "sand": ("#E4DED8", "#7A716A", "#4A443F"),
    "blue": ("#DAE3EE", "#4A6D93", "#314A64"),
    "green": ("#D3E9DB", "#468A63", "#2E6345"),
    "gold": ("#F5E6C8", "#AD7C22", "#7C5113"),
    "plum": ("#4A2B31", "#CD8F8E", "#F0D3CD"),
}

BADGES = {
    "wait": ("#FBF4E7", "#7C5113"),
    "ok": ("#EAF5EE", "#2E6345"),
    "done": ("#F2EEEA", "#4A443F"),
    "cancel": ("#FBEEEC", "#862720"),
}


def close_tags(markup):
    return re.sub(r"<(\w+)([^<>]*?)\s*/>", r"<\1\2></\1>", markup)


def icon(name, size="20", color="currentColor", width="1.8"):
    return close_tags(f'<svg width="{size}" height="{size}" viewBox="0 0 24 24" fill="none" stroke="currentColor" '
            f'stroke-width="{width}" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" '
            f'style="flex-shrink: 0; color: {color}">{ICONS[name]}</svg>')


def head(title, bg="#FAF8F6"):
    return f'''<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>{title}</title>
<script src="./support.js"></script>
</head>
<body>
<x-dc>
<helmet>
<link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&amp;display=swap" rel="stylesheet">
<style>
body{{margin:0;background:{bg}}}
a{{color:#7D4751}}a:hover{{color:#613840}}
</style>
</helmet>'''


def foot(w, h):
    return f'''</x-dc>
<script type="text/x-dc" data-dc-script data-props='{{"$preview":{{"width":{w},"height":{h}}}}}'>
class Component extends DCLogic {{
  renderVals() {{
    return {{}};
  }}
}}
</script>
</body>
</html>'''


def root(h, bg="#FAF8F6"):
    return (f'<div style="width: 390px; height: {h}px; box-sizing: border-box; background: {bg}; color: #2C191D; '
            f'font-family: {FONT}; display: flex; flex-direction: column; overflow: hidden; position: relative">')


def ph(w, h, r, tone, size="28"):
    bg, fg, _ = TONES[tone]
    return (f'<div style="width: {w}; height: {h}; border-radius: {r}; background: {bg}; display: flex; '
            f'align-items: center; justify-content: center; flex-shrink: 0" aria-hidden="true">{icon("image", size, fg, "1.5")}</div>')


def av(initials, size, tone):
    bg, _, fg = TONES[tone]
    fs = round(int(size) * 0.38)
    return (f'<div style="width: {size}px; height: {size}px; border-radius: 50%; background: {bg}; color: {fg}; '
            f'font-size: {fs}px; font-weight: 600; display: flex; align-items: center; justify-content: center; '
            f'flex-shrink: 0" aria-hidden="true">{initials}</div>')


def badge(kind, text):
    bg, fg = BADGES[kind]
    return (f'<span style="height: 26px; padding: 0 10px; border-radius: 13px; background: {bg}; color: {fg}; '
            f'font-size: 13px; font-weight: 500; display: inline-flex; align-items: center; gap: 6px; '
            f'white-space: nowrap; flex-shrink: 0"><span style="width: 6px; height: 6px; border-radius: 3px; '
            f'background: {fg}"></span>{text}</span>')


def back(href, label="Back"):
    return (f'<a href="{href}" aria-label="{label}" style="width: 44px; height: 44px; border-radius: 22px; '
            f'background: #FFFFFF; border: 1px solid #E4DED8; box-sizing: border-box; color: #2C191D; '
            f'display: flex; align-items: center; justify-content: center; flex-shrink: 0">{icon("left", "22")}</a>')


def iconbtn(href, name, label, bg="#FFFFFF", border="1px solid #E4DED8", color="#2C191D"):
    return (f'<a href="{href}" aria-label="{label}" style="width: 44px; height: 44px; border-radius: 22px; '
            f'background: {bg}; border: {border}; box-sizing: border-box; color: {color}; display: flex; '
            f'align-items: center; justify-content: center; flex-shrink: 0">{icon(name, "22")}</a>')


def stars(n, size="16"):
    n = float(n)
    out = []
    for i in range(5):
        out.append(icon("star", size, "#9C5A65") if i < n else icon("staro", size, "#A79D93", "1.5"))
    return f'<span style="display: inline-flex; gap: 2px" role="img" aria-label="{n:g} out of 5 stars">{"".join(out)}</span>'


def rating(score, count):
    return (f'<span style="display: inline-flex; align-items: center; gap: 4px; font-size: 14px; font-weight: 500; '
            f'color: #2C191D; white-space: nowrap">{icon("star", "16", "#9C5A65")}{score}'
            f'<span style="color: #5F5852; font-weight: 400">({count})</span></span>')


def slot(t, href="#"):
    return (f'<a href="{href}" style="height: 40px; padding: 0 14px; border-radius: 10px; background: #FFFFFF; '
            f'border: 1px solid #E4DED8; box-sizing: border-box; color: #2C191D; font-size: 15px; font-weight: 500; '
            f'text-decoration: none; display: flex; align-items: center; justify-content: center; flex-shrink: 0">{t}</a>')


def chip(text, selected="0", icon_name=""):
    on = selected == "1"
    bg = "#2C191D" if on else "#FFFFFF"
    fg = "#FFFFFF" if on else "#2C191D"
    border = "#2C191D" if on else "#E4DED8"
    ic = icon(icon_name, "18") if icon_name else ""
    return (f'<button type="button" aria-pressed="{"true" if on else "false"}" style="height: 38px; padding: 0 14px; '
            f'border-radius: 19px; background: {bg}; color: {fg}; border: 1px solid {border}; font-family: inherit; '
            f'font-size: 14px; font-weight: 500; display: inline-flex; align-items: center; justify-content: center; gap: 6px; '
            f'white-space: nowrap; flex-shrink: 0; cursor: pointer">{ic}{text}</button>')


def btn(href, text, kind="primary", icon_name=""):
    styles = {
        "primary": "background: #9C5A65; color: #FFFFFF; border: none",
        "secondary": "background: #FFFFFF; color: #2C191D; border: 1px solid #CEC6BD",
        "danger": "background: #A33229; color: #FFFFFF; border: none",
        "dark": "background: #2C191D; color: #FFFFFF; border: none",
    }
    ic = icon(icon_name, "20") if icon_name else ""
    return (f'<a href="{href}" style="height: 54px; border-radius: 14px; {styles[kind]}; box-sizing: border-box; '
            f'font-size: 16px; font-weight: 600; text-decoration: none; display: flex; align-items: center; '
            f'justify-content: center; gap: 8px; flex-shrink: 0">{ic}{text}</a>')


TABS = [("home", "Home.dc.html", "discover", "Discover"),
        ("bookings", "Bookings.dc.html", "calendar", "Bookings"),
        ("favourites", "Favourites.dc.html", "heart", "Favourites"),
        ("profile", "Profile.dc.html", "user", "Profile")]


def tabbar(active):
    items = []
    for key, href, ic, label in TABS:
        on = key == active
        color = "#9C5A65" if on else "#5F5852"
        cur = ' aria-current="page"' if on else ""
        name = "heartf" if (on and ic == "heart") else ic
        items.append(f'<a href="{href}"{cur} style="flex: 1 1 0; height: 54px; display: flex; flex-direction: column; '
                     f'align-items: center; justify-content: center; gap: 3px; text-decoration: none; color: {color}; '
                     f'font-size: 12px; font-weight: {600 if on else 500}">{icon(name, "24", color)}{label}</a>')
    return (f'<nav aria-label="Main" style="margin-top: auto; flex-shrink: 0; background: #FFFFFF; border-top: 1px solid #E4DED8; '
            f'padding: 6px 8px 28px; display: flex">{"".join(items)}</nav>')


def svgfile(name, height, label="Salonat"):
    src = (BRAND / name).read_text()
    m = re.search(r'viewBox="0 0 ([\d.]+) ([\d.]+)"', src)
    vw, vh = float(m.group(1)), float(m.group(2))
    w = round(vw * float(height) / vh, 1)
    inner = re.sub(r'^<svg[^>]*>', '', src.strip())
    inner = re.sub(r'<title>.*?</title>', '', inner)
    return close_tags(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {m.group(1)} {m.group(2)}" width="{w}" '
            f'height="{height}" role="img" aria-label="{label}" style="flex-shrink: 0; display: block">{inner}')


MACROS = {
    "i": icon, "head": head, "foot": foot, "root": root, "ph": ph, "av": av, "badge": badge, "back": back,
    "iconbtn": iconbtn, "stars": stars, "rating": rating, "slot": slot, "chip": chip, "btn": btn,
    "tabbar": tabbar,
    "lockup_dark": lambda h: svgfile("salonat-lockup-on-dark.svg", h),
    "lockup": lambda h: svgfile("salonat-lockup.svg", h),
    "mark": lambda h: svgfile("salonat-mark.svg", h),
}

PATTERN = re.compile(r"@@(\w+)(?:\(([^@]*?)\))?@@")


def expand(text):
    def sub(m):
        args = [a.strip() for a in m.group(2).split("|")] if m.group(2) is not None else []
        return MACROS[m.group(1)](*args)
    return PATTERN.sub(sub, text)


OUT.mkdir(parents=True, exist_ok=True)
for tpl in sorted(TPL.glob("*.html")):
    out = expand(tpl.read_text())
    if "@@" in out:
        raise SystemExit(f"unexpanded macro in {tpl.name}")
    (OUT / (tpl.stem + ".dc.html")).write_text(out)
    print(f"{tpl.stem}.dc.html {len(out)} bytes")
shutil.copyfile(HERE / "canvas.json", OUT / "canvas.json")
