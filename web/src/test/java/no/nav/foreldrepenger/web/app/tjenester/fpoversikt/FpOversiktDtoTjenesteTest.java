package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;

@CdiDbAwareTest
class FpOversiktDtoTjenesteTest {

    @Inject
    private FpOversiktDtoTjeneste tjeneste;
    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Test
    void henter_sak_med_foreldrepenger() {
        var vedtakstidspunkt = LocalDateTime.now();
        var fødselsdato = LocalDate.now();
        var behandling = opprettAvsluttetBehandling(vedtakstidspunkt, Dekningsgrad._80, fødselsdato);
        var annenPartAktørId = AktørId.dummy();
        repositoryProvider.getPersonopplysningRepository().lagre(behandling.getId(),
            new OppgittAnnenPartBuilder().medAktørId(annenPartAktørId).build());

        var uttak = new UttakResultatPerioderEntitet();
        var fom = LocalDate.of(2023, 3, 5);
        var tom = LocalDate.of(2023, 10, 5);
        var periode = new UttakResultatPeriodeEntitet.Builder(fom, tom).medResultatType(PeriodeResultatType.INNVILGET,
            PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE);
        uttak.leggTilPeriode(periode.build());
        repositoryProvider.getFpUttakRepository().lagreOpprinneligUttakResultatPerioder(behandling.getId(), uttak);

        var dto = (FpSak) tjeneste.hentSak(behandling.getFagsak().getSaksnummer().getVerdi());
        assertThat(dto.saksnummer()).isEqualTo(behandling.getFagsak().getSaksnummer().getVerdi());
        assertThat(dto.aktørId()).isEqualTo(behandling.getAktørId().getId());
        assertThat(dto.vedtakene()).hasSize(1);
        var vedtak = dto.vedtakene().stream().findFirst().orElseThrow();
        assertThat(vedtak.dekningsgrad()).isEqualTo(FpSak.Vedtak.Dekningsgrad.ÅTTI);
        assertThat(vedtak.vedtakstidspunkt()).isEqualTo(vedtakstidspunkt);
        assertThat(vedtak.uttaksperioder()).hasSize(1);
        assertThat(vedtak.uttaksperioder().get(0).fom()).isEqualTo(fom);
        assertThat(vedtak.uttaksperioder().get(0).tom()).isEqualTo(tom);
        assertThat(vedtak.uttaksperioder().get(0).resultat().type()).isEqualTo(FpSak.Uttaksperiode.Resultat.Type.INNVILGET);

        assertThat(dto.oppgittAnnenPart()).isEqualTo(annenPartAktørId.getId());

        var familieHendelse = dto.familieHendelse();
        assertThat(familieHendelse.fødselsdato()).isEqualTo(fødselsdato);
        assertThat(familieHendelse.antallBarn()).isEqualTo(1);
        assertThat(familieHendelse.termindato()).isNull();
        assertThat(familieHendelse.omsorgsovertakelse()).isNull();
    }

    private Behandling opprettAvsluttetBehandling(LocalDateTime vedtakstidspunkt, Dekningsgrad dekningsgrad, LocalDate fødselsdato) {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFødselAdopsjonsdato(fødselsdato);

        scenario.medBehandlingVedtak().medVedtakResultatType(VedtakResultatType.INNVILGET).medVedtakstidspunkt(vedtakstidspunkt);
        var behandling = scenario.medBehandlingsresultat(Behandlingsresultat.builder()
            .medBehandlingResultatType(BehandlingResultatType.INNVILGET))
            .lagre(repositoryProvider);

        behandling.avsluttBehandling();
        repositoryProvider.getBehandlingRepository().lagre(behandling, repositoryProvider.getBehandlingLåsRepository().taLås(behandling.getId()));

        repositoryProvider.getFagsakRelasjonRepository().opprettRelasjon(behandling.getFagsak(), dekningsgrad);
        return behandling;
    }

}
