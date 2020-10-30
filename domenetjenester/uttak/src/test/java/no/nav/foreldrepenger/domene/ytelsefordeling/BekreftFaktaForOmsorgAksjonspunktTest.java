package no.nav.foreldrepenger.domene.ytelsefordeling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PeriodeAleneOmsorgEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PeriodeAnnenforelderHarRettEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PeriodeUtenOmsorgEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderAleneOmsorgEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderAnnenforelderHarRettEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderUtenOmsorgEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class BekreftFaktaForOmsorgAksjonspunktTest extends EntityManagerAwareTest {

    private BehandlingRepository behandlingRepository;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private FagsakRepository fagsakRepository;

    private BekreftFaktaForOmsorgAksjonspunkt bekreftFaktaForOmsorgAksjonspunkt;

    @BeforeEach
    public void oppsett() {
        var entityManager = getEntityManager();
        behandlingRepository = new BehandlingRepository(entityManager);
        ytelsesFordelingRepository = new YtelsesFordelingRepository(entityManager);
        fagsakRepository = new FagsakRepository(entityManager);
        bekreftFaktaForOmsorgAksjonspunkt = new BekreftFaktaForOmsorgAksjonspunkt(ytelsesFordelingRepository);
    }

    @Test
    public void skal_lagre_ned_bekreftet_aksjonspunkt_omsorg() {
        var behandling = opprettBehandling();
        LocalDate iDag = LocalDate.now();
        // simulerer svar fra GUI
        List<DatoIntervallEntitet> ikkeOmsorgPerioder = new ArrayList<>();
        DatoIntervallEntitet ikkeOmsorgPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(iDag.minusMonths(2), iDag.minusMonths(1));
        ikkeOmsorgPerioder.add(ikkeOmsorgPeriode);
        BekreftFaktaForOmsorgVurderingAksjonspunktDto dto = new BekreftFaktaForOmsorgVurderingAksjonspunktDto(null, false, ikkeOmsorgPerioder);
        bekreftFaktaForOmsorgAksjonspunkt.oppdater(behandling.getId(), dto);

        Optional<PerioderUtenOmsorgEntitet> perioderUtenOmsorgOpt = ytelsesFordelingRepository.hentAggregat(behandling.getId()).getPerioderUtenOmsorg();
        assertThat(perioderUtenOmsorgOpt).isPresent();
        List<PeriodeUtenOmsorgEntitet> periodeUtenOmsorg = perioderUtenOmsorgOpt.get().getPerioder();
        assertThat(periodeUtenOmsorg).hasSize(1);
        assertThat(periodeUtenOmsorg.get(0).getPeriode()).isEqualTo(ikkeOmsorgPeriode);

        //må nullstille etter endret til har omsorg
        dto = new BekreftFaktaForOmsorgVurderingAksjonspunktDto(null,true, null);
        bekreftFaktaForOmsorgAksjonspunkt.oppdater(behandling.getId(), dto);
        perioderUtenOmsorgOpt = ytelsesFordelingRepository.hentAggregat(behandling.getId()).getPerioderUtenOmsorg();
        assertThat(perioderUtenOmsorgOpt).isPresent();
        periodeUtenOmsorg = perioderUtenOmsorgOpt.get().getPerioder();
        assertThat(periodeUtenOmsorg).isEmpty();
    }

    @Test
    public void skal_lagre_ned_bekreftet_aksjonspunkt_aleneomsorg() {
        var behandling = opprettBehandling();
        BekreftFaktaForOmsorgVurderingAksjonspunktDto dto = new BekreftFaktaForOmsorgVurderingAksjonspunktDto(false, null, null);
        Long behandlingId = behandling.getId();
        bekreftFaktaForOmsorgAksjonspunkt.oppdater(behandlingId, dto);

        Optional<PerioderAleneOmsorgEntitet> perioderAleneOmsorgOptional = ytelsesFordelingRepository.hentAggregat(behandlingId).getPerioderAleneOmsorg();
        assertThat(perioderAleneOmsorgOptional).isPresent();
        List<PeriodeAleneOmsorgEntitet> periodeAleneOmsorg = perioderAleneOmsorgOptional.get().getPerioder();
        assertThat(periodeAleneOmsorg).isEmpty();

        Optional<PerioderAnnenforelderHarRettEntitet> perioderAnnenforelderHarRettOptional = ytelsesFordelingRepository.hentAggregat(behandlingId).getPerioderAnnenforelderHarRett();
        assertThat(perioderAnnenforelderHarRettOptional).isPresent();
        List<PeriodeAnnenforelderHarRettEntitet> periodeAnnenforelderHarRett = perioderAnnenforelderHarRettOptional.get().getPerioder();
        assertThat(periodeAnnenforelderHarRett).hasSize(1);
        assertThat(periodeAnnenforelderHarRett.get(0).getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now()));

        //må legge inn etter endret til har aleneomsorg
        dto = new BekreftFaktaForOmsorgVurderingAksjonspunktDto(true, null, null);
        bekreftFaktaForOmsorgAksjonspunkt.oppdater(behandlingId, dto);
        perioderAleneOmsorgOptional = ytelsesFordelingRepository.hentAggregat(behandlingId).getPerioderAleneOmsorg();
        assertThat(perioderAleneOmsorgOptional).isPresent();
        periodeAleneOmsorg = perioderAleneOmsorgOptional.get().getPerioder();
        assertThat(periodeAleneOmsorg).hasSize(1);
        assertThat(periodeAleneOmsorg.get(0).getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now()));

        perioderAnnenforelderHarRettOptional = ytelsesFordelingRepository.hentAggregat(behandlingId).getPerioderAnnenforelderHarRett();
        assertThat(perioderAnnenforelderHarRettOptional).isPresent();
        periodeAnnenforelderHarRett = perioderAnnenforelderHarRettOptional.get().getPerioder();
        assertThat(periodeAnnenforelderHarRett).isEmpty();
    }

    private Behandling opprettBehandling() {
        final Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AktørId.dummy()));
        fagsakRepository.opprettNy(fagsak);
        final Behandling.Builder builder = Behandling.forFørstegangssøknad(fagsak);
        final Behandling behandling = builder.build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        return behandling;
    }
}
