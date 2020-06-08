package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BehandlingBeregningsresultatBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BehandlingBeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.fagsak.FagsakBuilder;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.dto.BeregningsresultatMedUttaksplanDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.dto.BeregningsresultatPeriodeAndelDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.dto.BeregningsresultatPeriodeDto;
import no.nav.vedtak.felles.testutilities.Whitebox;

public class BeregningsresultatMedUttaksplanMapperTest {

    private static final LocalDate P1_FOM = LocalDate.now();
    private static final LocalDate P1_TOM = LocalDate.now().plusDays(10);
    private static final LocalDate P2_FOM = LocalDate.now().plusDays(11);
    private static final LocalDate P2_TOM = LocalDate.now().plusDays(20);
    private static final LocalDate P3_FOM = LocalDate.now().plusDays(21);
    private static final LocalDate P3_TOM = LocalDate.now().plusDays(30);
    private static final AktørId AKTØR_ID = AktørId.dummy();

    private VirksomhetTjeneste virksomhetTjeneste = Mockito.mock(VirksomhetTjeneste.class);
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjenesteMock = Mockito.mock(InntektArbeidYtelseTjeneste.class);
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste = new ArbeidsgiverTjeneste(null, virksomhetTjeneste);
    private BeregningsresultatMedUttaksplanMapper beregningsresultatMedUttaksplanMapper = new BeregningsresultatMedUttaksplanMapper(arbeidsgiverTjeneste, inntektArbeidYtelseTjenesteMock);

    @Test
    public void skalLageDto() {
        Behandling behandling = lagBehandling(); // Behandling
        Behandlingsresultat.opprettFor(behandling);
        BehandlingBeregningsresultatEntitet beregningsresultat = lagBeregningsresultatAggregatFP(behandling); // Beregingsresultat uten perioder

        BeregningsresultatMedUttaksplanDto dto = beregningsresultatMedUttaksplanMapper.lagBeregningsresultatMedUttaksplan(behandling, beregningsresultat, Optional.empty());

        assertThat(dto.getSokerErMor()).isTrue();
        assertThat(dto.getPerioder()).isEmpty();
    }

    @Test
    public void skalLageEnPeriodePerBeregningsresultatPeriode() {
        Behandling behandling = lagBehandling(); // Behandling
        Behandlingsresultat.opprettFor(behandling);
        BeregningsresultatEntitet beregningsresultat = lagBeregningsresultatAggregatFP(behandling).getBgBeregningsresultatFP(); // Beregingsresultat uten perioder
        Arbeidsgiver virksomhet = arbeidsgiver("123");
        var aktivitetEntitet = ordinærtArbeidsforholdUttakAktivitet(virksomhet, InternArbeidsforholdRef.nyRef());
        var uttakResultat = lagUttakPeriodeMedEnPeriode(Collections.singletonList(aktivitetEntitet)); // Uttaksplan med én periode som inneholder de to beregningsresultatperiodene

        lagP1(beregningsresultat); // Legg til en periode

        List<BeregningsresultatPeriodeDto> periodeDtoer = beregningsresultatMedUttaksplanMapper.lagPerioder(behandling.getId(), beregningsresultat, Optional.of(uttakResultat));

        assertThat(periodeDtoer).hasSize(1);

        lagP2(beregningsresultat); // Legg til en periode til

        periodeDtoer = beregningsresultatMedUttaksplanMapper.lagPerioder(behandling.getId(), beregningsresultat, Optional.of(uttakResultat));

        assertThat(periodeDtoer).hasSize(2);

        BeregningsresultatPeriodeDto p1 = periodeDtoer.get(0);
        assertThat(p1.getDagsats()).isEqualTo(0);
        assertThat(p1.getFom()).isEqualTo(P1_FOM);
        assertThat(p1.getTom()).isEqualTo(P1_TOM);
        assertThat(p1.getAndeler()).isEmpty();

        BeregningsresultatPeriodeDto p2 = periodeDtoer.get(1);
        assertThat(p2.getDagsats()).isEqualTo(0);
        assertThat(p2.getFom()).isEqualTo(P2_FOM);
        assertThat(p2.getTom()).isEqualTo(P2_TOM);
        assertThat(p2.getAndeler()).isEmpty();
    }

