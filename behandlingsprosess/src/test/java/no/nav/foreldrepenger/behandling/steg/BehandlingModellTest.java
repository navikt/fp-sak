package no.nav.foreldrepenger.behandling.steg;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingModellRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.vedtak.util.Tuple;

/**
 * Sjekk at alle konfigurasjoner fungerer og har definerte steg
 * implementasjoner.
 */
@CdiDbAwareTest
public class BehandlingModellTest {

    @ParameterizedTest
    @MethodSource("parameters")
    public void skal_sjekke_alle_definerte_behandlingsteg_konfigurasjoner_har_matchende_steg_implementasjoner(
            Tuple<BehandlingType, FagsakYtelseType> tuple) {
        var behandlingType = tuple.getElement1();
        var ytelseType = tuple.getElement2();
        BehandlingModellRepository behandlingModellRepository = new BehandlingModellRepository();
        BehandlingModell modell = behandlingModellRepository.getModell(behandlingType, ytelseType);
        for (BehandlingStegType stegType : modell.getAlleBehandlingStegTyper()) {
            BehandlingStegModell steg = modell.finnSteg(stegType);
            String description = String.format("Feilet for %s, %s, %s", ytelseType.getKode(), behandlingType.getKode(),
                    stegType.getKode());
            assertThat(steg).as(description).isNotNull();
            BehandlingSteg behandlingSteg = steg.getSteg();
            assertThat(behandlingSteg).as(description).isNotNull();

            @SuppressWarnings("rawtypes")
            Class targetClass = ((org.jboss.weld.interceptor.util.proxy.TargetInstanceProxy) behandlingSteg)
                    .weld_getTargetClass();
            assertThat(targetClass).as(description).hasAnnotation(ApplicationScoped.class);
        }
    }

    public static Collection<Tuple<BehandlingType, FagsakYtelseType>> parameters() {
        List<Tuple<BehandlingType, FagsakYtelseType>> params = new ArrayList<>();
        List<FagsakYtelseType> ytelseTyper = List.of(FagsakYtelseType.ENGANGSTØNAD, FagsakYtelseType.FORELDREPENGER,
                FagsakYtelseType.SVANGERSKAPSPENGER);
        List<BehandlingType> behandlingTyper = List.of(BehandlingType.FØRSTEGANGSSØKNAD, BehandlingType.REVURDERING,
                BehandlingType.KLAGE, BehandlingType.INNSYN, BehandlingType.ANKE);
        for (FagsakYtelseType a : ytelseTyper) {
            for (BehandlingType b : behandlingTyper) {
                params.add(new Tuple<>(b, a));
            }
        }
        return params;
    }
}
