#!/usr/bin/env python3
"""Render the Mopane accounting training guide (markdown subset) to a printable PDF."""
import re, sys, html
from reportlab.lib.pagesizes import A4
from reportlab.lib.units import mm
from reportlab.lib import colors
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.enums import TA_LEFT
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (BaseDocTemplate, PageTemplate, Frame, Paragraph, Spacer,
                                Table, TableStyle, PageBreak, KeepTogether, HRFlowable,
                                NextPageTemplate)
from reportlab.platypus.tableofcontents import TableOfContents

SRC = sys.argv[1]
OUT = sys.argv[2]
MODE = sys.argv[3] if len(sys.argv) > 3 else "training"   # "training" | "handbook"

F = "/System/Library/Fonts/Supplemental/"
pdfmetrics.registerFont(TTFont("Body", F + "Arial.ttf"))
pdfmetrics.registerFont(TTFont("Body-Bold", F + "Arial Bold.ttf"))
pdfmetrics.registerFont(TTFont("Body-Italic", F + "Arial Italic.ttf"))
pdfmetrics.registerFont(TTFont("Body-BoldItalic", F + "Arial Bold Italic.ttf"))
pdfmetrics.registerFontFamily("Body", normal="Body", bold="Body-Bold",
                              italic="Body-Italic", boldItalic="Body-BoldItalic")
pdfmetrics.registerFont(TTFont("Mono", "/System/Library/Fonts/Menlo.ttc", subfontIndex=0))
pdfmetrics.registerFont(TTFont("Uni", F + "Arial Unicode.ttf"))  # checkbox glyphs

INK   = colors.HexColor("#1b1f23")
MUTED = colors.HexColor("#5c6670")
BRAND = colors.HexColor("#1f5c3d")
RULE  = colors.HexColor("#d5dbe0")
HEADBG= colors.HexColor("#eef2f4")
ZEBRA = colors.HexColor("#f8fafb")

def st(name, **kw):
    kw.setdefault("fontName", "Body"); kw.setdefault("textColor", INK)
    kw.setdefault("alignment", TA_LEFT)
    return ParagraphStyle(name, **kw)

S = {
 "cover_title":  st("ct", fontName="Body-Bold", fontSize=30, leading=35, textColor=BRAND, spaceAfter=6),
 "cover_sub":    st("cs", fontSize=15, leading=20, textColor=MUTED, spaceAfter=18),
 "cover_meta":   st("cm", fontSize=10.5, leading=17, textColor=INK),
 "h1":           st("h1", fontName="Body-Bold", fontSize=18, leading=22, textColor=BRAND,
                    spaceBefore=2, spaceAfter=10),
 "h2":           st("h2", fontName="Body-Bold", fontSize=13, leading=17, textColor=INK,
                    spaceBefore=14, spaceAfter=6),
 "h3":           st("h3", fontName="Body-Bold", fontSize=11, leading=14, textColor=colors.HexColor("#2c3e50"),
                    spaceBefore=11, spaceAfter=4),
 "body":         st("bd", fontSize=9.6, leading=13.6, spaceAfter=6),
 "bullet":       st("bu", fontSize=9.6, leading=13.4, spaceAfter=3, leftIndent=12, bulletIndent=2),
 "num":          st("nu", fontSize=9.6, leading=13.4, spaceAfter=3, leftIndent=16, bulletIndent=2),
 "cell":         st("ce", fontSize=8.5, leading=11.4),
 "cellh":        st("ch", fontSize=8.5, leading=11.4, fontName="Body-Bold"),
 "cell_s":       st("ces", fontSize=7.6, leading=10),
 "cellh_s":      st("chs", fontSize=7.6, leading=10, fontName="Body-Bold"),
 "toc1":         st("t1", fontName="Body-Bold", fontSize=10.5, leading=17, spaceBefore=6),
 "toc2":         st("t2", fontSize=9.3, leading=13.6, leftIndent=14, textColor=colors.HexColor("#3a444d")),
}