    @Test
    public void skalLageEnPeriodePerBeregningsresultatPeriodeUtenUttakResultat() {
        //Arrange
        Behandling behandling = lagBehandling(); // Behandling
        Behandlingsresultat.opprettFor(behandling);
        BehandlingBeregningsresultatEntitet beregningsresultat = lagBeregningsresultatAggregatFP(behandling); // Beregingsresultat uten perioder

        BeregningsresultatPeriode bgrPeriode = lagP1(beregningsresultat.getBgBeregningsresultatFP()); // Legg til en periode
        lagAndelTilSøker(bgrPeriode, 1000, arbeidsgiver("12345"));
        //Act
        BeregningsresultatMedUttaksplanDto uttaksplan = beregningsresultatMedUttaksplanMapper.lagBeregningsresultatMedUttaksplan(behandling, beregningsresultat, Optional.empty());

        //Assert
        List<BeregningsresultatPeriodeDto> perioder = List.of(uttaksplan.getPerioder());
        assertThat(uttaksplan.getOpphoersdato()).isNull();

        assertThat(perioder).hasSize(1);

        BeregningsresultatPeriodeDto p1 = perioder.get(0);
        assertThat(p1.getDagsats()).isEqualTo(1000);
        assertThat(p1.getFom()).isEqualTo(P1_FOM);
        assertThat(p1.getTom()).isEqualTo(P1_TOM);

        assertThat(p1.getAndeler()).hasSize(1);
        var andel = p1.getAndeler()[0];
        assertThat(andel.getUttak()).isNotNull();
        assertThat(andel.getUttak().isGradering()).isFalse();

    }

    @Test
    public void skalBeregneDagsatsPerPeriode() {
        Behandling behandling = lagBehandling(); // Behandling
        Arbeidsgiver virksomhet = arbeidsgiver("1234");

        BeregningsresultatEntitet beregningsresultat = lagBeregningsresultatAggregatFP(behandling).getBgBeregningsresultatFP();
        BeregningsresultatPeriode beregningsresultatPeriode1 = lagP1(beregningsresultat);
        lagAndelTilSøker(beregningsresultatPeriode1, 100, virksomhet);
        lagAndelTilArbeidsgiver(beregningsresultatPeriode1, virksomhet, 100);
        BeregningsresultatPeriode beregningsresultatPeriode2 = lagP2(beregningsresultat);
        lagAndelTilArbeidsgiver(beregningsresultatPeriode2, virksomhet, 100);

        int dagsatsP1 = beregningsresultatPeriode1.getDagsats();
        int dagsatsP2 = beregningsresultatPeriode2.getDagsats();

        assertThat(dagsatsP1).isEqualTo(200);
        assertThat(dagsatsP2).isEqualTo(100);
    }

    private Arbeidsgiver arbeidsgiver(String orgnr) {
        lagVirksomhet(orgnr);
        return Arbeidsgiver.virksomhet(orgnr);
    }

    private Virksomhet lagVirksomhet(String orgnr) {
        var virksomhet = new Virksomhet.Builder()
            .medOrgnr(orgnr)
            .medNavn("Virknavn " + orgnr)
            .build();
        when(virksomhetTjeneste.hentOgLagreOrganisasjon(orgnr)).thenReturn(virksomhet);

        return virksomhet;
    }

    @Test
    public void skalLageAndelerPerPeriodeEttArbeidsforhold() {
        // Arrange 1: Kun andel for søker
        Arbeidsgiver virksomhet = arbeidsgiver("1234");

        Behandling behandling = lagBehandling();
        Behandlingsresultat.opprettFor(behandling);
        var uttakAktivitet1 = ordinærtArbeidsforholdUttakAktivitet(virksomhet, null);
        var uttakResultat = lagUttakPeriodeMedEnPeriode(Collections.singletonList(uttakAktivitet1));

        BehandlingBeregningsresultatEntitet beregningsresultat = lagBeregningsresultatAggregatFP(behandling); // Beregingsresultat
        BeregningsresultatPeriode beregningsresultatPeriode = lagP1(beregningsresultat.getBgBeregningsresultatFP()); // Periode uten andeler
        InternArbeidsforholdRef arbeidsforholdId = uttakAktivitet1.getArbeidsforholdRef();
        lagAndelTilSøker(beregningsresultatPeriode, 100, uttakAktivitet1.getArbeidsgiver().get(),
            arbeidsforholdId); // Legg til en andel til søker

        List<BeregningsresultatPeriodeAndelDto> andeler = beregningsresultatMedUttaksplanMapper.lagAndeler(beregningsresultatPeriode, Optional.of(uttakResultat),
            Collections.emptyMap(), Optional.empty());

        assertThat(andeler).hasSize(1);

        // Arrange 2: Andel for søker og arbeidsgiver
        lagAndelTilArbeidsgiver(beregningsresultatPeriode, virksomhet, 100, arbeidsforholdId); // Legg til en andel til arbeidsgiver

        andeler = beregningsresultatMedUttaksplanMapper.lagAndeler(beregningsresultatPeriode, Optional.of(uttakResultat), Collections.emptyMap(), Optional.empty());

        assertThat(andeler).hasSize(1);
        assertAndelArbeidsgiver(andeler, virksomhet.getIdentifikator(), 100);
    }

