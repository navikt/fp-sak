package no.nav.foreldrepenger.domene.registerinnhenting.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.domene.registerinnhenting.RegisterdataEndringshåndterer;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ExtendWith(MockitoExtension.class)
class RegisterdataOppdatererTaskTest {

    private RegisterdataOppdatererTask task; // objektet vi tester

    @Mock
    private BehandlingRepository mockBehandlingRepository;
    @Mock
    private BehandlingskontrollTjeneste mockBehandlingskontrollTjeneste;
    @Mock
    private RegisterdataEndringshåndterer mockRegisterdataEndringshåndterer;
    @Mock
    private BehandlendeEnhetTjeneste mockEnhetsTjeneste;
    private OrganisasjonsEnhet organisasjonsEnhet = new OrganisasjonsEnhet("4802", "Nav Bærum");

    @BeforeEach
    public void setup() {
        task = new RegisterdataOppdatererTask(mockBehandlingRepository, mock(BehandlingLåsRepository.class), mockBehandlingskontrollTjeneste,
            mockEnhetsTjeneste, mockRegisterdataEndringshåndterer);
    }


    @Test
    void skal_gjenoppta_behandling_bytteenhet() {
        final Long behandlingId = 10L;

        var enhet = new OrganisasjonsEnhet("2103", "Nav Vikafossen");
        var lås = mock(BehandlingLås.class);
        var kontekst = mock(BehandlingskontrollKontekst.class);
        var scenario = ScenarioMorSøkerEngangsstønad
                .forFødsel();
        var behandling = scenario.lagMocked();

        behandling.setBehandlendeEnhet(organisasjonsEnhet);
        when(mockBehandlingRepository.hentBehandling(any(Long.class))).thenReturn(behandling);
        when(mockBehandlingRepository.taSkriveLås(any(Long.class))).thenReturn(new BehandlingLås(behandling.getId()));
        lenient().when(mockBehandlingRepository.lagre(any(Behandling.class), any())).thenReturn(0L);
        when(mockBehandlingskontrollTjeneste.initBehandlingskontroll(any(Behandling.class), any(BehandlingLås.class))).thenReturn(kontekst);
        lenient().when(kontekst.getSkriveLås()).thenReturn(lås);
        when(mockEnhetsTjeneste.sjekkEnhetEtterEndring(any())).thenReturn(Optional.of(enhet));

        var prosessTaskData = ProsessTaskData.forProsessTask(RegisterdataOppdatererTask.class);
        prosessTaskData.setBehandling("123", 0L, behandlingId);

        task.doTask(prosessTaskData);

        verify(mockBehandlingskontrollTjeneste).initBehandlingskontroll(any(Behandling.class), any(BehandlingLås.class));
        verify(mockRegisterdataEndringshåndterer).utledDiffOgReposisjonerBehandlingVedEndringer(any(), eq(null), anyBoolean());
        verify(mockEnhetsTjeneste).oppdaterBehandlendeEnhet(any(), eq(enhet), any(), any());
    }

    @Test
    void skal_gjenoppta_behandling_medPayload() {
        final Long behandlingId = 10L;

        var enhet = new OrganisasjonsEnhet("2103", "Nav Vikafossen");
        var lås = mock(BehandlingLås.class);
        var kontekst = mock(BehandlingskontrollKontekst.class);
        var scenario = ScenarioMorSøkerEngangsstønad
            .forFødsel();
        var behandling = scenario.lagMocked();

        behandling.setBehandlendeEnhet(organisasjonsEnhet);
        when(mockBehandlingRepository.hentBehandling(any(Long.class))).thenReturn(behandling);
        when(mockBehandlingRepository.taSkriveLås(any(Long.class))).thenReturn(new BehandlingLås(behandling.getId()));
        lenient().when(mockBehandlingRepository.lagre(any(Behandling.class), any())).thenReturn(0L);
        when(mockBehandlingskontrollTjeneste.initBehandlingskontroll(any(Behandling.class), any(BehandlingLås.class))).thenReturn(kontekst);
        lenient().when(kontekst.getSkriveLås()).thenReturn(lås);
        when(mockEnhetsTjeneste.sjekkEnhetEtterEndring(any())).thenReturn(Optional.of(enhet));

        var snapshot= EndringsresultatSnapshot.opprett()
            .leggTil(EndringsresultatSnapshot.utenSnapshot(PersonInformasjonEntitet.class));
        var prosessTaskData = ProsessTaskData.forProsessTask(RegisterdataOppdatererTask.class);
        prosessTaskData.setBehandling("123", 0L, behandlingId);
        prosessTaskData.setPayload(StandardJsonConfig.toJson(snapshot));

        task.doTask(prosessTaskData);

        verify(mockBehandlingskontrollTjeneste).initBehandlingskontroll(any(Behandling.class), any(BehandlingLås.class));
        verify(mockRegisterdataEndringshåndterer).utledDiffOgReposisjonerBehandlingVedEndringer(any(), eq(snapshot), anyBoolean());
        verify(mockEnhetsTjeneste).oppdaterBehandlendeEnhet(any(), eq(enhet), any(), any());
    }
}
