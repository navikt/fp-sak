package no.nav.foreldrepenger.domene.uttak.fakta.uttakperioder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.behandling.pleiepenger.PleiepengerInnleggelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PeriodeUttakDokumentasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.UttakDokumentasjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class SøknadsperiodeDokKontrollererSammenhengendeUttakTest {

    private static final LocalDate FOM = LocalDate.of(2018, 1, 14);
    private static final LocalDate TOM = LocalDate.of(2018, 1, 31);
    private static final LocalDate enDag = LocalDate.of(2018, 3, 15);

    @Test
    public void skal_si_at_utsettelse_pga_sykdom_trenger_bekreftelse() {
        assertThat(kontroller(periodeMedUtsettelse(UtsettelseÅrsak.SYKDOM)).erBekreftet()).isFalse();
    }

    @Test
    public void skal_si_at_periode_er_bekreftet_når_saksbehandler_allerede_er_vurdert() {
        var vurdertPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(FOM, TOM)
            .medVurdering(UttakPeriodeVurderingType.PERIODE_OK);
        var kontrollert = kontroller(vurdertPeriode.build());

        assertThat(kontrollert.erBekreftet()).isTrue();
    }

    @Test
    public void skal_ta_med_dokumentasjonsperioder_når_saksbehandler_har_behandlet_perioden() {
        var virksomhet = arbeidsgiver("orgnr");
        var søktPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medÅrsak(UtsettelseÅrsak.SYKDOM)
            .medPeriode(enDag, enDag.plusDays(7))
            .medBegrunnelse("erstatter")
            .medVurdering(UttakPeriodeVurderingType.PERIODE_OK)
            .medArbeidsgiver(virksomhet)
            .medGraderingAktivitetType(GraderingAktivitetType.ARBEID)
            .build();

        var dokumentasjonPerioder = List.of(
            lagDokumentasjon(enDag, enDag.plusDays(1), UttakDokumentasjonType.SYK_SØKER),
            lagDokumentasjon(enDag.plusDays(4), enDag.plusDays(7), UttakDokumentasjonType.SYK_SØKER)
        );

        var kontrollerer = new SøknadsperiodeDokKontrollerer(dokumentasjonPerioder, null,
                new UtsettelseDokKontrollererSammenhengendeUttak());
        var resultat = kontrollerer.kontrollerSøknadsperiode(søktPeriode);

        assertThat(resultat.getOppgittPeriode().getBegrunnelse().get()).isEqualTo("erstatter");
        assertThat(resultat.erBekreftet()).isTrue();
        assertThat(resultat.getDokumentertePerioder()).hasSize(2);
        assertThat(resultat.getDokumentertePerioder().get(0).getDokumentasjonType()).isEqualTo(UttakDokumentasjonType.SYK_SØKER);
        assertThat(resultat.getDokumentertePerioder().get(0).getPeriode().getFomDato()).isEqualTo(enDag);
        assertThat(resultat.getDokumentertePerioder().get(0).getPeriode().getTomDato()).isEqualTo(enDag.plusDays(1));
        assertThat(resultat.getDokumentertePerioder().get(1).getDokumentasjonType()).isEqualTo(UttakDokumentasjonType.SYK_SØKER);
        assertThat(resultat.getDokumentertePerioder().get(1).getPeriode().getFomDato()).isEqualTo(enDag.plusDays(4));
        assertThat(resultat.getDokumentertePerioder().get(1).getPeriode().getTomDato()).isEqualTo(enDag.plusDays(7));
    }

    private PeriodeUttakDokumentasjonEntitet lagDokumentasjon(LocalDate fom, LocalDate tom, UttakDokumentasjonType type) {
        var periode = Mockito.mock(PeriodeUttakDokumentasjonEntitet.class);
        when(periode.getDokumentasjonType()).thenReturn(type);
        when(periode.getPeriode()).thenReturn(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        return periode;
    }

    @Test
    public void feriePeriodeFraSøknad() {
        var arbeidsgiver = arbeidsgiver("orgnr");
        var feriePeriode = OppgittPeriodeBuilder.ny()
            .medÅrsak(UtsettelseÅrsak.FERIE)
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medArbeidsgiver(arbeidsgiver)
            .medGraderingAktivitetType(GraderingAktivitetType.ARBEID)
            .medPeriode(LocalDate.of(2018, 1, 13), LocalDate.of(2018, 1, 20))
            .build();

        var kontrollert = kontroller(feriePeriode);
        assertThat(kontrollert.erBekreftet()).isTrue();
    }

    private Arbeidsgiver arbeidsgiver(String virksomhetId) {
        return Arbeidsgiver.virksomhet(virksomhetId);
    }

    private Arbeidsgiver arbeidsgiver() {
        return arbeidsgiver(UUID.randomUUID().toString());
    }

    @Test
    public void farEllerMedmorSøktOmTidligOppstartFellesperiodeEllerFedrekvote() {
        var oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriode(FOM, FOM.plusWeeks(1))
            .build();

        var kontrollerer = new SøknadsperiodeDokKontrollerer(List.of(), FOM.minusWeeks(5),
            new UtsettelseDokKontrollererSammenhengendeUttak());

        var kontrollerFaktaPeriode = kontrollerer.kontrollerSøknadsperiode(oppgittPeriode);
        assertThat(kontrollerFaktaPeriode.erBekreftet()).isFalse();
        assertThat(kontrollerFaktaPeriode.isTidligOppstart()).isTrue();
    }

    @Test
    public void farEllerMedmorIkkeSøktOmTidligOppstartFellesperiodeEllerFedrekvote() {
        var oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medPeriode(FOM, FOM.plusWeeks(1))
            .build();

        var kontrollerer = new SøknadsperiodeDokKontrollerer(List.of(), FOM.minusWeeks(7),
            new UtsettelseDokKontrollererSammenhengendeUttak());

        var kontrollerFaktaPeriode = kontrollerer.kontrollerSøknadsperiode(oppgittPeriode);
        assertThat(kontrollerFaktaPeriode.erBekreftet()).isTrue();
        assertThat(kontrollerFaktaPeriode.isTidligOppstart()).isFalse();
    }

    @Test
    public void farEllerMedmorSøktOmTidligOppstartForeldrepenger() {
        var oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriode(FOM, FOM.plusWeeks(1))
            .build();

        var kontrollerer = new SøknadsperiodeDokKontrollerer(List.of(), FOM.minusWeeks(5),
            new UtsettelseDokKontrollererSammenhengendeUttak());

        var kontrollerFaktaPeriode = kontrollerer.kontrollerSøknadsperiode(oppgittPeriode);
        assertThat(kontrollerFaktaPeriode.erBekreftet()).isFalse();
        assertThat(kontrollerFaktaPeriode.isTidligOppstart()).isTrue();
    }

    @Test
    public void farEllerMedmorSøktOmTidligOppstartMedFlerbarnsdager() {
        var oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriode(FOM, FOM.plusWeeks(1))
            .medFlerbarnsdager(true)
            .build();

        var kontrollerer = new SøknadsperiodeDokKontrollerer(List.of(), FOM.minusWeeks(5),
            new UtsettelseDokKontrollererSammenhengendeUttak());

        var kontrollerFaktaPeriode = kontrollerer.kontrollerSøknadsperiode(oppgittPeriode);
        assertThat(kontrollerFaktaPeriode.erBekreftet()).isTrue();
        assertThat(kontrollerFaktaPeriode.isTidligOppstart()).isFalse();
    }

    @Test
    public void søktOmGraderingPeriode() {
        var arbeidsgiver = arbeidsgiver("orgnr");
        var graderingPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriode(FOM, TOM)
            .medArbeidsprosent(BigDecimal.valueOf(60))
            .medArbeidsgiver(arbeidsgiver)
            .medGraderingAktivitetType(GraderingAktivitetType.ARBEID)
            .build();

        var kontrollerFaktaPeriode = kontroller(graderingPeriode);
        assertThat(kontrollerFaktaPeriode.erBekreftet()).isTrue();
    }

    @Test
    public void gradertFrilansUtenVirksomhetOgHarITilleggEttArbeidsforhold() {
        var arbeidsprosent = BigDecimal.valueOf(60);
        var graderingPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriode(FOM, TOM)
            .medArbeidsgiver(null)
            .medArbeidsprosent(arbeidsprosent)
            .medGraderingAktivitetType(GraderingAktivitetType.FRILANS)
            .build();

        var kontrollerFaktaPeriode = kontroller(graderingPeriode);
        assertThat(kontrollerFaktaPeriode.erBekreftet()).isTrue();
    }

    @Test
    public void gradertFrilansMedVirksomhet() {
        var arbeidsgiver1 = arbeidsgiver("orgnr1");
        var arbeidsprosent = BigDecimal.valueOf(60);
        var graderingPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriode(FOM, TOM)
            .medArbeidsgiver(arbeidsgiver1)
            .medArbeidsprosent(arbeidsprosent)
            .medGraderingAktivitetType(GraderingAktivitetType.FRILANS)
            .build();

        var kontrollerFaktaPeriode = kontroller(graderingPeriode);
        assertThat(kontrollerFaktaPeriode.erBekreftet()).isTrue();
    }

    @Test
    public void søktOmGraderingSelvstendigNæringsdrivende() {
        var graderingPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriode(FOM, TOM)
            .medArbeidsprosent(BigDecimal.valueOf(60))
            .medGraderingAktivitetType(GraderingAktivitetType.SELVSTENDIG_NÆRINGSDRIVENDE)
            .build();

        var kontrollerFaktaPeriode = kontroller(graderingPeriode);
        assertThat(kontrollerFaktaPeriode.erBekreftet()).isTrue();
    }

    @Test
    public void graderingFraSøknadOgArbeidsgiverPrivatperson() {
        var arbeidsgiver = Arbeidsgiver.person(AktørId.dummy());
        var graderingsperiode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriode(FOM, TOM)
            .medArbeidsprosent(BigDecimal.TEN)
            .medArbeidsgiver(arbeidsgiver)
            .medGraderingAktivitetType(GraderingAktivitetType.ARBEID)
            .build();

        var kontrollerFaktaPeriode = kontroller(graderingsperiode);
        assertThat(kontrollerFaktaPeriode.erBekreftet()).isTrue();
    }

    @Test
    public void utsettelsePgaArbeidFraSøknadOgArbeidsgiverPrivatperson() {
        var arbeidsgiver = Arbeidsgiver.person(AktørId.dummy());
        var utsettelseArbeidPeriode = OppgittPeriodeBuilder.ny()
            .medÅrsak(UtsettelseÅrsak.ARBEID)
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriode(FOM, TOM)
            .medArbeidsgiver(arbeidsgiver)
            .medGraderingAktivitetType(GraderingAktivitetType.ARBEID)
            .build();

        var kontrollerFaktaPeriode = kontroller(utsettelseArbeidPeriode);
        assertThat(kontrollerFaktaPeriode.erBekreftet()).isTrue();
    }

    @Test
    public void utsettelsePgaFerieFraSøknadOgArbeidsgiverPrivatperson() {
        var arbeidsgiver = Arbeidsgiver.person(AktørId.dummy());
        var utsettelseFeriePeriode = OppgittPeriodeBuilder.ny()
            .medÅrsak(UtsettelseÅrsak.FERIE)
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriode(FOM, TOM)
            .medArbeidsgiver(arbeidsgiver)
            .medGraderingAktivitetType(GraderingAktivitetType.ARBEID)
            .build();

        var kontrollerFaktaPeriode = kontroller(utsettelseFeriePeriode);
        assertThat(kontrollerFaktaPeriode.erBekreftet()).isTrue();
    }

    @Test
    public void graderingFraSøknadForFrilansMenBrukerErArbeidstakerITillegg() {
        var graderingsperiode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(FOM, TOM)
            .medArbeidsprosent(BigDecimal.TEN)
            .medArbeidsgiver(null)
            .medGraderingAktivitetType(GraderingAktivitetType.FRILANS)
            .build();

        var kontrollerFaktaPeriode = kontroller(graderingsperiode);
        assertThat(kontrollerFaktaPeriode.erBekreftet()).isTrue();
    }

    @Test
    public void skal_si_at_periode_er_bekreftet_når_utsettelse_pga_innlagt_barn_og_vedtak_om_pleiepenger_med_innleggelse() {
        var vurdertPeriode = OppgittPeriodeBuilder.ny()
            .medÅrsak(UtsettelseÅrsak.INSTITUSJON_BARN)
            .medPeriode(FOM, TOM);
        var kontrollerer = new SøknadsperiodeDokKontrollerer(List.of(), null,
            new UtsettelseDokKontrollererSammenhengendeUttak(), List.of(new PleiepengerInnleggelseEntitet.Builder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM)).build()), Optional.empty());
        var resultat = kontrollerer.kontrollerSøknadsperiode(vurdertPeriode.build());

        assertThat(resultat.erBekreftet()).isTrue();
    }

    private KontrollerFaktaPeriode kontroller(OppgittPeriodeEntitet oppgittPeriode) {
        var kontrollerer = new SøknadsperiodeDokKontrollerer(List.of(), null,
            new UtsettelseDokKontrollererSammenhengendeUttak());
        return kontrollerer.kontrollerSøknadsperiode(oppgittPeriode);
    }

    private OppgittPeriodeEntitet periodeMedUtsettelse(UtsettelseÅrsak utsettelseÅrsak) {
        return OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medÅrsak(utsettelseÅrsak)
            .medPeriode(enDag, enDag).build();
    }
}
