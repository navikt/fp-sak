package no.nav.foreldrepenger.behandling.steg;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingModellRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;

/**
 * Sjekk at alle konfigurasjoner fungerer og har definerte steg
 * implementasjoner.
 */
@CdiDbAwareTest
public class BehandlingModellTest {

    @ParameterizedTest
    @MethodSource("parameters")
    public void skal_sjekke_alle_definerte_behandlingsteg_konfigurasjoner_har_matchende_steg_implementasjoner(
        BehandlingTypeYtelseType tuple) {
        var behandlingType = tuple.behandlingType();
        var ytelseType = tuple.ytelseType();
        var behandlingModellRepository = new BehandlingModellRepository();
        var modell = behandlingModellRepository.getModell(behandlingType, ytelseType);
        for (var stegType : modell.getAlleBehandlingStegTyper()) {
            var steg = modell.finnSteg(stegType);
            var description = String.format("Feilet for %s, %s, %s", ytelseType.getKode(), behandlingType.getKode(),
                    stegType.getKode());
            assertThat(steg).as(description).isNotNull();
            var behandlingSteg = steg.getSteg();
            assertThat(behandlingSteg).as(description).isNotNull();

            @SuppressWarnings("rawtypes") var targetClass = ((org.jboss.weld.interceptor.util.proxy.TargetInstanceProxy) behandlingSteg)
                    .weld_getTargetClass();
            assertThat(targetClass).as(description).hasAnnotation(ApplicationScoped.class);
        }
    }

    private static record BehandlingTypeYtelseType(BehandlingType behandlingType, FagsakYtelseType ytelseType) {}

    public static Collection<BehandlingTypeYtelseType> parameters() {
        List<BehandlingTypeYtelseType> params = new ArrayList<>();
        var ytelseTyper = List.of(FagsakYtelseType.ENGANGSTØNAD, FagsakYtelseType.FORELDREPENGER,
                FagsakYtelseType.SVANGERSKAPSPENGER);
        var behandlingTyper = List.of(BehandlingType.FØRSTEGANGSSØKNAD, BehandlingType.REVURDERING,
                BehandlingType.KLAGE, BehandlingType.INNSYN, BehandlingType.ANKE);
        for (var a : ytelseTyper) {
            for (var b : behandlingTyper) {
                params.add(new BehandlingTypeYtelseType(b, a));
            }
        }
        return params;
    }
}
