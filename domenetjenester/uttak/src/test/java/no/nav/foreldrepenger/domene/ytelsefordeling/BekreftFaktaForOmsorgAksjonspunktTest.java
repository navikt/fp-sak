package no.nav.foreldrepenger.domene.ytelsefordeling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;

public class BekreftFaktaForOmsorgAksjonspunktTest {

    private final UttakRepositoryStubProvider repositoryProvider = new UttakRepositoryStubProvider();
    private final YtelsesFordelingRepository ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
    private final BekreftFaktaForOmsorgAksjonspunkt bekreftFaktaForOmsorgAksjonspunkt = new BekreftFaktaForOmsorgAksjonspunkt(
        ytelsesFordelingRepository);

    @Test
    public void skal_lagre_ned_bekreftet_aksjonspunkt_omsorg() {
        var behandling = opprettBehandling();
        var iDag = LocalDate.now();
        // simulerer svar fra GUI
        List<DatoIntervallEntitet> ikkeOmsorgPerioder = new ArrayList<>();
        var ikkeOmsorgPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(iDag.minusMonths(2),
            iDag.minusMonths(1));
        ikkeOmsorgPerioder.add(ikkeOmsorgPeriode);
        var dto = new BekreftFaktaForOmsorgVurderingAksjonspunktDto(null,
            false, ikkeOmsorgPerioder);
        bekreftFaktaForOmsorgAksjonspunkt.oppdater(behandling.getId(), dto);

        var perioderUtenOmsorgOpt = ytelsesFordelingRepository.hentAggregat(
            behandling.getId()).getPerioderUtenOmsorg();
        assertThat(perioderUtenOmsorgOpt).isPresent();
        var periodeUtenOmsorg = perioderUtenOmsorgOpt.get().getPerioder();
        assertThat(periodeUtenOmsorg).hasSize(1);
        assertThat(periodeUtenOmsorg.get(0).getPeriode()).isEqualTo(ikkeOmsorgPeriode);

        //må nullstille etter endret til har omsorg
        dto = new BekreftFaktaForOmsorgVurderingAksjonspunktDto(null, true, null);
        bekreftFaktaForOmsorgAksjonspunkt.oppdater(behandling.getId(), dto);
        perioderUtenOmsorgOpt = ytelsesFordelingRepository.hentAggregat(behandling.getId()).getPerioderUtenOmsorg();
        assertThat(perioderUtenOmsorgOpt).isPresent();
        periodeUtenOmsorg = perioderUtenOmsorgOpt.get().getPerioder();
        assertThat(periodeUtenOmsorg).isEmpty();
    }

    @Test
    public void skal_lagre_ned_bekreftet_aksjonspunkt_aleneomsorg() {
        var behandling = opprettBehandling();
        var dto = new BekreftFaktaForOmsorgVurderingAksjonspunktDto(false,
            null, null);
        var behandlingId = behandling.getId();
        bekreftFaktaForOmsorgAksjonspunkt.oppdater(behandlingId, dto);

        var perioderAleneOmsorgOptional = ytelsesFordelingRepository.hentAggregat(
            behandlingId).getPerioderAleneOmsorg();
        assertThat(perioderAleneOmsorgOptional).isPresent();
        var periodeAleneOmsorg = perioderAleneOmsorgOptional.get().getPerioder();
        assertThat(periodeAleneOmsorg).isEmpty();

        var perioderAnnenforelderHarRettOptional = ytelsesFordelingRepository.hentAggregat(
            behandlingId).getPerioderAnnenforelderHarRett();
        assertThat(perioderAnnenforelderHarRettOptional).isPresent();
        var periodeAnnenforelderHarRett = perioderAnnenforelderHarRettOptional.get()
            .getPerioder();
        assertThat(periodeAnnenforelderHarRett).hasSize(1);
        assertThat(periodeAnnenforelderHarRett.get(0).getPeriode()).isEqualTo(
            DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now()));

        //må legge inn etter endret til har aleneomsorg
        dto = new BekreftFaktaForOmsorgVurderingAksjonspunktDto(true, null, null);
        bekreftFaktaForOmsorgAksjonspunkt.oppdater(behandlingId, dto);
        perioderAleneOmsorgOptional = ytelsesFordelingRepository.hentAggregat(behandlingId).getPerioderAleneOmsorg();
        assertThat(perioderAleneOmsorgOptional).isPresent();
        periodeAleneOmsorg = perioderAleneOmsorgOptional.get().getPerioder();
        assertThat(periodeAleneOmsorg).hasSize(1);
        assertThat(periodeAleneOmsorg.get(0).getPeriode()).isEqualTo(
            DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now()));

        perioderAnnenforelderHarRettOptional = ytelsesFordelingRepository.hentAggregat(behandlingId)
            .getPerioderAnnenforelderHarRett();
        assertThat(perioderAnnenforelderHarRettOptional).isPresent();
        periodeAnnenforelderHarRett = perioderAnnenforelderHarRettOptional.get().getPerioder();
        assertThat(periodeAnnenforelderHarRett).isEmpty();
    }

    private Behandling opprettBehandling() {
        return ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
    }
}
