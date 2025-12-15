package no.nav.foreldrepenger.web.app.konfig;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.folketrygdloven.kalkulus.annoteringer.Fritekst;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.validering.ValidKodeverk;
import no.nav.foreldrepenger.web.app.IndexClasses;

class RestApiOppdragInputValideringDtoTest extends RestApiTester {

    /**
     * IKKE ignorer eller fjern denne testen, den sørger for at inputvalidering er i orden for REST-grensesnittene
     * <p>
     * Kontakt Team Humle hvis du trenger hjelp til å endre koden din slik at den går igjennom her
     */
    @ParameterizedTest
    @MethodSource("finnAlleDtoTyper")
    void alle_felter_i_objekter_som_brukes_som_inputDTO_skal_enten_ha_valideringsannotering_eller_være_av_godkjent_type(Class<?> dto) throws Exception {
        Set<Class<?>> validerteKlasser = new HashSet<>(); // trengs for å unngå løkker og unngå å validere samme klasse flere multipliser dobbelt
        validerRekursivt(validerteKlasser, dto, null);
    }

    private static final Set<Class<? extends Object>> ALLOWED_ENUM_ANNOTATIONS = Set.of(JsonProperty.class,
        JsonValue.class, JsonIgnore.class, JsonAlias.class, Valid.class, Null.class, NotNull.class, ValidKodeverk.class, DefaultValue.class, FormParam.class, QueryParam.class,
        PathParam.class);

    @SuppressWarnings("rawtypes")
    private static final Set<Class> UNNTATT_FRA_VALIDERING = Set.of(PersonIdent.class, boolean.class, Boolean.class, UUID.class,
        LocalDate.class, LocalDateTime.class);

    @SuppressWarnings("rawtypes")
    private static final Map<Class, List<List<Class<? extends Annotation>>>> VALIDERINGSALTERNATIVER = new HashMap<>() {
        {
            put(String.class, asList(asList(Pattern.class, Size.class), List.of(Pattern.class), List.of(Digits.class),
                List.of(Fritekst.class, Size.class), List.of(Fritekst.class)));
            put(Long.class, asList(asList(Min.class, Max.class), List.of(Digits.class)));
            put(long.class, asList(asList(Min.class, Max.class), List.of(Digits.class)));
            put(Integer.class, singletonList(asList(Min.class, Max.class)));
            put(int.class, singletonList(asList(Min.class, Max.class)));
            put(BigDecimal.class, asList(asList(Min.class, Max.class), asList(DecimalMin.class, DecimalMax.class), List.of(Digits.class)));

            UNNTATT_FRA_VALIDERING.forEach(k -> put(k, List.of(List.of())));
        }
    };

    private static List<List<Class<? extends Annotation>>> getVurderingsalternativer(Field field) {
        var type = field.getType();
        if (field.getType().isEnum()) {
            return Collections.singletonList(Collections.singletonList(Valid.class));
        }
        if (Properties.class.isAssignableFrom(type) ) {
            return List.of(List.of(Size.class));
        }
        return VALIDERINGSALTERNATIVER.get(type);
    }

    private static boolean erKodeverk(Type... args) {
        return Kodeverdi.class.isAssignableFrom((Class<?>) args[0]);
    }

    private static Set<Class<?>> finnAlleDtoTyper() {
        Set<Class<?>> parametre = new TreeSet<>(Comparator.comparing(Class::getName));
        for (var method : finnAlleRestMetoder()) {
            parametre.addAll(List.of(method.getParameterTypes()));
            for (var type : method.getGenericParameterTypes()) {
                if (type instanceof ParameterizedType genericTypes) {
                    for (var gen : genericTypes.getActualTypeArguments()) {
                        parametre.add((Class<?>) gen);
                    }
                }
            }
        }
        parametre.addAll(RestApiTester.finnAlleJsonTypeNameClasses());
        Set<Class<?>> filtreteParametre = new TreeSet<>(Comparator.comparing(Class::getName));
        for (var klasse : parametre) {
            if (klasse.getName().startsWith("java") || klasse.isInterface()) {
                // ikke sjekk nedover i innebygde klasser, det skal brukes annoteringer på tidligere tidspunkt
                continue;
            }
            filtreteParametre.add(klasse);
        }
        return filtreteParametre;
    }

