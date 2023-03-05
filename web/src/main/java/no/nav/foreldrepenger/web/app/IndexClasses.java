package no.nav.foreldrepenger.web.app;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexReader;
import org.jboss.jandex.Indexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Henter persistert index (hvis generert) eller genererer index for angitt
 * location (typisk matcher en jar/war fil).
 */
public class IndexClasses {
    private static final Logger LOG = LoggerFactory.getLogger(IndexClasses.class);

    private static final ConcurrentMap<URI, IndexClasses> INDEXES = new ConcurrentHashMap<>();

    private URI scanLocation;
    private String jandexIndexFileName;

    private IndexClasses(URI location) {
        this(location, "jandex.idx");
    }

    private IndexClasses(URI scanLocation, String jandexIndexFileName) {
        this.scanLocation = scanLocation;
        this.jandexIndexFileName = jandexIndexFileName;
    }

    public Index getIndex() {

        if ("file".equals(scanLocation.getScheme()) && scanLocation.getSchemeSpecificPart().contains("/target/")
                && !scanLocation.getSchemeSpecificPart().endsWith(".jar")) {
            // må regenerere index fra fil system i IDE ved å scanne dir, ellers kan den
            // mulig være utdatert (når kjører Jetty i IDE f.eks)
            return scanIndexFromFilesystem(scanLocation);
        }
        return getPersistedJandexIndex(scanLocation);

    }

    private static Index scanIndexFromFilesystem(URI location) {
        try {
            var indexer = new Indexer();
            var source = Paths.get(location);
            try (var paths = Files.walk(source)) {
                paths.filter(Files::isRegularFile).forEach(f -> {
                    var fileName = f.getFileName();
                    if (fileName != null && fileName.toString().endsWith(".class")) {
                        try (var newInputStream = Files.newInputStream(f, StandardOpenOption.READ)) {
                            indexer.index(newInputStream);
                        } catch (IOException e) {
                            throw new IllegalStateException("Fikk ikke indeksert klasse " + f + ", kan ikke scanne klasser", e);
                        }
                    }
                });
            }
            var index = indexer.complete();
            return index;
        } catch (IOException e) {
            throw new IllegalStateException("Fikk ikke lest path " + location + ", kan ikke scanne klasser", e);
        }
    }

    // fra pre-generert index, slipper runtime scanning for raskere startup
    private Index getPersistedJandexIndex(URI location) {
        var jandexIdxUrl = getJandexIndexUrl(location);

        try (var jandexStream = jandexIdxUrl.openStream()) {
            var reader = new IndexReader(jandexStream);
            return reader.read();
        } catch (IOException e) {
            throw new IllegalStateException("Fikk ikke lest jandex index, kan ikke scanne klasser", e);
        }
    }

    private URL getJandexIndexUrl(URI location) {
        var uriString = location.toString();
        var classLoaders = new LinkedHashSet<ClassLoader>();
        classLoaders.add(getClass().getClassLoader());
        classLoaders.add(Thread.currentThread().getContextClassLoader());

        return classLoaders
                .stream()
                .flatMap(cl -> {
                    try {
                        return Collections.list(cl.getResources("META-INF/" + jandexIndexFileName)).stream();
                    } catch (IOException e2) {
                        throw new IllegalArgumentException("Kan ikke lese jandex index fil", e2);
                    }
                })
                .filter(url -> {
                    try {
                        return String.valueOf(url.toURI()).startsWith(uriString) ||
                                String.valueOf(url.toURI().getSchemeSpecificPart()).startsWith(uriString);
                    } catch (URISyntaxException e1) {
                        throw new IllegalArgumentException("Kan ikke scanne URI", e1);
                    }
                })
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Fant ikke jandex index for location=" + location));
    }

    public List<Class<?>> getClassesWithAnnotation(Class<?> annotationClass) {

        var search = DotName.createSimple(annotationClass.getName());
        var annotations = getIndex().getAnnotations(search);

        List<Class<?>> jsonTypes = new ArrayList<>();
        for (var annotation : annotations) {
            if (Kind.CLASS.equals(annotation.target().kind())) {
                var className = annotation.target().asClass().name().toString();
                try {
                    jsonTypes.add(Class.forName(className));
                } catch (ClassNotFoundException e) {
                    LOG.error("Kan ikke finne klasse i Classpath, som funnet i Jandex index", e);
                }
            }
        }

        return jsonTypes;
    }

    public List<Class<?>> getSubClassesWithAnnotation(Class<?> klasse, Class<?> annotationClass) {
        var classesWithAnnotation = getClassesWithAnnotation(annotationClass);
        return classesWithAnnotation.stream().filter(c -> klasse.isAssignableFrom(c)).collect(Collectors.toList());
    }

    public List<Class<?>> getClasses(Predicate<ClassInfo> predicate, Predicate<Class<?>> classPredicate) {
        var knownClasses = getIndex().getKnownClasses();

        List<Class<?>> cls = new ArrayList<>();
        for (var ci : knownClasses) {
            var className = ci.name().toString();
            try {
                if (predicate.test(ci)) {
                    var c = Class.forName(className);
                    if (classPredicate.test(c)) {
                        cls.add(c);
                    }
                }
            } catch (ClassNotFoundException e) {
                LOG.error("Kan ikke finne klasse i Classpath, som funnet i Jandex index", e);
            }
        }
        return cls;
    }

    public static IndexClasses getIndexFor(final URI location) {
        return INDEXES.computeIfAbsent(location, uri -> new IndexClasses(uri));
    }

    public static IndexClasses getIndexFor(final URI location, final String jandexIdxFileName) {
        return INDEXES.computeIfAbsent(location, uri -> new IndexClasses(uri, jandexIdxFileName));
    }
}
