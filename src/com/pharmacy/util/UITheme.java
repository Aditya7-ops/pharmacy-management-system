package com.pharmacy.util;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class UITheme {
    // High-Contrast Botanical Palette
    public static final Color FOREST_DEEP     = new Color(18, 52, 34);   // Deep Pine (#123422)
    public static final Color HERBAL_SAGE     = new Color(38, 102, 72);  // Rich Herbal (#266648)
    public static final Color BOTANICAL_FERN  = new Color(52, 128, 85);  // Fern Green
    public static final Color MINT_ACCENT     = new Color(208, 235, 216);// Mint Accent
    public static final Color BIRCH_BG        = new Color(242, 246, 240);// Crisp Canvas
    public static final Color LEAF_WHITE      = new Color(255, 255, 255);
    public static final Color CLAY_TERRACOTTA = new Color(175, 48, 32);  // Alert / Warning Red
    public static final Color EARTH_TEXT      = new Color(20, 24, 20);   // Dark, High-Contrast Black/Green
    public static final Color MUTED_BORDER    = new Color(185, 204, 190);

    // Fonts
    public static final Font FONT_TITLE    = new Font("Segoe UI", Font.BOLD, 17);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_BODY     = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_BOLD     = new Font("Segoe UI", Font.BOLD, 12);

    public static JButton createHerbalButton(String text, Color baseColor) {
        JButton btn = new JButton(text);
        btn.setUI(new BasicButtonUI());
        btn.setFont(FONT_BOLD);
        btn.setBackground(baseColor);
        btn.setForeground(Color.WHITE);
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setFocusPainted(false);
        btn.setBorder(new CompoundBorder(
            new LineBorder(baseColor.darker(), 1, true),
            new EmptyBorder(8, 16, 8, 16)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        Color hoverColor = baseColor.darker();
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(hoverColor); }
            @Override public void mouseExited(MouseEvent e)  { btn.setBackground(baseColor); }
        });
        return btn;
    }

    public static JButton createSecondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setUI(new BasicButtonUI());
        btn.setFont(FONT_BOLD);
        btn.setBackground(new Color(225, 238, 229));
        btn.setForeground(FOREST_DEEP);
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setFocusPainted(false);
        btn.setBorder(new CompoundBorder(
            new LineBorder(HERBAL_SAGE, 2, true),
            new EmptyBorder(7, 14, 7, 14)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(MINT_ACCENT.brighter()); }
            @Override public void mouseExited(MouseEvent e)  { btn.setBackground(new Color(225, 238, 229)); }
        });
        return btn;
    }

    /**
     * Fixes Windows table header rendering:
     * Forces solid deep pine background with bold, high-contrast white text,
     * or bold black text if preferred.
     */
    public static void formatHerbalTable(JTable table) {
        table.setRowHeight(28);
        table.setFont(FONT_BODY);
        table.setGridColor(MUTED_BORDER);
        table.setSelectionBackground(MINT_ACCENT);
        table.setSelectionForeground(EARTH_TEXT);

        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(100, 32));
        header.setReorderingAllowed(false);

        // Custom Header Renderer to stop Windows UI from washing out text/background
        header.setDefaultRenderer(new TableCellRenderer() {
            private final DefaultTableCellRenderer defaultRenderer = new DefaultTableCellRenderer();

            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int col) {
                JLabel label = (JLabel) defaultRenderer.getTableCellRendererComponent(
                        tbl, value, isSelected, hasFocus, row, col);

                label.setFont(new Font("Segoe UI", Font.BOLD, 12));
                // High contrast: Deep Pine background with clean, bright white text
                label.setBackground(FOREST_DEEP);
                label.setForeground(Color.WHITE);
                label.setOpaque(true);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 1, MUTED_BORDER),
                    BorderFactory.createEmptyBorder(4, 6, 4, 6)
                ));
                return label;
            }
        });

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.setDefaultRenderer(Integer.class, centerRenderer);
    }
}