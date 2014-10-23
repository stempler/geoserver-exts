package org.geotools.ysld.parse;

import org.geotools.process.function.ProcessFunction;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.SLD;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.ysld.Ysld;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;

import java.awt.*;
import java.io.IOException;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.describedAs;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class YsldParseTest {

    @Test
    public void testAnchor() throws Exception {
        String yaml =
        "blue: &blue rgb(0,0,255)\n" +
        "point: \n"+
        "  symbols: \n" +
        "  - mark: \n" +
        "      fill-color: *blue\n";

        StyledLayerDescriptor sld = Ysld.parse(yaml);
        PointSymbolizer p = SLD.pointSymbolizer(SLD.defaultStyle(sld));
        assertEquals(Color.BLUE, SLD.color(SLD.fill(p)));
    }

    @Test
    public void testNamedColor() throws Exception {
        String yaml =
        "point: \n"+
        "  symbols: \n" +
        "  - mark: \n" +
        "      fill-color: blue\n";

        StyledLayerDescriptor sld = Ysld.parse(yaml);
        PointSymbolizer p = SLD.pointSymbolizer(SLD.defaultStyle(sld));
        assertEquals(Color.BLUE, SLD.color(SLD.fill(p)));
    }

    @Test
    public void testFunction() throws Exception {
        String yaml =
        "rules: \n"+
        "- filter: strEndsWith(foo,'bar') = true\n";

        StyledLayerDescriptor sld = Ysld.parse(yaml);
        Rule r = SLD.defaultStyle(sld).featureTypeStyles().get(0).rules().get(0);

        PropertyIsEqualTo f = (PropertyIsEqualTo) r.getFilter();
        Function func = (Function) f.getExpression1();
        assertEquals("strEndsWith", func.getName());
        assertTrue(func.getParameters().get(0) instanceof PropertyName);
        assertTrue(func.getParameters().get(1) instanceof Literal);

        Literal lit = (Literal) f.getExpression2();
    }

    @Test
    public void testRenderingTransformation() throws IOException {
        String yaml =
        "feature-styles: \n"+
        "- transform:\n" +
        "    name: ras:Contour\n" +
        "    params:\n" +
        "      levels:\n" +
        "      - 1000\n" +
        "      - 1100\n" +
        "      - 1200\n";

        StyledLayerDescriptor sld = Ysld.parse(yaml);
        FeatureTypeStyle fs = SLD.defaultStyle(sld).featureTypeStyles().get(0);

        Expression tx = fs.getTransformation();
        assertNotNull(tx);

        ProcessFunction pf = (ProcessFunction) tx;
        assertEquals(2, pf.getParameters().size());

        Function e1 = (Function) pf.getParameters().get(0);
        assertEquals(1, e1.getParameters().size());
        assertTrue(e1.getParameters().get(0) instanceof Literal);
        assertEquals("data", e1.getParameters().get(0).evaluate(null, String.class));

        Function e2 = (Function) pf.getParameters().get(1);
        assertEquals(4, e2.getParameters().size());
        assertTrue(e2.getParameters().get(0) instanceof Literal);
        assertEquals("levels", e2.getParameters().get(0).evaluate(null, String.class));
        assertEquals(1000, e2.getParameters().get(1).evaluate(null, Integer.class).intValue());
        assertEquals(1100, e2.getParameters().get(2).evaluate(null, Integer.class).intValue());
        assertEquals(1200, e2.getParameters().get(3).evaluate(null, Integer.class).intValue());
    }

    @Test
    public void testRenderingTransformationHeatmap() throws IOException {
        String yaml =
            "feature-styles: \n"+
            "- transform:\n" +
            "    name: vec:Heatmap\n" +
            "    params:\n" +
            "      weightAttr: pop2000\n" +
            "      radius: 100\n" +
            "      pixelsPerCell: 10\n" +
            "      outputBBOX: wms_bbox\n" +
            "      outputWidth: wms_width\n" +
            "      outputHeight: wms_height\n";


        StyledLayerDescriptor sld = Ysld.parse(yaml);
        FeatureTypeStyle fs = SLD.defaultStyle(sld).featureTypeStyles().get(0);

        Expression tx = fs.getTransformation();
        assertNotNull(tx);

        ProcessFunction pf = (ProcessFunction) tx;
        assertEquals(7, pf.getParameters().size());

        Function e = (Function) pf.getParameters().get(0);
        assertEquals(1, e.getParameters().size());
        assertTrue(e.getParameters().get(0) instanceof Literal);
        assertEquals("data", e.getParameters().get(0).evaluate(null, String.class));

        e = (Function) pf.getParameters().get(1);
        assertEquals(2, e.getParameters().size());
        assertTrue(e.getParameters().get(0) instanceof Literal);
        assertEquals("weightAttr", e.getParameters().get(0).evaluate(null, String.class));
        assertEquals("pop2000", e.getParameters().get(1).evaluate(null, String.class));
    }
    
    @Test
    public void testLabelShield() throws Exception {
        String yaml =
                "feature-styles:\n"+
                "- name: name\n"+
                " rules:\n"+
                " - symbolizers:\n"+
                "   - line:\n"+
                "       stroke-color: '#555555'\n"+
                "       stroke-width: 1.0\n"+
                "    - text:\n"+
                "       label: name\n"+
                "       symbols:\n"+
                "        - mark:\n"+
                "           shape: circle\n"+
                "           fill-color: '#995555'\n"+
                "       geometry: geom";
                        
    }
    
    /**
     * Matches a rule if it applies to a given scale
     * @param scale denominator of the scale
     */
    @SuppressWarnings("unchecked")
    static Matcher<Rule> appliesToScale(double scale) {
        return describedAs("rule applies to scale denom %0",
                allOf(
                    Matchers.<Rule>hasProperty("maxScaleDenominator", 
                        greaterThan(scale)
                    ),
                    Matchers.<Rule>hasProperty("minScaleDenominator", 
                        lessThan(scale)
                    )),
                    scale
                );
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testZoomSimple() throws IOException {
        String yaml =
            "gridset: simple:2x5000000\n"+
            "feature-styles: \n"+
            "- name: name\n"+
            "  rules:\n"+
            "  - zoom: (0,0)\n";
        
        
        StyledLayerDescriptor sld = Ysld.parse(yaml);
        FeatureTypeStyle fs = SLD.defaultStyle(sld).featureTypeStyles().get(0);
        
        assertThat((Iterable<Rule>)fs.rules(), hasItems(
                allOf(
                        appliesToScale(5000000),
                        not(appliesToScale(5000000/2)),
                        not(appliesToScale(5000000*2))
                    )));
        
    }
    
    @Test
    public void testZoomSimpleRange() throws IOException {
        String yaml =
            "gridset: simple:2x5000000\n"+
            "feature-styles: \n"+
            "- name: name\n"+
            "  rules:\n"+
            "  - zoom: (1,2)\n";
        
        
        StyledLayerDescriptor sld = Ysld.parse(yaml);
        FeatureTypeStyle fs = SLD.defaultStyle(sld).featureTypeStyles().get(0);
        
        Rule r = fs.rules().get(0);
        
        assertThat(r, appliesToScale(5000000/2)); // Z=1
        assertThat(r, appliesToScale(5000000/4)); // Z=2
        assertThat(r, not(appliesToScale(5000000)));  // Z=0
        assertThat(r, not(appliesToScale(5000000/8))); // Z=3
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testZoomSimpleWithDifferentInitial() throws IOException {
        String yaml =
            "gridset: simple:2x5000000@3\n"+
            "feature-styles: \n"+
            "- name: name\n"+
            "  rules:\n"+
            "  - zoom: (0,0)\n"+
            "  - zoom: (3,3)\n";
        
        
        StyledLayerDescriptor sld = Ysld.parse(yaml);
        FeatureTypeStyle fs = SLD.defaultStyle(sld).featureTypeStyles().get(0);
        
        assertThat((Iterable<Rule>)fs.rules(), hasItems(
                allOf(
                        appliesToScale(5000000*8),
                        not(appliesToScale(5000000/2*8)),
                        not(appliesToScale(5000000*2*8))
                    ),
                allOf(
                        appliesToScale(5000000*8),
                        not(appliesToScale(5000000/2*8)),
                        not(appliesToScale(5000000*2*8))
                    )
                ));
        
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testZoomDefault() throws IOException {
        String yaml =
            "feature-styles: \n"+
            "- name: name\n"+
            "  rules:\n"+
            "  - zoom: (0,0)\n"+
            "  - zoom: (1,1)\n"+
            "  - zoom: (2,2)\n"+
            "  - zoom: (3,3)\n"+
            "  - zoom: (4,4)\n"+
            "  - zoom: (5,5)\n"+
            "  - zoom: (6,6)\n"+
            "  - zoom: (7,7)\n"+
            "  - zoom: (8,8)\n"+
            "  - zoom: (9,9)\n"+
            "  - zoom: (10,10)\n"+
            "  - zoom: (11,11)\n"+
            "  - zoom: (12,12)\n"+
            "  - zoom: (13,13)\n"+
            "  - zoom: (14,14)\n"+
            "  - zoom: (15,15)\n"+
            "  - zoom: (16,16)\n"+
            "  - zoom: (17,17)\n"+
            "  - zoom: (18,18)\n"+
            "  - zoom: (19,19)\n";
        
        // m/px
        double[] googlePixelSizes = {
                156543.0339280410,
                78271.51696402048,
                39135.75848201023,
                19567.87924100512,
                9783.939620502561,
                4891.969810251280,
                2445.984905125640,
                1222.992452562820,
                611.4962262814100,
                305.7481131407048,
                152.8740565703525,
                76.43702828517624,
                38.21851414258813,
                19.10925707129406,
                9.554628535647032,
                4.777314267823516,
                2.388657133911758,
                1.194328566955879,
                0.5971642834779395,
                0.29858214173896974,
                0.14929107086948487
        };
        
        double INCHES_PER_METRE = 39.3701;
        double OGC_DPI = 90;
        
        double scaleDenominators[] = new double[googlePixelSizes.length];
        for(int i=0; i<googlePixelSizes.length; i++){
            scaleDenominators[i]=OGC_DPI*INCHES_PER_METRE*googlePixelSizes[i];
        }
        
        StyledLayerDescriptor sld = Ysld.parse(yaml);
        FeatureTypeStyle fs = SLD.defaultStyle(sld).featureTypeStyles().get(0);
        
        for(int i=0; i<googlePixelSizes.length; i++){
            scaleDenominators[i]=OGC_DPI*INCHES_PER_METRE*googlePixelSizes[i];
        }
        
        assertThat(fs.rules().size(), is(20));
        
        assertThat(fs.rules().get(0) ,allOf(
                appliesToScale(scaleDenominators[0]),
                not(appliesToScale(scaleDenominators[1]))
            ));
        assertThat(fs.rules().get(19) ,allOf(
                appliesToScale(scaleDenominators[19]),
                not(appliesToScale(scaleDenominators[18]))
            ));
        for(int i = 1; i<19; i++){
            assertThat(fs.rules().get(i) ,describedAs("rule applies only to level %0 (%1)",
                    allOf(
                        appliesToScale(scaleDenominators[i]),
                        not(appliesToScale(scaleDenominators[i+1])),
                        not(appliesToScale(scaleDenominators[i+-1]))
                    ),
                    i,scaleDenominators[i]
               ));
        }
        
    }

}
