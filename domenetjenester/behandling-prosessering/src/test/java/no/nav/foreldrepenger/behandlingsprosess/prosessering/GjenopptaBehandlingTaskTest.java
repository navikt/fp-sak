package no.nav.foreldrepenger.behandlingsprosess.prosessering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.task.GjenopptaBehandlingTask;
import no.nav.foreldrepenger.domene.registerinnhenting.RegisterdataEndringshåndterer;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ExtendWith(MockitoExtension.class)
public class GjenopptaBehandlingTaskTest {

    private GjenopptaBehandlingTask task; // objektet vi tester

    @Mock
    private BehandlingRepository mockBehandlingRepository;
    @Mock
    private BehandlingskontrollTjeneste mockBehandlingskontrollTjeneste;
    @Mock
    private RegisterdataEndringshåndterer mockRegisterdataEndringshåndterer;
    @Mock
    private BehandlendeEnhetTjeneste mockEnhetsTjeneste;
    private OrganisasjonsEnhet organisasjonsEnhet = new OrganisasjonsEnhet("4802", "NAV Bærum");

    @BeforeEach
    public void setup() {
        task = new GjenopptaBehandlingTask(mockBehandlingRepository, mock(BehandlingLåsRepository.class), mockBehandlingskontrollTjeneste,
                mockRegisterdataEndringshåndterer,
                mockEnhetsTjeneste);
    }

    @Test
    public void skal_gjenoppta_behandling() {
        final Long behandlingId = 10L;

        var scenario = ScenarioMorSøkerEngangsstønad
                .forFødsel();
        var behandling = scenario.lagMocked();
        behandling.setBehandlendeEnhet(organisasjonsEnhet);
        when(mockBehandlingRepository.hentBehandling(any(Long.class))).thenReturn(behandling);
        lenient().when(mockEnhetsTjeneste.sjekkEnhetEtterEndring(any())).thenReturn(Optional.empty());

        var prosessTaskData = new ProsessTaskData(GjenopptaBehandlingTask.TASKTYPE);
        prosessTaskData.setBehandling(0L, behandlingId, AktørId.dummy().getId());

        task.doTask(prosessTaskData);

        verify(mockBehandlingskontrollTjeneste).initBehandlingskontroll(anyLong());
        verify(mockBehandlingskontrollTjeneste).prosesserBehandling(any());
    }

    @Test
    public void skal_gjenoppta_behandling_bytteenhet() {
        final Long behandlingId = 10L;

        var enhet = new OrganisasjonsEnhet("2103", "NAV Viken");
        var lås = mock(BehandlingLås.class);
        var kontekst = mock(BehandlingskontrollKontekst.class);
        var scenario = ScenarioMorSøkerEngangsstønad
                .forFødsel();
        var behandling = scenario.lagMocked();

        behandling.setBehandlendeEnhet(organisasjonsEnhet);
        when(mockBehandlingRepository.hentBehandling(any(Long.class))).thenReturn(behandling);
        lenient().when(mockBehandlingRepository.lagre(any(Behandling.class), any())).thenReturn(0L);
        when(mockBehandlingskontrollTjeneste.initBehandlingskontroll(anyLong())).thenReturn(kontekst);
        when(mockBehandlingskontrollTjeneste.erStegPassert(behandling, BehandlingStegType.INNHENT_REGISTEROPP)).thenReturn(true);
        lenient().when(kontekst.getSkriveLås()).thenReturn(lås);
        when(mockEnhetsTjeneste.sjekkEnhetEtterEndring(any())).thenReturn(Optional.of(enhet));
        when(mockRegisterdataEndringshåndterer.skalInnhenteRegisteropplysningerPåNytt(any())).thenReturn(true);

        var prosessTaskData = new ProsessTaskData(GjenopptaBehandlingTask.TASKTYPE);
        prosessTaskData.setBehandling(0L, behandlingId, "0");

        task.doTask(prosessTaskData);

        verify(mockBehandlingskontrollTjeneste).initBehandlingskontroll(anyLong());
        verify(mockBehandlingskontrollTjeneste).prosesserBehandling(any());
        var enhetArgumentCaptor = ArgumentCaptor.forClass(OrganisasjonsEnhet.class);
        verify(mockEnhetsTjeneste).oppdaterBehandlendeEnhet(any(), enhetArgumentCaptor.capture(), any(), any());
        assertThat(enhetArgumentCaptor.getValue()).isEqualTo(enhet);
    }
}
