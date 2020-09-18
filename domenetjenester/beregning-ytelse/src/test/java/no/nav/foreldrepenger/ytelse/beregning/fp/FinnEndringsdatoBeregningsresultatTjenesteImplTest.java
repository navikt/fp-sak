package no.nav.foreldrepenger.ytelse.beregning.fp;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.ytelse.beregning.FinnEndringsdatoBeregningsresultatTjeneste;
import no.nav.foreldrepenger.ytelse.beregning.FinnEndringsdatoMellomPeriodeLister;
import no.nav.foreldrepenger.ytelse.beregning.SjekkForEndringMellomPerioder;
import no.nav.foreldrepenger.ytelse.beregning.SjekkForIngenAndelerOgAndelerUtenDagsats;
import no.nav.foreldrepenger.ytelse.beregning.SjekkOmPerioderHarEndringIAndeler;
import no.nav.vedtak.exception.TekniskException;

public class FinnEndringsdatoBeregningsresultatTjenesteImplTest {

    private static final InternArbeidsforholdRef ARBEIDSFORHOLD_ID = InternArbeidsforholdRef.namedRef("TEST-REF");
    private static final String ORGNR = KUNSTIG_ORG;

    private static final LocalDate FØRSTE_AUGUST = LocalDate.of(2018, 8,1);

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();
    private FinnEndringsdatoBeregningsresultatTjeneste finnEndringsdatoBeregningsresultatTjeneste;
    private BeregningsresultatRepository beregningsresultatRepository = Mockito.mock(BeregningsresultatRepository.class);
    private BeregningsresultatEntitet brFørstegangsbehandling;
    private BeregningsresultatEntitet brRevurdering;
    private Behandling førstegangsbehandling;
    private Behandling revurdering;

    @Before
    public void oppsett() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        førstegangsbehandling = scenario.lagMocked();
        revurdering = opprettRevurdering(førstegangsbehandling);
        SjekkForIngenAndelerOgAndelerUtenDagsats sjekkForIngenAndelerOgAndelerUtenDagsats = new SjekkForIngenAndelerOgAndelerUtenDagsats();
        SjekkOmPerioderHarEndringIAndeler SjekkOmPerioderHarEndringIAndeler = new SjekkOmPerioderHarEndringIAndeler();
        SjekkForEndringMellomPerioder sjekkForEndringMellomPerioder = new SjekkForEndringMellomPerioder(sjekkForIngenAndelerOgAndelerUtenDagsats, SjekkOmPerioderHarEndringIAndeler);
        FinnEndringsdatoMellomPeriodeLister finnEndringsdatoMellomPeriodeLister = new FinnEndringsdatoMellomPeriodeLister(sjekkForEndringMellomPerioder);
        finnEndringsdatoBeregningsresultatTjeneste = new FinnEndringsdatoBeregningsresultatTjenesteImpl(beregningsresultatRepository, finnEndringsdatoMellomPeriodeLister);
        brFørstegangsbehandling = BeregningsresultatEntitet.builder().medRegelInput("clob1").medRegelSporing("clob2").build();
        brRevurdering = BeregningsresultatEntitet.builder().medRegelInput("clob1").medRegelSporing("clob2").build();

