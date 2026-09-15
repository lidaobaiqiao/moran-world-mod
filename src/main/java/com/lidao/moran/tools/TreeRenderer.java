package com.lidao.moran.tools;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * 树形渲染器 —— 把模拟世界画成侧视 PNG（供 TreeSimulator 批量出图）。
 *
 * <p>投影：X（东西）为横轴、Y 为纵轴，Z（南北）折叠进深度 ——
 * 用亮度区分远近（越靠南越亮），四向枝在侧视图里重叠但可辨。
 * 枝干 = 线段（线宽 = 成熟度档位），花苞 = 粉圆，树叶 = 绿圆。
 */
public final class TreeRenderer {

    private static final Color TRUNK = new Color(0x5A, 0x3D, 0x26);
    private static final Color BRANCH = new Color(0x8A, 0x62, 0x38);
    private static final Color LEAF = new Color(0x7F, 0xAE, 0x6A);
    private static final Color BUD = new Color(0xE0, 0x8A, 0xA0);
    private static final Color BG = new Color(0xF7, 0xF4, 0xEF);
    private static final Color TEXT = new Color(0x40, 0x40, 0x40);

    private TreeRenderer() {
    }

    /** 单棵大图 */
    public static void renderTreePng(TreeSimulator.SimWorld w, Path out, String label) throws Exception {
        Bounds b = bounds(w);
        int cell = 22;
        int wPx = (b.spanX + 3) * cell + 20;
        int hPx = (b.maxY + 3) * cell + 60;
        BufferedImage img = new BufferedImage(wPx, hPx, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(BG);
        g.fillRect(0, 0, wPx, hPx);
        paint(g, w, b, cell, 10, (b.maxY + 1) * cell + 14);
        g.setColor(TEXT);
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15));
        g.drawString(label, 10, hPx - 14);
        g.dispose();
        ImageIO.write(img, "png", out.toFile());
    }

    /** 总览拼图：每行 cols 棵 */
    public static void renderSheetPng(List<TreeSimulator.SimWorld> trees, List<String> labels,
                                      int cols, Path out, String title) throws Exception {
        int cell = 11;
        Bounds proto = bounds(trees.get(0));
        int treeW = (proto.spanX + 3) * cell;
        int treeH = (proto.maxY + 4) * cell + 26;
        int rows = (trees.size() + cols - 1) / cols;
        int wPx = treeW * cols + 20;
        int hPx = (treeH + 26) * rows + 50;
        BufferedImage img = new BufferedImage(wPx, hPx, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(BG);
        g.fillRect(0, 0, wPx, hPx);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        for (int i = 0; i < trees.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            int ox = 10 + col * treeW;
            int oy = 40 + row * (treeH + 26);
            Bounds b = bounds(trees.get(i));
            paint(g, trees.get(i), b, cell, ox, oy + (b.maxY + 1) * cell);
            g.setColor(TEXT);
            g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            g.drawString(labels.get(i), ox + 4, oy + (b.maxY + 3) * cell);
        }
        g.setColor(TEXT);
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
        g.drawString(title, 12, 24);
        g.dispose();
        ImageIO.write(img, "png", out.toFile());
    }

    // ===== 内部 =====

    private record Bounds(int minY, int maxY, int minX, int maxX, int minZ, int maxZ, int spanX) {
    }

    private static Bounds bounds(TreeSimulator.SimWorld w) {
        int maxY = 1, minX = 0, maxX = 0, minZ = 0, maxZ = 0;
        for (Map.Entry<BlockPos, TreeSimulator.SimBlock> e : w.blocks.entrySet()) {
            BlockPos p = e.getKey();
            maxY = Math.max(maxY, p.getY());
            minX = Math.min(minX, p.getX());
            maxX = Math.max(maxX, p.getX());
            minZ = Math.min(minZ, p.getZ());
            maxZ = Math.max(maxZ, p.getZ());
        }
        return new Bounds(0, maxY, minX, maxX, minZ, maxZ, Math.max(1, maxX - minX));
    }

    private static void paint(Graphics2D g, TreeSimulator.SimWorld w, Bounds b, int cell, int ox, int oyGround) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        int zSpan = Math.max(1, b.maxZ - b.minZ);

        // 底线（地面）
        g.setColor(new Color(0xD8, 0xCF, 0xC2));
        g.fillRect(ox, oyGround, (b.spanX + 2) * cell, 2);

        // 先画深度远的（亮度低），再画近的 —— 近处覆盖远处，符合直觉
        for (int pass = 0; pass < 2; pass++) {
            for (Map.Entry<BlockPos, TreeSimulator.SimBlock> e : w.blocks.entrySet()) {
                BlockPos p = e.getKey();
                TreeSimulator.SimBlock s = e.getValue();
                boolean near = p.getZ() >= (b.minZ + b.maxZ) / 2;
                if ((pass == 0) == near) {
                    continue;
                }
                float depth = 0.55F + 0.45F * (p.getZ() - b.minZ) / zSpan;
                int cx = ox + (p.getX() - b.minX + 1) * cell + cell / 2;
                int cy = oyGround - p.getY() * cell - cell / 2;
                switch (s.type) {
                    case BRANCH -> drawBranch(g, w, p, s, cx, cy, cell, depth);
                    case BUD -> {
                        g.setColor(BUD);
                        fillCircle(g, cx, cy, cell * 0.32);
                    }
                    case LEAF -> {
                        g.setColor(leafColor(depth));
                        fillCircle(g, cx, cy, cell * 0.38);
                    }
                }
            }
        }
    }

    /** 枝干线段：从「来向」画到「去向」，线宽 = 档位（贯穿格的柱体投影） */
    private static void drawBranch(Graphics2D g, TreeSimulator.SimWorld w, BlockPos p,
                                   TreeSimulator.SimBlock s, int cx, int cy, int cell, float depth) {
        Color base = s.trunk ? TRUNK : BRANCH;
        g.setColor(shade(base, depth));
        float width = Math.max(1.6F, s.growth * cell / 9F);
        g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        Direction f = s.facing;
        Direction back = f.getOpposite();

        // 出点：facing 邻格是同类（枝/花/叶）→ 画到邻格中心；否则画到本格边界
        BlockPos front = p.offset(f);
        TreeSimulator.SimBlock fb = w.get(front);
        int ex, ey;
        if (fb != null) {
            ex = cx + f.getOffsetX() * cell;
            ey = cy - f.getOffsetY() * cell;
        } else {
            ex = cx + f.getOffsetX() * cell / 2;
            ey = cy - f.getOffsetY() * cell / 2;
        }
        // 入点：back 邻格是同类枝 → 从邻格中心起（避免链上出现断点）；否则从本格边界起
        TreeSimulator.SimBlock bb = w.get(p.offset(back));
        int sx, sy;
        if (bb != null && bb.type == TreeSimulator.SimType.BRANCH) {
            sx = cx - f.getOffsetX() * cell;
            sy = cy + f.getOffsetY() * cell;
        } else {
            sx = cx - f.getOffsetX() * cell / 2;
            sy = cy + f.getOffsetY() * cell / 2;
        }
        g.drawLine(sx, sy, ex, ey);

        // 分叉点标记：本节分出的子枝方向画短刺（视觉上强调分叉位置）
        if (s.forkMask != 0) {
            g.setStroke(new BasicStroke(Math.max(1.2F, width * 0.6F),
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (Direction side : perpendiculars(f)) {
                int slot = slotOf(f, side);
                if (slot >= 0 && (s.forkMask & (1 << slot)) != 0) {
                    g.drawLine(cx, cy,
                            cx + side.getOffsetX() * cell / 2,
                            cy - side.getOffsetY() * cell / 2);
                }
            }
        }
    }

    private static Color leafColor(float depth) {
        return shade(LEAF, depth);
    }

    private static Color shade(Color c, float factor) {
        return new Color(
                clamp((int) (c.getRed() * factor + 255 * (1 - factor) * 0.35F)),
                clamp((int) (c.getGreen() * factor + 255 * (1 - factor) * 0.35F)),
                clamp((int) (c.getBlue() * factor + 255 * (1 - factor) * 0.35F)));
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private static void fillCircle(Graphics2D g, int cx, int cy, double r) {
        int d = (int) Math.round(r * 2);
        g.fillOval((int) (cx - r), (int) (cy - r), d, d);
    }

    // ===== 分叉槽位（与 TreeSimulator 一致的复刻） =====

    private static Direction[] perpendiculars(Direction facing) {
        if (facing.getAxis() == Direction.Axis.Y) {
            return new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        }
        return new Direction[]{facing.rotateYClockwise(), facing.rotateYCounterclockwise(),
                Direction.UP, Direction.DOWN};
    }

    private static int slotOf(Direction facing, Direction side) {
        Direction[] slots = perpendiculars(facing);
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == side) {
                return i;
            }
        }
        return -1;
    }
}
