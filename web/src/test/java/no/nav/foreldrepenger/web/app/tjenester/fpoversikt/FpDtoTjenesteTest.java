package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakUtsettelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.JournalpostId;

@CdiDbAwareTest
class FpDtoTjenesteTest {

    @Inject
    private FpDtoTjeneste tjeneste;
    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Test
    void henter_sak_med_foreldrepenger() {
        var vedtakstidspunkt = LocalDateTime.now();
        var fødselsdato = LocalDate.now();
        var oppgittRettighet = OppgittRettighetEntitet.beggeRett();
        var oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriode(fødselsdato, fødselsdato.plusWeeks(10))
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medMorsAktivitet(no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet.INNLAGT)
            .medArbeidsprosent(BigDecimal.TEN)
            .medArbeidsgiver(no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver.virksomhet("111"))
            .medÅrsak(no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak.FRI)
            .medSamtidigUttak(true)
            .medSamtidigUttaksprosent(new SamtidigUttaksprosent(30))
            .medFlerbarnsdager(true)
            .medGraderingAktivitetType(GraderingAktivitetType.ARBEID)
            .build();
        var fordeling = new OppgittFordelingEntitet(List.of(oppgittPeriode), true, true);
        var behandling = opprettAvsluttetFpBehandling(vedtakstidspunkt, Dekningsgrad._80, fødselsdato, fordeling, oppgittRettighet);
        var annenPartAktørId = AktørId.dummy();
        repositoryProvider.getPersonopplysningRepository().lagre(behandling.getId(),
            new OppgittAnnenPartBuilder().medAktørId(annenPartAktørId).build());
        var mottattDokument = new MottattDokument.Builder()
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .medDokumentType(DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL)
            .medMottattTidspunkt(LocalDateTime.now().minusDays(5))
            .medJournalPostId(new JournalpostId(1L))
            .build();
        repositoryProvider.getMottatteDokumentRepository().lagre(mottattDokument);

        var uttak = new UttakResultatPerioderEntitet();
        var fom = LocalDate.of(2023, 3, 5);
        var tom = LocalDate.of(2023, 10, 5);
        var periode = new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medFlerbarnsdager(true)
            .medGraderingInnvilget(true)
            .medOppholdÅrsak(no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER)
            .medUtsettelseType(UttakUtsettelseType.ARBEID)
            .medOverføringÅrsak(no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak.SYKDOM_ANNEN_FORELDER)
            .build();
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

        var dto = (FpSak) tjeneste.hentSak(behandling.getFagsak());
        assertThat(dto.saksnummer()).isEqualTo(behandling.getSaksnummer().getVerdi());
        assertThat(dto.aktørId()).isEqualTo(behandling.getAktørId().getId());
        assertThat(dto.ønskerJustertUttakVedFødsel()).isTrue();
        assertThat(dto.vedtak()).hasSize(1);
        assertThat(dto.avsluttet()).isFalse();
        var vedtak = dto.vedtak().stream().findFirst().orElseThrow();
        assertThat(vedtak.dekningsgrad()).isEqualTo(FpSak.Dekningsgrad.ÅTTI);
        assertThat(vedtak.vedtakstidspunkt()).isEqualTo(vedtakstidspunkt);
        assertThat(vedtak.uttaksperioder()).hasSize(1);
        var vedtaksperiode = vedtak.uttaksperioder().get(0);
        assertThat(vedtaksperiode.fom()).isEqualTo(fom);
        assertThat(vedtaksperiode.tom()).isEqualTo(tom);
        assertThat(vedtaksperiode.resultat().aktiviteter()).hasSize(1);
        var vedtaksAktivitet = vedtaksperiode.resultat().aktiviteter().stream().findFirst().get();
        assertThat(vedtaksAktivitet.arbeidstidsprosent()).isEqualTo(uttakAktivitet.getArbeidsprosent());
        assertThat(vedtaksAktivitet.trekkdager()).isEqualTo(uttakAktivitet.getTrekkdager().decimalValue());
        assertThat(vedtaksAktivitet.aktivitet().arbeidsforholdId()).isEqualTo(arbeidsforholdRef.getReferanse());
        assertThat(vedtaksAktivitet.aktivitet().arbeidsgiver().identifikator()).isEqualTo(arbeidsgiver.getIdentifikator());
        assertThat(vedtaksperiode.resultat().type()).isEqualTo(FpSak.Uttaksperiode.Resultat.Type.INNVILGET_GRADERING);
        assertThat(vedtaksperiode.flerbarnsdager()).isTrue();
        assertThat(vedtaksperiode.oppholdÅrsak()).isEqualTo(no.nav.foreldrepenger.web.app.tjenester.fpoversikt.OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER);
        assertThat(vedtaksperiode.utsettelseÅrsak()).isEqualTo(no.nav.foreldrepenger.web.app.tjenester.fpoversikt.UtsettelseÅrsak.ARBEID);
        assertThat(vedtaksperiode.overføringÅrsak()).isEqualTo(no.nav.foreldrepenger.web.app.tjenester.fpoversikt.OverføringÅrsak.SYKDOM_ANNEN_FORELDER);

        assertThat(dto.oppgittAnnenPart()).isEqualTo(annenPartAktørId.getId());
        assertThat(dto.brukerRolle()).isEqualTo(FpSak.BrukerRolle.MOR);

        assertThat(dto.rettigheter().annenForelderTilsvarendeRettEØS()).isEqualTo(oppgittRettighet.getAnnenForelderRettEØS());
        assertThat(dto.rettigheter().aleneomsorg()).isEqualTo(oppgittRettighet.getHarAleneomsorgForBarnet());
        assertThat(dto.rettigheter().morUføretrygd()).isEqualTo(oppgittRettighet.getMorMottarUføretrygd());

        var familieHendelse = dto.familieHendelse();
        assertThat(familieHendelse.fødselsdato()).isEqualTo(fødselsdato);
        assertThat(familieHendelse.antallBarn()).isEqualTo(1);
        assertThat(familieHendelse.termindato()).isNull();
        assertThat(familieHendelse.omsorgsovertakelse()).isNull();

        assertThat(dto.søknader()).hasSize(1);
        var søknad = dto.søknader().stream().findFirst().get();
        assertThat(søknad.mottattTidspunkt()).isEqualTo(mottattDokument.getMottattTidspunkt());
        assertThat(søknad.dekningsgrad()).isEqualTo(vedtak.dekningsgrad());
        assertThat(søknad.perioder()).hasSize(1);
        var søknadsperiode = søknad.perioder().stream().findFirst().get();
        assertThat(søknadsperiode.fom()).isEqualTo(oppgittPeriode.getFom());
        assertThat(søknadsperiode.tom()).isEqualTo(oppgittPeriode.getTom());
        assertThat(søknadsperiode.konto()).isEqualTo(Konto.MØDREKVOTE);
        assertThat(søknadsperiode.flerbarnsdager()).isTrue();
        assertThat(søknadsperiode.gradering().prosent()).isEqualTo(oppgittPeriode.getArbeidsprosent());
        assertThat(søknadsperiode.gradering().uttakAktivitet().type()).isEqualTo(UttakAktivitet.Type.ORDINÆRT_ARBEID);
        assertThat(søknadsperiode.gradering().uttakAktivitet().arbeidsforholdId()).isNull();
        assertThat(søknadsperiode.gradering().uttakAktivitet().arbeidsgiver().identifikator()).isEqualTo(oppgittPeriode.getArbeidsgiver().getIdentifikator());
        assertThat(søknadsperiode.morsAktivitet()).isEqualTo(no.nav.foreldrepenger.web.app.tjenester.fpoversikt.MorsAktivitet.INNLAGT);
        assertThat(søknadsperiode.oppholdÅrsak()).isNull();
        assertThat(søknadsperiode.utsettelseÅrsak()).isEqualTo(no.nav.foreldrepenger.web.app.tjenester.fpoversikt.UtsettelseÅrsak.FRI);
        assertThat(søknadsperiode.overføringÅrsak()).isNull();
        assertThat(søknadsperiode.samtidigUttak()).isEqualTo(oppgittPeriode.getSamtidigUttaksprosent().decimalValue());
    }

    private Long avsluttBehandling(Behandling behandling) {
        behandling.avsluttBehandling();
        return repositoryProvider.getBehandlingRepository()
            .lagre(behandling, repositoryProvider.getBehandlingLåsRepository().taLås(behandling.getId()));
    }


    private Behandling opprettAvsluttetFpBehandling(LocalDateTime vedtakstidspunkt,
                                                    Dekningsgrad dekningsgrad,
                                                    LocalDate fødselsdato,
                                                    OppgittFordelingEntitet fordeling,
                                                    OppgittRettighetEntitet oppgittRettighet) {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(oppgittRettighet)
            .medFordeling(fordeling)
            .medFødselAdopsjonsdato(fødselsdato)
            .medOppgittDekningsgrad(dekningsgrad);

        scenario.medBehandlingVedtak().medVedtakResultatType(VedtakResultatType.INNVILGET).medVedtakstidspunkt(vedtakstidspunkt);
        var behandling = scenario.medBehandlingsresultat(Behandlingsresultat.builder()
                .medBehandlingResultatType(BehandlingResultatType.INNVILGET))
            .lagre(repositoryProvider);

        avsluttBehandling(behandling);
        return behandling;
    }

}
