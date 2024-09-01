package no.nav.foreldrepenger.domene.uttak;


import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING;
import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER;
import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType.RE_HENDELSE_DØD_BARN;
import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType.RE_SATS_REGULERING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.domene.uttak.input.Barn;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.OriginalBehandling;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;

class SkalKopiereUttakTjenesteTest {

    private final UttakRepositoryStubProvider repositoryProvider = new UttakRepositoryStubProvider();

    @Test
    void endret_arbeid_skal_ikke_kopiere() {
        assertThat(skalKopiereStegResultat(Set.of(RE_ENDRET_INNTEKTSMELDING), true, true)).isFalse();
    }

    @Test
    void endret_inntektsmelding_sammen_med_søknad_skal_ikke_kopiere() {
        assertThat(
            skalKopiereStegResultat(Set.of(RE_ENDRET_INNTEKTSMELDING, RE_ENDRING_FRA_BRUKER), false, true)).isFalse();
    }

    @Test
    void endret_inntektsmelding_skal_kopiere() {
        assertThat(skalKopiereStegResultat(Set.of(RE_ENDRET_INNTEKTSMELDING), false, true)).isTrue();
    }

    @Test
    void endret_inntektsmelding_men_årsak_om_død_skal_ikke_kopiere() {
        assertThat(skalKopiereStegResultat(Set.of(RE_HENDELSE_DØD_BARN), false, true)).isFalse();
    }

    @Test
    void endret_inntektsmelding_men_med_dødsfall_skal_ikke_kopiere() {
        assertThat(skalKopiereStegResultat(Set.of(RE_ENDRET_INNTEKTSMELDING), false, true, true)).isFalse();
    }

    @Test
    void endret_inntektsmelding_og_g_reg_skal_kopiere() {
        assertThat(
            skalKopiereStegResultat(Set.of(RE_ENDRET_INNTEKTSMELDING, RE_SATS_REGULERING), false, true)).isTrue();
    }

    @Test
    void endret_inntektsmelding_i_førstegangsbehandling_skal_ikke_kopiere() {
        assertThat(skalKopiereStegResultat(Set.of(RE_ENDRET_INNTEKTSMELDING), false, false)).isFalse();
    }

    @Test
    void g_reg_skal_kopiere() {
        assertThat(skalKopiereStegResultat(Set.of(RE_SATS_REGULERING), false, true)).isTrue();
    }

    @Test
    void saksbehandler_har_ikke_avklart_startdato_skal_kopiere() {
        var uttakInput = lagInput(Set.of(RE_ENDRET_INNTEKTSMELDING), true, false);
        var tjeneste = opprettTjeneste(false);

        //null = ikke avklart i denne behandlingen
        settFørsteUttaksdato(null, uttakInput.getBehandlingReferanse());
        assertThat(tjeneste.skalKopiereStegResultat(uttakInput)).isTrue();
    }

    @Test
    void saksbehandler_har_avklart_startdato_skal_ikke_kopiere() {
        var uttakInput = lagInput(Set.of(RE_ENDRET_INNTEKTSMELDING), true, false);
        var tjeneste = opprettTjeneste(false);

        settFørsteUttaksdato(LocalDate.now(), uttakInput.getBehandlingReferanse());
        assertThat(tjeneste.skalKopiereStegResultat(uttakInput)).isFalse();
    }

    @Test
    void endret_inntektsmelding_skal_ikke_kopiere_hvis_fødsel() {
        var originalBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medOriginalBehandling(originalBehandling, Set.of(RE_ENDRET_INNTEKTSMELDING));
        var behandling = scenario.lagre(repositoryProvider);
        var termin = LocalDate.of(2021, 12, 10);
        var søknadHendelse = FamilieHendelse.forFødsel(termin, null, List.of(new Barn()), 1);
        var fødselsdato = termin.plusWeeks(1);
        var bekreftetHendelse = FamilieHendelse.forFødsel(termin, fødselsdato, List.of(new Barn()), 1);
        var ytelsespesifiktGrunnlag = new ForeldrepengerGrunnlag()
            .medOriginalBehandling(new OriginalBehandling(originalBehandling.getId(), new FamilieHendelser().medSøknadHendelse(søknadHendelse)))
            .medFamilieHendelser(new FamilieHendelser()
                .medSøknadHendelse(søknadHendelse)
                .medBekreftetHendelse(bekreftetHendelse)
            );
        var uttakInput = new UttakInput(BehandlingReferanse.fra(behandling), null, null, ytelsespesifiktGrunnlag)
            .medBehandlingÅrsaker(Set.of(RE_ENDRET_INNTEKTSMELDING));

        var tjeneste = opprettTjeneste(false);

        assertThat(tjeneste.skalKopiereStegResultat(uttakInput)).isFalse();
    }

    private void settFørsteUttaksdato(LocalDate førsteUttaksdato, BehandlingReferanse behandlingReferanse) {
        var ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        var behandlingId = behandlingReferanse.behandlingId();
        var yfa = ytelsesFordelingRepository.opprettBuilder(behandlingId);
        var avklarteDatoer = new AvklarteUttakDatoerEntitet.Builder()
            .medFørsteUttaksdato(førsteUttaksdato)
            .medJustertEndringsdato(LocalDate.now())
            .build();
        ytelsesFordelingRepository.lagre(behandlingId, yfa.medAvklarteDatoer(avklarteDatoer).build());
    }

    private boolean skalKopiereStegResultat(Set<BehandlingÅrsakType> årsaker,
                                            boolean arbeidEndret,
                                            boolean erRevurdering) {
        return skalKopiereStegResultat(årsaker, arbeidEndret, erRevurdering, false);
    }

    private boolean skalKopiereStegResultat(Set<BehandlingÅrsakType> årsaker,
                                            boolean arbeidEndret,
                                            boolean erRevurdering,
                                            boolean dødsfall) {
        var input = lagInput(årsaker, erRevurdering, dødsfall);
        var tjeneste = opprettTjeneste(arbeidEndret);
        return tjeneste.skalKopiereStegResultat(input);
    }

    private SkalKopiereUttakTjeneste opprettTjeneste(boolean arbeidEndret) {
        var relevanteArbeidsforholdTjeneste = mock(RelevanteArbeidsforholdTjeneste.class);
        when(relevanteArbeidsforholdTjeneste.arbeidsforholdRelevantForUttakErEndretSidenForrigeBehandling(
            any())).thenReturn(arbeidEndret);
        return new SkalKopiereUttakTjeneste(relevanteArbeidsforholdTjeneste,
            new YtelseFordelingTjeneste(repositoryProvider.getYtelsesFordelingRepository()));
    }

    private UttakInput lagInput(Set<BehandlingÅrsakType> årsaker,
                                boolean erRevurdering,
                                boolean dødsfall) {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        if (erRevurdering) {
            scenario.medOriginalBehandling(ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider),
                årsaker);
        }
        var behandling = scenario.lagre(repositoryProvider);
        return new UttakInput(BehandlingReferanse.fra(behandling), null, null, new ForeldrepengerGrunnlag().medDødsfall(dødsfall))
            .medBehandlingÅrsaker(årsaker);
    }
}
