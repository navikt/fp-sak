package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.fakta;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.fp.BeregningUttakTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeSøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;

@CdiDbAwareTest
class FaktaUttakPeriodeDtoTjenesteTest {

    @Inject
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    @Inject
    private MedlemTjeneste medlemTjeneste;
    @Inject
    private BeregningUttakTjeneste beregningUttakTjeneste;

    @Test
    void skal_returnere_tom_liste_hvis_ingen_perioder() {
        var dtoTjeneste = dtoTjeneste();

        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFødselAdopsjonsdato(LocalDate.now())
            .lagre(repositoryProvider);
        var resultat = dtoTjeneste.lagDtos(new UuidDto(behandling.getUuid()));

        assertThat(resultat).isEmpty();
    }

    @Test
    void skal_returnere_perioder_som_dtos() {

        var dtoTjeneste = dtoTjeneste();

        var fødselsdato = LocalDate.now();
        var førstegangsbehandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFødselAdopsjonsdato(fødselsdato)
            .lagre(repositoryProvider);

        var uttaksperiode = new UttakResultatPeriodeEntitet.Builder(fødselsdato, fødselsdato.plusWeeks(10))
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medSamtidigUttak(true)
            .medSamtidigUttaksprosent(SamtidigUttaksprosent.TEN)
            .medGraderingInnvilget(true)
            .medFlerbarnsdager(true)
            .medPeriodeSoknad(new UttakResultatPeriodeSøknadEntitet.Builder()
                .medMorsAktivitet(MorsAktivitet.ARBEID)
                .medGraderingArbeidsprosent(BigDecimal.TEN)
                .medUttakPeriodeType(UttakPeriodeType.FELLESPERIODE)
                .build())
            .build();
        new UttakResultatPeriodeAktivitetEntitet.Builder(uttaksperiode, new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.FRILANS)
            .build())
                .medArbeidsprosent(BigDecimal.ZERO)
                .medUtbetalingsgrad(Utbetalingsgrad.FULL)
                .medTrekkdager(new Trekkdager(10))
                .medErSøktGradering(true)
                .medTrekkonto(StønadskontoType.FELLESPERIODE)
            .build();
        var uttakperioder = new UttakResultatPerioderEntitet();
        uttakperioder.leggTilPeriode(uttaksperiode);
        repositoryProvider.getFpUttakRepository().lagreOpprinneligUttakResultatPerioder(førstegangsbehandling.getId(), uttakperioder);

        var op = OppgittPeriodeBuilder.ny()
            .medPeriode(uttaksperiode.getTom().plusDays(1), uttaksperiode.getTom().plusDays(1).plusWeeks(6))
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medArbeidsprosent(BigDecimal.TEN)
            .medGraderingAktivitetType(GraderingAktivitetType.ARBEID)
            .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
            .medMorsAktivitet(MorsAktivitet.UFØRE)
            .medSamtidigUttaksprosent(SamtidigUttaksprosent.TEN)
            .medSamtidigUttak(true)
            .medDokumentasjonVurdering(DokumentasjonVurdering.MORS_AKTIVITET_IKKE_DOKUMENTERT)
            .medÅrsak(UtsettelseÅrsak.SYKDOM)
            .medFlerbarnsdager(true)
            .medBegrunnelse("begrunnelse")
            .build();

        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(op), true))
            .medFødselAdopsjonsdato(fødselsdato)
            .medOriginalBehandling(førstegangsbehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
            .medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medOpprinneligEndringsdato(op.getFom()).build())
            .lagre(repositoryProvider);

        var resultat = dtoTjeneste.lagDtos(new UuidDto(behandling.getUuid()));

        assertThat(resultat).hasSize(2);

        assertThat(resultat.get(0).fom()).isEqualTo(uttaksperiode.getFom());
        assertThat(resultat.get(0).tom()).isEqualTo(uttaksperiode.getTom());
        assertThat(resultat.get(0).uttakPeriodeType()).isEqualTo(UttakPeriodeType.FELLESPERIODE);
        assertThat(resultat.get(0).arbeidstidsprosent()).isEqualTo(uttaksperiode.getPeriodeSøknad().get().getGraderingArbeidsprosent());
        assertThat(resultat.get(0).arbeidsforhold().arbeidsgiverReferanse()).isNull();
        assertThat(resultat.get(0).morsAktivitet()).isEqualTo(uttaksperiode.getPeriodeSøknad().orElseThrow().getMorsAktivitet());
        assertThat(resultat.get(0).samtidigUttaksprosent()).isEqualTo(uttaksperiode.getSamtidigUttaksprosent());
        assertThat(resultat.get(0).utsettelseÅrsak()).isNull();
        assertThat(resultat.get(0).oppholdÅrsak()).isNull();
        assertThat(resultat.get(0).overføringÅrsak()).isNull();
        assertThat(resultat.get(0).flerbarnsdager()).isEqualTo(uttaksperiode.isFlerbarnsdager());
        assertThat(resultat.get(0).periodeKilde()).isEqualTo(FordelingPeriodeKilde.TIDLIGERE_VEDTAK);

        assertThat(resultat.get(1).fom()).isEqualTo(op.getFom());
        assertThat(resultat.get(1).tom()).isEqualTo(op.getTom());
        assertThat(resultat.get(1).uttakPeriodeType()).isEqualTo(op.getPeriodeType());
        assertThat(resultat.get(1).periodeKilde()).isEqualTo(op.getPeriodeKilde());
        assertThat(resultat.get(1).arbeidstidsprosent()).isEqualTo(op.getArbeidsprosent());
        assertThat(resultat.get(1).arbeidsforhold().arbeidsgiverReferanse()).isEqualTo(op.getArbeidsgiver().getIdentifikator());
        assertThat(resultat.get(1).morsAktivitet()).isEqualTo(op.getMorsAktivitet());
        assertThat(resultat.get(1).samtidigUttaksprosent()).isEqualTo(op.getSamtidigUttaksprosent());
        assertThat(resultat.get(1).utsettelseÅrsak()).isEqualTo(op.getÅrsak());
        assertThat(resultat.get(1).oppholdÅrsak()).isNull();
        assertThat(resultat.get(1).overføringÅrsak()).isNull();
        assertThat(resultat.get(1).flerbarnsdager()).isEqualTo(op.isFlerbarnsdager());
        assertThat(resultat.get(1).begrunnelse()).isEqualTo(op.getBegrunnelse().orElseThrow());
    }

    private FaktaUttakPeriodeDtoTjeneste dtoTjeneste() {
        var uttakInputTjeneste = new UttakInputTjeneste(repositoryProvider,
            new HentOgLagreBeregningsgrunnlagTjeneste(repositoryProvider.getEntityManager()), new AbakusInMemoryInntektArbeidYtelseTjeneste(),
            skjæringstidspunktTjeneste, medlemTjeneste, beregningUttakTjeneste,
            new YtelseFordelingTjeneste(repositoryProvider.getYtelsesFordelingRepository()), true);
        return new FaktaUttakPeriodeDtoTjeneste(uttakInputTjeneste, ytelseFordelingTjeneste, repositoryProvider.getBehandlingRepository(),
            repositoryProvider.getFpUttakRepository());
    }

}
