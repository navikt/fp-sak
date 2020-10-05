package no.nav.foreldrepenger.domene.uttak.kontroller.fakta.uttakperioder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PeriodeUttakDokumentasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.UttakDokumentasjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class SøknadsperiodeDokumentasjonKontrollererTest {

    private static final LocalDate FOM = LocalDate.of(2018, 1, 14);
    private static final LocalDate TOM = LocalDate.of(2018, 1, 31);
    private static final LocalDate enDag = LocalDate.of(2018, 3, 15);

    @Test
    public void skal_si_at_utsettelse_pga_sykdom_trenger_bekreftelse() {
        assertThat(kontroller(arbeidstakerPeriodeMedUtsettelseSykdom()).erBekreftet()).isFalse();
        assertThat(kontroller(frilansNæringsdrivendePeriodeMedUtsettelseSykdom()).erBekreftet()).isFalse();
    }

    @Test
    public void skal_si_at_periode_er_bekreftet_når_saksbehandler_allerede_er_vurdert() {
        OppgittPeriodeBuilder vurdertPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(FOM, TOM)
            .medVurdering(UttakPeriodeVurderingType.PERIODE_OK);
        KontrollerFaktaPeriode kontrollert = kontroller(vurdertPeriode.build());

        assertThat(kontrollert.erBekreftet()).isTrue();
    }

    @Test
    public void skal_ta_med_dokumentasjonsperioder_når_saksbehandler_har_behandlet_perioden() {
        Arbeidsgiver virksomhet = arbeidsgiver("orgnr");
        OppgittPeriodeEntitet søktPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medÅrsak(UtsettelseÅrsak.SYKDOM)
            .medPeriode(enDag, enDag.plusDays(7))
            .medBegrunnelse("erstatter")
            .medVurdering(UttakPeriodeVurderingType.PERIODE_OK)
            .medArbeidsgiver(virksomhet)
            .medErArbeidstaker(true)
            .build();

        List<PeriodeUttakDokumentasjonEntitet> dokumentasjonPerioder = List.of(
            lagDokumentasjon(enDag, enDag.plusDays(1), UttakDokumentasjonType.SYK_SØKER),
            lagDokumentasjon(enDag.plusDays(4), enDag.plusDays(7), UttakDokumentasjonType.SYK_SØKER)
        );

        SøknadsperiodeDokumentasjonKontrollerer kontrollerer = new SøknadsperiodeDokumentasjonKontrollerer(dokumentasjonPerioder, null);
        KontrollerFaktaPeriode resultat = kontrollerer.kontrollerSøknadsperiode(søktPeriode);

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
        PeriodeUttakDokumentasjonEntitet periode = Mockito.mock(PeriodeUttakDokumentasjonEntitet.class);
        when(periode.getDokumentasjonType()).thenReturn(type);
        when(periode.getPeriode()).thenReturn(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        return periode;
    }

    @Test
    public void feriePeriodeFraSøknad() {
        Arbeidsgiver arbeidsgiver = arbeidsgiver("orgnr");
        OppgittPeriodeEntitet feriePeriode = OppgittPeriodeBuilder.ny()
            .medÅrsak(UtsettelseÅrsak.FERIE)
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medArbeidsgiver(arbeidsgiver)
            .medErArbeidstaker(true)
            .medPeriode(LocalDate.of(2018, 1, 13), LocalDate.of(2018, 1, 20))
            .build();

        KontrollerFaktaPeriode kontrollert = kontroller(feriePeriode);
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
        OppgittPeriodeEntitet oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriode(FOM, FOM.plusWeeks(1))
            .build();

        SøknadsperiodeDokumentasjonKontrollerer kontrollerer = new SøknadsperiodeDokumentasjonKontrollerer(List.of(), FOM.minusWeeks(5));

        KontrollerFaktaPeriode kontrollerFaktaPeriode = kontrollerer.kontrollerSøknadsperiode(oppgittPeriode);
        assertThat(kontrollerFaktaPeriode.erBekreftet()).isFalse();
        assertThat(kontrollerFaktaPeriode.isTidligOppstart()).isTrue();
    }

    @Test
    public void farEllerMedmorIkkeSøktOmTidligOppstartFellesperiodeEllerFedrekvote() {
        OppgittPeriodeEntitet oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medPeriode(FOM, FOM.plusWeeks(1))
            .build();

        SøknadsperiodeDokumentasjonKontrollerer kontrollerer = new SøknadsperiodeDokumentasjonKontrollerer(List.of(), FOM.minusWeeks(7));

        KontrollerFaktaPeriode kontrollerFaktaPeriode = kontrollerer.kontrollerSøknadsperiode(oppgittPeriode);
        assertThat(kontrollerFaktaPeriode.erBekreftet()).isTrue();
        assertThat(kontrollerFaktaPeriode.isTidligOppstart()).isFalse();
    }

    @Test
    public void farEllerMedmorSøktOmTidligOppstartForeldrepenger() {
        OppgittPeriodeEntitet oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriode(FOM, FOM.plusWeeks(1))
            .build();

        SøknadsperiodeDokumentasjonKontrollerer kontrollerer = new SøknadsperiodeDokumentasjonKontrollerer(List.of(), FOM.minusWeeks(5));

        KontrollerFaktaPeriode kontrollerFaktaPeriode = kontrollerer.kontrollerSøknadsperiode(oppgittPeriode);
        assertThat(kontrollerFaktaPeriode.erBekreftet()).isFalse();
        assertThat(kontrollerFaktaPeriode.isTidligOppstart()).isTrue();
    }

    @Test
    public void farEllerMedmorSøktOmTidligOppstartMedFlerbarnsdager() {
        OppgittPeriodeEntitet oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriode(FOM, FOM.plusWeeks(1))
            .medFlerbarnsdager(true)
            .build();

        SøknadsperiodeDokumentasjonKontrollerer kontrollerer = new SøknadsperiodeDokumentasjonKontrollerer(List.of(), FOM.minusWeeks(5));

        KontrollerFaktaPeriode kontrollerFaktaPeriode = kontrollerer.kontrollerSøknadsperiode(oppgittPeriode);
        assertThat(kontrollerFaktaPeriode.erBekreftet()).isTrue();
        assertThat(kontrollerFaktaPeriode.isTidligOppstart()).isFalse();
    }

    @Test
    public void søktOmGraderingPeriode() {
        Arbeidsgiver arbeidsgiver = arbeidsgiver("orgnr");
        OppgittPeriodeEntitet graderingPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriode(FOM, TOM)
            .medArbeidsprosent(BigDecimal.valueOf(60))
            .medArbeidsgiver(arbeidsgiver)
            .medErArbeidstaker(true)
            .build();

        KontrollerFaktaPeriode kontrollerFaktaPeriode = kontroller(graderingPeriode);
        assertThat(kontrollerFaktaPeriode.erBekreftet()).isTrue();
    }

    @Test
    public void gradertFrilansUtenVirksomhetOgHarITilleggEttArbeidsforhold() {
        BigDecimal arbeidsprosent = BigDecimal.valueOf(60);
        OppgittPeriodeEntitet graderingPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriode(FOM, TOM)
            .medArbeidsgiver(null)
            .medArbeidsprosent(arbeidsprosent)
            .medErFrilanser(true)
            .build();

        KontrollerFaktaPeriode kontrollerFaktaPeriode = kontroller(graderingPeriode);
        assertThat(kontrollerFaktaPeriode.erBekreftet()).isTrue();
    }

    @Test
    public void gradertFrilansMedVirksomhet() {
        Arbeidsgiver arbeidsgiver1 = arbeidsgiver("orgnr1");
        BigDecimal arbeidsprosent = BigDecimal.valueOf(60);
        OppgittPeriodeEntitet graderingPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriode(FOM, TOM)
            .medArbeidsgiver(arbeidsgiver1)
            .medArbeidsprosent(arbeidsprosent)
            .medErFrilanser(true)
            .build();

        KontrollerFaktaPeriode kontrollerFaktaPeriode = kontroller(graderingPeriode);
        assertThat(kontrollerFaktaPeriode.erBekreftet()).isTrue();
    }

    @Test
    public void søktOmGraderingSelvstendigNæringsdrivende() {
        OppgittPeriodeEntitet graderingPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriode(FOM, TOM)
            .medArbeidsprosent(BigDecimal.valueOf(60))
            .medErSelvstendig(true)
            .build();

        KontrollerFaktaPeriode kontrollerFaktaPeriode = kontroller(graderingPeriode);
        assertThat(kontrollerFaktaPeriode.erBekreftet()).isTrue();
    }

    @Test
    public void graderingFraSøknadOgArbeidsgiverPrivatperson() {
        var arbeidsgiver = Arbeidsgiver.person(AktørId.dummy());
        OppgittPeriodeEntitet graderingsperiode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriode(FOM, TOM)
            .medArbeidsprosent(BigDecimal.TEN)
            .medArbeidsgiver(arbeidsgiver)
            .medErArbeidstaker(true)
            .build();

        KontrollerFaktaPeriode kontrollerFaktaPeriode = kontroller(graderingsperiode);
        assertThat(kontrollerFaktaPeriode.erBekreftet()).isTrue();
    }

    @Test
    public void utsettelsePgaArbeidFraSøknadOgArbeidsgiverPrivatperson() {
        var arbeidsgiver = Arbeidsgiver.person(AktørId.dummy());
        OppgittPeriodeEntitet utsettelseArbeidPeriode = OppgittPeriodeBuilder.ny()
            .medÅrsak(UtsettelseÅrsak.ARBEID)
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriode(FOM, TOM)
            .medArbeidsgiver(arbeidsgiver)
            .medErArbeidstaker(true)
            .build();

        KontrollerFaktaPeriode kontrollerFaktaPeriode = kontroller(utsettelseArbeidPeriode);
        assertThat(kontrollerFaktaPeriode.erBekreftet()).isTrue();
    }

    @Test
    public void utsettelsePgaFerieFraSøknadOgArbeidsgiverPrivatperson() {
        var arbeidsgiver = Arbeidsgiver.person(AktørId.dummy());
        OppgittPeriodeEntitet utsettelseFeriePeriode = OppgittPeriodeBuilder.ny()
            .medÅrsak(UtsettelseÅrsak.FERIE)
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriode(FOM, TOM)
            .medArbeidsgiver(arbeidsgiver)
            .medErArbeidstaker(true)
            .build();

        KontrollerFaktaPeriode kontrollerFaktaPeriode = kontroller(utsettelseFeriePeriode);
        assertThat(kontrollerFaktaPeriode.erBekreftet()).isTrue();
    }

    @Test
    public void graderingFraSøknadForFrilansMenBrukerErArbeidstakerITillegg() {
        OppgittPeriodeEntitet graderingsperiode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(FOM, TOM)
            .medArbeidsprosent(BigDecimal.TEN)
            .medArbeidsgiver(null)
            .medErFrilanser(true)
            .build();

        KontrollerFaktaPeriode kontrollerFaktaPeriode = kontroller(graderingsperiode);
        assertThat(kontrollerFaktaPeriode.erBekreftet()).isTrue();
    }

    private KontrollerFaktaPeriode kontroller(OppgittPeriodeEntitet oppgittPeriode) {
        SøknadsperiodeDokumentasjonKontrollerer kontrollerer = new SøknadsperiodeDokumentasjonKontrollerer(List.of(), null);
        return kontrollerer.kontrollerSøknadsperiode(oppgittPeriode);
    }

    private OppgittPeriodeEntitet arbeidstakerPeriodeMedUtsettelseSykdom() {
        return arbeidstakerPeriodeMedUtsettelse(UtsettelseÅrsak.SYKDOM);
    }

    private OppgittPeriodeEntitet frilansNæringsdrivendePeriodeMedUtsettelseSykdom() {
        return frilansNæringsdrivendePeriodeMedUtsettelse(UtsettelseÅrsak.SYKDOM);
    }

    private OppgittPeriodeEntitet arbeidstakerPeriodeMedUtsettelse(UtsettelseÅrsak årsak) {
        return periodeMedUtsettelse(true, arbeidsgiver(), årsak);
    }

    private OppgittPeriodeEntitet frilansNæringsdrivendePeriodeMedUtsettelse(UtsettelseÅrsak utsettelseÅrsak) {
        return periodeMedUtsettelse(false, null, utsettelseÅrsak);
    }

    private OppgittPeriodeEntitet periodeMedUtsettelse(Boolean arbeidstaker, Arbeidsgiver arbeidsgiver, UtsettelseÅrsak utsettelseÅrsak) {
        return OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medErArbeidstaker(arbeidstaker)
            .medArbeidsgiver(arbeidsgiver)
            .medÅrsak(utsettelseÅrsak).medPeriode(enDag, enDag).build();
    }
}