    private static void validerRekursivt(Set<Class<?>> besøkteKlasser,
                                         Class<?> klasse,
                                         Class<?> forrigeKlasse) throws URISyntaxException {
        if (erKodeverk(klasse)) {
            return;
        }
        if (besøkteKlasser.contains(klasse)) {
            return;
        }

        var protectionDomain = klasse.getProtectionDomain();
        var codeSource = protectionDomain.getCodeSource();
        if (codeSource == null) {
            // system klasse
            return;
        }

        besøkteKlasser.add(klasse);
        assertThat(klasse.getAnnotation(Entity.class))
            .withFailMessage("Klassen " + klasse + " er en entitet, kan ikke brukes som DTO. Brukes i " + forrigeKlasse)
            .isNull();
        assertThat(klasse.getAnnotation(MappedSuperclass.class))
            .withFailMessage("Klassen " + klasse + " er en entitet, kan ikke brukes som DTO. Brukes i " + forrigeKlasse)
            .isNull();

        var klasseLocation = codeSource.getLocation();
        for (var subklasse : IndexClasses.getIndexFor(klasseLocation.toURI())
            .getSubClassesWithAnnotation(klasse, JsonTypeName.class)) {
            validerRekursivt(besøkteKlasser, subklasse, forrigeKlasse);
        }
        for (var subklasse : finnAlleJsonSubTypeClasses(klasse)) {
            validerRekursivt(besøkteKlasser, subklasse, forrigeKlasse);
        }
        for (var field : getRelevantFields(klasse)) {
            if (field.getAnnotation(JsonIgnore.class) != null) {
                continue; // feltet blir hverken serialisert elle deserialisert, unntas fra sjekk
            }
            if (erKodeverk(field.getType())) {
                validerHarValidkodeverkAnnotering(field);
            } else if (field.getType().isEnum()) {
                validerEnum(field);
                continue; // enum er OK
            } else if (isCollectionOrMapNotProperties(field)) { // Containers håndteres av brukergenerics
                validerCollectionOrMap(field);
            } else if (getVurderingsalternativer(field) != null) {
                validerRiktigAnnotert(field); // har konfigurert opp spesifikk validering
            } else if (field.getType().getName().startsWith("java")) {
                throw new AssertionError(
                    "Feltet " + field + " har ikke påkrevde annoteringer. Trenger evt. utvidelse av denne testen for å akseptere denne typen.");
            } else {
                validerHarValidAnnotering(field);
                validerRekursivt(besøkteKlasser, field.getType(), forrigeKlasse);
            }
            if (field.getGenericType() instanceof ParameterizedType parameterizedType) {
                validerRekursivt(besøkteKlasser, field.getType(), forrigeKlasse);
                for (var klazz : genericTypes(parameterizedType)) {
                    validerRekursivt(besøkteKlasser, klazz, forrigeKlasse);
                }
            }
        }
    }

    private static void validerEnum(Field field) {
        if (!erKodeverk(field.getType())) {
            validerRiktigAnnotert(field);
        }
        var illegal = Stream.of(field.getAnnotations()).filter(a -> !ALLOWED_ENUM_ANNOTATIONS.contains(a.annotationType())).toList();
        if (!illegal.isEmpty()) {
            throw new AssertionError("Ugyldige annotasjoner funnet på [" + field + "]: " + illegal);
        }

    }

    private static boolean erKodeverk(Class<?> klasse) {
        return Kodeverdi.class.isAssignableFrom(klasse);
    }

    private static void validerHarValidAnnotering(Field field) {
        if (field.getAnnotation(Valid.class) == null) {
            throw new AssertionError("Feltet " + field + " må ha @Valid-annotering.");
        }
    }

