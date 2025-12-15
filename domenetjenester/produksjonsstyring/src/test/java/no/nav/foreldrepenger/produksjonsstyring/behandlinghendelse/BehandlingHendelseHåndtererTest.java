package no.nav.foreldrepenger.produksjonsstyring.behandlinghendelse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.produksjonsstyring.fagsakstatus.OppdaterFagsakStatusTjeneste;
import no.nav.vedtak.hendelser.behandling.Behandlingstype;
import no.nav.vedtak.hendelser.behandling.Hendelse;
import no.nav.vedtak.hendelser.behandling.Kildesystem;
import no.nav.vedtak.hendelser.behandling.Ytelse;
import no.nav.vedtak.hendelser.behandling.v1.BehandlingHendelseV1;

@ExtendWith(MockitoExtension.class)
class BehandlingHendelseHåndtererTest {

    private BehandlingHendelseHåndterer håndterer;

    @Mock
    private FagsakTjeneste fagsakTjeneste;
    @Mock
    private OppdaterFagsakStatusTjeneste fagsakStatusTjeneste;

    @Test
    void skalOppdatereFagsakStatusPlussLageTaskVedTBKOpprettet() {

        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medFødselAdopsjonsdato(Collections.singletonList(LocalDate.now().plusDays(1)));

        var behandling = scenario.lagMocked();
        behandling.avsluttBehandling();
        var fagsak = behandling.getFagsak();

        when(fagsakTjeneste.finnFagsakGittSaksnummer(any(), anyBoolean())).thenReturn(Optional.of(fagsak));

        håndterer = new BehandlingHendelseHåndterer("topic", fagsakTjeneste, fagsakStatusTjeneste);

        var hendelse = new BehandlingHendelseV1.Builder()
            .medHendelseUuid(UUID.randomUUID())
            .medBehandlingUuid(UUID.randomUUID())
            .medKildesystem(Kildesystem.FPTILBAKE)
            .medHendelse(Hendelse.OPPRETTET)
            .medTidspunkt(LocalDateTime.now())
            .medAktørId(fagsak.getAktørId().getId())
            .medSaksnummer(fagsak.getSaksnummer().getVerdi())
            .medYtelse(Ytelse.ENGANGSTØNAD)
            .medBehandlingstype(Behandlingstype.TILBAKEBETALING)
            .build();

        håndterer.handleRecord("key", StandardJsonConfig.toJson(hendelse));

        verify(fagsakStatusTjeneste).oppdaterFagsakNårBehandlingOpprettet(fagsak, null, BehandlingStatus.UTREDES);
    }

    @Test
    void skalOppdatereFagsakStatusPlussLageTaskVedTBKAvsluttet()  {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medFødselAdopsjonsdato(Collections.singletonList(LocalDate.now().plusDays(1)));

        var behandling = scenario.lagMocked();
        var fagsak = behandling.getFagsak();

        when(fagsakTjeneste.finnFagsakGittSaksnummer(any(), anyBoolean())).thenReturn(Optional.of(fagsak));

        håndterer = new BehandlingHendelseHåndterer("topic", fagsakTjeneste,  fagsakStatusTjeneste);

        var hendelse = new BehandlingHendelseV1.Builder()
            .medHendelseUuid(UUID.randomUUID())
            .medBehandlingUuid(UUID.randomUUID())
            .medKildesystem(Kildesystem.FPTILBAKE)
            .medHendelse(Hendelse.AVSLUTTET)
            .medTidspunkt(LocalDateTime.now())
            .medAktørId(fagsak.getAktørId().getId())
            .medSaksnummer(fagsak.getSaksnummer().getVerdi())
            .medYtelse(Ytelse.ENGANGSTØNAD)
            .medBehandlingstype(Behandlingstype.TILBAKEBETALING_REVURDERING)
            .build();

        håndterer.handleRecord("key", StandardJsonConfig.toJson(hendelse));

        verify(fagsakStatusTjeneste).lagBehandlingAvsluttetTask(fagsak, null);
    }
}
