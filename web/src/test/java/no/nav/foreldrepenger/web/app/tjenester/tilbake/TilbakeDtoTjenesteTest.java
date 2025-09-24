package no.nav.foreldrepenger.web.app.tjenester.tilbake;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingValg;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeOrganisasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomioppdragRepository;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;

@ExtendWith(MockitoExtension.class)
class TilbakeDtoTjenesteTest {

    private static final AktørId AKTØR_ID = AktørId.dummy();
    private TilbakeDtoTjeneste tjeneste;
    @Mock
    private ØkonomioppdragRepository økonomioppdragRepository;
    @Mock
    private TilbakekrevingRepository tilbakekrevingRepository;
    @Mock
    private VergeRepository vergeRepository;
    @Mock
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    @Mock
    private BehandlingVedtakRepository behandlingVedtakRepository;

    @Test
    void skal_lage_dto_fra_behandling_med_inntrekk() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medSøknadHendelse().medAntallBarn(1).medFødselsDato(LocalDate.now());
        var repository = scenario.mockBehandlingRepositoryProvider();
        var behandling = scenario.lagre(repository);

        var t = setupstandardmocks(behandling, repository, TilbakekrevingVidereBehandling.INNTREKK, null);
        var dto = t.lagTilbakeDto(behandling);

