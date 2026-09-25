package no.nav.foreldrepenger.web.app.tjenester.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.Personopplysning;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.HenleggBehandlingTjeneste;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsprosessTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsutredningTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.HenleggBehandlingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.sikkerhet.kontekst.RequestKontekst;

@CdiDbAwareTest
class BehandlingRestTjenesteTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private BehandlingsprosessTjeneste behandlingsprosessTjeneste;
    @Inject
    private BehandlingsutredningTjeneste behandlingsutredningTjeneste;
    @Inject
    private HenleggBehandlingTjeneste henleggBehandlingTjeneste;
    @Inject
    private BehandlingDtoTjeneste behandlingDtoTjeneste;

    private BehandlingRestTjeneste behandlingRestTjeneste() {
        // Direkte instans unngår REST-klassens ABAC-interceptor; underliggende tjenester er reelle CDI-beans.
        return new BehandlingRestTjeneste(behandlingsutredningTjeneste, null, null, behandlingsprosessTjeneste, null, henleggBehandlingTjeneste,
            behandlingDtoTjeneste, null, null, null);
    }

    @AfterEach
    void fjernKontekst() {
        KontekstHolder.fjernKontekst();
    }

    @Test
    void skal_sette_henleggende_saksbehandler_før_henleggelse() {
        var behandling = ScenarioMorSøkerEngangsstønad.forFødsel().lagre(repositoryProvider);
        behandling.setAnsvarligSaksbehandler("Z111111");
        repositoryProvider.getBehandlingRepository().lagre(behandling, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));
        KontekstHolder.setKontekst(RequestKontekst.forRequest("Z222222", "Z222222", IdentType.InternBruker, null, UUID.randomUUID(), Set.of()));

        var dto = new HenleggBehandlingDto();
        dto.setBehandlingUuid(behandling.getUuid());
        dto.setÅrsakKode(BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET.getKode());
        dto.setBehandlingVersjon(behandling.getVersjon());
        behandlingRestTjeneste().henleggBehandling(dto);

        var henlagt = repositoryProvider.getBehandlingRepository().hentBehandling(behandling.getId());
        assertThat(henlagt.getAnsvarligSaksbehandler()).isEqualTo("Z222222");
        assertThat(henlagt.erAvsluttet()).isTrue();
    }

    @Test
    void skal_hente_behandlinger_for_saksnummer() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        var personInformasjon = scenario.opprettBuilderForRegisteropplysninger()
            .leggTilPersonopplysninger(Personopplysning.builder()
                .aktørId(AktørId.dummy())
                .navn("Helga")
                .fødselsdato(LocalDate.now())
                .sivilstand(SivilstandType.SAMBOER)
                .brukerKjønn(NavBrukerKjønn.KVINNE))
            .build();
        scenario.medRegisterOpplysninger(personInformasjon);
        scenario.medDefaultBekreftetTerminbekreftelse();
        var behandling = scenario.lagre(repositoryProvider);

        var dto = behandlingRestTjeneste().hentBehandlinger(new SaksnummerDto(behandling.getSaksnummer().getVerdi()));

        assertThat(dto).hasSize(1);
    }
}
