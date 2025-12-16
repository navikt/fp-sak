package no.nav.foreldrepenger.web.app.tjenester.kodeverk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.foreldrepenger.domene.modell.kodeverk.AndelKilde;
import no.nav.foreldrepenger.web.app.IndexClasses;

class KodeverdiKonsistensTest {

    // Mulig videre arbeid 1: Lag et grensesnitt "FrontendMedNavn" som implementeres av enums listet i HentKodeverdierTjeneste
    // Deretter kan man bruke Jandex til å finne alle implementasjoner i stedet for å liste eksplisitte klasser.

    // Mulig videre arbeid 2: Supplier av en map for databasekoder - så man slipper lokale "fraKode".

    @Test
    void sjekk_alle_kodeverk_kodeverdier() throws URISyntaxException {
        Set<Class<? extends Kodeverdi>> kodeverdiKlasser = new LinkedHashSet<>();
        // Det ligger for tiden implementasjoner av Kodeverdi i 3 ulike moduler, tar inn en fra hver modul for å finne alle kodeverdi-klasser
        List<Class<?>> kildeKlasser = List.of(FagsakYtelseType.class, RelatertYtelseTilstand.class, AndelKilde.class);
        for (var k : kildeKlasser) {
            var indexClasses = IndexClasses.getIndexFor(k.getProtectionDomain().getCodeSource().getLocation().toURI());
            @SuppressWarnings("unchecked")
            var classes = indexClasses.getClasses(
                    ci -> true,
                    c -> !c.isInterface() && !c.isAnonymousClass() && Kodeverdi.class.isAssignableFrom(c)).stream()
                .map(c -> (Class<? extends Kodeverdi>) c)
                .toList();
            kodeverdiKlasser.addAll(classes);
        }
        // Vi ønsker kun enum-implementasjoner av Kodeverdi (utenom den ene anonyme i Årsak)
        assertThat(kodeverdiKlasser).allMatch(Class::isEnum);
        // Sjekk for duplikate koder innen hver enum
        kodeverdiKlasser.forEach(k -> {
            var antallEnum = k.getEnumConstants().length;
            var antallUnikKode = Arrays.stream(k.getEnumConstants()).map(Kodeverdi::getKode).distinct().count();
            assertThat(antallEnum).withFailMessage("Duplikate koder i %s", k.getSimpleName()).isEqualTo(antallUnikKode);
        });
        assertDoesNotThrow(() -> kodeverdiKlasser.stream()
            .map(k -> Map.entry(k.getSimpleName(),
                Arrays.stream(k.getEnumConstants()).collect(Collectors.toMap(Kodeverdi::getKode, Function.identity()))))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

}