        assertThat(dto.behandling().uuid()).isEqualTo(behandling.getUuid());
        assertThat(dto.behandling().henvisning().henvisning()).isEqualTo(behandling.getId());
        assertThat(dto.behandling().behandlendeEnhetId()).isEqualTo("4867");
        assertThat(dto.fagsak().saksnummer()).isEqualTo(behandling.getSaksnummer().getVerdi());
        assertThat(dto.fagsak().fagsakYtelseType()).isEqualTo(TilbakeDto.YtelseType.FORELDREPENGER);
        assertThat(dto.familieHendelse().familieHendelseType()).isEqualTo(TilbakeDto.FamilieHendelseType.FØDSEL);
        assertThat(dto.familieHendelse().antallBarn()).isEqualTo(1);
        assertThat(dto.feilutbetaling().feilutbetalingValg()).isEqualTo(TilbakeDto.FeilutbetalingValg.INNTREKK);
        assertThat(dto.feilutbetaling().varseltekst()).isNull();
        assertThat(dto.sendtoppdrag()).isTrue();
        assertThat(dto.verge()).isNull();
    }

    @Test
    void skal_lage_dto_fra_behandling_med_varsel() {
        var scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        scenario.medSøknadHendelse().medAntallBarn(1).medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder().medTermindato(LocalDate.now().plusMonths(3)).medUtstedtDato(LocalDate.now()));
        var repository = scenario.mockBehandlingRepositoryProvider();
        var behandling = scenario.lagre(repository);

        var t = setupstandardmocks(behandling, repository, TilbakekrevingVidereBehandling.OPPRETT_TILBAKEKREVING, "Du har fått lønn og utbetaling fra Nav for samme periode");
        var dto = t.lagTilbakeDto(behandling);

        assertThat(dto.behandling().uuid()).isEqualTo(behandling.getUuid());
        assertThat(dto.behandling().henvisning().henvisning()).isEqualTo(behandling.getId());
        assertThat(dto.fagsak().saksnummer()).isEqualTo(behandling.getSaksnummer().getVerdi());
        assertThat(dto.fagsak().fagsakYtelseType()).isEqualTo(TilbakeDto.YtelseType.SVANGERSKAPSPENGER);
        assertThat(dto.familieHendelse().familieHendelseType()).isEqualTo(TilbakeDto.FamilieHendelseType.FØDSEL);
        assertThat(dto.familieHendelse().antallBarn()).isEqualTo(1);
        assertThat(dto.feilutbetaling().feilutbetalingValg()).isEqualTo(TilbakeDto.FeilutbetalingValg.OPPRETT);
        assertThat(dto.feilutbetaling().varseltekst()).isNotNull();
        assertThat(dto.sendtoppdrag()).isTrue();
        assertThat(dto.verge()).isNull();
    }

    @Test
    void skal_lage_dto_fra_behandling_uten_valg() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medAntallBarn(1)
            .medFødselsDato(LocalDate.now().minusYears(3))
            .medAdopsjon(scenario.medSøknadHendelse().getAdopsjonBuilder()
                .medOmsorgsovertakelseDato(LocalDate.now()));
        var repository = scenario.mockBehandlingRepositoryProvider();
        var behandling = scenario.lagre(repository);

        when(vergeRepository.hentAggregat(anyLong())).thenReturn(Optional.of(new VergeAggregat(new VergeEntitet.Builder()
            .medVergeType(VergeType.BARN)
            .gyldigPeriode(LocalDate.now(), LocalDate.now().plusYears(1))
            .medVergeOrganisasjon(new VergeOrganisasjonEntitet.Builder().medNavn("Hjem").medOrganisasjonsnummer("123456789").build()).build())));

        var t = setupstandardmocks(behandling, repository, null, null);
        var dto = t.lagTilbakeDto(behandling);

        assertThat(dto.behandling().uuid()).isEqualTo(behandling.getUuid());
        assertThat(dto.behandling().henvisning().henvisning()).isEqualTo(behandling.getId());
        assertThat(dto.behandling().behandlendeEnhetId()).isEqualTo("4867");
        assertThat(dto.fagsak().saksnummer()).isEqualTo(behandling.getSaksnummer().getVerdi());
        assertThat(dto.fagsak().fagsakYtelseType()).isEqualTo(TilbakeDto.YtelseType.ENGANGSSTØNAD);
        assertThat(dto.familieHendelse().familieHendelseType()).isEqualTo(TilbakeDto.FamilieHendelseType.ADOPSJON);
        assertThat(dto.familieHendelse().antallBarn()).isEqualTo(1);
        assertThat(dto.feilutbetaling()).isNull();
        assertThat(dto.verge().gyldigFom()).isEqualTo(LocalDate.now());
        assertThat(dto.verge().gyldigTom()).isEqualTo(LocalDate.now().plusYears(1));
        assertThat(dto.verge().navn()).isEqualTo("Hjem");
        assertThat(dto.verge().organisasjonsnummer()).isEqualTo("123456789");
        assertThat(dto.verge().vergeType()).isEqualTo(TilbakeDto.VergeType.BARN);
        assertThat(dto.verge().aktørId()).isNull();
    }


    private TilbakeDtoTjeneste setupstandardmocks(Behandling behandling, BehandlingRepositoryProvider provider, TilbakekrevingVidereBehandling videre, String varseltekst) {
        when(behandlingVedtakRepository.hentForBehandlingHvisEksisterer(anyLong())).thenReturn(Optional.of(BehandlingVedtak.builder()
            .medVedtakstidspunkt(LocalDateTime.now()).medAnsvarligSaksbehandler("SBH").medVedtakResultatType(VedtakResultatType.INNVILGET)
            .build()));
        when(økonomioppdragRepository.finnOppdragForBehandling(anyLong())).thenReturn(Optional.of(Oppdragskontroll.builder()
            .medSaksnummer(behandling.getSaksnummer()).medBehandlingId(behandling.getId()).medVenterKvittering(false).medProsessTaskId(1L).build()));
        if (videre != null) {
            when(tilbakekrevingRepository.hent(anyLong())).thenReturn(Optional.of(new TilbakekrevingValg(true, true, videre, varseltekst)));
        }
        when(behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(any(Fagsak.class))).thenReturn(BehandlendeEnhetTjeneste.getNasjonalEnhet());
        var fht = new FamilieHendelseTjeneste(null, provider.getFamilieHendelseRepository());
        return new TilbakeDtoTjeneste(økonomioppdragRepository, tilbakekrevingRepository, vergeRepository, behandlendeEnhetTjeneste, fht,
            behandlingVedtakRepository, provider.getSøknadRepository());
    }

}
