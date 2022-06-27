package no.nav.foreldrepenger.web.app.konfig;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

import javax.json.JsonValue;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import javax.validation.Valid;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.ws.rs.QueryParam;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import io.swagger.v3.oas.annotations.media.Schema;
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
        JsonValue.class, JsonIgnore.class, Valid.class, Null.class, NotNull.class, ValidKodeverk.class, QueryParam.class, Schema.class);

    @SuppressWarnings("rawtypes")
    private static final Map<Class, List<List<Class<? extends Annotation>>>> UNNTATT_FRA_VALIDERING = new HashMap<>() {
        {

            put(PersonIdent.class, singletonList(emptyList()));

            put(boolean.class, singletonList(emptyList()));
            put(Boolean.class, singletonList(emptyList()));

            // LocalDate og LocalDateTime har egne deserializers
            put(LocalDate.class, singletonList(emptyList()));
            put(LocalDateTime.class, singletonList(emptyList()));

            // Enforces av UUID selv
            put(UUID.class, singletonList(emptyList()));
        }
    };

    @SuppressWarnings("rawtypes")
    private static final Map<Class, List<List<Class<? extends Annotation>>>> VALIDERINGSALTERNATIVER = new HashMap<>() {
        {
            put(String.class, asList(asList(Pattern.class, Size.class), asList(Pattern.class), singletonList(Digits.class)));
            put(Long.class, asList(asList(Min.class, Max.class), asList(Digits.class)));
            put(long.class, asList(asList(Min.class, Max.class), asList(Digits.class)));
            put(Integer.class, singletonList(asList(Min.class, Max.class)));
            put(int.class, singletonList(asList(Min.class, Max.class)));
            put(BigDecimal.class, asList(asList(Min.class, Max.class, Digits.class),
                asList(DecimalMin.class, DecimalMax.class, Digits.class)));

            putAll(UNNTATT_FRA_VALIDERING);
        }
    };

    private static List<List<Class<? extends Annotation>>> getVurderingsalternativer(Field field) {
        var type = field.getType();
        if (field.getType().isEnum()) {
            return Collections.singletonList(Collections.singletonList(Valid.class));
        }
        if (Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type)) {
            if (brukerGenerics(field)) {
                var args = ((ParameterizedType) field.getGenericType()).getActualTypeArguments();
                if (Arrays.stream(args).allMatch(UNNTATT_FRA_VALIDERING::containsKey)) {
                    return Collections.singletonList(List.of(Size.class));
                }
                if (args.length == 1 && erKodeverk(args)) {
                    return Collections.singletonList(List.of(Valid.class, Size.class));
                }

            }
            return singletonList(List.of(Valid.class, Size.class));

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
                if (type instanceof ParameterizedType) {
                    var genericTypes = (ParameterizedType) type;
                    for (var gen : genericTypes.getActualTypeArguments()) {
                        parametre.add((Class<?>) gen);
                    }
                }
            }
        }
        Set<Class<?>> filtreteParametre = new TreeSet<>(Comparator.comparing(Class::getName));
        for (var klasse : parametre) {
            if (klasse.getName().startsWith("java")) {
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
        if (klasse.getAnnotation(Entity.class) != null || klasse.getAnnotation(MappedSuperclass.class) != null) {
            throw new AssertionError(
                "Klassen " + klasse + " er en entitet, kan ikke brukes som DTO. Brukes i " + forrigeKlasse);
        }

        var klasseLocation = codeSource.getLocation();
        for (var subklasse : IndexClasses.getIndexFor(klasseLocation.toURI())
            .getSubClassesWithAnnotation(klasse, JsonTypeName.class)) {
            validerRekursivt(besøkteKlasser, subklasse, forrigeKlasse);
        }
        for (var field : getRelevantFields(klasse)) {
            if (field.getAnnotation(JsonIgnore.class) != null) {
                continue; // feltet blir hverken serialisert elle deserialisert, unntas fra sjekk
            }
            if (field.getType().isEnum()) {
                validerEnum(field);
                continue; // enum er OK
            }
            if (erKodeverk(field.getType())) {
                validerHarValidkodeverkAnnotering(field);
            } else if (getVurderingsalternativer(field) != null) {
                validerRiktigAnnotert(field); // har konfigurert opp spesifikk validering
            } else if (field.getType().getName().startsWith("java")) {
                throw new AssertionError(
                    "Feltet " + field + " har ikke påkrevde annoteringer. Trenger evt. utvidelse av denne testen for å akseptere denne typen.");
            } else {
                validerHarValidAnnotering(field);
                validerRekursivt(besøkteKlasser, field.getType(), forrigeKlasse);
            }
            if (brukerGenerics(field)) {
                validerRekursivt(besøkteKlasser, field.getType(), forrigeKlasse);
                for (var klazz : genericTypes(field)) {
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

    private static Set<Class<?>> genericTypes(Field field) {
        Set<Class<?>> klasser = new HashSet<>();
        var type = (ParameterizedType) field.getGenericType();
        for (var t : type.getActualTypeArguments()) {
            klasser.add((Class<?>) t);
        }
        return klasser;
    }

    private static boolean brukerGenerics(Field field) {
        return field.getGenericType() instanceof ParameterizedType;
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
        return fields.stream().filter(f -> !Modifier.isStatic(f.getModifiers())).collect(Collectors.toList());
    }

    private static void validerRiktigAnnotert(Field field) {
        var alternativer = getVurderingsalternativer(field);
        for (var alternativ : alternativer) {
            var harAlleAnnoteringerForAlternativet = true;
            for (var annotering : alternativ) {
                if (field.getAnnotation(annotering) == null) {
                    harAlleAnnoteringerForAlternativet = false;
                }
            }
            if (harAlleAnnoteringerForAlternativet) {
                validerRiktigAnnotertForCollectionsAndMaps(field);
                return;
            }
        }
        throw new IllegalArgumentException("Feltet " + field + " har ikke påkrevde annoteringer: " + alternativer);
    }

    private static void validerRiktigAnnotertForCollectionsAndMaps(Field field) {
        if (!Properties.class.isAssignableFrom(field.getType()) && (Collection.class.isAssignableFrom(
            field.getType()) || Map.class.isAssignableFrom(field.getType()))) {
            var annType = (AnnotatedParameterizedType) field.getAnnotatedType();
            var annotatedTypes = annType.getAnnotatedActualTypeArguments();
            for (var at : List.of(annotatedTypes)) {
                if (erKodeverk(at.getType())) {
                    if (!at.isAnnotationPresent(ValidKodeverk.class)) {
                        throw new IllegalArgumentException(
                            "Feltet " + field + " har ikke påkrevd annotering for kodeverk: @" + ValidKodeverk.class.getSimpleName());
                    }
                }
            }
        }
    }

}
