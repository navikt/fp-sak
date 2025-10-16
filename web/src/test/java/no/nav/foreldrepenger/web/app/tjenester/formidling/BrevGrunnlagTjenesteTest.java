package no.nav.foreldrepenger.web.app.tjenester.formidling;

import static no.nav.foreldrepenger.web.app.tjenester.formidling.BrevGrunnlagDto.FagsakYtelseType;
import static no.nav.foreldrepenger.web.app.tjenester.formidling.BrevGrunnlagDto.Rettigheter;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;


@CdiDbAwareTest
class BrevGrunnlagTjenesteTest {

    @Inject
    private BrevGrunnlagTjeneste brevGrunnlagTjeneste;

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Test
    void foreldrepenger() {
        var fødselsdato = LocalDate.now();
        var oppgittRettighet = OppgittRettighetEntitet.beggeRett();
        var behandling = opprettAvsluttetFpBehandling(fødselsdato, oppgittRettighet);

        var uttak = new UttakResultatPerioderEntitet();
        var fom = LocalDate.of(2023, 3, 5);
        var tom = LocalDate.of(2023, 10, 5);
        var periode = new UttakResultatPeriodeEntitet.Builder(fom, tom).medResultatType(PeriodeResultatType.INNVILGET,
            PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE).build();
        uttak.leggTilPeriode(periode);
        var arbeidsgiver = Arbeidsgiver.virksomhet("123");
        var arbeidsforholdRef = InternArbeidsforholdRef.nyRef();
        var uttakAktivitet = new UttakResultatPeriodeAktivitetEntitet.Builder(periode,
            new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
                .medArbeidsforhold(arbeidsgiver, arbeidsforholdRef)
                .build()).medArbeidsprosent(BigDecimal.TEN)
            .medTrekkdager(new Trekkdager(10))
            .medErSøktGradering(true)
            .medUtbetalingsgrad(new Utbetalingsgrad(90))
            .build();
        repositoryProvider.getFpUttakRepository().lagreOpprinneligUttakResultatPerioder(behandling.getId(), uttak);


        var brevGrunnlagDto = brevGrunnlagTjeneste.lagGrunnlagDto(behandling);

        assertThat(brevGrunnlagDto.fagsakYtelseType()).isEqualTo(FagsakYtelseType.FORELDREPENGER);

        assertThat(brevGrunnlagDto.saksnummer()).isEqualTo(behandling.getSaksnummer().getVerdi());
        assertThat(brevGrunnlagDto.aktørId()).isEqualTo(behandling.getAktørId().getId());
        assertThat(brevGrunnlagDto.behandlendeEnhet()).isEqualTo(behandling.getBehandlendeEnhet());
        assertThat(brevGrunnlagDto.foreldrepenger()).isNotNull();
        assertThat(brevGrunnlagDto.foreldrepenger().dekningsgrad()).isEqualTo(BrevGrunnlagDto.Dekningsgrad.ÅTTI);
        assertThat(brevGrunnlagDto.foreldrepenger().perioderSøker()).hasSize(1);
        assertThat(brevGrunnlagDto.foreldrepenger().perioderSøker().getFirst().fom()).isEqualTo(periode.getFom());
        assertThat(brevGrunnlagDto.foreldrepenger().perioderSøker().getFirst().periodeResultatType()).isEqualTo(
            BrevGrunnlagDto.PeriodeResultatType.INNVILGET);
        assertThat(brevGrunnlagDto.foreldrepenger().perioderSøker().getFirst().aktiviteter().getFirst().arbeidsgiverReferanse()).isEqualTo(
            arbeidsgiver.getIdentifikator());
        assertThat(brevGrunnlagDto.foreldrepenger().perioderSøker().getFirst().aktiviteter().getFirst().utbetalingsgrad()).isEqualTo(
            uttakAktivitet.getUtbetalingsgrad().decimalValue());
        assertThat(brevGrunnlagDto.foreldrepenger().rettigheter().gjeldende()).isEqualTo(Rettigheter.Rettighetstype.BEGGE_RETT);
    }

    @Test
    void engangsstønad() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);
        var brevGrunnlagDto = brevGrunnlagTjeneste.lagGrunnlagDto(behandling);

        assertThat(brevGrunnlagDto.fagsakYtelseType()).isEqualTo(FagsakYtelseType.ENGANGSTØNAD);
    }

    private Behandling opprettAvsluttetFpBehandling(LocalDate fødselsdato, OppgittRettighetEntitet oppgittRettighet) {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(oppgittRettighet)
            .medFødselAdopsjonsdato(fødselsdato)
            .medDefaultFordeling(fødselsdato.minusWeeks(3))
            .medOppgittDekningsgrad(Dekningsgrad._80);

        scenario.medBehandlingVedtak().medVedtakResultatType(VedtakResultatType.INNVILGET);
        var behandling = scenario.medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET))
            .lagre(repositoryProvider);

        behandling.avsluttBehandling();
        repositoryProvider.getBehandlingRepository().lagre(behandling, repositoryProvider.getBehandlingLåsRepository().taLås(behandling.getId()));
        return behandling;
    }
}
