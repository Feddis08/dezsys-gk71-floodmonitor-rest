from pathlib import Path
from html import escape
import re
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Preformatted, Table, TableStyle
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
root=Path(__file__).resolve().parents[1]
pdfmetrics.registerFont(TTFont('DejaVu','/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'))
pdfmetrics.registerFont(TTFont('DejaVuBold','/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'))
pdfmetrics.registerFont(TTFont('DejaVuMono','/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf'))
styles=getSampleStyleSheet()
for name in ['Normal','Heading1','Heading2','Heading3']:
    styles[name].fontName='DejaVuBold' if name.startswith('Heading') else 'DejaVu'
styles['Normal'].fontSize=9
styles['Normal'].leading=13
styles['Normal'].spaceAfter=6
styles['Heading1'].fontSize=16
styles['Heading2'].fontSize=12
styles['Heading2'].leading=16
code=ParagraphStyle('Code',fontName='DejaVuMono',fontSize=6.5,leading=9,spaceAfter=9)
cell=ParagraphStyle('Cell',parent=styles['Normal'],fontSize=8,leading=11,spaceAfter=0)
def inline(s):
    s=escape(s)
    s=re.sub(r'\[([^\]]+)\]\((https?://[^)]+)\)',r'<a href="\2" color="#174d86">\1</a>',s)
    s=re.sub(r'`([^`]+)`',r'<font name="DejaVuMono">\1</font>',s)
    s=re.sub(r'\*\*([^*]+)\*\*',r'<b>\1</b>',s)
    return s
story=[]
lines=(root/'docs/CHEATSHEET.md').read_text().splitlines()
i=0
while i<len(lines):
    line=lines[i]
    if line.startswith('```'):
        buf=[]; i+=1
        while i<len(lines) and not lines[i].startswith('```'):
            buf.append(lines[i]);i+=1
        story.append(Preformatted('\n'.join(buf),code,maxLineLength=106))
    elif line.startswith('|'):
        rows=[]
        while i<len(lines) and lines[i].startswith('|'):
            vals=[s.strip() for s in lines[i].strip('|').split('|')]
            if not all(re.fullmatch(r'[-: ]+',s) for s in vals):rows.append([Paragraph(inline(s),cell) for s in vals])
            i+=1
        t=Table(rows,colWidths=[62,238,195],repeatRows=1,hAlign='LEFT')
        t.setStyle(TableStyle([('BACKGROUND',(0,0),(-1,0),colors.HexColor('#eeeeee')),('VALIGN',(0,0),(-1,-1),'TOP'),('LINEBELOW',(0,0),(-1,0),0.5,colors.grey),('BOTTOMPADDING',(0,0),(-1,-1),6),('TOPPADDING',(0,0),(-1,-1),6)]))
        story.extend([t,Spacer(1,10)]); continue
    elif line.startswith('# '):story.append(Paragraph(inline(line[2:]),styles['Heading1']))
    elif line.startswith('## '):story.append(Paragraph(inline(line[3:]),styles['Heading2']))
    elif line.strip():
        para=[line];i+=1
        while i<len(lines) and lines[i].strip() and not lines[i].startswith(('#','```','|','- ')):
            para.append(lines[i]);i+=1
        story.append(Paragraph(inline(' '.join(para)),styles['Normal'])); continue
    i+=1
def footer(canvas,doc):
    canvas.setFont('DejaVu',8);canvas.drawRightString(A4[0]-42,25,str(doc.page))
doc=SimpleDocTemplate(str(root/'docs/CHEATSHEET.pdf'),pagesize=A4,rightMargin=42,leftMargin=42,topMargin=35,bottomMargin=40,title='GK7.1 – REST, OpenAPI und Datenformate',author='')
doc.build(story,onFirstPage=footer,onLaterPages=footer)
print(root/'docs/CHEATSHEET.pdf')
