package com.esgdev.sparkpaint.engine.tools.renderers;

import com.esgdev.sparkpaint.engine.tools.LineTool;
import org.junit.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LineToolRendererTest {

    @Test
    public void curveThroughControlPointsPassesThroughEndpoints() {
        LineToolRenderer renderer = new LineToolRenderer();
        Point first = new Point(0, 0);
        Point last = new Point(40, 0);
        List<Point> controls = Arrays.asList(first, new Point(20, 30), last);

        List<Point> curve = renderer.calculateCurvePoints(controls);

        assertTrue("curve should be sampled densely", curve.size() > controls.size());
        assertEquals(first, curve.get(0));
        assertEquals(last, curve.get(curve.size() - 1));
    }

    @Test
    public void curveWithFewerThanTwoPointsIsReturnedUnchanged() {
        LineToolRenderer renderer = new LineToolRenderer();
        List<Point> single = Arrays.asList(new Point(3, 4));

        assertEquals(single, renderer.calculateCurvePoints(single));
    }

    @Test
    public void thickLineIsPaintedAlongItsPath() {
        BufferedImage image = new BufferedImage(20, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        try {
            new LineToolRenderer().drawLine(image, g2d, new Point(0, 5), new Point(19, 5),
                    Color.RED, 3f);
        } finally {
            g2d.dispose();
        }

        assertEquals(Color.RED.getRGB(), image.getRGB(10, 5));
        assertEquals(0, image.getRGB(10, 0));
    }

    @Test
    public void previewDrawsOnTempCanvas() {
        BufferedImage tempCanvas = new BufferedImage(20, 10, BufferedImage.TYPE_INT_ARGB);
        List<Point> points = Arrays.asList(new Point(0, 5));

        new LineToolRenderer().drawPreview(tempCanvas, LineTool.LineMode.SINGLE_LINE, points,
                new Point(19, 5), Color.RED, 3f);

        assertEquals(Color.RED.getRGB(), tempCanvas.getRGB(10, 5));
    }
}