# ---------- inline markdown -> reportlab mini-html ----------
def inline(t):
    t = html.escape(t, quote=False)
    t = re.sub(r"`([^`]+)`", lambda m: '<font face="Mono" size="8.4" color="#8a2f5f">%s</font>' % m.group(1), t)
    t = re.sub(r"\*\*\*(.+?)\*\*\*", r"<b><i>\1</i></b>", t)
    t = re.sub(r"\*\*(.+?)\*\*", r"<b>\1</b>", t)
    t = re.sub(r"(?<![\w*])\*([^*\n]+?)\*(?![\w*])", r"<i>\1</i>", t)
    t = re.sub(r"\[([^\]]+)\]\(([^)]+)\)",
               lambda m: '<link href="%s" color="#1a5fb4">%s</link>' % (html.escape(m.group(2), quote=True), m.group(1)), t)
    t = t.replace("[ ]", '<font face="Uni" size="12">\u25a1</font>')
    t = t.replace("[x]", '<font face="Uni" size="12">\u2611</font>')
    return t

def split_row(line):
    line = line.strip()
    if line.startswith("|"): line = line[1:]
    if line.endswith("|"): line = line[:-1]
    out, cur, esc = [], "", False
    for ch in line:
        if esc: cur += ch; esc = False
        elif ch == "\\": esc = True
        elif ch == "|": out.append(cur.strip()); cur = ""
        else: cur += ch
    out.append(cur.strip())
    return out

def is_sep(line):
    return bool(re.fullmatch(r"\|?[\s:|-]+\|[\s:|-]*", line.strip())) and "-" in line

PAGE_W = A4[0] - 34*mm   # usable width

def make_table(rows):
    ncol = max(len(r) for r in rows)
    rows = [r + [""]*(ncol-len(r)) for r in rows]
    # column widths from content length, clamped
    weights = []
    for c in range(ncol):
        longest = max(len(re.sub(r"[*`\[\]]", "", r[c])) for r in rows)
        weights.append(max(6, min(longest, 62)))
    total = float(sum(weights))
    widths = [PAGE_W * w / total for w in weights]
    # enforce a minimum readable width; tighten for wide tables
    wide = ncol >= 7
    minw = (15*mm if wide else 21*mm)
    deficit = sum(max(0, minw - w) for w in widths)
    if deficit:
        spare = [w - minw for w in widths if w > minw]
        pool = sum(spare) or 1
        widths = [minw if w < minw else w - (w - minw)/pool*deficit for w in widths]
    cs, chs = (S["cell_s"], S["cellh_s"]) if wide else (S["cell"], S["cellh"])
    data = [[Paragraph(inline(c), chs) for c in rows[0]]]
    blank = []
    for n, r in enumerate(rows[1:], start=1):
        data.append([Paragraph(inline(c), cs) for c in r])
        if sum(1 for c in r if c.strip()) <= 1:
            blank.append(n)   # signature / fill-in row: give it writing space
    rowh = [None] * len(data)
    for n in blank:
        rowh[n] = 11*mm
    t = Table(data, colWidths=widths, rowHeights=rowh, repeatRows=1, hAlign="LEFT")
    style = [("BACKGROUND", (0,0), (-1,0), HEADBG),
             ("LINEBELOW", (0,0), (-1,0), 0.9, BRAND),
             ("GRID", (0,0), (-1,-1), 0.35, RULE),
             ("VALIGN", (0,0), (-1,-1), "TOP"),
             ("LEFTPADDING", (0,0), (-1,-1), 5), ("RIGHTPADDING", (0,0), (-1,-1), 5),
             ("TOPPADDING", (0,0), (-1,-1), 4), ("BOTTOMPADDING", (0,0), (-1,-1), 4)]
    for i in range(2, len(data), 2):
        style.append(("BACKGROUND", (0,i), (-1,i), ZEBRA))
    t.setStyle(TableStyle(style))
    return t

