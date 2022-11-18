package no.nav.foreldrepenger.domene.uttak.fakta.aktkrav;


import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.KONTROLLER_AKTIVITETSKRAV;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.FELLESPERIODE;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.FORELDREPENGER;
import static no.nav.foreldrepenger.domene.uttak.fakta.aktkrav.KontrollerAktivitetskravAksjonspunktUtleder.skalKontrollereAktivitetskrav;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AktivitetskravPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.KontrollerAktivitetskravAvklaring;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.input.Annenpart;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;

public class KontrollerAktivitetskravAksjonspunktUtlederTest {

    private final UttakRepositoryStubProvider repositoryProvider = new UttakRepositoryStubProvider();
    private FpUttakRepository fpUttakRepository = repositoryProvider.getFpUttakRepository();
    private final YtelseFordelingTjeneste ytelseFordelingTjeneste = new YtelseFordelingTjeneste(
        repositoryProvider.getYtelsesFordelingRepository());
    private final KontrollerAktivitetskravAksjonspunktUtleder utleder = new KontrollerAktivitetskravAksjonspunktUtleder(
        ytelseFordelingTjeneste, new ForeldrepengerUttakTjeneste(fpUttakRepository));

    @Test
    public void utledeAPForFarSomHarSøktFellesperiode() {
        var fødselsdato = LocalDate.of(2020, 1, 1);
        var søknadsperiode = fellesperiode(fødselsdato);
        var behandlingReferanse = farBehandling(søknadsperiode);
        var familieHendelse = FamilieHendelse.forFødsel(fødselsdato, fødselsdato, List.of(), 1);
        var uttakInput = uttakInput(behandlingReferanse, familieHendelse);
        var ap = utleder.utledFor(uttakInput);

        assertThat(ap).containsOnly(KONTROLLER_AKTIVITETSKRAV);
    }

    @Test
    public void ikkeUtledeAPForFarSomHarSøktFellesperiodeMedFlerbarnsdager() {
        var fødselsdato = LocalDate.of(2020, 1, 1);
        var søknadsperiode = OppgittPeriodeBuilder.ny()
            .medMorsAktivitet(MorsAktivitet.ARBEID)
            .medPeriodeType(FELLESPERIODE)
            .medPeriode(fødselsdato, fødselsdato.plusWeeks(10))
            .medFlerbarnsdager(true)
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .build();
        var behandlingReferanse = farBehandling(søknadsperiode);
        var familieHendelse = FamilieHendelse.forFødsel(fødselsdato, fødselsdato, List.of(), 1);
        var uttakInput = uttakInput(behandlingReferanse, familieHendelse);
        var ap = utleder.utledFor(uttakInput);

        assertThat(ap).isEmpty();
    }

    // @Test - TODO (JOL) re-enable eller remove avhengig av volum
    public void ikkeUtledeAPForFarSomHarSøktFellesperiodeMensMorTarUtFullKvote() {
        var fødselsdato = LocalDate.of(2020, 1, 1);
        var søknadsperiode = OppgittPeriodeBuilder.ny()
            .medMorsAktivitet(MorsAktivitet.ARBEID)
            .medPeriodeType(FELLESPERIODE)
            .medPeriode(fødselsdato.plusWeeks(6), fødselsdato.plusWeeks(10))
            .medSamtidigUttak(true)
            .medSamtidigUttaksprosent(new SamtidigUttaksprosent(40))
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .build();
        var behandlingReferanse = farBehandling(søknadsperiode);
        var familieHendelse = FamilieHendelse.forFødsel(fødselsdato, fødselsdato, List.of(), 1);
        var uttakInput = uttakInputMedAnnenpart(behandlingReferanse, familieHendelse, 321L);
        var virksomhetForMor = Arbeidsgiver.virksomhet("123");
        var uttakAktivitetForMor = new UttakAktivitetEntitet.Builder()
            .medArbeidsforhold(virksomhetForMor, InternArbeidsforholdRef.nyRef())
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .build();
        var periode = new UttakResultatPeriodeEntitet.Builder(fødselsdato, fødselsdato.plusWeeks(15).minusDays(1))
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .build();

        var aktivitet = new UttakResultatPeriodeAktivitetEntitet.Builder(periode, uttakAktivitetForMor)
            .medTrekkdager(new Trekkdager(75))
            .medTrekkonto(StønadskontoType.MØDREKVOTE)
            .medArbeidsprosent(BigDecimal.ZERO)
            .medUtbetalingsgrad(new Utbetalingsgrad(100))
            .build();
        periode.leggTilAktivitet(aktivitet);
        var uttakResultatPerioderForMor = new UttakResultatPerioderEntitet();
        uttakResultatPerioderForMor.leggTilPeriode(periode);
        repositoryProvider.getBehandlingsresultatRepository().lagre(321L,  Behandlingsresultat.builder()
            .medBehandlingResultatType(BehandlingResultatType.INNVILGET).build());
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(321L, uttakResultatPerioderForMor);

        var ap = utleder.utledFor(uttakInput);

        assertThat(ap).isEmpty();
    }