    @Test
    public void skalLageAndelerPerPeriodeToArbeidsforhold() {
        Arbeidsgiver virksomhet1 = arbeidsgiver("1234");
        Arbeidsgiver virksomhet2 = arbeidsgiver("3456");

        Behandling behandling = lagBehandling();
        Behandlingsresultat.opprettFor(behandling);
        var uttakAktivitet1 = ordinærtArbeidsforholdUttakAktivitet(virksomhet1, null);
        var uttakAktivitet2 = ordinærtArbeidsforholdUttakAktivitet(virksomhet2, InternArbeidsforholdRef.nyRef());
        var uttakResultat = lagUttakPeriodeMedEnPeriode(List.of(uttakAktivitet1, uttakAktivitet2));

        BehandlingBeregningsresultatEntitet beregningsresultat = lagBeregningsresultatAggregatFP(behandling); // Beregingsresultat
        BeregningsresultatPeriode beregningsresultatPeriode = lagP1(beregningsresultat.getBgBeregningsresultatFP()); // Periode uten andeler
        lagAndelTilSøker(beregningsresultatPeriode, 100, uttakAktivitet1.getArbeidsgiver().get(), uttakAktivitet1.getArbeidsforholdRef()); // Legg til en andel til søker
        lagAndelTilSøker(beregningsresultatPeriode, 200, uttakAktivitet2.getArbeidsgiver().get(), uttakAktivitet2.getArbeidsforholdRef()); // Legg til en andel til søker
        lagAndelTilArbeidsgiver(beregningsresultatPeriode, virksomhet1, 200, uttakAktivitet1.getArbeidsforholdRef());
        lagAndelTilArbeidsgiver(beregningsresultatPeriode, virksomhet2, 100, uttakAktivitet2.getArbeidsforholdRef());

        List<BeregningsresultatPeriodeAndelDto> andeler = beregningsresultatMedUttaksplanMapper.lagAndeler(beregningsresultatPeriode, Optional.of(uttakResultat),
            Collections.emptyMap(), Optional.empty());

        assertThat(andeler).hasSize(2);
        assertAndelArbeidsgiver(andeler, virksomhet1.getIdentifikator(), 200);
        assertAndelArbeidsgiver(andeler, virksomhet2.getIdentifikator(), 100);
    }

    @Test
    public void skalLageAndelerForKombibasjonsstatuser() {
        // Arrange 1
        Behandling behandling = lagBehandling();
        Behandlingsresultat.opprettFor(behandling);
        var uttakResultat = lagUttakPeriodeMedEnPeriode(List.of(
            new ForeldrepengerUttakAktivitet(UttakArbeidType.ANNET),
            new ForeldrepengerUttakAktivitet(UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE))
        );

        BehandlingBeregningsresultatEntitet beregningsresultat = lagBeregningsresultatAggregatFP(behandling); // Beregingsresultat
        BeregningsresultatPeriode beregningsresultatPeriode = lagP1(beregningsresultat.getBgBeregningsresultatFP()); // Periode uten andeler

        lagAndelTilSøkerMedAktivitetStatus(beregningsresultatPeriode, 1000, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);
        lagAndelTilSøkerMedAktivitetStatus(beregningsresultatPeriode, 2000, AktivitetStatus.DAGPENGER);

        List<BeregningsresultatPeriodeAndelDto> andeler = beregningsresultatMedUttaksplanMapper.lagAndeler(beregningsresultatPeriode, Optional.of(uttakResultat),
            Collections.emptyMap(), Optional.empty());

        assertThat(andeler).hasSize(2);
        BeregningsresultatPeriodeAndelDto andel1 = andeler.stream().filter(a -> a.getAktivitetStatus().equals(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)).findFirst().orElse(null);
        BeregningsresultatPeriodeAndelDto andel2 = andeler.stream().filter(a -> a.getAktivitetStatus().equals(AktivitetStatus.DAGPENGER)).findFirst().orElse(null);
        assertThat(andel1.getTilSoker()).isEqualTo(1000);
        assertThat(andel2.getTilSoker()).isEqualTo(2000);
        assertThat(andel1.getRefusjon()).isEqualTo(0);
        assertThat(andel2.getRefusjon()).isEqualTo(0);
    }

