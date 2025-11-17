package no.nav.foreldrepenger.web.app.tjenester.formidling;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.GraderingAvslagÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;

final class UttakHjemmelUtleder {

    private static final Pattern PARAGRAF_I_KODENAVN_PATTERN = Pattern.compile("§[\\s-0-9]+");
    private static final Map<PeriodeResultatÅrsak, Set<String>> PERIODE_RESULTAT_ÅRSAK_TIL_HJEMMEL = Arrays.stream(PeriodeResultatÅrsak.values())
        .collect(Collectors.toMap(årsak -> årsak, UttakHjemmelUtleder::finnLovhjemler));
    private static final Map<GraderingAvslagÅrsak, Set<String>> GRADERING_ÅRSAK_TIL_HJEMMEL = Arrays.stream(GraderingAvslagÅrsak.values())
        .collect(Collectors.toMap(årsak -> årsak, UttakHjemmelUtleder::finnLovhjemler));

    private UttakHjemmelUtleder() {
    }

    static Set<String> finnLovhjemler(ForeldrepengerUttakPeriode periode) {
        var hjemler = new HashSet<>(PERIODE_RESULTAT_ÅRSAK_TIL_HJEMMEL.get(periode.getResultatÅrsak()));
        if (periode.getGraderingAvslagÅrsak() != null) {
            hjemler.addAll(GRADERING_ÅRSAK_TIL_HJEMMEL.get(periode.getGraderingAvslagÅrsak()));
        }
        return hjemler;
    }

    static Set<String> finnLovhjemler(Kodeverdi årsak) {
        var m = PARAGRAF_I_KODENAVN_PATTERN.matcher(årsak.getNavn());
        var matched = new HashSet<String>();
        while (m.find()) {
            matched.add(m.group().substring(1).trim());
        }
        return matched;
    }

}