    @Test
    public void ikkeUtledeAPForFarSomHarSøktFellesperiodeMenUfør() {
        var fødselsdato = LocalDate.of(2020, 1, 1);
        var søknadsperiode = OppgittPeriodeBuilder.ny()
            .medPeriode(fødselsdato.plusWeeks(10), fødselsdato.plusWeeks(15))
            .medPeriodeType(FELLESPERIODE)
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medMorsAktivitet(MorsAktivitet.UFØRE)
            .build();
        var behandlingReferanse = farBehandling(søknadsperiode);
        var familieHendelse = FamilieHendelse.forFødsel(fødselsdato, fødselsdato, List.of(), 1);
        var uttakInput = uttakInput(behandlingReferanse, familieHendelse);
        var ap = utleder.utledFor(uttakInput);

        assertThat(ap).isEmpty();
    }

    @Test
    public void utledeAPForFarSomHarSøktForeldrepenger() {
        var fødselsdato = LocalDate.of(2020, 1, 1);
        var søknadsperiode = foreldrepenger(fødselsdato, fødselsdato.plusWeeks(10));
        var behandlingReferanse = farBehandling(søknadsperiode);
        var familieHendelse = FamilieHendelse.forFødsel(fødselsdato, fødselsdato, List.of(), 1);
        var uttakInput = uttakInput(behandlingReferanse, familieHendelse);
        var ap = utleder.utledFor(uttakInput);

        assertThat(ap).containsOnly(KONTROLLER_AKTIVITETSKRAV);
    }

    @Test
    public void ikkeUtledeAPForFarSomHarSøktUtsettelseHvisBeggeRett() {
        var fødselsdato = LocalDate.of(2020, 1, 1);
        var søknadsperiode = OppgittPeriodeBuilder.ny()
            .medÅrsak(UtsettelseÅrsak.ARBEID)
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fødselsdato, fødselsdato.plusWeeks(10))
            .build();
        var behandlingReferanse = farBehandling(søknadsperiode);
        var familieHendelse = FamilieHendelse.forFødsel(fødselsdato, fødselsdato, List.of(), 1);
        var uttakInput = uttakInput(behandlingReferanse, familieHendelse);
        var ap = utleder.utledFor(uttakInput);