# ---------- headings that report into the TOC ----------
class TocPara(Paragraph):
    def __init__(self, text, style, level, key):
        Paragraph.__init__(self, text, style)
        self._lvl, self._key = level, key
    def draw(self):
        Paragraph.draw(self)
        self.canv.bookmarkPage(self._key)
        self.canv.addOutlineEntry(self.plain(), self._key, self._lvl, 0)
    def plain(self):
        return re.sub(r"<[^>]+>", "", self.getPlainText())

# ---------- parse ----------
lines = open(SRC, encoding="utf-8").read().split("\n")
story = []
i = 0
nkey = 0
first_h1 = True
# skip the original title block: we build our own cover
START = "## About this handbook" if MODE == "handbook" else "## How to use"
while i < len(lines) and not lines[i].startswith(START):
    i += 1

while i < len(lines):
    ln = lines[i]
    s_ = ln.strip()

    if not s_:
        i += 1; continue

    if s_ in ("---", "***", "___"):
        i += 1; continue

    m = re.match(r"^(#{1,4})\s+(.*)$", s_)
    if m:
        lvl, txt = len(m.group(1)), m.group(2).strip()
        nkey += 1; key = "h%d" % nkey
        if lvl == 1:
            if not first_h1:
                story.append(PageBreak())
            first_h1 = False
            story.append(TocPara(inline(txt), S["h1"], 0, key))
            story.append(HRFlowable(width="100%", thickness=1.1, color=BRAND,
                                    spaceBefore=1, spaceAfter=10))
        elif lvl == 2:
            story.append(TocPara(inline(txt), S["h2"], 0 if first_h1 else 1, key))
        else:
            story.append(Paragraph(inline(txt), S["h3"]))
        i += 1; continue

    # table
    if s_.startswith("|") and i + 1 < len(lines) and is_sep(lines[i+1]):
        rows = [split_row(s_)]
        i += 2
        while i < len(lines) and lines[i].strip().startswith("|"):
            rows.append(split_row(lines[i])); i += 1
        story.append(Spacer(1, 3)); story.append(make_table(rows)); story.append(Spacer(1, 9))
        continue

    # bullet / checkbox list
    if re.match(r"^[-*+]\s+", s_):
        items = []
        while i < len(lines) and re.match(r"^[-*+]\s+", lines[i].strip()):
            txt = re.sub(r"^[-*+]\s+", "", lines[i].strip())
            i += 1
            while i < len(lines) and lines[i].startswith("  ") and lines[i].strip() \
                  and not re.match(r"^[-*+]\s+", lines[i].strip()):
                txt += " " + lines[i].strip(); i += 1
            items.append(Paragraph(inline(txt), S["bullet"], bulletText="•"))
        story.extend(items); story.append(Spacer(1, 5)); continue

    # numbered list
    if re.match(r"^\d+[.)]\s+", s_):
        n = 0
        while i < len(lines) and re.match(r"^\d+[.)]\s+", lines[i].strip()):
            raw = lines[i].strip(); n += 1
            txt = re.sub(r"^\d+[.)]\s+", "", raw)
            i += 1
            while i < len(lines) and lines[i].startswith("   ") and lines[i].strip() \
                  and not re.match(r"^\d+[.)]\s+", lines[i].strip()):
                txt += " " + lines[i].strip(); i += 1
            story.append(Paragraph(inline(txt), S["num"], bulletText="%d." % n))
        story.append(Spacer(1, 5)); continue

    # paragraph
    buf = []
    while i < len(lines) and lines[i].strip() and not lines[i].strip().startswith(("|", "#", "- ", "* ")) \
          and lines[i].strip() not in ("---", "***") and not re.match(r"^\d+[.)]\s", lines[i].strip()):
        buf.append(lines[i].strip()); i += 1
    if buf:
        story.append(Paragraph(inline(" ".join(buf)), S["body"]))