    @Test
    public void skalSlåSammenAndelerMedSammeArbeidsforholdId() {
        // Arrange
        Arbeidsgiver virksomhet = arbeidsgiver("1234");

        Behandling behandling = lagBehandling();
        Behandlingsresultat.opprettFor(behandling);
        InternArbeidsforholdRef arbeidsforholdRef = InternArbeidsforholdRef.nyRef();
        var uttakAktivitet = ordinærtArbeidsforholdUttakAktivitet(virksomhet, arbeidsforholdRef);
        var uttakResultat = lagUttakPeriodeMedEnPeriode(Collections.singletonList(uttakAktivitet));

        BehandlingBeregningsresultatEntitet beregningsresultat = lagBeregningsresultatAggregatFP(behandling); // Beregingsresultat
        BeregningsresultatPeriode beregningsresultatPeriode = lagP1(beregningsresultat.getBgBeregningsresultatFP()); // Periode uten andeler
        ArrayList<BeregningsresultatPeriode> beregningsresultatPerioder = new ArrayList<>();
        beregningsresultatPerioder.add(beregningsresultatPeriode);

        lagAndelTilSøker(beregningsresultatPeriode, 500, virksomhet, uttakAktivitet.getArbeidsforholdRef());
        lagAndelTilSøker(beregningsresultatPeriode, 1000, virksomhet, uttakAktivitet.getArbeidsforholdRef());
        lagAndelTilArbeidsgiver(beregningsresultatPeriode, virksomhet, 250, uttakAktivitet.getArbeidsforholdRef());
        lagAndelTilArbeidsgiver(beregningsresultatPeriode, virksomhet, 500, uttakAktivitet.getArbeidsforholdRef());

        List<BeregningsresultatPeriodeAndelDto> andeler = beregningsresultatMedUttaksplanMapper.lagAndeler(beregningsresultatPeriode, Optional.of(uttakResultat),
            Collections.emptyMap(), Optional.empty());

        assertThat(andeler).hasSize(1);
        assertThat(andeler.get(0).getTilSoker()).isEqualTo(1500);
        assertAndelArbeidsgiver(andeler, virksomhet.getIdentifikator(), 750);
    }

