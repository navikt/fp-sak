package no.nav.foreldrepenger.web.app.metrics;

import java.io.IOException;
import java.io.Writer;
import java.util.Enumeration;

import io.prometheus.client.Collector;

public final class TextFormater {
    /**
     * Content-type for text version 0.0.4.
     */
    public final static String CONTENT_TYPE_004 = "text/plain; version=0.0.4; charset=utf-8";

    private TextFormater() {
        // util class
    }

    /**
     * Write out the text version 0.0.4 of the given MetricFamilySamples.
     */
    public static void write004(Writer writer, Enumeration<Collector.MetricFamilySamples> mfs) throws IOException {
        /* See http://prometheus.io/docs/instrumenting/exposition_formats/
         * for the output format specification. */
        while(mfs.hasMoreElements()) {
            var metricFamilySamples = mfs.nextElement();
            writer.write("# HELP ");
            writer.write(metricFamilySamples.name);
            writer.write(' ');
            writeEscapedHelp(writer, metricFamilySamples.help);
            writer.write('\n');

            writer.write("# TYPE ");
            writer.write(metricFamilySamples.name);
            writer.write(' ');
            writer.write(typeString(metricFamilySamples.type));
            writer.write('\n');

            for (var sample: metricFamilySamples.samples) {
                writer.write(sample.name);
                if (sample.labelNames.size() > 0) {
                    writer.write('{');
                    for (var i = 0; i < sample.labelNames.size(); ++i) {
                        writer.write(sample.labelNames.get(i));
                        writer.write("=\"");
                        writeEscapedLabelValue(writer, sample.labelValues.get(i));
                        writer.write("\",");
                    }
                    writer.write('}');
                }
                writer.write(' ');
                writer.write(Collector.doubleToGoString(sample.value));
                if (sample.timestampMs != null){
                    writer.write(' ');
                    writer.write(sample.timestampMs.toString());
                }
                writer.write('\n');
            }
        }
    }

    private static String typeString(Collector.Type t) {
        return switch (t) {
            case GAUGE -> "gauge";
            case COUNTER -> "counter";
            case SUMMARY -> "summary";
            case HISTOGRAM -> "histogram";
            default -> "untyped";
        };
    }

    private static void writeEscapedHelp(Writer writer, String s) throws IOException {
        for (var i = 0; i < s.length(); i++) {
            var c = s.charAt(i);
            switch (c) {
                case '\\' -> writer.append("\\\\");
                case '\n' -> writer.append("\\n");
                default -> writer.append(c);
            }
        }
    }

    private static void writeEscapedLabelValue(Writer writer, String s) throws IOException {
        for (var i = 0; i < s.length(); i++) {
            var c = s.charAt(i);
            switch (c) {
                case '\\' -> writer.append("\\\\");
                case '\"' -> writer.append("\\\"");
                case '\n' -> writer.append("\\n");
                default -> writer.append(c);
            }
        }
    }
}
