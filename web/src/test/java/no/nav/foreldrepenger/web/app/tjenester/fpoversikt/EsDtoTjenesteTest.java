package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.typer.JournalpostId;

@CdiDbAwareTest
class EsDtoTjenesteTest {

    @Inject
    private EsDtoTjeneste tjeneste;
    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Test
    void henter_sak_med_engangsstønad() {
        var vedtakstidspunkt = LocalDateTime.now();

        var termindato = LocalDate.of(2023, 10, 19);
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder().medTermindato(termindato));
        scenario.medBehandlingVedtak().medVedtakResultatType(VedtakResultatType.INNVILGET).medVedtakstidspunkt(vedtakstidspunkt);
        var behandling = scenario.lagre(repositoryProvider);
        avsluttBehandling(behandling);
        var mottattDokument = new MottattDokument.Builder().medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .medDokumentType(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL)
            .medMottattTidspunkt(LocalDateTime.now().minusDays(5))
            .medJournalPostId(new JournalpostId(1L))
            .build();
        repositoryProvider.getMottatteDokumentRepository().lagre(mottattDokument);

        var dto = (EsSak) tjeneste.hentSak(behandling.getFagsak());
        assertThat(dto.saksnummer()).isEqualTo(behandling.getSaksnummer().getVerdi());
        assertThat(dto.aktørId()).isEqualTo(behandling.getAktørId().getId());
        assertThat(dto.vedtak()).hasSize(1);
        assertThat(dto.avsluttet()).isFalse();
        var vedtak = dto.vedtak().stream().findFirst().orElseThrow();
        assertThat(vedtak.vedtakstidspunkt()).isEqualTo(vedtakstidspunkt);

        var familieHendelse = dto.familieHendelse();
        assertThat(familieHendelse.fødselsdato()).isNull();
        assertThat(familieHendelse.antallBarn()).isZero();
        assertThat(familieHendelse.termindato()).isEqualTo(termindato);
        assertThat(familieHendelse.omsorgsovertakelse()).isNull();

        assertThat(dto.søknader()).hasSize(1);
        var søknad = dto.søknader().stream().findFirst().get();
        assertThat(søknad.mottattTidspunkt()).isEqualTo(mottattDokument.getMottattTidspunkt());
    }

    @Test
    void henter_aksjonspunkt() {
        var apDef = AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT;
        var behandling = ScenarioMorSøkerEngangsstønad.forFødsel()
            .leggTilAksjonspunkt(apDef, BehandlingStegType.VURDER_KOMPLETT_BEH)
            .lagre(repositoryProvider);

        var dto = (EsSak) tjeneste.hentSak(behandling.getFagsak());
        assertThat(dto.saksnummer()).isEqualTo(behandling.getSaksnummer().getVerdi());
        assertThat(dto.aktørId()).isEqualTo(behandling.getAktørId().getId());
        assertThat(dto.aksjonspunkt()).hasSize(1);
        var apDto = dto.aksjonspunkt().stream().findFirst().orElseThrow();
        assertThat(apDto.type()).isEqualTo(Sak.Aksjonspunkt.Type.VENT_MANUELT_SATT);
        assertThat(apDto.venteårsak()).isNull();
        assertThat(apDto.tidsfrist()).isNotNull();
    }

    private Long avsluttBehandling(Behandling behandling) {
        behandling.avsluttBehandling();
        return repositoryProvider.getBehandlingRepository()
            .lagre(behandling, repositoryProvider.getBehandlingLåsRepository().taLås(behandling.getId()));
    }

}
