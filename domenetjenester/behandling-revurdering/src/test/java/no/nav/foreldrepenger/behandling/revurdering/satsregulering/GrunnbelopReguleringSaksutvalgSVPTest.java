package no.nav.foreldrepenger.behandling.revurdering.satsregulering;

import jakarta.persistence.EntityManager;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.SatsReguleringRepository;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static no.nav.foreldrepenger.behandling.revurdering.satsregulering.SatsReguleringUtil.opprettSVAT;
import static no.nav.foreldrepenger.behandling.revurdering.satsregulering.SatsReguleringUtil.opprettSVP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ExtendWith(JpaExtension.class)
class GrunnbelopReguleringSaksutvalgSVPTest {

    private BeregningsresultatRepository beregningsresultatRepository;
    @Mock
    private ProsessTaskTjeneste taskTjeneste;

    private GrunnbeløpFinnSakerTask tjeneste;


    @BeforeEach
    public void setUp(EntityManager entityManager) {
        beregningsresultatRepository = new BeregningsresultatRepository(entityManager);
        tjeneste = new GrunnbeløpFinnSakerTask(new SatsReguleringRepository(entityManager), beregningsresultatRepository, taskTjeneste);
    }

    @Test
    void skal_finne_en_sak_å_revurdere(EntityManager em) {
        var cutoff = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, LocalDate.now()).getPeriode().getFomDato();
        var gammelSats = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, cutoff.minusDays(1)).getVerdi();
        var kandidat = opprettSVAT(em, BehandlingStatus.AVSLUTTET, cutoff.plusDays(5), gammelSats, 6 * gammelSats);

        tjeneste.doTask(SatsReguleringUtil.lagFinnSakerTask("SVP", "G6"));

        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(taskTjeneste).lagre(captor.capture());
        var taskTypeExpected = TaskType.forProsessTask(GrunnbeløpReguleringTask.class);
        assertThat(captor.getValue().taskType()).isEqualTo(taskTypeExpected);

        assertThat(SatsReguleringUtil.finnTaskFor(captor, kandidat)).isPresent();
    }

    @Test
    void skal_ikke_finne_saker_til_revurdering(EntityManager em) {
        var cutoff = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, LocalDate.now()).getPeriode().getFomDato();
        var gammelSats = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, cutoff.minusDays(1)).getVerdi();
        opprettSVAT(em, BehandlingStatus.UTREDES, cutoff.plusDays(5), gammelSats, 6 * gammelSats);

        tjeneste.doTask(SatsReguleringUtil.lagFinnSakerTask("SVP", "G6"));

        verifyNoInteractions(taskTjeneste);
    }

    @Test
    void skal_finne_to_saker_å_revurdere(EntityManager em) {
        var nySats = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, LocalDate.now()).getVerdi();
        var cutoff = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, LocalDate.now()).getPeriode().getFomDato();
        var gammelSats = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, cutoff.minusDays(1)).getVerdi();
        var kan1 = opprettSVAT(em, BehandlingStatus.AVSLUTTET, cutoff.plusDays(5), gammelSats, 6 * gammelSats);
        var kan2 = opprettSVP(em, AktivitetStatus.ARBEIDSTAKER, BehandlingStatus.AVSLUTTET, cutoff.plusDays(5), gammelSats, 6 * gammelSats, 0); // Ikke uttak, bare utsettelse
        var kan3 = opprettSVAT(em, BehandlingStatus.AVSLUTTET, cutoff.minusDays(5), gammelSats, 6 * gammelSats); // FØR
        var kan4 = opprettSVAT(em, BehandlingStatus.AVSLUTTET, cutoff.plusDays(5), gammelSats, 6 * gammelSats);
        var kan5 = opprettSVAT(em, BehandlingStatus.AVSLUTTET, cutoff.plusDays(5), gammelSats, 4 * gammelSats); // Ikke avkortet
        var kan6 = opprettSVAT(em, BehandlingStatus.AVSLUTTET, cutoff.plusDays(5), nySats, 6 * nySats); // Ny sats

        tjeneste.doTask(SatsReguleringUtil.lagFinnSakerTask("SVP", "G6"));

        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(taskTjeneste, times(2)).lagre(captor.capture());
        var taskTypeExpected = TaskType.forProsessTask(GrunnbeløpReguleringTask.class);
        assertThat(captor.getValue().taskType()).isEqualTo(taskTypeExpected);

        assertThat(SatsReguleringUtil.finnTaskFor(captor, kan1)).isPresent();
        assertThat(SatsReguleringUtil.finnTaskFor(captor, kan2)).isEmpty();
        assertThat(SatsReguleringUtil.finnTaskFor(captor, kan3)).isEmpty();
        assertThat(SatsReguleringUtil.finnTaskFor(captor, kan4)).isPresent();
        assertThat(SatsReguleringUtil.finnTaskFor(captor, kan5)).isEmpty();
        assertThat(SatsReguleringUtil.finnTaskFor(captor, kan6)).isEmpty();
    }

}
