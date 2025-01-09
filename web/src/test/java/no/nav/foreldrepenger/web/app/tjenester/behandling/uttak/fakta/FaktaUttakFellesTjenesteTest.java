package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.fakta;

import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.Type.MORS_AKTIVITET_IKKE_DOKUMENTERT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.Type.SYKDOM_ANNEN_FORELDER_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde.ANDRE_NAV_VEDTAK;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde.SAKSBEHANDLER;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde.SØKNAD;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde.TIDLIGERE_VEDTAK;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.FEDREKVOTE;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.FELLESPERIODE;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.MØDREKVOTE;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.UDEFINERT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.Årsak;
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
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeSøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;

@CdiDbAwareTest
class FaktaUttakFellesTjenesteTest {

    @Inject
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private FaktaUttakFellesTjeneste faktaUttakFellesTjeneste;

    @Test
    void skal_lagre_perioder() {
        var opprinneligPeriode1 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(MØDREKVOTE)
            .medPeriodeKilde(TIDLIGERE_VEDTAK)
            .medPeriode(LocalDate.now(), LocalDate.now().plusWeeks(2))
            .build();
        var opprinneligPeriode2 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(MØDREKVOTE)
            .medPeriodeKilde(SØKNAD)
            .medPeriode(opprinneligPeriode1.getTom().plusDays(1), opprinneligPeriode1.getTom().plusWeeks(5))
            .build();
        var opprinneligEndringsdato = opprinneligPeriode2.getFom();
        var avklartFørsteUttaksdag = opprinneligPeriode1.getFom().minusWeeks(2);
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(opprinneligPeriode1, opprinneligPeriode2), true))
            .medFødselAdopsjonsdato(opprinneligPeriode2.getFom())
            .medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medOpprinneligEndringsdato(opprinneligEndringsdato)
                .medFørsteUttaksdato(avklartFørsteUttaksdag)
                .build())
            .lagre(repositoryProvider);
        var mødrekvoteFørOpprinnelig = mødrekvote(avklartFørsteUttaksdag, opprinneligPeriode1.getFom().minusDays(1), SAKSBEHANDLER);
        var mødrekvoteDto = mødrekvote(opprinneligPeriode1.getFom(), opprinneligPeriode1.getTom(), TIDLIGERE_VEDTAK);
        var utsettelseDto = new FaktaUttakPeriodeDto(mødrekvoteDto.tom().plusDays(1), mødrekvoteDto.tom().plusWeeks(1), null, UtsettelseÅrsak.SYKDOM,
            null, null, null, null, null, false, MorsAktivitet.ARBEID, SAKSBEHANDLER, "begrunnelse");
        var oppholdDto = new FaktaUttakPeriodeDto(utsettelseDto.tom().plusDays(1), utsettelseDto.tom().plusWeeks(1), null, null,
            null, OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER, null, null, null, false, null, SAKSBEHANDLER, null);
        var overføringDto = new FaktaUttakPeriodeDto(oppholdDto.tom().plusDays(1), oppholdDto.tom().plusWeeks(1), FEDREKVOTE,
            null, OverføringÅrsak.SYKDOM_ANNEN_FORELDER, null, null, null, null, false, null, ANDRE_NAV_VEDTAK, null);
        var samtidigUttak = new FaktaUttakPeriodeDto(overføringDto.tom().plusDays(1), overføringDto.tom().plusWeeks(1), FELLESPERIODE,
            null, null, null, null, null, SamtidigUttaksprosent.TEN, true, null, null, null);
        var resultat = kjørOppdaterer(behandling, List.of(mødrekvoteFørOpprinnelig, mødrekvoteDto, utsettelseDto,
            oppholdDto, overføringDto, samtidigUttak));
        var yfa = ytelseFordelingTjeneste.hentAggregat(behandling.getId());
        var lagretPerioder = yfa.getGjeldendeFordeling().getPerioder();

        assertThat(yfa.getAvklarteDatoer().orElseThrow().getOpprinneligEndringsdato()).isEqualTo(opprinneligEndringsdato);
        //Endrer endringsdato pga første periode som lagres starter før opprinnelig endringsdato
        assertThat(yfa.getGjeldendeEndringsdato()).isEqualTo(mødrekvoteFørOpprinnelig.fom());
        assertThat(resultat.skalUtføreAksjonspunkt()).isTrue();
        assertThat(lagretPerioder).hasSize(6);

        assertThat(lagretPerioder.get(0).getFom()).isEqualTo(mødrekvoteFørOpprinnelig.fom());
        assertThat(lagretPerioder.get(0).getTom()).isEqualTo(mødrekvoteFørOpprinnelig.tom());
        assertThat(lagretPerioder.get(0).getGraderingAktivitetType()).isNull();
        assertThat(lagretPerioder.get(0).getArbeidsprosent()).isEqualTo(mødrekvoteFørOpprinnelig.arbeidstidsprosent());
        assertThat(lagretPerioder.get(0).getDokumentasjonVurdering()).isNull();
        assertThat(lagretPerioder.get(0).getArbeidsgiver()).isNull();
        assertThat(lagretPerioder.get(0).getPeriodeKilde()).isEqualTo(SAKSBEHANDLER);
        assertThat(lagretPerioder.get(0).getSamtidigUttaksprosent()).isEqualTo(mødrekvoteFørOpprinnelig.samtidigUttaksprosent());
        assertThat(lagretPerioder.get(0).getPeriodeType()).isEqualTo(mødrekvoteFørOpprinnelig.uttakPeriodeType());
        assertThat(lagretPerioder.get(0).isFlerbarnsdager()).isEqualTo(mødrekvoteFørOpprinnelig.flerbarnsdager());
        assertThat(lagretPerioder.get(0).getÅrsak()).isEqualTo(Årsak.UKJENT);

        assertThat(lagretPerioder.get(1).getFom()).isEqualTo(mødrekvoteDto.fom());
        assertThat(lagretPerioder.get(1).getTom()).isEqualTo(mødrekvoteDto.tom());
        assertThat(lagretPerioder.get(1).getGraderingAktivitetType()).isNull();
        assertThat(lagretPerioder.get(1).getArbeidsprosent()).isEqualTo(mødrekvoteDto.arbeidstidsprosent());
        assertThat(lagretPerioder.get(1).getDokumentasjonVurdering()).isNull();
        assertThat(lagretPerioder.get(1).getArbeidsgiver()).isNull();
        assertThat(lagretPerioder.get(1).getPeriodeKilde()).isEqualTo(TIDLIGERE_VEDTAK);
        assertThat(lagretPerioder.get(1).getSamtidigUttaksprosent()).isEqualTo(mødrekvoteDto.samtidigUttaksprosent());
        assertThat(lagretPerioder.get(1).getPeriodeType()).isEqualTo(mødrekvoteDto.uttakPeriodeType());
        assertThat(lagretPerioder.get(1).isFlerbarnsdager()).isEqualTo(mødrekvoteDto.flerbarnsdager());
        assertThat(lagretPerioder.get(1).getÅrsak()).isEqualTo(Årsak.UKJENT);

        assertThat(lagretPerioder.get(2).getFom()).isEqualTo(utsettelseDto.fom());
        assertThat(lagretPerioder.get(2).getTom()).isEqualTo(utsettelseDto.tom());
        assertThat(lagretPerioder.get(2).getGraderingAktivitetType()).isNull();
        assertThat(lagretPerioder.get(2).getArbeidsprosent()).isNull();
        assertThat(lagretPerioder.get(2).getDokumentasjonVurdering()).isNull();
        assertThat(lagretPerioder.get(2).getArbeidsgiver()).isNull();
        assertThat(lagretPerioder.get(2).getPeriodeKilde()).isEqualTo(SAKSBEHANDLER);
        assertThat(lagretPerioder.get(2).getSamtidigUttaksprosent()).isNull();
        assertThat(lagretPerioder.get(2).getPeriodeType()).isEqualTo(UDEFINERT);
        assertThat(lagretPerioder.get(2).isFlerbarnsdager()).isEqualTo(utsettelseDto.flerbarnsdager());
        assertThat(lagretPerioder.get(2).getÅrsak()).isEqualTo(utsettelseDto.utsettelseÅrsak());
        assertThat(lagretPerioder.get(2).getBegrunnelse()).isEmpty();

        assertThat(lagretPerioder.get(3).getFom()).isEqualTo(oppholdDto.fom());
        assertThat(lagretPerioder.get(3).getTom()).isEqualTo(oppholdDto.tom());
        assertThat(lagretPerioder.get(3).getGraderingAktivitetType()).isNull();
        assertThat(lagretPerioder.get(3).getArbeidsprosent()).isNull();
        assertThat(lagretPerioder.get(3).getDokumentasjonVurdering()).isNull();
        assertThat(lagretPerioder.get(3).getArbeidsgiver()).isNull();
        assertThat(lagretPerioder.get(3).getPeriodeKilde()).isEqualTo(SAKSBEHANDLER);
        assertThat(lagretPerioder.get(3).getSamtidigUttaksprosent()).isNull();
        assertThat(lagretPerioder.get(3).getPeriodeType()).isEqualTo(UDEFINERT);
        assertThat(lagretPerioder.get(3).isFlerbarnsdager()).isEqualTo(oppholdDto.flerbarnsdager());
        assertThat(lagretPerioder.get(3).getÅrsak()).isEqualTo(oppholdDto.oppholdÅrsak());

        assertThat(lagretPerioder.get(4).getFom()).isEqualTo(overføringDto.fom());
        assertThat(lagretPerioder.get(4).getTom()).isEqualTo(overføringDto.tom());
        assertThat(lagretPerioder.get(4).getGraderingAktivitetType()).isNull();
        assertThat(lagretPerioder.get(4).getArbeidsprosent()).isNull();
        assertThat(lagretPerioder.get(4).getDokumentasjonVurdering()).isNull();
        assertThat(lagretPerioder.get(4).getArbeidsgiver()).isNull();
        assertThat(lagretPerioder.get(4).getPeriodeKilde()).isEqualTo(ANDRE_NAV_VEDTAK);
        assertThat(lagretPerioder.get(4).getSamtidigUttaksprosent()).isNull();
        assertThat(lagretPerioder.get(4).getPeriodeType()).isEqualTo(overføringDto.uttakPeriodeType());
        assertThat(lagretPerioder.get(4).isFlerbarnsdager()).isEqualTo(overføringDto.flerbarnsdager());
        assertThat(lagretPerioder.get(4).getÅrsak()).isEqualTo(overføringDto.overføringÅrsak());

        assertThat(lagretPerioder.get(5).getFom()).isEqualTo(samtidigUttak.fom());
        assertThat(lagretPerioder.get(5).getTom()).isEqualTo(samtidigUttak.tom());
        assertThat(lagretPerioder.get(5).getGraderingAktivitetType()).isNull();
        assertThat(lagretPerioder.get(5).getArbeidsprosent()).isNull();
        assertThat(lagretPerioder.get(5).getDokumentasjonVurdering()).isNull();
        assertThat(lagretPerioder.get(5).getArbeidsgiver()).isNull();
        assertThat(lagretPerioder.get(5).getPeriodeKilde()).isEqualTo(FordelingPeriodeKilde.SAKSBEHANDLER);
        assertThat(lagretPerioder.get(5).getSamtidigUttaksprosent()).isEqualTo(samtidigUttak.samtidigUttaksprosent());
        assertThat(lagretPerioder.get(5).getPeriodeType()).isEqualTo(samtidigUttak.uttakPeriodeType());
        assertThat(lagretPerioder.get(5).isFlerbarnsdager()).isEqualTo(samtidigUttak.flerbarnsdager());
        assertThat(lagretPerioder.get(5).getÅrsak()).isEqualTo(Årsak.UKJENT);
    }

    @Test
    void skal_oppdatere_endringsdato_hvis_endring_før_opprinnelig_endringsdato() {
        var fødsel = LocalDate.of(2023, 1, 10);
        var søknadsperiode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(MØDREKVOTE)
            .medPeriodeKilde(SØKNAD)
            .medPeriode(fødsel.plusWeeks(4), fødsel.plusWeeks(10))
            .build();
        var lagretUendretVedtaksperiode = mødrekvote(fødsel, fødsel.plusWeeks(1).minusDays(1), TIDLIGERE_VEDTAK);
        var lagretEndretVedtaksperiode = periode(lagretUendretVedtaksperiode.tom().plusDays(1),
            lagretUendretVedtaksperiode.tom().plusWeeks(1), SAKSBEHANDLER, FELLESPERIODE);
        var originalBehandling = lagreVedtak(fødsel, lagretUendretVedtaksperiode, lagretEndretVedtaksperiode);

        var opprinneligEndringsdato = søknadsperiode.getFom();
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(søknadsperiode), true))
            .medFødselAdopsjonsdato(fødsel)
            .medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
            .medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder()
                .medOpprinneligEndringsdato(opprinneligEndringsdato)
                .build())
            .lagre(repositoryProvider);
        var lagretUendretSøknadsperiode = mødrekvote(søknadsperiode.getFom(), søknadsperiode.getTom(), SØKNAD);

        kjørOppdaterer(behandling, List.of(lagretUendretVedtaksperiode, lagretEndretVedtaksperiode, lagretUendretSøknadsperiode));
        var yfa = ytelseFordelingTjeneste.hentAggregat(behandling.getId());

        //Endrer endringsdato pga første periode som lagres starter før opprinnelig endringsdato
        assertThat(yfa.getGjeldendeEndringsdato()).isEqualTo(lagretEndretVedtaksperiode.fom());
    }

    @Test
    void skal_arve_fra_gjeldende() {
        var fødsel = LocalDate.of(2023, 1, 10);
        var søknadsperiode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(MØDREKVOTE)
            .medPeriodeKilde(SØKNAD)
            .medPeriode(fødsel.plusWeeks(4), fødsel.plusWeeks(10))
            .medMottattDato(fødsel.plusWeeks(2))
            .medTidligstMottattDato(fødsel.plusWeeks(1))
            .medDokumentasjonVurdering(new DokumentasjonVurdering(SYKDOM_ANNEN_FORELDER_GODKJENT))
            .build();
        var lagretEndretVedtaksperiode = periode(fødsel, fødsel.plusWeeks(1), SAKSBEHANDLER, MØDREKVOTE);
        var originalBehandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFødselAdopsjonsdato(fødsel)
            .lagre(repositoryProvider);

        var uttakperioder = new UttakResultatPerioderEntitet();
        var uttaksperiode = new UttakResultatPeriodeEntitet.Builder(lagretEndretVedtaksperiode.fom(), lagretEndretVedtaksperiode.tom())
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medPeriodeSoknad(new UttakResultatPeriodeSøknadEntitet.Builder()
                .medUttakPeriodeType(lagretEndretVedtaksperiode.uttakPeriodeType())
                .medMottattDato(fødsel.minusWeeks(2))
                .medTidligstMottattDato(fødsel.minusWeeks(3))
                .medDokumentasjonVurdering(new DokumentasjonVurdering(MORS_AKTIVITET_IKKE_DOKUMENTERT))
                .build())
            .build();
        new UttakResultatPeriodeAktivitetEntitet.Builder(uttaksperiode, new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.FRILANS)
            .build())
            .medArbeidsprosent(BigDecimal.ZERO)
            .medUtbetalingsgrad(Utbetalingsgrad.FULL)
            .medTrekkdager(new Trekkdager(10))
            .medTrekkonto(lagretEndretVedtaksperiode.uttakPeriodeType())
            .build();
            uttakperioder.leggTilPeriode(uttaksperiode);

        repositoryProvider.getFpUttakRepository().lagreOpprinneligUttakResultatPerioder(originalBehandling.getId(), uttakperioder);

        var opprinneligEndringsdato = søknadsperiode.getFom();
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(søknadsperiode), true))
            .medFødselAdopsjonsdato(fødsel)
            .medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
            .medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder()
                .medOpprinneligEndringsdato(opprinneligEndringsdato)
                .build())
            .lagre(repositoryProvider);
        var lagretUendretSøknadsperiode = mødrekvote(søknadsperiode.getFom(), søknadsperiode.getTom(), SØKNAD);

        kjørOppdaterer(behandling, List.of(lagretEndretVedtaksperiode, lagretUendretSøknadsperiode));
        var lagretPerioder = ytelseFordelingTjeneste.hentAggregat(behandling.getId()).getGjeldendeFordeling().getPerioder();

        assertThat(lagretPerioder).hasSize(2);

        assertThat(lagretPerioder.get(0).getFom()).isEqualTo(lagretEndretVedtaksperiode.fom());
        var periodeSøknad = uttaksperiode.getPeriodeSøknad().orElseThrow();
        assertThat(lagretPerioder.get(0).getDokumentasjonVurdering()).isNull();
        assertThat(lagretPerioder.get(0).getMottattDato()).isEqualTo(periodeSøknad.getMottattDato());
        assertThat(lagretPerioder.get(0).getTidligstMottattDato()).isEqualTo(periodeSøknad.getTidligstMottattDato());

        assertThat(lagretPerioder.get(1).getFom()).isEqualTo(søknadsperiode.getFom());
        assertThat(lagretPerioder.get(1).getDokumentasjonVurdering()).isEqualTo(søknadsperiode.getDokumentasjonVurdering());
        assertThat(lagretPerioder.get(1).getMottattDato()).isEqualTo(søknadsperiode.getMottattDato());
        assertThat(lagretPerioder.get(1).getTidligstMottattDato()).isEqualTo(søknadsperiode.getTidligstMottattDato());
    }

    private Behandling lagreVedtak(LocalDate fødsel, FaktaUttakPeriodeDto... perioder) {
        var originalBehandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFødselAdopsjonsdato(fødsel)
            .lagre(repositoryProvider);

        var uttakperioder = new UttakResultatPerioderEntitet();
        for (var faktaUttakPeriodeDto : perioder) {
            var uttaksperiode = new UttakResultatPeriodeEntitet.Builder(faktaUttakPeriodeDto.fom(), faktaUttakPeriodeDto.tom())
                .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
                .medPeriodeSoknad(new UttakResultatPeriodeSøknadEntitet.Builder()
                    .medUttakPeriodeType(faktaUttakPeriodeDto.uttakPeriodeType())
                    .build())
                .build();
            new UttakResultatPeriodeAktivitetEntitet.Builder(uttaksperiode, new UttakAktivitetEntitet.Builder()
                .medUttakArbeidType(UttakArbeidType.FRILANS)
                .build())
                .medArbeidsprosent(BigDecimal.ZERO)
                .medUtbetalingsgrad(Utbetalingsgrad.FULL)
                .medTrekkdager(new Trekkdager(10))
                .medTrekkonto(faktaUttakPeriodeDto.uttakPeriodeType())
                .build();
            uttakperioder.leggTilPeriode(uttaksperiode);
        }
        repositoryProvider.getFpUttakRepository().lagreOpprinneligUttakResultatPerioder(originalBehandling.getId(), uttakperioder);
        return originalBehandling;
    }

    @Test
    void gir_exception_hvis_det_lagres_uttak_som_starter_før_gyldig_første_uttaksdag() {
        var opprinneligFom = LocalDate.now();
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(OppgittPeriodeBuilder.ny()
                .medPeriodeType(MØDREKVOTE)
                .medPeriodeKilde(SØKNAD)
                .medPeriode(opprinneligFom, opprinneligFom.plusWeeks(5))
                .build()), true))
            .medFødselAdopsjonsdato(opprinneligFom)
            .lagre(repositoryProvider);
        var mødrekvoteDto = mødrekvote(opprinneligFom.minusWeeks(1), opprinneligFom.plusWeeks(2), SØKNAD);
        var perioder = List.of(mødrekvoteDto);
        assertThatThrownBy(() -> kjørOppdaterer(behandling, perioder)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void gir_ikke_exception_hvis_det_lagres_utsettelse_som_starter_før_gyldig_første_uttaksdag() {
        var opprinneligFom = LocalDate.now();
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(OppgittPeriodeBuilder.ny()
                .medPeriodeType(MØDREKVOTE)
                .medPeriodeKilde(SØKNAD)
                .medPeriode(opprinneligFom, opprinneligFom.plusWeeks(5))
                .build()), true))
            .medFødselAdopsjonsdato(opprinneligFom)
            .lagre(repositoryProvider);
        var mødrekvoteDto = mødrekvote(opprinneligFom, opprinneligFom.plusWeeks(2), SØKNAD);
        var utsettelseDto = new FaktaUttakPeriodeDto(opprinneligFom.minusWeeks(2), mødrekvoteDto.fom().minusDays(1),
            null, UtsettelseÅrsak.SYKDOM, null, null, null, null, null, false, null, SØKNAD, null);
        kjørOppdaterer(behandling, List.of(mødrekvoteDto, utsettelseDto));

        var lagretPerioder = ytelseFordelingTjeneste.hentAggregat(behandling.getId()).getGjeldendeFordeling().getPerioder();
        assertThat(lagretPerioder).hasSize(2);
    }

    private static FaktaUttakPeriodeDto mødrekvote(LocalDate fom, LocalDate tom, FordelingPeriodeKilde kilde) {
        return periode(fom, tom, kilde, MØDREKVOTE);
    }

    private static FaktaUttakPeriodeDto periode(LocalDate fom, LocalDate tom, FordelingPeriodeKilde kilde, UttakPeriodeType uttakPeriodeType) {
        return new FaktaUttakPeriodeDto(fom, tom, uttakPeriodeType, null, null, null, null,
            null, null, false, null, kilde, null);
    }

    private OppdateringResultat kjørOppdaterer(Behandling behandling, List<FaktaUttakPeriodeDto> perioder) {
        return faktaUttakFellesTjeneste.oppdater("begrunnelse", perioder, behandling.getId(), behandling.getFagsakId(), false);
    }
}