    @Test
    public void skalFinneRiktigSisteUtbetalingsdato() {
        Arbeidsgiver virksomhet1 = arbeidsgiver("123");
        Arbeidsgiver virksomhet2 = arbeidsgiver("456");
        Arbeidsgiver virksomhet3 = arbeidsgiver("789");
        Behandling behandling = lagBehandling();
        Behandlingsresultat.opprettFor(behandling);
        var uttakAktivitet1 = ordinærtArbeidsforholdUttakAktivitet(virksomhet1, InternArbeidsforholdRef.nyRef());
        var uttakAktivitet2 = ordinærtArbeidsforholdUttakAktivitet(virksomhet2, InternArbeidsforholdRef.nyRef());
        var uttakAktivitet3 = ordinærtArbeidsforholdUttakAktivitet(virksomhet3, InternArbeidsforholdRef.nyRef());
        var uttakResultat = lagUttakPeriodeMedEnPeriode(P1_FOM, P3_TOM, List.of(uttakAktivitet1, uttakAktivitet2, uttakAktivitet3));
        BehandlingBeregningsresultatEntitet beregningsresultat = lagBeregningsresultatAggregatFP(behandling);
        BeregningsresultatPeriode beregningsresultatPeriode = lagP1(beregningsresultat.getBgBeregningsresultatFP());
        BeregningsresultatPeriode beregningsresultatPeriode2 = lagP2(beregningsresultat.getBgBeregningsresultatFP());
        BeregningsresultatPeriode beregningsresultatPeriode3 = lagP3(beregningsresultat.getBgBeregningsresultatFP());

        lagAndelTilSøker(beregningsresultatPeriode, 500, uttakAktivitet1.getArbeidsgiver().get(), uttakAktivitet1.getArbeidsforholdRef());
        lagAndelTilSøker(beregningsresultatPeriode, 1000, uttakAktivitet2.getArbeidsgiver().get(), uttakAktivitet2.getArbeidsforholdRef());
        lagAndelTilSøker(beregningsresultatPeriode2, 0, uttakAktivitet1.getArbeidsgiver().get(), uttakAktivitet1.getArbeidsforholdRef());
        lagAndelTilSøker(beregningsresultatPeriode2, 1000, uttakAktivitet2.getArbeidsgiver().get(), uttakAktivitet2.getArbeidsforholdRef());
        lagAndelTilSøker(beregningsresultatPeriode, 300, uttakAktivitet3.getArbeidsgiver().get(), uttakAktivitet3.getArbeidsforholdRef());
        lagAndelTilArbeidsgiver(beregningsresultatPeriode, uttakAktivitet3.getArbeidsgiver().get(), 250, uttakAktivitet3.getArbeidsforholdRef());
        lagAndelTilSøker(beregningsresultatPeriode2, 0, uttakAktivitet3.getArbeidsgiver().get(), uttakAktivitet3.getArbeidsforholdRef());
        lagAndelTilArbeidsgiver(beregningsresultatPeriode2, uttakAktivitet3.getArbeidsgiver().get(), 250, uttakAktivitet3.getArbeidsforholdRef());
        lagAndelTilSøker(beregningsresultatPeriode3, 0, uttakAktivitet3.getArbeidsgiver().get(), uttakAktivitet3.getArbeidsforholdRef());

        //Act
        List<BeregningsresultatPeriodeDto> andeler = beregningsresultatMedUttaksplanMapper.lagPerioder(behandling.getId(), beregningsresultat.getBgBeregningsresultatFP(), Optional.of(uttakResultat));

        //Assert
        andeler.stream().flatMap(a -> Arrays.stream(a.getAndeler())).filter(andel -> andel.getArbeidsgiverOrgnr().equals(virksomhet1.getIdentifikator()))
            .forEach(andel1 -> assertThat(andel1.getSisteUtbetalingsdato()).isEqualTo(P1_TOM));
        andeler.stream().flatMap(a -> Arrays.stream(a.getAndeler())).filter(andel -> andel.getArbeidsgiverOrgnr().equals(virksomhet2.getIdentifikator()))
            .forEach(andel1 -> assertThat(andel1.getSisteUtbetalingsdato()).isEqualTo(P2_TOM));
        andeler.stream().flatMap(a -> Arrays.stream(a.getAndeler())).filter(andel -> andel.getArbeidsgiverOrgnr().equals(virksomhet3.getIdentifikator()))
            .forEach(andel1 -> assertThat(andel1.getSisteUtbetalingsdato()).isEqualTo(P2_TOM));
    }

    private ForeldrepengerUttakAktivitet ordinærtArbeidsforholdUttakAktivitet(Arbeidsgiver virksomhet, InternArbeidsforholdRef arbeidsforholdRef) {
        return new ForeldrepengerUttakAktivitet(UttakArbeidType.ORDINÆRT_ARBEID, virksomhet, arbeidsforholdRef);
    }

    private void assertAndelArbeidsgiver(List<BeregningsresultatPeriodeAndelDto> andeler, String arbeidsgiver, int forventetRefusjon) {
        Optional<BeregningsresultatPeriodeAndelDto> andel = hentAndelForArbeidgiver(andeler, arbeidsgiver);
        assertThat(andel).as("arbeidsgiverAndel").hasValueSatisfying(a -> {
            assertThat(a.getArbeidsgiverOrgnr()).as("arbeidsgiver").isEqualTo(arbeidsgiver);
            assertThat(a.getRefusjon()).as("refusjon").isEqualTo(forventetRefusjon);
        });
    }

    private Optional<BeregningsresultatPeriodeAndelDto> hentAndelForArbeidgiver(List<BeregningsresultatPeriodeAndelDto> andeler, String arbeidsgiver) {
        return andeler.stream().filter(a -> a.getArbeidsgiverOrgnr().equals(arbeidsgiver)).findFirst();
    }

    private static Behandling lagBehandling() {
        NavBruker søker = NavBruker.opprettNy(new Personinfo.Builder()
            .medAktørId(AKTØR_ID)
            .medPersonIdent(PersonIdent.fra("42424242424"))
            .medNavn("42")
            .medFødselsdato(LocalDate.of(42, 42 % 12 + 1, 42 % 31 + 1))
            .medNavBrukerKjønn(NavBrukerKjønn.UDEFINERT)
            .build());
        Fagsak fagsak = FagsakBuilder.nyForeldrepengerForMor().medBruker(søker).build();
        var behandling = Behandling.forFørstegangssøknad(fagsak)
            .build();
        Whitebox.setInternalState(behandling, "id", 1L);
        return behandling;
    }

