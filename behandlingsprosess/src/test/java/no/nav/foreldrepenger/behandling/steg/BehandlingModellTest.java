package no.nav.foreldrepenger.behandling.steg;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.CDI;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingModellRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;

/** Sjekk at alle konfigurasjoner fungerer og har definerte steg implementasjoner. */
@RunWith(Parameterized.class)
public class BehandlingModellTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private FagsakYtelseType ytelseType;

    private BehandlingType behandlingType;

    public BehandlingModellTest(FagsakYtelseType ytelseType, BehandlingType behandlingType) {
        this.ytelseType = ytelseType;
        this.behandlingType = behandlingType;
    }

    @Test
    public void skal_sjekke_alle_definerte_behandlingsteg_konfigurasjoner_har_matchende_steg_implementasjoner() {
        @SuppressWarnings("resource")
        BehandlingModellRepository behandlingModellRepository = CDI.current().select(BehandlingModellRepository.class).get();
        BehandlingModell modell = behandlingModellRepository.getModell(behandlingType, ytelseType);
        for (BehandlingStegType stegType : modell.getAlleBehandlingStegTyper()) {
            BehandlingStegModell steg = modell.finnSteg(stegType);
            String description = String.format("Feilet for %s, %s, %s", ytelseType.getKode(), behandlingType.getKode(), stegType.getKode());
            assertThat(steg).as(description) .isNotNull();
            BehandlingSteg behandlingSteg = steg.getSteg();
            assertThat(behandlingSteg).as(description).isNotNull();

            @SuppressWarnings("rawtypes")
            Class targetClass = ((org.jboss.weld.interceptor.util.proxy.TargetInstanceProxy) behandlingSteg).weld_getTargetClass();
            assertThat(targetClass).as(description).hasAnnotation(ApplicationScoped.class);
        }
    }

    @org.junit.runners.Parameterized.Parameters(name = "{0}-{1}")
    public static Collection<Object[]> parameters() {
        List<Object[]> params = new ArrayList<>();
        List<FagsakYtelseType> ytelseTyper = List.of(FagsakYtelseType.ENGANGSTØNAD, FagsakYtelseType.FORELDREPENGER, FagsakYtelseType.SVANGERSKAPSPENGER);
        List<BehandlingType> behandlingTyper = List.of(BehandlingType.FØRSTEGANGSSØKNAD, BehandlingType.REVURDERING, BehandlingType.KLAGE,
            BehandlingType.INNSYN, BehandlingType.ANKE);
        for (FagsakYtelseType a : ytelseTyper) {
            for (BehandlingType b : behandlingTyper) {
                params.add(new Object[] { a, b });
            }
        }
        return params;
    }
}
