#!/usr/bin/env python3
"""Derive a headless 'twin' of a JFrame/Frame class: same code, but a JPanel instead of a top-level window.
usage: make_twin.py Source.java NewClassName out_dir
The event-handling and layout code is untouched; only window-only statements are removed/redirected."""
import re, sys, os
src, newname, outdir = sys.argv[1:4]
t = open(src).read()
m = re.search(r'public class (\w+)', t); old = m.group(1)
t = re.sub(r'\b%s\b' % old, newname, t)
t = t.replace('extends JFrame', 'extends JPanel').replace('extends Frame', 'extends JPanel')
t = re.sub(r'\n\s*super\("[^"]*"\);', '', t)                       # super("title")
t = re.sub(r'\n\s*setDefaultCloseOperation\([^)]*\);', '', t)       # close operation is window-only
t = re.sub(r'\n\s*addWindowListener\(new WindowAdapter\(\) \{.*?\n\s*\}\);', '', t, flags=re.S)   # window listener block
t = t.replace('pack();', 'setSize(getPreferredSize());')
t = t.replace('JOptionPane.showMessageDialog(', 'Twin.dialog(')    # modal dialogs need a display: record the message instead
t = re.sub(r'\bLabel\b', 'JLabel', t)                               # AWT Label -> JLabel (DrawPad)
t = t.replace('import java.awt.JLabel;', '')
if 'import javax.swing.*;' not in t: t = 'import javax.swing.*;\n' + t
os.makedirs(outdir, exist_ok=True); open(os.path.join(outdir, newname + '.java'), 'w').write(t)