        Mockito.when(beregningsresultatRepository.hentUtbetBeregningsresultat(Mockito.any())).thenReturn(Optional.of(brFørstegangsbehandling));
    }

    @Test
    public void skal_feile_hvis_behandling_ikke_er_en_revurdering(){
        // Expect
        expectedException.expect(TekniskException.class);
        expectedException.expectMessage(String.format("Behandlingen med id %s er ikke en revurdering", førstegangsbehandling.getId()));
        // Act
        finnEndringsdatoBeregningsresultatTjeneste.finnEndringsdato(førstegangsbehandling, brFørstegangsbehandling);
    }

    @Test
    public void skal_feile_hvis_revurdering_ikke_har_en_original_behandling(){
        // Arrange
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medBehandlingType(BehandlingType.REVURDERING);
        Behandling revurdering = scenario.lagMocked();
        // Expect
        expectedException.expect(TekniskException.class);
        expectedException.expectMessage(String.format("Fant ikke en original behandling for revurdering med id %s", revurdering.getId()));
        // Act
        finnEndringsdatoBeregningsresultatTjeneste.finnEndringsdato(revurdering, brRevurdering);
    }

    @Test
    public void skal_feile_hvis_revurderingen_ikke_har_noen_beregningsresultaltperioder() {
        // Arrange : Førstegangsbehandling
        opprettPeriode(brFørstegangsbehandling, FØRSTE_AUGUST, FØRSTE_AUGUST.plusDays(6));
        beregningsresultatRepository.lagre(førstegangsbehandling, brFørstegangsbehandling);
        // Arrange : Revurdering
        beregningsresultatRepository.lagre(revurdering, brRevurdering);
        // Expect
        expectedException.expect(TekniskException.class);
        expectedException.expectMessage(String.format("Fant ikke beregningsresultatperiode for beregningsresultat med id %s", brRevurdering.getId()));
        // Act
        finnEndringsdatoBeregningsresultatTjeneste.finnEndringsdato(revurdering, brRevurdering);
    }

    @Test
    public void skal_finne_en_tom_endringsdato_for_revurdering_hvor_forrige_behandling_er_avslått_og_mangler_beregningsresultat(){
        Mockito.when(beregningsresultatRepository.hentUtbetBeregningsresultat(Mockito.any())).thenReturn(Optional.empty());

        // Act
        Optional<LocalDate> endringsdato = finnEndringsdatoBeregningsresultatTjeneste.finnEndringsdato(revurdering, brRevurdering);
        // Assert
        assertThat(endringsdato).isEmpty();
    }

    @Test
    public void skal_finne_en_tom_endringsdato_hvor_revurdering_ikke_er_endret_i_forhold_til_førstegangsbehandlingen(){
        // Arrange : Førstegangsbehandling
        BeregningsresultatPeriode p1_førstegangsbehandling = opprettPeriode(brFørstegangsbehandling, FØRSTE_AUGUST, FØRSTE_AUGUST.plusDays(4));
        opprettAndel(p1_førstegangsbehandling, true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        BeregningsresultatPeriode p2_førstegangsbehandling = opprettPeriode(brFørstegangsbehandling, FØRSTE_AUGUST.plusDays(5), FØRSTE_AUGUST.plusDays(7));
        opprettAndel(p2_førstegangsbehandling, true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        beregningsresultatRepository.lagre(førstegangsbehandling, brFørstegangsbehandling);
        // Arrange : Revurdering
        BeregningsresultatPeriode p3_revurdering = opprettPeriode(brRevurdering, FØRSTE_AUGUST, FØRSTE_AUGUST.plusDays(4));
        opprettAndel(p3_revurdering, true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        BeregningsresultatPeriode p4_revurdering = opprettPeriode(brRevurdering, FØRSTE_AUGUST.plusDays(5), FØRSTE_AUGUST.plusDays(7));
        opprettAndel(p4_revurdering, true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        beregningsresultatRepository.lagre(revurdering, brRevurdering);
        // Act
        Optional<LocalDate> endringsdato = finnEndringsdatoBeregningsresultatTjeneste.finnEndringsdato(revurdering, brRevurdering);
        // Assert
        assertThat(endringsdato).isEmpty();
    }

    @Test
    public void skal_finne_tom_endringsdato_hvor_revurderingen_har_blitt_forlenget_med_en_periode_med_ingen_dagsats_hvor_det_er_ingen_korresponderende_periode(){
        // Arrange : Førstegangsbehandling
        BeregningsresultatPeriode periode1 = opprettPeriode(brFørstegangsbehandling, LocalDate.of(2018, 10, 1),
            LocalDate.of(2018, 12, 15));
        opprettAndel(periode1, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 1322, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        BeregningsresultatPeriode periode2 = opprettPeriode(brFørstegangsbehandling, LocalDate.of(2018, 12, 16),
            LocalDate.of(2019, 2, 17));
        opprettAndel(periode2, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 0, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        BeregningsresultatPeriode periode3 = opprettPeriode(brFørstegangsbehandling, LocalDate.of(2019, 2, 18),
            LocalDate.of(2019, 3, 17));
        opprettAndel(periode3, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 1322, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        beregningsresultatRepository.lagre(førstegangsbehandling, brFørstegangsbehandling);
        // Arrange : Revurdering
        BeregningsresultatPeriode periode4 = opprettPeriode(brRevurdering, LocalDate.of(2018, 10, 1),
            LocalDate.of(2018, 12, 15));
        opprettAndel(periode4, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 1322, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        BeregningsresultatPeriode periode5 = opprettPeriode(brRevurdering, LocalDate.of(2018, 12, 16),
            LocalDate.of(2019, 2, 17));
        opprettAndel(periode5, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 0, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        BeregningsresultatPeriode periode6 = opprettPeriode(brRevurdering, LocalDate.of(2019, 2, 18),
            LocalDate.of(2019, 3, 17));
        opprettAndel(periode6, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 1322, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        BeregningsresultatPeriode periode7 = opprettPeriode(brRevurdering, LocalDate.of(2019, 3, 18),
            LocalDate.of(2019, 5, 4));
        opprettAndel(periode7, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 0, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        beregningsresultatRepository.lagre(revurdering, brRevurdering);
        // Act
        Optional<LocalDate> endringsdato = finnEndringsdatoBeregningsresultatTjeneste.finnEndringsdato(revurdering, brRevurdering);
        // Assert
        assertThat(endringsdato).isEmpty();
    }

    @Test
    public void skal_finne_tom_endringsdato_hvor_periode_med_ingen_dagsats_i_førstegangsbehandling_blir_oppholdsperiode_i_revurdering(){
        // Arrange : Førstegangsbehandling
        BeregningsresultatPeriode periode1 = opprettPeriode(brFørstegangsbehandling, LocalDate.of(2018, 10, 1),
            LocalDate.of(2018, 12, 15));
        opprettAndel(periode1, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 1322, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        BeregningsresultatPeriode periode2 = opprettPeriode(brFørstegangsbehandling, LocalDate.of(2018, 12, 16),
            LocalDate.of(2019, 2, 17));
        opprettAndel(periode2, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 0, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        BeregningsresultatPeriode periode3 = opprettPeriode(brFørstegangsbehandling, LocalDate.of(2019, 2, 18),
            LocalDate.of(2019, 3, 17));
        opprettAndel(periode3, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 1322, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        beregningsresultatRepository.lagre(førstegangsbehandling, brFørstegangsbehandling);
        // Arrange : Revurdering
        BeregningsresultatPeriode periode4 = opprettPeriode(brRevurdering, LocalDate.of(2018, 10, 1),
            LocalDate.of(2018, 12, 15));
        opprettAndel(periode4, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 1322, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        opprettPeriode(brRevurdering, LocalDate.of(2018, 12, 16),
            LocalDate.of(2019, 2, 17));
        BeregningsresultatPeriode periode6 = opprettPeriode(brRevurdering, LocalDate.of(2019, 2, 18),
            LocalDate.of(2019, 3, 17));
        opprettAndel(periode6, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 1322, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        beregningsresultatRepository.lagre(revurdering, brRevurdering);
        // Act
        Optional<LocalDate> endringsdato = finnEndringsdatoBeregningsresultatTjeneste.finnEndringsdato(revurdering, brRevurdering);
        // Assert
        assertThat(endringsdato).isEmpty();
    }

    @Test
    public void skal_finne_endringsdato_hvor_den_midterste_perioden_er_delt_i_to_i_revurderingen_hvor_dagsatsen_er_endret(){
        // Arrange : Førstegangsbehandling
        BeregningsresultatPeriode periode1 = opprettPeriode(brFørstegangsbehandling, LocalDate.of(2018, 10, 1),
            LocalDate.of(2018, 12, 15));
        opprettAndel(periode1, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 1322, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        BeregningsresultatPeriode periode2 = opprettPeriode(brFørstegangsbehandling, LocalDate.of(2018, 12, 16),
            LocalDate.of(2019, 2, 17));
        opprettAndel(periode2, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 0, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        BeregningsresultatPeriode periode3 = opprettPeriode(brFørstegangsbehandling, LocalDate.of(2019, 2, 18),
            LocalDate.of(2019, 3, 17));
        opprettAndel(periode3, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 1322, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        beregningsresultatRepository.lagre(førstegangsbehandling, brFørstegangsbehandling);
        // Arrange : Revurdering
        BeregningsresultatPeriode periode4 = opprettPeriode(brRevurdering, LocalDate.of(2018, 10, 1),
            LocalDate.of(2018, 12, 15));
        opprettAndel(periode4, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 1322, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        BeregningsresultatPeriode periode5 = opprettPeriode(brRevurdering, LocalDate.of(2018, 12, 16),
            LocalDate.of(2019, 2, 1));
        opprettAndel(periode5, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 0, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        BeregningsresultatPeriode periode6 = opprettPeriode(brRevurdering, LocalDate.of(2019, 2, 2),
            LocalDate.of(2019, 2, 17));
        opprettAndel(periode6, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 1022, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        BeregningsresultatPeriode periode7 = opprettPeriode(brRevurdering, LocalDate.of(2019, 2, 18),
            LocalDate.of(2019, 3, 17));
        opprettAndel(periode7, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 1322, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        beregningsresultatRepository.lagre(revurdering, brRevurdering);
        // Act
        Optional<LocalDate> endringsdato = finnEndringsdatoBeregningsresultatTjeneste.finnEndringsdato(revurdering, brRevurdering);
        // Assert
        assertThat(endringsdato).hasValueSatisfying(ed -> assertThat(ed).isEqualTo(LocalDate.of(2019,2,2)));
    }

    @Test
    public void skal_finne_endringsdato_hvor_inntektskategorien_blir_endret_i_siste_periode_uten_å_reagere_på_perioden_i_midten_som_er_delt_i_to_i_revurderingen_med_ingen_dagsats(){
        // Arrange : Førstegangsbehandling
        BeregningsresultatPeriode periode1 = opprettPeriode(brFørstegangsbehandling, LocalDate.of(2018, 10, 1),
            LocalDate.of(2018, 12, 15));
        opprettAndel(periode1, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 1322, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        BeregningsresultatPeriode periode2 = opprettPeriode(brFørstegangsbehandling, LocalDate.of(2018, 12, 16),
            LocalDate.of(2019, 2, 17));
        opprettAndel(periode2, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 0, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        BeregningsresultatPeriode periode3 = opprettPeriode(brFørstegangsbehandling, LocalDate.of(2019, 2, 18),
            LocalDate.of(2019, 3, 17));
        opprettAndel(periode3, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 1322, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        beregningsresultatRepository.lagre(førstegangsbehandling, brFørstegangsbehandling);
        // Arrange : Revurdering
        BeregningsresultatPeriode periode4 = opprettPeriode(brRevurdering, LocalDate.of(2018, 10, 1),
            LocalDate.of(2018, 12, 15));
        opprettAndel(periode4, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 1322, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        BeregningsresultatPeriode periode5 = opprettPeriode(brRevurdering, LocalDate.of(2018, 12, 16),
            LocalDate.of(2019, 2, 1));
        opprettAndel(periode5, false, ARBEIDSFORHOLD_ID, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER,
            ORGNR, 0, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        BeregningsresultatPeriode periode6 = opprettPeriode(brRevurdering, LocalDate.of(2019, 2, 2),
            LocalDate.of(2019, 2, 17));
        opprettAndel(periode6, false, ARBEIDSFORHOLD_ID, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER,
            ORGNR, 0, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        BeregningsresultatPeriode periode7 = opprettPeriode(brRevurdering, LocalDate.of(2019, 2, 18),
            LocalDate.of(2019, 3, 17));
        opprettAndel(periode7, false, ARBEIDSFORHOLD_ID, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER,
            ORGNR, 1322, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        beregningsresultatRepository.lagre(revurdering, brRevurdering);
        // Act
        Optional<LocalDate> endringsdato = finnEndringsdatoBeregningsresultatTjeneste.finnEndringsdato(revurdering, brRevurdering);
        // Assert
        assertThat(endringsdato).hasValueSatisfying(ed -> assertThat(ed).isEqualTo(LocalDate.of(2019,2,18)));
    }

    @Test
    public void skal_finne_tom_endringsdato_hvor_siste_periode_i_revurdering_er_delt_i_to_med_ingen_dagsats(){
        // Arrange : Førstegangsbehandling
        BeregningsresultatPeriode periode1 = opprettPeriode(brFørstegangsbehandling, LocalDate.of(2018, 10, 1),
            LocalDate.of(2018, 12, 15));
        opprettAndel(periode1, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 1322, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        BeregningsresultatPeriode periode2 = opprettPeriode(brFørstegangsbehandling, LocalDate.of(2018, 12, 16),
            LocalDate.of(2019, 2, 17));
        opprettAndel(periode2, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 0, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        beregningsresultatRepository.lagre(førstegangsbehandling, brFørstegangsbehandling);
        // Arrange : Revurdering
        BeregningsresultatPeriode periode3 = opprettPeriode(brRevurdering, LocalDate.of(2018, 10, 1),
            LocalDate.of(2018, 12, 15));
        opprettAndel(periode3, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 1322, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        BeregningsresultatPeriode periode4 = opprettPeriode(brRevurdering, LocalDate.of(2018, 12, 16),
            LocalDate.of(2019, 2, 1));
        opprettAndel(periode4, true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 0, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        BeregningsresultatPeriode periode5 = opprettPeriode(brRevurdering, LocalDate.of(2019, 2, 2),
            LocalDate.of(2019, 3, 2));
        opprettAndel(periode5, true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 0, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        beregningsresultatRepository.lagre(revurdering, brRevurdering);
        // Act
        Optional<LocalDate> endringsdato = finnEndringsdatoBeregningsresultatTjeneste.finnEndringsdato(revurdering, brRevurdering);
        // Assert
        assertThat(endringsdato).isEmpty();
    }

    @Test
    public void skal_ikke_finne_endringsdato_hvor_siste_periode_i_revurdering_er_delt_i_to_perioder_men_andelene_er_uendret(){
        // Arrange : Førstegangsbehandling
        BeregningsresultatPeriode originalPeriode1 = opprettPeriode(brFørstegangsbehandling, FØRSTE_AUGUST, FØRSTE_AUGUST.plusDays(6));
        opprettAndel(originalPeriode1, true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER, ORGNR,
            1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        BeregningsresultatPeriode originalPeriode2 = opprettPeriode(brFørstegangsbehandling, FØRSTE_AUGUST.plusDays(7), FØRSTE_AUGUST.plusDays(13));
        opprettAndel(originalPeriode2, true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER, ORGNR,
            1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        beregningsresultatRepository.lagre(førstegangsbehandling, brFørstegangsbehandling);
        // Arrange : Revurdering
        BeregningsresultatPeriode revurderingPeriode1 = opprettPeriode(brRevurdering, FØRSTE_AUGUST, FØRSTE_AUGUST.plusDays(6));
        opprettAndel(revurderingPeriode1, true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER, ORGNR,
            1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        BeregningsresultatPeriode revurderingPeriode2 = opprettPeriode(brRevurdering, FØRSTE_AUGUST.plusDays(7), FØRSTE_AUGUST.plusDays(9));
        opprettAndel(revurderingPeriode2, true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER, ORGNR,
            1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        BeregningsresultatPeriode revurderingPeriode3 = opprettPeriode(brRevurdering, FØRSTE_AUGUST.plusDays(10), FØRSTE_AUGUST.plusDays(13));
        opprettAndel(revurderingPeriode3, true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER, ORGNR,
            1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        beregningsresultatRepository.lagre(revurdering, brRevurdering);
        // Act
        Optional<LocalDate> endringsdato = finnEndringsdatoBeregningsresultatTjeneste.finnEndringsdato(revurdering, brRevurdering);
        // Assert
        assertThat(endringsdato).isEmpty();
    }

    @Test
    public void skal_finne_endringsdato_hvor_den_siste_perioden_i_revurderingen_ikke_har_en_korresponderende_periode(){
        // Arrange : Førstegangsbehandling
        BeregningsresultatPeriode originalPeriode1 = opprettPeriode(brFørstegangsbehandling, FØRSTE_AUGUST, FØRSTE_AUGUST.plusDays(6));
        opprettAndel(originalPeriode1, true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        beregningsresultatRepository.lagre(førstegangsbehandling, brFørstegangsbehandling);
        // Arrange : Revurdering
        BeregningsresultatPeriode revurderingPeriode1 = opprettPeriode(brRevurdering, FØRSTE_AUGUST, FØRSTE_AUGUST.plusDays(6));
        opprettAndel(revurderingPeriode1, true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        BeregningsresultatPeriode revurderingPeriode2 = opprettPeriode(brRevurdering, FØRSTE_AUGUST.plusDays(7), FØRSTE_AUGUST.plusDays(13));
        opprettAndel(revurderingPeriode2, true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        beregningsresultatRepository.lagre(revurdering, brRevurdering);
        // Act
        Optional<LocalDate> endringsdato = finnEndringsdatoBeregningsresultatTjeneste.finnEndringsdato(revurdering, brRevurdering);
        // Assert
        assertThat(endringsdato).hasValueSatisfying(ed -> assertThat(ed).isEqualTo(FØRSTE_AUGUST.plusDays(7)));
    }

    @Test
    public void skal_finne_endringsdato_hvor_den_første_perioden_i_førstegangsbehandlingen_ikke_har_en_korresponderende_periode(){
        // Arrange : Førstegangsbehandling
        BeregningsresultatPeriode originalPeriode1 = opprettPeriode(brFørstegangsbehandling, FØRSTE_AUGUST, FØRSTE_AUGUST.plusDays(6));
        opprettAndel(originalPeriode1, true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        BeregningsresultatPeriode originalPeriode2 = opprettPeriode(brFørstegangsbehandling, FØRSTE_AUGUST.plusDays(7), FØRSTE_AUGUST.plusDays(13));
        opprettAndel(originalPeriode2, true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        beregningsresultatRepository.lagre(førstegangsbehandling, brFørstegangsbehandling);
        // Arrange : Revurdering
        BeregningsresultatPeriode revurderingPeriode1 = opprettPeriode(brRevurdering, FØRSTE_AUGUST.plusDays(7), FØRSTE_AUGUST.plusDays(13));
        opprettAndel(revurderingPeriode1, true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        beregningsresultatRepository.lagre(revurdering, brRevurdering);
        // Act
        Optional<LocalDate> endringsdato = finnEndringsdatoBeregningsresultatTjeneste.finnEndringsdato(revurdering, brRevurdering);
        // Assert
        assertThat(endringsdato).hasValueSatisfying(ed -> assertThat(ed).isEqualTo(FØRSTE_AUGUST));
    }

    @Test
    public void skal_finne_endringsdato_hvor_revurdering_har_forskjellig_antall_andeler_fra_førstegangsbehandling(){
        // Arrange : Førstegangsbehandling
        BeregningsresultatPeriode originalPeriode1 = opprettPeriode(brFørstegangsbehandling, FØRSTE_AUGUST, FØRSTE_AUGUST.plusDays(6));
        opprettAndel(originalPeriode1, true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        BeregningsresultatPeriode originalPeriode2 = opprettPeriode(brFørstegangsbehandling, FØRSTE_AUGUST.plusDays(7), FØRSTE_AUGUST.plusDays(14));
        opprettAndel(originalPeriode2, true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        beregningsresultatRepository.lagre(førstegangsbehandling, brFørstegangsbehandling);
        // Arrange : Revurdering
        BeregningsresultatPeriode revurderingPeriode1 = opprettPeriode(brRevurdering, FØRSTE_AUGUST, FØRSTE_AUGUST.plusDays(6));
        opprettAndel(revurderingPeriode1, true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        BeregningsresultatPeriode revurderingPeriode2 = opprettPeriode(brRevurdering, FØRSTE_AUGUST.plusDays(7), FØRSTE_AUGUST.plusDays(14));
        opprettAndel(revurderingPeriode2, true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        opprettAndel(revurderingPeriode2, true, InternArbeidsforholdRef.nyRef(), AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            "2", 1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        beregningsresultatRepository.lagre(revurdering, brRevurdering);
        // Act
        Optional<LocalDate> endringsdato = finnEndringsdatoBeregningsresultatTjeneste.finnEndringsdato(revurdering, brRevurdering);
        // Assert
        assertThat(endringsdato).hasValueSatisfying(ed -> assertThat(ed).isEqualTo(FØRSTE_AUGUST.plusDays(7)));
    }

    @Test
    public void skal_finne_endringsdato_hvor_andel_i_revurderingen_er_endret_fra_andelen_i_førstegangsbehandlingen(){
        // Arrange : Førstegangsbehandling
        BeregningsresultatPeriode originalPeriode1 = opprettPeriode(brFørstegangsbehandling, FØRSTE_AUGUST, FØRSTE_AUGUST.plusDays(6));
        opprettAndel(originalPeriode1, true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        BeregningsresultatPeriode originalPeriode2 = opprettPeriode(brFørstegangsbehandling, FØRSTE_AUGUST.plusDays(7), FØRSTE_AUGUST.plusDays(13));
        opprettAndel(originalPeriode2, true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        beregningsresultatRepository.lagre(førstegangsbehandling, brFørstegangsbehandling);
        // Arrange : Revurdering
        BeregningsresultatPeriode revurderingPeriode1 = opprettPeriode(brRevurdering, FØRSTE_AUGUST, FØRSTE_AUGUST.plusDays(6));
        opprettAndel(revurderingPeriode1, true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        BeregningsresultatPeriode revurderingPeriode2 = opprettPeriode(brRevurdering, FØRSTE_AUGUST.plusDays(7), FØRSTE_AUGUST.plusDays(13));
        opprettAndel(revurderingPeriode2, true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 2000, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        beregningsresultatRepository.lagre(revurdering, brRevurdering);
        // Act
        Optional<LocalDate> endringsdato = finnEndringsdatoBeregningsresultatTjeneste.finnEndringsdato(revurdering, brRevurdering);
        // Assert
        assertThat(endringsdato).hasValueSatisfying(ed -> assertThat(ed).isEqualTo(FØRSTE_AUGUST.plusDays(7)));
    }

    @Test
    public void skal_finne_endringsdato_hvor_siste_periode_i_revurdering_er_kortere_enn_siste_periode_i_førstegangsbehandling_og_hvor_andelene_er_uendret(){
        // Arrange : Førstegangsbehandling
        BeregningsresultatPeriode originalPeriode1 = opprettPeriode(brFørstegangsbehandling, FØRSTE_AUGUST, FØRSTE_AUGUST.plusDays(6));
        opprettAndel(originalPeriode1, true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        BeregningsresultatPeriode originalPeriode2 = opprettPeriode(brFørstegangsbehandling, FØRSTE_AUGUST.plusDays(7), FØRSTE_AUGUST.plusDays(13));
        opprettAndel(originalPeriode2, true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        beregningsresultatRepository.lagre(førstegangsbehandling, brFørstegangsbehandling);
        // Arrange : Revurdering
        BeregningsresultatPeriode revurderingPeriode1 = opprettPeriode(brRevurdering, FØRSTE_AUGUST, FØRSTE_AUGUST.plusDays(6));
        opprettAndel(revurderingPeriode1, true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        BeregningsresultatPeriode revurderingPeriode2 = opprettPeriode(brRevurdering, FØRSTE_AUGUST.plusDays(7), FØRSTE_AUGUST.plusDays(10));
        opprettAndel(revurderingPeriode2, true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        beregningsresultatRepository.lagre(revurdering, brRevurdering);
        // Act
        Optional<LocalDate> endringsdato = finnEndringsdatoBeregningsresultatTjeneste.finnEndringsdato(revurdering, brRevurdering);
        // Assert
        assertThat(endringsdato).hasValueSatisfying(ed -> assertThat(ed).isEqualTo(FØRSTE_AUGUST.plusDays(11)));
    }

    @Test
    public void skal_finne_endringsdato_hvor_den_første_perioden_i_revurderingen_er_lengere_enn_første_periode_i_førstegangsbehandlingen_og_hvor_andelene_er_undret(){
        // Arrange : Førstegangsbehandling
        BeregningsresultatPeriode originalPeriode1 = opprettPeriode(brFørstegangsbehandling, FØRSTE_AUGUST, FØRSTE_AUGUST.plusDays(6));
        opprettAndel(originalPeriode1, true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        BeregningsresultatPeriode originalPeriode2 = opprettPeriode(brFørstegangsbehandling, FØRSTE_AUGUST.plusDays(7), FØRSTE_AUGUST.plusDays(13));
        opprettAndel(originalPeriode2, true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        beregningsresultatRepository.lagre(førstegangsbehandling, brFørstegangsbehandling);
        // Arrange : Revurdering
        BeregningsresultatPeriode revurderingPeriode1 = opprettPeriode(brRevurdering, FØRSTE_AUGUST, FØRSTE_AUGUST.plusDays(9));
        opprettAndel(revurderingPeriode1, true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        BeregningsresultatPeriode revurderingPeriode2 = opprettPeriode(brRevurdering, FØRSTE_AUGUST.plusDays(10), FØRSTE_AUGUST.plusDays(16));
        opprettAndel(revurderingPeriode2, true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR, 1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        beregningsresultatRepository.lagre(revurdering, brRevurdering);
        // Act
        Optional<LocalDate> endringsdato = finnEndringsdatoBeregningsresultatTjeneste.finnEndringsdato(revurdering, brRevurdering);
        // Assert
        assertThat(endringsdato).hasValueSatisfying(ed -> assertThat(ed).isEqualTo(FØRSTE_AUGUST.plusDays(14)));
    }

    private Behandling opprettRevurdering(Behandling førstegangsbehandling) {
        return Behandling.fraTidligereBehandling(førstegangsbehandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_MANGLER_FØDSEL)
                .medOriginalBehandlingId(førstegangsbehandling.getId()))
            .build();
    }

    private BeregningsresultatPeriode opprettPeriode(BeregningsresultatEntitet beregningsresultat, LocalDate fom, LocalDate tom){
        return BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(fom, tom)
            .build(beregningsresultat);
    }

    private void opprettAndel(BeregningsresultatPeriode beregningsresultatPeriode, boolean erBrukerMottaker,
                              InternArbeidsforholdRef arbeidsforholdId, AktivitetStatus aktivitetStatus,
                              Inntektskategori inntektskategori, String orgNr, int dagsats,
                              BigDecimal stillingsprosent, BigDecimal utbetalingsgrad) {
        BeregningsresultatAndel.builder()
            .medBrukerErMottaker(erBrukerMottaker)
            .medArbeidsgiver(Arbeidsgiver.virksomhet(orgNr))
            .medArbeidsforholdRef(arbeidsforholdId)
            .medAktivitetStatus(aktivitetStatus)
            .medInntektskategori(inntektskategori)
            .medStillingsprosent(stillingsprosent)
            .medUtbetalingsgrad(utbetalingsgrad)
            .medDagsats(dagsats)
            .medDagsatsFraBg(dagsats)
            .build(beregningsresultatPeriode);
    }

}
