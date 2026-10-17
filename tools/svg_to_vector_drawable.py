#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Converts OneShot Film Cinema SVG icons to Android VectorDrawable XML files.
"""
import os
import re
import xml.etree.ElementTree as ET

def circle_to_path(cx, cy, r):
    return f"M {cx - r},{cy} a {r},{r} 0 1 0 {2 * r},0 a {r},{r} 0 1 0 -{2 * r},0"

def rect_to_path(x, y, w, h, rx=0, ry=0):
    if rx == 0 and ry == 0:
        return f"M {x},{y} h {w} v {h} h {-w} Z"
    rx = rx if rx != 0 else ry
    ry = ry if ry != 0 else rx
    # rounded rect
    return (
        f"M {x + rx},{y} "
        f"h {w - 2 * rx} "
        f"a {rx},{ry} 0 0 1 {rx},{ry} "
        f"v {h - 2 * ry} "
        f"a {rx},{ry} 0 0 1 {-rx},{ry} "
        f"h {-(w - 2 * rx)} "
        f"a {rx},{ry} 0 0 1 {-rx},{-ry} "
        f"v {-(h - 2 * ry)} "
        f"a {rx},{ry} 0 0 1 {rx},{-ry} Z"
    )

def line_to_path(x1, y1, x2, y2):
    return f"M {x1},{y1} L {x2},{y2}"

def polygon_to_path(points_str):
    pts = [p.strip() for p in re.split(r'[\s,]+', points_str.strip()) if p.strip()]
    if not pts:
        return ""
    d = f"M {pts[0]},{pts[1]}"
    for i in range(2, len(pts), 2):
        d += f" L {pts[i]},{pts[i+1]}"
    d += " Z"
    return d

def convert_svg_to_vector(svg_path, out_xml_path):
    tree = ET.parse(svg_path)
    root = tree.getroot()

    xml_lines = [
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
        '    android:width="24dp"',
        '    android:height="24dp"',
        '    android:viewportWidth="24"',
        '    android:viewportHeight="24">',
    ]

    # Process all child elements
    for el in root.iter():
        tag = el.tag.split('}')[-1]
        if tag == 'svg' or tag.startswith('{'):
            continue

        stroke_color = '#FFFFFFFF'
        fill_color = None
        stroke_width = el.attrib.get('stroke-width', '1.6')
        opacity = el.attrib.get('opacity', None)

        fill_attr = el.attrib.get('fill', 'none')
        if fill_attr == 'currentColor':
            fill_color = '#FFFFFFFF'
        elif fill_attr != 'none':
            fill_color = fill_attr

        path_data = None

        if tag == 'path':
            path_data = el.attrib.get('d', '')
        elif tag == 'circle':
            cx = float(el.attrib['cx'])
            cy = float(el.attrib['cy'])
            r = float(el.attrib['r'])
            path_data = circle_to_path(cx, cy, r)
        elif tag == 'rect':
            x = float(el.attrib['x'])
            y = float(el.attrib['y'])
            w = float(el.attrib['width'])
            h = float(el.attrib['height'])
            rx = float(el.attrib.get('rx', 0))
            ry = float(el.attrib.get('ry', 0))
            path_data = rect_to_path(x, y, w, h, rx, ry)
        elif tag == 'line':
            x1 = float(el.attrib['x1'])
            y1 = float(el.attrib['y1'])
            x2 = float(el.attrib['x2'])
            y2 = float(el.attrib['y2'])
            path_data = line_to_path(x1, y1, x2, y2)
        elif tag == 'polygon':
            path_data = polygon_to_path(el.attrib['points'])

        if path_data:
            path_data = ' '.join(path_data.split())
            p_attrs = [f'        android:pathData="{path_data}"']
            if fill_color:
                p_attrs.append(f'        android:fillColor="{fill_color}"')
            if fill_attr == 'none' or not fill_color:
                p_attrs.append('        android:fillColor="#00000000"')
            if el.attrib.get('stroke') == 'currentColor' or el.attrib.get('stroke') is not None or stroke_width:
                p_attrs.append(f'        android:strokeColor="{stroke_color}"')
                p_attrs.append(f'        android:strokeWidth="{stroke_width}"')
                p_attrs.append('        android:strokeLineCap="round"')
                p_attrs.append('        android:strokeLineJoin="round"')
            if opacity:
                p_attrs.append(f'        android:strokeAlpha="{opacity}"')
                if fill_color:
                    p_attrs.append(f'        android:fillAlpha="{opacity}"')

            xml_lines.append('    <path')
            xml_lines.append('\n'.join(p_attrs) + ' />')

    xml_lines.append('</vector>')
    with open(out_xml_path, 'w', encoding='utf-8') as f:
        f.write('\n'.join(xml_lines) + '\n')

def main():
    icons_dir = "preview/icons"
    out_dir = "app/src/main/res/drawable"
    os.makedirs(out_dir, exist_ok=True)

    count = 0
    for fname in os.listdir(icons_dir):
        if fname.endswith(".svg"):
            base = os.path.splitext(fname)[0]
            svg_file = os.path.join(icons_dir, fname)
            xml_file = os.path.join(out_dir, f"ic_{base}.xml")
            convert_svg_to_vector(svg_file, xml_file)
            count += 1

    print(f"Successfully converted {count} SVGs into Android VectorDrawable XMLs in {out_dir}!")

if __name__ == "__main__":
    main()
