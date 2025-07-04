package no.nav.foreldrepenger.behandling.revurdering.satsregulering;

import static no.nav.foreldrepenger.behandling.revurdering.satsregulering.SatsReguleringUtil.opprettES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDate;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.SatsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.SatsReguleringRepository;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

@ExtendWith(MockitoExtension.class)
@ExtendWith(JpaExtension.class)
class EngangsstønadReguleringSaksutvalgTest {

    @Mock
    private ProsessTaskTjeneste taskTjeneste;
    private SatsRepository satsRepository;

    private EngangsstønadFinnSakerTask tjeneste;


    @BeforeEach
    void setUp(EntityManager entityManager) {
        satsRepository = new SatsRepository(entityManager);
        tjeneste = new EngangsstønadFinnSakerTask(new SatsReguleringRepository(entityManager), new SatsRepository(entityManager), taskTjeneste);
    }

    @Test
    void skal_finne_en_sak_å_revurdere(EntityManager em) {
        var cutoff = satsRepository.finnEksaktSats(BeregningSatsType.ENGANG, LocalDate.now()).getPeriode().getFomDato();
        var gammelSats = satsRepository.finnEksaktSats(BeregningSatsType.ENGANG, cutoff.minusDays(1)).getVerdi();
        var kandidat = opprettES(em, BehandlingStatus.AVSLUTTET, cutoff.plusDays(5), gammelSats);

        tjeneste.doTask(lagFinnSakerTask());

        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(taskTjeneste).lagre(captor.capture());
        var taskTypeExpected = TaskType.forProsessTask(EngangsstønadReguleringTask.class);
        assertThat(captor.getValue().taskType()).isEqualTo(taskTypeExpected);

        assertThat(SatsReguleringUtil.finnTaskFor(captor, kandidat)).isPresent();
    }

    @Test
    void skal_ikke_finne_saker_til_revurdering(EntityManager em) {
        var cutoff = satsRepository.finnEksaktSats(BeregningSatsType.ENGANG, LocalDate.now()).getPeriode().getFomDato();
        var gammelSats = satsRepository.finnEksaktSats(BeregningSatsType.ENGANG, cutoff.minusDays(1)).getVerdi();
        opprettES(em, BehandlingStatus.UTREDES, cutoff.plusDays(5), gammelSats);

        tjeneste.doTask(lagFinnSakerTask());

        verifyNoInteractions(taskTjeneste);
    }

    @Test
    void skal_finne_to_saker_å_revurdere(EntityManager em) {
        var nySats = satsRepository.finnEksaktSats(BeregningSatsType.ENGANG, LocalDate.now()).getVerdi();
        var cutoff = satsRepository.finnEksaktSats(BeregningSatsType.ENGANG, LocalDate.now()).getPeriode().getFomDato();
        var gammelSats = satsRepository.finnEksaktSats(BeregningSatsType.ENGANG, cutoff.minusDays(1)).getVerdi();
        var kandidat1 = opprettES(em, BehandlingStatus.AVSLUTTET, cutoff.plusDays(5), gammelSats);
        var kandidat2 = opprettES(em, BehandlingStatus.AVSLUTTET, cutoff.minusDays(5), gammelSats); // FØR
        var kandidat3 = opprettES(em, BehandlingStatus.AVSLUTTET, cutoff.plusDays(5), gammelSats);
        var kandidat4 = opprettES(em, BehandlingStatus.UTREDES, cutoff.plusDays(5), gammelSats); // Ikke vedtatt
        var kandidat5 = opprettES(em, BehandlingStatus.AVSLUTTET, cutoff.plusDays(5), nySats); // Ny sats

        tjeneste.doTask(lagFinnSakerTask());

        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(taskTjeneste, times(2)).lagre(captor.capture());
        var taskTypeExpected = TaskType.forProsessTask(EngangsstønadReguleringTask.class);
        assertThat(captor.getValue().taskType()).isEqualTo(taskTypeExpected);

        assertThat(SatsReguleringUtil.finnTaskFor(captor, kandidat1)).isPresent();
        assertThat(SatsReguleringUtil.finnTaskFor(captor, kandidat2)).isEmpty();
        assertThat(SatsReguleringUtil.finnTaskFor(captor, kandidat3)).isPresent();
        assertThat(SatsReguleringUtil.finnTaskFor(captor, kandidat4)).isEmpty();
        assertThat(SatsReguleringUtil.finnTaskFor(captor, kandidat5)).isEmpty();

    }

    private static ProsessTaskData lagFinnSakerTask() {
        var data = ProsessTaskData.forProsessTask(EngangsstønadFinnSakerTask.class);
        data.setProperty(GrunnbeløpFinnSakerTask.REVURDERING_KEY, "true");
        return data;
    }

}
