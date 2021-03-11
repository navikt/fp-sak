package no.nav.foreldrepenger.domene.uttak;


import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING;
import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER;
import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType.RE_SATS_REGULERING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;

class SkalKopiereUttakTjenesteTest {

    @Test
    public void endret_arbeid_skal_ikke_kopiere() {
        assertThat(skalKopiereStegResultat(Set.of(RE_ENDRET_INNTEKTSMELDING), true, true)).isFalse();
    }

    @Test
    public void endret_inntektsmelding_sammen_med_søknad_skal_ikke_kopiere() {
        assertThat(
            skalKopiereStegResultat(Set.of(RE_ENDRET_INNTEKTSMELDING, RE_ENDRING_FRA_BRUKER), false, true)).isFalse();
    }

    @Test
    public void endret_inntektsmelding_skal_kopiere() {
        assertThat(skalKopiereStegResultat(Set.of(RE_ENDRET_INNTEKTSMELDING), false, true)).isTrue();
    }

    @Test
    public void endret_inntektsmelding_og_g_reg_skal_kopiere() {
        assertThat(
            skalKopiereStegResultat(Set.of(RE_ENDRET_INNTEKTSMELDING, RE_SATS_REGULERING), false, true)).isTrue();
    }

    @Test
    public void endret_inntektsmelding_i_førstegangsbehandling_skal_ikke_kopiere() {
        assertThat(skalKopiereStegResultat(Set.of(RE_ENDRET_INNTEKTSMELDING), false, false)).isFalse();
    }

    @Test
    public void g_reg_skal_kopiere() {
        assertThat(skalKopiereStegResultat(Set.of(RE_SATS_REGULERING), false, true)).isTrue();
    }

    private boolean skalKopiereStegResultat(Set<BehandlingÅrsakType> årsaker,
                                            boolean arbeidEndret,
                                            boolean erRevurdering) {
        var repoProvider = new UttakRepositoryStubProvider();
        var relevanteArbeidsforholdTjeneste = mock(RelevanteArbeidsforholdTjeneste.class);
        when(relevanteArbeidsforholdTjeneste.arbeidsforholdRelevantForUttakErEndretSidenForrigeBehandling(
            any())).thenReturn(arbeidEndret);
        var tjeneste = new SkalKopiereUttakTjeneste(relevanteArbeidsforholdTjeneste);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        if (erRevurdering) {
            scenario.medOriginalBehandling(ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repoProvider), årsaker);
        }
        var behandling = scenario .lagre(repoProvider);
        var input = new UttakInput(BehandlingReferanse.fra(behandling), null, null).medBehandlingÅrsaker(årsaker);
        return tjeneste.skalKopiereStegResultat(input);
    }
}
