package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class YtelsesFordelingRepositoryTest extends EntityManagerAwareTest {

    private FagsakRepository fagsakRepository;
    private YtelsesFordelingRepository fordelingRepository;
    private BehandlingRepository behandlingRepository;

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        fagsakRepository = new FagsakRepository(entityManager);
        fordelingRepository = new YtelsesFordelingRepository(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
    }

    @Test
    public void skal_lagre_grunnlaget() {
        var behandling = opprettBehandlingMedYtelseFordeling();
        //Endre periode for å teste overstyring
        final OppgittPeriodeEntitet periode_12 = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.now().minusDays(10).plusDays(1), LocalDate.now())
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();
        final OppgittPeriodeEntitet periode_22 = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.now().minusDays(20).plusDays(1), LocalDate.now().minusDays(10))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();

        fordelingRepository.lagreOverstyrtFordeling(behandling.getId(), new OppgittFordelingEntitet(List.of(periode_12, periode_22), true));

        final YtelseFordelingAggregat aggregat = fordelingRepository.hentAggregat(behandling.getId());

        assertThat(aggregat).isNotNull();
        assertThat(aggregat.getOppgittDekningsgrad()).isNotNull();
        assertThat(aggregat.getOppgittRettighet()).isNotNull();
        assertThat(aggregat.getOppgittFordeling()).isNotNull();
        assertThat(aggregat.getOppgittFordeling().getOppgittePerioder()).isNotEmpty();
        assertThat(aggregat.getOppgittFordeling().getOppgittePerioder()).hasSize(3);
        assertThat(aggregat.getOverstyrtFordeling()).isNotNull();
        assertThat(aggregat.getOverstyrtFordeling().get().getOppgittePerioder()).isNotEmpty();
        assertThat(aggregat.getOverstyrtFordeling().get().getOppgittePerioder()).hasSize(2);

    }

    private Behandling opprettBehandlingMedYtelseFordeling() {
        var fagsak = opprettFagsak();
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        OppgittRettighetEntitet oppgittRettighetEntitet = new OppgittRettighetEntitet(true, true, false);
        fordelingRepository.lagre(behandling.getId(), oppgittRettighetEntitet);
        fordelingRepository.lagre(behandling.getId(), OppgittDekningsgradEntitet.bruk80());


        OppgittPeriodeEntitet periode_1 = lagOppgittPeriode(LocalDate.now().minusDays(10), LocalDate.now(),
            UttakPeriodeType.FORELDREPENGER);
        OppgittPeriodeEntitet periode_2 = lagOppgittPeriode(LocalDate.now().minusDays(20), LocalDate.now().minusDays(20),
            UttakPeriodeType.FORELDREPENGER);
        OppgittPeriodeEntitet periode_3 = lagOppgittPeriode(LocalDate.now().minusDays(20), LocalDate.now().minusDays(20),
            UttakPeriodeType.ANNET);

        fordelingRepository.lagre(behandling.getId(), new OppgittFordelingEntitet(List.of(periode_1, periode_2, periode_3), true));
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