        assertThat(ap).isEmpty();
    }

    @Test
    public void utledeAPForFarSomHarSøktUtsettelseFerieOgBareFarRett() {
        var uttakInput = bareFarRettMedSøktUtsettelse(UtsettelseÅrsak.FERIE);
        var ap = utleder.utledFor(uttakInput);

        assertThat(ap).containsOnly(KONTROLLER_AKTIVITETSKRAV);
    }

    @Test
    public void utledeAPForFarSomHarSøktUtsettelseArbeidOgBareFarRett() {
        var uttakInput = bareFarRettMedSøktUtsettelse(UtsettelseÅrsak.ARBEID);
        var ap = utleder.utledFor(uttakInput);

        assertThat(ap).containsOnly(KONTROLLER_AKTIVITETSKRAV);
    }

    @Test
    public void utledeAPForFarSomHarSøktUtsettelseSykdomOgBareFarRett() {
        var uttakInput = bareFarRettMedSøktUtsettelse(UtsettelseÅrsak.SYKDOM);
        var ap = utleder.utledFor(uttakInput);

        assertThat(ap).containsOnly(KONTROLLER_AKTIVITETSKRAV);
    }

    @Test
    public void utledeAPForFarSomHarSøktUtsettelseInnleggelseOgBareFarRett() {
        var uttakInput = bareFarRettMedSøktUtsettelse(UtsettelseÅrsak.INSTITUSJON_SØKER);
        var ap = utleder.utledFor(uttakInput);

        assertThat(ap).containsOnly(KONTROLLER_AKTIVITETSKRAV);
    }

    @Test
    public void utledeAPForFarSomHarSøktUtsettelseInnleggelseBarnOgBareFarRett() {
        var uttakInput = bareFarRettMedSøktUtsettelse(UtsettelseÅrsak.INSTITUSJON_BARN);
        var ap = utleder.utledFor(uttakInput);

        assertThat(ap).containsOnly(KONTROLLER_AKTIVITETSKRAV);
    }

    @Test
    public void ikkeUtledeAPForFarSomHarSøktUtsettelseFriOgBareFarRettUdefinertAktivitet() {
        var uttakInput = bareFarRettMedSøktUtsettelse(UtsettelseÅrsak.FRI);
        var ap = utleder.utledFor(uttakInput);

        assertThat(ap).isEmpty();
    }

    @Test
    public void utledeAPForFarSomHarSøktUtsettelseFriOgBareFarRettUføre() {
        var uttakInput = bareFarRettMedSøktUtsettelse(UtsettelseÅrsak.FRI, MorsAktivitet.UFØRE);
        var ap = utleder.utledFor(uttakInput);

        assertThat(ap).isEmpty();
    }

    @Test
    public void utledeAPForFarSomHarSøktUtsettelseFriOgBareFarRettUtenAktivitet() {
        var uttakInput = bareFarRettMedSøktUtsettelse(UtsettelseÅrsak.FRI, MorsAktivitet.IKKE_OPPGITT);
        var ap = utleder.utledFor(uttakInput);

        assertThat(ap).isEmpty();
    }

    @Test
    public void utledeAPForFarSomHarSøktUtsettelseFriOgBareFarRettMedAktivitet() {
        var uttakInput = bareFarRettMedSøktUtsettelse(UtsettelseÅrsak.FRI, MorsAktivitet.UTDANNING);
        var ap = utleder.utledFor(uttakInput);

        assertThat(ap).containsOnly(KONTROLLER_AKTIVITETSKRAV);
    }

    @Test
    public void ikkeUtledeAPForFarSomHarSøktUtsettelseHvOgBareFarRett() {
        var uttakInput = bareFarRettMedSøktUtsettelse(UtsettelseÅrsak.HV_OVELSE);
        var ap = utleder.utledFor(uttakInput);

        assertThat(ap).isEmpty();
    }

    @Test
    public void ikkeUtledeAPForFarSomHarSøktUtsettelseNavTiltakOgBareFarRett() {
        var uttakInput = bareFarRettMedSøktUtsettelse(UtsettelseÅrsak.NAV_TILTAK);
        var ap = utleder.utledFor(uttakInput);

        assertThat(ap).isEmpty();
    }

    private UttakInput bareFarRettMedSøktUtsettelse(UtsettelseÅrsak utsettelseÅrsak) {
        return bareFarRettMedSøktUtsettelse(utsettelseÅrsak, MorsAktivitet.UDEFINERT);
    }

    private UttakInput bareFarRettMedSøktUtsettelse(UtsettelseÅrsak utsettelseÅrsak, MorsAktivitet morsAktivitet) {
        var fødselsdato = LocalDate.of(2020, 1, 1);
        var søknadsperiode = OppgittPeriodeBuilder.ny()
            .medÅrsak(utsettelseÅrsak)
            .medMorsAktivitet(morsAktivitet)
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fødselsdato, fødselsdato.plusWeeks(10))
            .build();
        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(new OppgittRettighetEntitet(false, false, false, false))
            .medFordeling(new OppgittFordelingEntitet(List.of(søknadsperiode), true))
            .medAvklarteUttakDatoer(
                new AvklarteUttakDatoerEntitet.Builder().medOpprinneligEndringsdato(søknadsperiode.getFom()).build())
            .lagre(repositoryProvider);
        var familieHendelse = FamilieHendelse.forFødsel(fødselsdato, fødselsdato, List.of(), 1);
        return uttakInput(BehandlingReferanse.fra(behandling), familieHendelse);
    }

    @Test
    public void ikkeUtledeAPForMorSomHarSøktFellesperiode() {
        var fødselsdato = LocalDate.of(2020, 1, 1);
        var søknadsperiode = fellesperiode(fødselsdato);
        var behandlingReferanse = morBehandling(søknadsperiode);
        var familieHendelse = FamilieHendelse.forFødsel(fødselsdato, fødselsdato, List.of(), 1);
        var uttakInput = uttakInput(behandlingReferanse, familieHendelse);
        var ap = utleder.utledFor(uttakInput);

        assertThat(ap).isEmpty();
    }

    @Test
    public void ikkeUtledeAPForAleneomsorgFarSomHarSøktFellesperiode() {
        var fødselsdato = LocalDate.of(2020, 1, 1);
        var søknadsperiode = fellesperiode(fødselsdato);
        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(aleneomsorg())
            .medFordeling(new OppgittFordelingEntitet(List.of(søknadsperiode), true))
            .lagre(repositoryProvider);
        var behandlingReferanse = BehandlingReferanse.fra(behandling);
        var familieHendelse = FamilieHendelse.forFødsel(fødselsdato, fødselsdato, List.of(), 1);
        var uttakInput = uttakInput(behandlingReferanse, familieHendelse);
        var ap = utleder.utledFor(uttakInput);

        assertThat(ap).isEmpty();
    }

    @Test
    public void ikkeUtledeAPForStebarnsadopsjon() {
        var omsorgsovertakelse = LocalDate.of(2020, 1, 1);
        var søknadsperiode = fellesperiode(omsorgsovertakelse);
        var behandlingReferanse = farBehandling(søknadsperiode);
        var familieHendelse = FamilieHendelse.forAdopsjonOmsorgsovertakelse(omsorgsovertakelse, List.of(), 1,
            omsorgsovertakelse, true);
        var uttakInput = uttakInput(behandlingReferanse, familieHendelse);
        var ap = utleder.utledFor(uttakInput);

        assertThat(ap).isEmpty();
    }

    @Test
    public void ikkeUtledeAPForFarSomHarSøktFellesperiodeHvisAvklartAvSaksbehandler() {
        var fødselsdato = LocalDate.of(2020, 1, 1);
        var søknadsperiode = fellesperiode(fødselsdato);
        var avklartPeriode = new AktivitetskravPeriodeEntitet(søknadsperiode.getFom(), søknadsperiode.getTom(),
            KontrollerAktivitetskravAvklaring.I_AKTIVITET, "begrunnelse");
        ;
        var behandlingReferanse = farBehandling(søknadsperiode, avklartPeriode);
        var familieHendelse = FamilieHendelse.forFødsel(fødselsdato, fødselsdato, List.of(), 1);
        var uttakInput = uttakInput(behandlingReferanse, familieHendelse);
        var ap = utleder.utledFor(uttakInput);

        assertThat(ap).isEmpty();
    }

    @Test
    public void ignorererHelg() {
        //Mandag
        var fødselsdato = LocalDate.of(2020, 11, 30);
        //Slutter søndag
        var søknadsperiode = fellesperiode(fødselsdato, LocalDate.of(2020, 12, 13));
        //Dokumentert fom fredag (altså før søknadsperiode sluttdato)
        var avklartPeriode = new AktivitetskravPeriodeEntitet(søknadsperiode.getFom(), LocalDate.of(2020, 12, 11),
            KontrollerAktivitetskravAvklaring.I_AKTIVITET, "begrunnelse");
        var behandlingReferanse = farBehandling(søknadsperiode, avklartPeriode);
        var familieHendelse = FamilieHendelse.forFødsel(fødselsdato, fødselsdato, List.of(), 1);
        var uttakInput = uttakInput(behandlingReferanse, familieHendelse);
        var ap = utleder.utledFor(uttakInput);

        assertThat(ap).isEmpty();
    }

    @Test
    public void utledeAPForFarSomHarSøktFellesperiodeHvisBareDelvisAvklartAvSaksbehandler() {
        var fødselsdato = LocalDate.of(2020, 1, 1);
        var søknadsperiode = fellesperiode(fødselsdato);
        var avklartPeriode = new AktivitetskravPeriodeEntitet(søknadsperiode.getFom(),
            søknadsperiode.getTom().minusWeeks(2), KontrollerAktivitetskravAvklaring.I_AKTIVITET, "begrunnelse");
        var behandlingReferanse = farBehandling(søknadsperiode, avklartPeriode);
        var familieHendelse = FamilieHendelse.forFødsel(fødselsdato, fødselsdato, List.of(), 1);
        var uttakInput = uttakInput(behandlingReferanse, familieHendelse);
        var ap = utleder.utledFor(uttakInput);

        assertThat(ap).containsOnly(KONTROLLER_AKTIVITETSKRAV);
    }

    @Test
    public void ikkeUtledeAPForFarSomHarSøktFellesperiodeHvisAvklartPeriodeOmslutterSøknadsperiode() {
        var fødselsdato = LocalDate.of(2020, 1, 1);
        var søknadsperiode = fellesperiode(fødselsdato);
        var avklartPeriode = new AktivitetskravPeriodeEntitet(søknadsperiode.getFom().minusWeeks(2),
            søknadsperiode.getTom().plusWeeks(2), KontrollerAktivitetskravAvklaring.I_AKTIVITET, "begrunnelse");
        var behandlingReferanse = farBehandling(søknadsperiode, avklartPeriode);
        var familieHendelse = FamilieHendelse.forFødsel(fødselsdato, fødselsdato, List.of(), 1);
        var uttakInput = uttakInput(behandlingReferanse, familieHendelse);
        var ap = utleder.utledFor(uttakInput);

        assertThat(ap).isEmpty();
    }

    @Test
    public void ikkeUtledeAPForFarSomHarSøktFellesperiodeHvisAvklartPeriodeOmslutterToSøknadsperioder() {
        var fødselsdato = LocalDate.of(2020, 1, 1);
        var søknadsperiode1 = fellesperiode(fødselsdato, fødselsdato.plusWeeks(10));
        var søknadsperiode2 = fellesperiode(søknadsperiode1.getTom().plusDays(1),
            søknadsperiode1.getTom().plusWeeks(5));
        var avklartPeriode = new AktivitetskravPeriodeEntitet(søknadsperiode1.getFom(), søknadsperiode2.getTom(),
            KontrollerAktivitetskravAvklaring.IKKE_I_AKTIVITET_IKKE_DOKUMENTERT, "begrunnelse");
        var behandlingReferanse = farBehandling(List.of(søknadsperiode1, søknadsperiode2), avklartPeriode);
        var familieHendelse = FamilieHendelse.forFødsel(fødselsdato, fødselsdato, List.of(), 1);
        var uttakInput = uttakInput(behandlingReferanse, familieHendelse);
        var ap = utleder.utledFor(uttakInput);

        assertThat(ap).isEmpty();
    }

    @Test
    public void ikkeUtledeAPForFarSomHarSøktFellesperiodeHvisFlereAvklartePerioderDekkerSøknadsperioden() {
        var fødselsdato = LocalDate.of(2020, 1, 1);
        var søknadsperiode = fellesperiode(fødselsdato);
        var avklartPeriode1 = new AktivitetskravPeriodeEntitet(søknadsperiode.getFom().minusWeeks(1),
            søknadsperiode.getFom().plusWeeks(2), KontrollerAktivitetskravAvklaring.I_AKTIVITET, "ok.");
        var avklartPeriode2 = new AktivitetskravPeriodeEntitet(
            avklartPeriode1.getTidsperiode().getTomDato().plusDays(1), søknadsperiode.getTom(),
            KontrollerAktivitetskravAvklaring.IKKE_I_AKTIVITET_IKKE_DOKUMENTERT,
            "Ikke i aktivitet siste del av periode");
        var behandlingReferanse = farBehandling(søknadsperiode, avklartPeriode1, avklartPeriode2);
        var familieHendelse = FamilieHendelse.forFødsel(fødselsdato, fødselsdato, List.of(), 1);
        var uttakInput = uttakInput(behandlingReferanse, familieHendelse);
        var ap = utleder.utledFor(uttakInput);

        assertThat(ap).isEmpty();
    }

    @Test
    public void ikkeUtledeHvisPeriodeUtenVirkedager() {
        var fødselsdato = LocalDate.of(2020, 12, 19);
        var søknadsperiode = fellesperiode(fødselsdato, fødselsdato);
        var behandlingReferanse = farBehandling(søknadsperiode);
        var familieHendelse = FamilieHendelse.forFødsel(fødselsdato, fødselsdato, List.of(), 1);
        var uttakInput = uttakInput(behandlingReferanse, familieHendelse);
        var ap = utleder.utledFor(uttakInput);

        assertThat(ap).isEmpty();
    }

    @Test
    public void skalMåtteAvklareAllePerioderEtterEndringsdato() {
        var fødselsdato = LocalDate.of(2020, 1, 1);
        var fellesperiode1 = fellesperiode(fødselsdato);
        var fellesperiode2 = fellesperiode(fellesperiode1.getTom().plusDays(1));
        var fellesperiode3 = fellesperiode(fellesperiode2.getTom().plusDays(1));

        var førstegangsbehandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        var avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder().medOpprinneligEndringsdato(
            fellesperiode2.getFom()).build();
        var avklartPeriode1 = new AktivitetskravPeriodeEntitet(fellesperiode1.getFom(), fellesperiode2.getTom(),
            KontrollerAktivitetskravAvklaring.IKKE_I_AKTIVITET_DOKUMENTERT, "begrunnelse");
        var avklartPeriode2 = new AktivitetskravPeriodeEntitet(fellesperiode3.getFom(), fellesperiode3.getTom(),
            KontrollerAktivitetskravAvklaring.IKKE_I_AKTIVITET_DOKUMENTERT, "begrunnelse");
        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOriginalBehandling(førstegangsbehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
            .medFordeling(new OppgittFordelingEntitet(List.of(fellesperiode2, fellesperiode3), true))
            .medJustertFordeling(
                new OppgittFordelingEntitet(List.of(fellesperiode1, fellesperiode2, fellesperiode3), true))
            .medOppgittRettighet(new OppgittRettighetEntitet(true, false, false, false))
            .medAktivitetskravPerioder(List.of(avklartPeriode1, avklartPeriode2))
            .medAvklarteUttakDatoer(avklarteUttakDatoer)
            .lagre(repositoryProvider);

        var familieHendelse = FamilieHendelse.forFødsel(fødselsdato, fødselsdato, List.of(), 1);
        var ref = BehandlingReferanse.fra(behandling);
        var fellesperiode1Resultat = skalKontrollereAktivitetskrav(ref, fellesperiode1,
            ytelseFordelingTjeneste.hentAggregat(behandling.getId()), familieHendelse, true, List.of(), false);
        var fellesperiode2Resultat = skalKontrollereAktivitetskrav(ref, fellesperiode2,
            ytelseFordelingTjeneste.hentAggregat(behandling.getId()), familieHendelse, true, List.of(), false);
        var fellesperiode3Resultat = skalKontrollereAktivitetskrav(ref, fellesperiode3,
            ytelseFordelingTjeneste.hentAggregat(behandling.getId()), familieHendelse, true, List.of(), false);


        assertThat(fellesperiode1Resultat.isAvklart()).isTrue();
        assertThat(fellesperiode1Resultat.kravTilAktivitet()).isTrue();
        assertThat(fellesperiode1Resultat.avklartePerioder()).hasSize(1);

        assertThat(fellesperiode2Resultat.isAvklart()).isFalse();
        assertThat(fellesperiode2Resultat.kravTilAktivitet()).isTrue();
        assertThat(fellesperiode2Resultat.avklartePerioder()).isEmpty();

        assertThat(fellesperiode3Resultat.isAvklart()).isFalse();
        assertThat(fellesperiode3Resultat.kravTilAktivitet()).isTrue();
        assertThat(fellesperiode3Resultat.avklartePerioder()).isEmpty();
    }

    @Test
    public void skalBrukeOpprinneligeAvklartePerioderEtterEndringsdatoHvisIkkeSaksbehandlet() {
        var fødselsdato = LocalDate.of(2020, 1, 1);
        var fellesperiode = fellesperiode(fødselsdato);

        var førstegangsbehandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        var avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder().medOpprinneligEndringsdato(
            fellesperiode.getFom()).build();
        var avklartPeriode = new AktivitetskravPeriodeEntitet(fellesperiode.getFom(), fellesperiode.getTom(),
            KontrollerAktivitetskravAvklaring.IKKE_I_AKTIVITET_DOKUMENTERT, "begrunnelse");
        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOriginalBehandling(førstegangsbehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
            .medFordeling(new OppgittFordelingEntitet(List.of(fellesperiode), true))
            .medJustertFordeling(new OppgittFordelingEntitet(List.of(fellesperiode), true))
            .medOppgittRettighet(new OppgittRettighetEntitet(true, false, false, false))
            .medAktivitetskravPerioder(List.of(avklartPeriode))
            .medAvklarteUttakDatoer(avklarteUttakDatoer)
            .lagre(repositoryProvider);

        var familieHendelse = FamilieHendelse.forFødsel(fødselsdato, fødselsdato, List.of(), 1);
        var ref = BehandlingReferanse.fra(behandling);
        var fellesperiode1Resultat = skalKontrollereAktivitetskrav(ref, fellesperiode,
            ytelseFordelingTjeneste.hentAggregat(behandling.getId()), familieHendelse, true, List.of(), false);

        assertThat(fellesperiode1Resultat.isAvklart()).isFalse();
        assertThat(fellesperiode1Resultat.kravTilAktivitet()).isTrue();
        assertThat(fellesperiode1Resultat.avklartePerioder()).isEmpty();
    }

    @Test
    public void skalBrukeSaksbehandledeAvklartePerioderEtterEndringsdato() {
        var fødselsdato = LocalDate.of(2020, 1, 1);
        var fellesperiode = fellesperiode(fødselsdato);

        var førstegangsbehandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        var avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder().medOpprinneligEndringsdato(
            fellesperiode.getFom()).build();
        var avklartPeriode = new AktivitetskravPeriodeEntitet(fellesperiode.getFom(), fellesperiode.getTom(),
            KontrollerAktivitetskravAvklaring.IKKE_I_AKTIVITET_DOKUMENTERT, "begrunnelse");
        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOriginalBehandling(førstegangsbehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
            .medFordeling(new OppgittFordelingEntitet(List.of(fellesperiode), true))
            .medJustertFordeling(new OppgittFordelingEntitet(List.of(fellesperiode), true))
            .medOppgittRettighet(new OppgittRettighetEntitet(true, false, false, false))
            .medAktivitetskravPerioder(List.of(avklartPeriode))
            .medSaksbehandledeAktivitetskravPerioder(List.of(avklartPeriode))
            .medAvklarteUttakDatoer(avklarteUttakDatoer)
            .lagre(repositoryProvider);

        var familieHendelse = FamilieHendelse.forFødsel(fødselsdato, fødselsdato, List.of(), 1);
        var ref = BehandlingReferanse.fra(behandling);
        var fellesperiode1Resultat = skalKontrollereAktivitetskrav(ref, fellesperiode,
            ytelseFordelingTjeneste.hentAggregat(behandling.getId()), familieHendelse, true, List.of(), false);

        assertThat(fellesperiode1Resultat.isAvklart()).isTrue();
        assertThat(fellesperiode1Resultat.kravTilAktivitet()).isTrue();
        assertThat(fellesperiode1Resultat.avklartePerioder()).hasSize(1);
    }

    @Test
    public void skalIkkeKontrollereAktivitetskravHvisBehandlingManglerEndringsdato() {
        var fødselsdato = LocalDate.of(2020, 1, 1);
        var fellesperiode = fellesperiode(fødselsdato);

        var førstegangsbehandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        var avklartPeriode = new AktivitetskravPeriodeEntitet(fellesperiode.getFom(), fellesperiode.getTom(),
            KontrollerAktivitetskravAvklaring.IKKE_I_AKTIVITET_DOKUMENTERT, "begrunnelse");
        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOriginalBehandling(førstegangsbehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
            .medFordeling(new OppgittFordelingEntitet(List.of(fellesperiode), true))
            .medJustertFordeling(new OppgittFordelingEntitet(List.of(fellesperiode), true))
            .medOppgittRettighet(new OppgittRettighetEntitet(true, false, false, false))
            .medAktivitetskravPerioder(List.of(avklartPeriode))
            .medSaksbehandledeAktivitetskravPerioder(List.of(avklartPeriode))
            .lagre(repositoryProvider);

        var familieHendelse = FamilieHendelse.forFødsel(fødselsdato, fødselsdato, List.of(), 1);
        var ref = BehandlingReferanse.fra(behandling);
        var fellesperiode1Resultat = skalKontrollereAktivitetskrav(ref, fellesperiode,
            ytelseFordelingTjeneste.hentAggregat(behandling.getId()), familieHendelse, true, List.of(), false);

        assertThat(fellesperiode1Resultat.isAvklart()).isFalse();
        assertThat(fellesperiode1Resultat.kravTilAktivitet()).isFalse();
        assertThat(fellesperiode1Resultat.avklartePerioder()).isEmpty();
    }

    @Test
    public void skalIkkeKontrollereAktivitetskravHvisBehandlingHarAvklarteDatoerMenManglerEndringsdato() {
        var fødselsdato = LocalDate.of(2020, 1, 1);
        var fellesperiode = fellesperiode(fødselsdato);

        var førstegangsbehandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        var avklartPeriode = new AktivitetskravPeriodeEntitet(fellesperiode.getFom(), fellesperiode.getTom(),
            KontrollerAktivitetskravAvklaring.IKKE_I_AKTIVITET_DOKUMENTERT, "begrunnelse");
        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOriginalBehandling(førstegangsbehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
            .medFordeling(new OppgittFordelingEntitet(List.of(fellesperiode), true))
            .medJustertFordeling(new OppgittFordelingEntitet(List.of(fellesperiode), true))
            .medOppgittRettighet(new OppgittRettighetEntitet(true, false, false, false))
            .medAktivitetskravPerioder(List.of(avklartPeriode))
            .medAvklarteUttakDatoer(
                new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(fellesperiode.getFom()).build())
            .lagre(repositoryProvider);

        var familieHendelse = FamilieHendelse.forFødsel(fødselsdato, fødselsdato, List.of(), 1);
        var ref = BehandlingReferanse.fra(behandling);
        var fellesperiode1Resultat = skalKontrollereAktivitetskrav(ref, fellesperiode,
            ytelseFordelingTjeneste.hentAggregat(behandling.getId()), familieHendelse, true, List.of(), false);

        assertThat(fellesperiode1Resultat.isAvklart()).isFalse();
        assertThat(fellesperiode1Resultat.kravTilAktivitet()).isFalse();
        assertThat(fellesperiode1Resultat.avklartePerioder()).isEmpty();
    }

    private OppgittPeriodeEntitet fellesperiode(LocalDate fom, LocalDate tom) {
        return OppgittPeriodeBuilder.ny()
            .medPeriode(fom, tom)
            .medPeriodeType(FELLESPERIODE)
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medMorsAktivitet(MorsAktivitet.ARBEID)
            .build();
    }

    private OppgittPeriodeEntitet foreldrepenger(LocalDate fom, LocalDate tom) {
        return OppgittPeriodeBuilder.ny()
            .medPeriode(fom, tom)
            .medPeriodeType(FORELDREPENGER)
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medMorsAktivitet(MorsAktivitet.ARBEID)
            .build();
    }

    private OppgittPeriodeEntitet fellesperiode(LocalDate fom) {
        return fellesperiode(fom, fom.plusWeeks(10));
    }

    private OppgittRettighetEntitet aleneomsorg() {
        return new OppgittRettighetEntitet(false, true, false, false);
    }

    private UttakInput uttakInput(BehandlingReferanse behandlingReferanse, FamilieHendelse familieHendelse) {
        var ytelsespesifiktGrunnlag = new ForeldrepengerGrunnlag().medFamilieHendelser(
            new FamilieHendelser().medSøknadHendelse(familieHendelse));
        return new UttakInput(behandlingReferanse, null, ytelsespesifiktGrunnlag);
    }

    private UttakInput uttakInputMedAnnenpart(BehandlingReferanse behandlingReferanse, FamilieHendelse familieHendelse, Long behandlingAnnenpart) {
        var ytelsespesifiktGrunnlag = new ForeldrepengerGrunnlag()
            .medFamilieHendelser(new FamilieHendelser().medSøknadHendelse(familieHendelse))
            .medAnnenpart(new Annenpart(behandlingAnnenpart, LocalDateTime.now()));
        return new UttakInput(behandlingReferanse, null, ytelsespesifiktGrunnlag);
    }

    private BehandlingReferanse morBehandling(OppgittPeriodeEntitet søknadsperiode,
                                              AktivitetskravPeriodeEntitet... aktivitetskravPerioder) {
        return behandling(List.of(søknadsperiode), ScenarioMorSøkerForeldrepenger.forFødsel(), aktivitetskravPerioder);
    }

    private BehandlingReferanse farBehandling(OppgittPeriodeEntitet søknadsperiode,
                                              AktivitetskravPeriodeEntitet... aktivitetskravPerioder) {
        return behandling(List.of(søknadsperiode), ScenarioFarSøkerForeldrepenger.forFødsel(), aktivitetskravPerioder);
    }

    private BehandlingReferanse farBehandling(List<OppgittPeriodeEntitet> søknadsperioder,
                                              AktivitetskravPeriodeEntitet... aktivitetskravPerioder) {
        return behandling(søknadsperioder, ScenarioFarSøkerForeldrepenger.forFødsel(), aktivitetskravPerioder);
    }

    private BehandlingReferanse behandling(List<OppgittPeriodeEntitet> søknadsperioder,
                                           AbstractTestScenario scenario,
                                           AktivitetskravPeriodeEntitet... aktivitetskravPerioder) {
        var endringsdato = søknadsperioder.stream()
            .min(Comparator.comparing(OppgittPeriodeEntitet::getFom))
            .orElseThrow()
            .getFom();
        var avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder().medJustertEndringsdato(endringsdato).build();
        var behandling = scenario.medOppgittRettighet(new OppgittRettighetEntitet(true, false, false, false))
            .medAktivitetskravPerioder(List.of(aktivitetskravPerioder))
            .medAvklarteUttakDatoer(avklarteUttakDatoer)
            .medJustertFordeling(new OppgittFordelingEntitet(søknadsperioder, true))
            .lagre(repositoryProvider);
        return BehandlingReferanse.fra(behandling);
    }
}