# ---------- document ----------
if MODE == "handbook":
    TITLE = "Mopane CBS \u2014 Accounting Module"
    SUB   = "User Handbook"
    RUNHEAD = "Mopane CBS \u2014 Accounting Module User Handbook"
    META = [("Product", "Mopane Core Banking System (CBS)"),
            ("Interface", "Mopane CBS web app"),
            ("Platform", "Apache Fineract / Mifos X \u2014 release 17.07.01.RELEASE"),
            ("Audience", "Accountants, finance officers, branch operations staff, administrators")]
    BLURB = ("This handbook explains how accounting works in Mopane CBS: what the system posts for you, "
             "what you post by hand, and how to close a period and produce financial statements. Screen "
             "names, buttons and field labels match what you see in the web app. Where your institution\u2019s "
             "chart of accounts, codes, products or branch names differ from the examples, use your own.")
else:
    TITLE = "Mopane CBS \u2014 Accounting Module"
    SUB   = "Training Guide and User Handbook"
    RUNHEAD = "Mopane CBS \u2014 Accounting Module Training Guide"
    META = [("Platform", "Apache Fineract / Mifos X \u2014 release 17.07.01.RELEASE"),
            ("Interface", "Mopane CBS web app (community-app UI)"),
            ("Audience", "Accountants, finance officers, branch operations staff, administrators"),
            ("Document type", "Trainer script and take-home reference"),
            ("Duration", "One full day (about 6 hours), or two half-days")]
    BLURB = ("Use this document two ways: follow Parts A\u2013I as the session plan while training, then "
             "keep it as the day-to-day operating handbook. Screen names, buttons and field labels match "
             "what users see in the Mopane CBS web app.")

def footer(canv, doc):
    canv.saveState()
    canv.setStrokeColor(RULE); canv.setLineWidth(0.5)
    canv.line(17*mm, 14*mm, A4[0]-17*mm, 14*mm)
    canv.setFont("Body", 7.6); canv.setFillColor(MUTED)
    if doc.page > 1:
        canv.drawString(17*mm, 9.5*mm, RUNHEAD)
        canv.drawRightString(A4[0]-17*mm, 9.5*mm, "Page %d" % (doc.page - 1))
    canv.restoreState()

def cover(canv, doc):
    canv.saveState()
    canv.setFillColor(BRAND); canv.rect(0, A4[1]-13*mm, A4[0], 13*mm, stroke=0, fill=1)
    canv.setFillColor(BRAND); canv.rect(0, 0, A4[0], 8*mm, stroke=0, fill=1)
    canv.restoreState()

class Doc(BaseDocTemplate):
    def afterFlowable(self, flowable):
        if isinstance(flowable, TocPara):
            # page numbering excludes the cover, matching the printed footer
            self.notify("TOCEntry", (flowable._lvl, flowable.plain(),
                                     max(1, self.page - 1), flowable._key))

doc = Doc(OUT, pagesize=A4, leftMargin=17*mm, rightMargin=17*mm,
          topMargin=18*mm, bottomMargin=20*mm, title=TITLE,
          author="Mopane Core Banking System", subject="Accounting module training")
frame = Frame(doc.leftMargin, doc.bottomMargin, doc.width, doc.height, id="n")
doc.addPageTemplates([
    PageTemplate(id="cover", frames=[frame], onPage=cover),
    PageTemplate(id="main", frames=[frame], onPage=footer),
])

toc = TableOfContents()
toc.levelStyles = [S["toc1"], S["toc2"]]
toc.dotsMinLevel = 0

front = [
    Spacer(1, 42*mm),
    Paragraph(TITLE, S["cover_title"]),
    Paragraph(SUB, S["cover_sub"]),
    HRFlowable(width="45%", thickness=2, color=BRAND, spaceAfter=16, hAlign="LEFT"),
] + [Paragraph("<b>%s</b>  %s" % kv, S["cover_meta"]) for kv in META] + [
    Spacer(1, 26*mm),
    Paragraph(BLURB, S["cover_meta"]),
    NextPageTemplate("main"),
    PageBreak(),
    Paragraph("Contents", S["h1"]),
    HRFlowable(width="100%", thickness=1.1, color=BRAND, spaceBefore=1, spaceAfter=10),
    toc,
    PageBreak(),
]

doc.multiBuild(front + story)
print("wrote", OUT)
