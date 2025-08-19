package no.nav.foreldrepenger.behandling.steg.uttak.fp;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.eøs.EøsUttakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.eøs.EøsUttaksperioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;

@CdiDbAwareTest
class EøsUttakAnnenpartTjenesteTest {

    @Inject
    private EøsUttakAnnenpartTjeneste tjeneste;

    @Inject
    private EøsUttakRepository eøsUttakRepository;

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Test
    void fjerner_uttak() {
        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        eøsUttakRepository.lagreEøsUttak(behandling.getId(), new EøsUttaksperioderEntitet.Builder().build());

        assertThat(eøsUttakRepository.hentGrunnlag(behandling.getId())).isPresent();
        tjeneste.fjernEøsUttak(BehandlingReferanse.fra(behandling));

        assertThat(eøsUttakRepository.hentGrunnlag(behandling.getId())).isEmpty();
    }

    @Test
    void utleder_aksjonspunkt_hvis_eøsrett_og_førstegangsbehandling() {
        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOverstyrtRettighet(eøsRett())
            .lagre(repositoryProvider);
        var ap = tjeneste.utledUttakIEøsForAnnenpartAP(BehandlingReferanse.fra(behandling));
        assertThat(ap).contains(AksjonspunktDefinisjon.AVKLAR_UTTAK_I_EØS_FOR_ANNENPART);
    }

    @Test
    void utleder_aksjonspunkt_hvis_eøsrett_og_revurdering_uten_eksisterende_eøs_uttak() {
        var revurdering = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOverstyrtRettighet(eøsRett())
            .medOriginalBehandling(ScenarioFarSøkerForeldrepenger.forFødsel().lagre(repositoryProvider), BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
            .lagre(repositoryProvider);
        var ap = tjeneste.utledUttakIEøsForAnnenpartAP(BehandlingReferanse.fra(revurdering));
        assertThat(ap).contains(AksjonspunktDefinisjon.AVKLAR_UTTAK_I_EØS_FOR_ANNENPART);
    }

    @Test
    void utleder_aksjonspunkt_hvis_endringssøknad_og_revurdering_med_eksisterende_eøs_uttak() {
        var originalBehandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        eøsUttakRepository.lagreEøsUttak(originalBehandling.getId(), new EøsUttaksperioderEntitet.Builder().build());
        var revurdering = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOverstyrtRettighet(eøsRett())
            .medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
            .lagre(repositoryProvider);
        eøsUttakRepository.lagreEøsUttak(revurdering.getId(), new EøsUttaksperioderEntitet.Builder().build());
        var ap = tjeneste.utledUttakIEøsForAnnenpartAP(BehandlingReferanse.fra(revurdering));
        assertThat(ap).contains(AksjonspunktDefinisjon.AVKLAR_UTTAK_I_EØS_FOR_ANNENPART);
    }

    @Test
    void utleder_ikke_aksjonspunkt_hvis_eøsrett_og_revurdering_med_eksisterende_eøs_uttak() {
        var revurdering = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOverstyrtRettighet(eøsRett())
            .medOriginalBehandling(ScenarioFarSøkerForeldrepenger.forFødsel().lagre(repositoryProvider), BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING)
            .lagre(repositoryProvider);
        eøsUttakRepository.lagreEøsUttak(revurdering.getId(), new EøsUttaksperioderEntitet.Builder().build());
        var ap = tjeneste.utledUttakIEøsForAnnenpartAP(BehandlingReferanse.fra(revurdering));
        assertThat(ap).isEmpty();
    }

    @Test
    void utleder_ikke_aksjonspunkt_hvis_enerett_og_førstegangsbehandling() {
        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOverstyrtRettighet(OppgittRettighetEntitet.bareSøkerRett())
            .lagre(repositoryProvider);
        var ap = tjeneste.utledUttakIEøsForAnnenpartAP(BehandlingReferanse.fra(behandling));
        assertThat(ap).isEmpty();
    }

    @Test
    void utleder_ikke_aksjonspunkt_hvis_begge_rett_og_førstegangsbehandling() {
        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOverstyrtRettighet(OppgittRettighetEntitet.beggeRett())
            .lagre(repositoryProvider);
        var ap = tjeneste.utledUttakIEøsForAnnenpartAP(BehandlingReferanse.fra(behandling));
        assertThat(ap).isEmpty();
    }

    @Test
    void utleder_ikke_aksjonspunkt_hvis_begge_rett_og_revurdering() {
        var revurdering = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOverstyrtRettighet(OppgittRettighetEntitet.beggeRett())
            .medOriginalBehandling(ScenarioFarSøkerForeldrepenger.forFødsel().lagre(repositoryProvider), BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
            .lagre(repositoryProvider);
        var ap = tjeneste.utledUttakIEøsForAnnenpartAP(BehandlingReferanse.fra(revurdering));
        assertThat(ap).isEmpty();
    }

    @Test
    void utleder_ikke_aksjonspunkt_hvis_enerett_og_revurdering() {
        var revurdering = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOverstyrtRettighet(OppgittRettighetEntitet.bareSøkerRett())
            .medOriginalBehandling(ScenarioFarSøkerForeldrepenger.forFødsel().lagre(repositoryProvider), BehandlingÅrsakType.RE_HENDELSE_FØDSEL)
            .lagre(repositoryProvider);
        var ap = tjeneste.utledUttakIEøsForAnnenpartAP(BehandlingReferanse.fra(revurdering));
        assertThat(ap).isEmpty();
    }

    private static OppgittRettighetEntitet eøsRett() {
        return new OppgittRettighetEntitet(false, false, false, true, true);
    }
}
