package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling;

import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.Type.MORS_AKTIVITET_GODKJENT;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.MorsStillingsprosent;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;

class YtelsesFordelingRepositoryTest extends EntityManagerAwareTest {

    private FagsakRepository fagsakRepository;
    private YtelsesFordelingRepository repository;
    private BehandlingRepository behandlingRepository;

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        fagsakRepository = new FagsakRepository(entityManager);
        repository = new YtelsesFordelingRepository(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
    }

    @Test
    void skal_lagre_grunnlaget() {
        var behandling = opprettBehandlingMedYtelseFordeling();
        //Endre periode for å teste overstyring
        var dokumentasjonVurdering = new DokumentasjonVurdering(MORS_AKTIVITET_GODKJENT, new MorsStillingsprosent(BigDecimal.TEN));
        var periode_12 = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.now().minusDays(10).plusDays(1), LocalDate.now())
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medDokumentasjonVurdering(dokumentasjonVurdering)
            .medMorsAktivitet(MorsAktivitet.ARBEID)
            .build();
        var periode_22 = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.now().minusDays(20).plusDays(1), LocalDate.now().minusDays(10))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();
        var yfBuilder = repository.opprettBuilder(behandling.getId())
            .medOverstyrtFordeling(new OppgittFordelingEntitet(List.of(periode_12, periode_22), true));

        repository.lagre(behandling.getId(), yfBuilder.build());

        var aggregat = repository.hentAggregat(behandling.getId());

        assertThat(aggregat).isNotNull();
        assertThat(aggregat.getOppgittDekningsgrad()).isNotNull();
        assertThat(aggregat.getOppgittRettighet()).isNotNull();
        assertThat(aggregat.getOppgittFordeling()).isNotNull();
        assertThat(aggregat.getOppgittFordeling().getPerioder()).isNotEmpty();
        assertThat(aggregat.getOppgittFordeling().getPerioder()).hasSize(3);
        assertThat(aggregat.getOverstyrtFordeling()).isNotNull();
        assertThat(aggregat.getOverstyrtFordeling().orElseThrow().getPerioder()).isNotEmpty();
        assertThat(aggregat.getOverstyrtFordeling().get().getPerioder()).hasSize(2);

        assertThat(aggregat.getOverstyrtFordeling().get().getPerioder().get(0).getDokumentasjonVurdering()).isEqualTo(dokumentasjonVurdering);

    }

    private Behandling opprettBehandlingMedYtelseFordeling() {
        var fagsak = opprettFagsak();
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        var periode_1 = lagOppgittPeriode(LocalDate.now().minusDays(10), LocalDate.now(),
            UttakPeriodeType.FORELDREPENGER);
        var periode_2 = lagOppgittPeriode(LocalDate.now().minusDays(20), LocalDate.now().minusDays(20),
            UttakPeriodeType.FORELDREPENGER);
        var periode_3 = lagOppgittPeriode(LocalDate.now().minusDays(20), LocalDate.now().minusDays(20),
            UttakPeriodeType.FEDREKVOTE);

        var yf = repository.opprettBuilder(behandling.getId())
            .medOppgittRettighet(OppgittRettighetEntitet.beggeRett())
            .medOppgittDekningsgrad(Dekningsgrad._80)
            .medOppgittFordeling(new OppgittFordelingEntitet(List.of(periode_1, periode_2, periode_3), true))
            .build();
        repository.lagre(behandling.getId(), yf);
        return behandling;
    }

    private Fagsak opprettFagsak() {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AktørId.dummy()));
        fagsakRepository.opprettNy(fagsak);
        return fagsak;
    }

    private OppgittPeriodeEntitet lagOppgittPeriode(LocalDate fom, LocalDate tom, UttakPeriodeType uttakPeriodeType){
        return OppgittPeriodeBuilder.ny()
            .medPeriode(fom, tom)
            .medPeriodeType(uttakPeriodeType)
            .build();
    }

}