    private static void validerHarValidkodeverkAnnotering(Field field) {
        if (field.getAnnotation(ValidKodeverk.class) == null) {
            throw new AssertionError("Feltet " + field + " er et kodeverk, og må ha @ValidKodeverk-annotering");
        }
    }

    private static Set<Class<?>> genericTypes(ParameterizedType parameterizedType) {
        return Arrays.stream(parameterizedType.getActualTypeArguments()).map(a -> (Class<?>) a).collect(Collectors.toSet());
    }

    private static void validerCollectionOrMap(Field field) {
        // Ikke Valid før Collection/Map - men inne i generics-delen
        if (field.getAnnotatedType().getAnnotation(Valid.class) != null) {
            throw new AssertionError("Flytt annotering @Valid inn i List/Set/Collection/Map for feltet " + field + ".");
        }
        // Har Validering inne i generics-delen
        if (field.getAnnotatedType() instanceof AnnotatedParameterizedType annotatedParameterizedType) {
            var annotert = annotatedParameterizedType.getAnnotatedActualTypeArguments();
            for (var ann : annotert) {
                var kreverAnnoteringer = utledKrav(ann.getType());
                validerRiktigAnnotert(ann, kreverAnnoteringer);
            }
            return;
        }
        throw new IllegalArgumentException("Feltet " + field + " har ikke påkrevde annoteringer.");
    }

    private static List<List<Class<? extends Annotation>>> utledKrav(Type type) {
        var kreverAnnoteringer = new ArrayList<List<Class<? extends Annotation>>>();
        if (erKodeverk(type)) {
            kreverAnnoteringer.add(List.of(ValidKodeverk.class));
        } else if (isCollectionOrMap(type)) {
            kreverAnnoteringer.add(List.of(Size.class));
        } else if (VALIDERINGSALTERNATIVER.keySet().contains(type)) {
            kreverAnnoteringer.addAll(VALIDERINGSALTERNATIVER.get(type));
        } else {
            kreverAnnoteringer.add(List.of(Valid.class));
        }
        return kreverAnnoteringer;
    }


    private static Set<Field> getRelevantFields(Class<?> klasse) {
        Set<Field> fields = new LinkedHashSet<>();
        while (!klasse.isPrimitive() && !klasse.getName().startsWith("java")) {
            fields.addAll(fjernStaticFields(List.of(klasse.getDeclaredFields())));
            klasse = klasse.getSuperclass();
        }
        return fields;
    }

    private static Collection<Field> fjernStaticFields(List<Field> fields) {
        return fields.stream().filter(f -> !Modifier.isStatic(f.getModifiers())).toList();
    }

    private static void validerRiktigAnnotert(Field field) {
        var alternativer = getVurderingsalternativer(field);
        for (var alternativ : alternativer) {
            if (alternativ.isEmpty() || alternativ.stream().allMatch(field::isAnnotationPresent)) {
                return;
            }
        }
        throw new IllegalArgumentException("Feltet " + field + " har ikke påkrevde annoteringer: " + alternativer);
    }

    private static void validerRiktigAnnotert(AnnotatedType ann, List<List<Class<? extends Annotation>>> alternativer) {
        for (var alternativ : alternativer) {
            if (alternativ.isEmpty() || alternativ.stream().allMatch(ann::isAnnotationPresent)) {
                return;
            }
        }
        throw new IllegalArgumentException("Type " + ann + " har ikke påkrevde annoteringer: " + alternativer);
    }

    private static boolean isCollectionOrMapNotProperties(Field field) {
        return Collection.class.isAssignableFrom(field.getType()) || Map.class.isAssignableFrom(field.getType()) && !Properties.class.isAssignableFrom(field.getType());
    }

    private static boolean isCollectionOrMap(Type type) {
        return Collection.class.isAssignableFrom(type.getClass()) || Map.class.isAssignableFrom(type.getClass());
    }


}