    private static ForeldrepengerUttak lagUttakPeriodeMedEnPeriode(List<ForeldrepengerUttakAktivitet> uttakAktiviteter) {
        return lagUttakPeriodeMedEnPeriode(P1_FOM, P1_TOM, uttakAktiviteter);
    }

    private static ForeldrepengerUttak lagUttakPeriodeMedEnPeriode(LocalDate p1Fom, LocalDate p1Tom, List<ForeldrepengerUttakAktivitet> uttakAktiviteter) {
        var periodeAktiviteter = uttakAktiviteter.stream()
            .map(ua -> new ForeldrepengerUttakPeriodeAktivitet.Builder()
                .medAktivitet(ua)
                .medTrekkonto(StønadskontoType.FELLESPERIODE)
                .medTrekkdager(new Trekkdager(20))
                .medArbeidsprosent(BigDecimal.ZERO)
                .build())
            .collect(Collectors.toList());
        var uttakPeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(p1Fom, p1Tom)
            .medResultatType(PeriodeResultatType.INNVILGET)
            .medAktiviteter(periodeAktiviteter)
            .build();
        return new ForeldrepengerUttak(List.of(uttakPeriode));
    }

    private static BehandlingBeregningsresultatEntitet lagBeregningsresultatAggregatFP(Behandling behandling) {
        BeregningsresultatEntitet bgres = BeregningsresultatEntitet.builder()
            .medRegelInput("")
            .medRegelSporing("")
            .build();
        BehandlingBeregningsresultatBuilder builder = BehandlingBeregningsresultatBuilder.oppdatere(Optional.empty())
            .medBgBeregningsresultatFP(bgres);
        return builder.build(behandling);
    }

    private static BeregningsresultatPeriode lagP1(BeregningsresultatEntitet beregningsresultat) {
        return BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(P1_FOM, P1_TOM)
            .build(beregningsresultat);
    }

    private static BeregningsresultatPeriode lagP2(BeregningsresultatEntitet beregningsresultat) {
        return BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(P2_FOM, P2_TOM)
            .build(beregningsresultat);
    }

    private static BeregningsresultatPeriode lagP3(BeregningsresultatEntitet beregningsresultat) {
        return BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(P3_FOM, P3_TOM)
            .build(beregningsresultat);
    }

    private static BeregningsresultatAndel lagAndelTilArbeidsgiver(BeregningsresultatPeriode periode, Arbeidsgiver arbeidsgiver, int refusjon) {
        return lagAndelTilArbeidsgiver(periode, arbeidsgiver, refusjon, null);
    }

    private static BeregningsresultatAndel lagAndelTilArbeidsgiver(BeregningsresultatPeriode periode,
                                                                   Arbeidsgiver arbeidsgiver,
                                                                   int refusjon,
                                                                   InternArbeidsforholdRef arbeidsforholdId) {
        return BeregningsresultatAndel.builder()
            .medArbeidsgiver(arbeidsgiver)
            .medDagsats(refusjon)
            .medArbeidsforholdRef(arbeidsforholdId)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medDagsatsFraBg(refusjon)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medBrukerErMottaker(false)
            .build(periode);
    }

    private static BeregningsresultatAndel lagAndelTilSøker(BeregningsresultatPeriode periode, int tilSøker, Arbeidsgiver virksomhet) {
        return lagAndelTilSøker(periode, tilSøker, virksomhet, null);
    }

    private static BeregningsresultatAndel lagAndelTilSøker(BeregningsresultatPeriode periode, int tilSøker, Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdId) {
        return BeregningsresultatAndel.builder()
            .medArbeidsgiver(arbeidsgiver)
            .medDagsats(tilSøker)
            .medArbeidsforholdRef(arbeidsforholdId)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medDagsatsFraBg(tilSøker)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medBrukerErMottaker(true)
            .build(periode);
    }

    private static BeregningsresultatAndel lagAndelTilSøkerMedAktivitetStatus(BeregningsresultatPeriode periode, int tilSøker, AktivitetStatus aktivitetStatus) {
        return BeregningsresultatAndel.builder()
            .medArbeidsgiver(null)
            .medDagsats(tilSøker)
            .medAktivitetStatus(aktivitetStatus)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medDagsatsFraBg(tilSøker)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medBrukerErMottaker(true)
            .build(periode);
    }
}
