package no.nav.foreldrepenger.behandling.revurdering.satsregulering;

import static no.nav.foreldrepenger.behandling.revurdering.satsregulering.SatsReguleringUtil.opprettES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.revurdering.ytelse.es.RevurderingTjenesteImpl;
import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.SatsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ExtendWith(MockitoExtension.class)
@ExtendWith(JpaExtension.class)
class EngangsstønadReguleringTaskTest {

    @Mock
    private PersoninfoAdapter personinfoAdapter;
    @Mock
    private RevurderingTjenesteImpl revurderingTjeneste;
    @Mock
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    @Mock
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;

    private LegacyESBeregningRepository legacyESBeregningRepository;
    private SatsRepository satsRepository;

    private EngangsstønadReguleringTask task;

    @BeforeEach
    public void setUp(EntityManager entityManager) {
        var repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        legacyESBeregningRepository = new LegacyESBeregningRepository(entityManager);
        satsRepository = new SatsRepository(entityManager);
        FamilieHendelseTjeneste familieHendelseTjeneste = new FamilieHendelseTjeneste(null, repositoryProvider.getFamilieHendelseRepository());
        task = new EngangsstønadReguleringTask(repositoryProvider, familieHendelseTjeneste, personinfoAdapter, behandlendeEnhetTjeneste,
            legacyESBeregningRepository, behandlingProsesseringTjeneste, revurderingTjeneste);
        lenient().when(behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(any(Fagsak.class)))
                .thenReturn(new OrganisasjonsEnhet("1234", "Testlokasjon"));
    }

    @Test
    void skal_ikke_opprette_revurderingsbehandling_når_riktig_sats(EntityManager em) {
        var cutoff = satsRepository.finnEksaktSats(BeregningSatsType.ENGANG, LocalDate.now()).getPeriode().getFomDato();
        var gammelSats = satsRepository.finnEksaktSats(BeregningSatsType.ENGANG, cutoff.minusDays(1)).getVerdi();
        var behandling = opprettES(em, BehandlingStatus.AVSLUTTET, cutoff.minusDays(5), gammelSats);

        var prosessTaskData = ProsessTaskData.forProsessTask(EngangsstønadReguleringTask.class);
        prosessTaskData.setBehandling(behandling.getSaksnummer().getVerdi(), behandling.getFagsakId(), behandling.getId());

        task.doTask(prosessTaskData);

        verifyNoInteractions(revurderingTjeneste);
    }

    @Test
    void skal_opprette_revurderingsbehandling_med_når_avvik_sats(EntityManager em) {

        var cutoff = satsRepository.finnEksaktSats(BeregningSatsType.ENGANG, LocalDate.now()).getPeriode().getFomDato();
        var gammelSats = satsRepository.finnEksaktSats(BeregningSatsType.ENGANG, cutoff.minusDays(1)).getVerdi();
        var fødselsdato = cutoff.plusDays(5);
        var behandling = opprettES(em, BehandlingStatus.AVSLUTTET, fødselsdato, gammelSats);

        lenient().when(personinfoAdapter.innhentAlleFødteForBehandlingIntervaller(any(), any(), any())).thenReturn(List.of(byggBaby(fødselsdato)));

        var prosessTaskData = ProsessTaskData.forProsessTask(EngangsstønadReguleringTask.class);
        prosessTaskData.setBehandling(behandling.getSaksnummer().getVerdi(), behandling.getFagsakId(), behandling.getId());

        task.doTask(prosessTaskData);

        var captor = ArgumentCaptor.forClass(BehandlingÅrsakType.class);
        verify(revurderingTjeneste).opprettAutomatiskRevurdering(any(), captor.capture(), any());
        assertThat(captor.getValue()).isEqualTo(BehandlingÅrsakType.RE_SATS_REGULERING);
    }

    private FødtBarnInfo byggBaby(LocalDate fødselsdato) {
        return new FødtBarnInfo.Builder()
            .medFødselsdato(fødselsdato)
            .medIdent(PersonIdent.fra("12345678901"))
            .build();
    }


}
