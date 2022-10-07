package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatDokRegelEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeSøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.Totrinnsvurdering;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.app.UttakPeriodeEndringDtoTjeneste;

@CdiDbAwareTest
public class UttakPeriodeEndringDtoTjenesteTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Inject
    private UttakPeriodeEndringDtoTjeneste uttakPeriodeEndringDtoTjeneste;

    private Behandling behandling;
    private LocalDate dato;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private FpUttakRepository fpUttakRepository;

    @BeforeEach
    public void setUp() {
        ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        fpUttakRepository = repositoryProvider.getFpUttakRepository();
        dato = LocalDate.of(2018, 8, 1);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        behandling = scenario.lagre(repositoryProvider);
    }

    @Test
    public void hent_endring_på_uttak_perioder_med_aksjonspunkt_avklar_fakta_uttak_finn_en_endret_periode_en_avklart_periode_og_en_slettet_periode() {

        // Legg til 3 gamle perioder
        var gammelPeriode1 = OppgittPeriodeBuilder.ny()
            .medPeriode(dato.minusMonths(3), dato.minusMonths(2))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();
        var gammelPeriode2 = OppgittPeriodeBuilder.ny()
            .medPeriode(dato.minusMonths(2).plusDays(1), dato.minusMonths(1).plusDays(1))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();
        var gammelPeriode3 = OppgittPeriodeBuilder.ny()
            .medPeriode(dato.minusMonths(1).plusDays(2), dato.plusDays(2))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();
        var gamlePerioder = new ArrayList<OppgittPeriodeEntitet>();
        gamlePerioder.add(gammelPeriode1);
        gamlePerioder.add(gammelPeriode2);
        gamlePerioder.add(gammelPeriode3);
        var gammelFordeling = new OppgittFordelingEntitet(gamlePerioder, true);
        var yfBuilder = ytelsesFordelingRepository.opprettBuilder(behandling.getId())
            .medOppgittFordeling(gammelFordeling);
        ytelsesFordelingRepository.lagre(behandling.getId(), yfBuilder.build());

        // Legg til 2 ny periode
        var nyPeriode1 = OppgittPeriodeBuilder.ny()
            .medPeriode(dato.minusMonths(2).plusWeeks(1), dato.minusMonths(1).plusWeeks(1))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();
        var nyPeriode2 = OppgittPeriodeBuilder.ny()
            .medPeriode(dato.minusMonths(1).plusWeeks(1).plusDays(1), dato.plusWeeks(1).plusDays(1))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medBegrunnelse("Dette er en kort begrunnelse for hvorfor denne ble avklart.")
            .build();
        var nyePerioder = new ArrayList<OppgittPeriodeEntitet>();
        nyePerioder.add(nyPeriode1);
        nyePerioder.add(nyPeriode2);
        var nyFordeling = new OppgittFordelingEntitet(nyePerioder, true);
        var nyYfBuilder = ytelsesFordelingRepository.opprettBuilder(behandling.getId())
            .medOverstyrtFordeling(nyFordeling);
        ytelsesFordelingRepository.lagre(behandling.getId(), nyYfBuilder.build());

        // Legg til data i totrinnsvurdering.
        var ttvurderingBuilder = new Totrinnsvurdering.Builder(behandling,
            AksjonspunktDefinisjon.AVKLAR_FAKTA_UTTAK_KONTROLLER_SØKNADSPERIODER);
        var ttvurdering = ttvurderingBuilder.medGodkjent(false).medBegrunnelse("").build();

        // Hent endring på perioder
        var uttakPeriodeEndringer = uttakPeriodeEndringDtoTjeneste.hentEndringPåUttakPerioder(ttvurdering, behandling,
            Optional.empty());

        assertThat(uttakPeriodeEndringer).hasSize(3);

        // assert på første av 3 endringer
        assertThat(uttakPeriodeEndringer.get(0).getFom()).isEqualTo(LocalDate.of(2018, 6, 8));
        assertThat(uttakPeriodeEndringer.get(0).getTom()).isEqualTo(LocalDate.of(2018, 7, 8));
        assertThat(uttakPeriodeEndringer.get(0).getErEndret()).isTrue();

        // assert på andre av 3 endringer
        assertThat(uttakPeriodeEndringer.get(1).getFom()).isEqualTo(LocalDate.of(2018, 7, 3));
        assertThat(uttakPeriodeEndringer.get(1).getTom()).isEqualTo(LocalDate.of(2018, 8, 3));
        assertThat(uttakPeriodeEndringer.get(1).getErSlettet()).isTrue();

        // assert på tredje av 3 endringer
        assertThat(uttakPeriodeEndringer.get(2).getFom()).isEqualTo(LocalDate.of(2018, 7, 9));
        assertThat(uttakPeriodeEndringer.get(2).getTom()).isEqualTo(LocalDate.of(2018, 8, 9));
        assertThat(uttakPeriodeEndringer.get(2).getErAvklart()).isTrue();

    }

    @Test
    public void hent_endring_på_uttak_perioder_med_aksjonspunkt_avklar_fakta_uttak_finn_en_endret_periode_en_lagt_til_periode_og_filtrer_ut_en_periode() {

        // Legg til 1 gammel perioder
        var gammelPeriode = OppgittPeriodeBuilder.ny()
            .medPeriode(dato.minusMonths(2), dato.minusMonths(1))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();
        var gammelFordeling = new OppgittFordelingEntitet(List.of(gammelPeriode), true);
        var yfBuilder = ytelsesFordelingRepository.opprettBuilder(behandling.getId())
            .medOppgittFordeling(gammelFordeling);
        ytelsesFordelingRepository.lagre(behandling.getId(), yfBuilder.build());

        // Legg til 2 ny periode
        var nyPeriode1 = OppgittPeriodeBuilder.ny()
            .medPeriode(dato.minusMonths(3).plusDays(3), dato.minusMonths(2).plusDays(3))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();
        var nyPeriode2 = OppgittPeriodeBuilder.ny()
            .medPeriode(dato.minusMonths(2).plusDays(4), dato.minusMonths(1).plusDays(4))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medBegrunnelse("Dette er en kort begrunnelse for hvorfor denne ble avklart.")
            .build();
        var nyPeriode3 = OppgittPeriodeBuilder.ny()
            .medPeriode(dato.minusMonths(1).plusDays(5), dato.plusDays(5))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriodeKilde(FordelingPeriodeKilde.TIDLIGERE_VEDTAK) // Denne burde bli ignorert pga. kilde
            .build();
        var nyePerioder = new ArrayList<OppgittPeriodeEntitet>();
        nyePerioder.add(nyPeriode1);
        nyePerioder.add(nyPeriode2);
        nyePerioder.add(nyPeriode3);
        var nyFordeling = new OppgittFordelingEntitet(nyePerioder, true);

        var nyYfBuilder = ytelsesFordelingRepository.opprettBuilder(behandling.getId())
            .medOverstyrtFordeling(nyFordeling);
        ytelsesFordelingRepository.lagre(behandling.getId(), nyYfBuilder.build());

        // Legg til data i totrinnsvurdering.
        var ttvurderingBuilder = new Totrinnsvurdering.Builder(behandling,
            AksjonspunktDefinisjon.AVKLAR_FAKTA_UTTAK_KONTROLLER_SØKNADSPERIODER);
        var ttvurdering = ttvurderingBuilder.medGodkjent(false).medBegrunnelse("").build();

        // Hent endring på perioder
        var uttakPeriodeEndringer = uttakPeriodeEndringDtoTjeneste.hentEndringPåUttakPerioder(ttvurdering, behandling,
            Optional.empty());

        assertThat(uttakPeriodeEndringer).hasSize(3);

        // assert på første av 2 endringer
        assertThat(uttakPeriodeEndringer.get(0).getFom()).isEqualTo(LocalDate.of(2018, 5, 4));
        assertThat(uttakPeriodeEndringer.get(0).getTom()).isEqualTo(LocalDate.of(2018, 6, 4));
        assertThat(uttakPeriodeEndringer.get(0).getErEndret()).isTrue();

        // assert på andre av 2 endringer
        assertThat(uttakPeriodeEndringer.get(1).getFom()).isEqualTo(LocalDate.of(2018, 6, 5));
        assertThat(uttakPeriodeEndringer.get(1).getTom()).isEqualTo(LocalDate.of(2018, 7, 5));
        assertThat(uttakPeriodeEndringer.get(1).getErLagtTil()).isTrue();

    }

    @Test
    public void hent_endring_på_uttak_perioder_med_aksjonspunkt_fastsett_uttakperioder_finn_endret_utakk_resultat_periode() {

        // Legg til opprinnelig periode
        var opprinneligPeriode = opprettUttakResultatPeriode(PeriodeResultatType.MANUELL_BEHANDLING, dato,
            dato.plusMonths(1), StønadskontoType.FORELDREPENGER, new BigDecimal("100"), new Utbetalingsgrad(100));
        var opprinneligFordeling = new UttakResultatPerioderEntitet();
        opprinneligFordeling.leggTilPeriode(opprinneligPeriode);
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandling.getId(), opprinneligFordeling);

        // Legg til overstyrende periode
        var overstyrendePeriode = opprettUttakResultatPeriode(PeriodeResultatType.INNVILGET, dato, dato.plusMonths(1),
            StønadskontoType.FORELDREPENGER, new BigDecimal("100"), new Utbetalingsgrad(100));
        var overstyrendeFordeling = new UttakResultatPerioderEntitet();
        overstyrendeFordeling.leggTilPeriode(overstyrendePeriode);
        fpUttakRepository.lagreOverstyrtUttakResultatPerioder(behandling.getId(), overstyrendeFordeling);

        // Legg til data i totrinnsvurdering.
        var ttvurderingBuilder = new Totrinnsvurdering.Builder(behandling,
            AksjonspunktDefinisjon.FASTSETT_UTTAKPERIODER);
        var ttvurdering = ttvurderingBuilder.medGodkjent(false).medBegrunnelse("").build();

        // Hent endring på perioder
        var uttakPeriodeEndringer = uttakPeriodeEndringDtoTjeneste.hentEndringPåUttakPerioder(ttvurdering, behandling,
            Optional.empty());

        assertThat(uttakPeriodeEndringer).hasSize(1);
        assertThat(uttakPeriodeEndringer.get(0).getErEndret()).isTrue();
        assertThat(uttakPeriodeEndringer.get(0).getFom()).isEqualTo(LocalDate.of(2018, 8, 1));
        assertThat(uttakPeriodeEndringer.get(0).getTom()).isEqualTo(LocalDate.of(2018, 9, 1));

    }

    @Test
    public void hent_endring_på_uttak_perioder_med_aksjonspunkt_overstyring_av_uttakperioder_finn_lagt_til_utakk_resultat_periode() {

        // Legg til opprinnelig periode
        var opprinneligPeriode = opprettUttakResultatPeriode(PeriodeResultatType.MANUELL_BEHANDLING, dato,
            dato.plusMonths(1), StønadskontoType.FORELDREPENGER, new BigDecimal("100"), new Utbetalingsgrad(100));
        var opprinneligFordeling = new UttakResultatPerioderEntitet();
        opprinneligFordeling.leggTilPeriode(opprinneligPeriode);
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandling.getId(), opprinneligFordeling);

        // Legg til overstyrende periode
        var overstyrendePeriode = opprettUttakResultatPeriode(PeriodeResultatType.INNVILGET, dato.plusWeeks(2),
            dato.plusMonths(1).plusWeeks(2), StønadskontoType.FORELDREPENGER, new BigDecimal("100"),
            new Utbetalingsgrad(100));
        var overstyrendeFordeling = new UttakResultatPerioderEntitet();
        overstyrendeFordeling.leggTilPeriode(overstyrendePeriode);
        fpUttakRepository.lagreOverstyrtUttakResultatPerioder(behandling.getId(), overstyrendeFordeling);

        // Legg til data i totrinnsvurdering.
        var ttvurderingBuilder = new Totrinnsvurdering.Builder(behandling,
            AksjonspunktDefinisjon.OVERSTYRING_AV_UTTAKPERIODER);
        var ttvurdering = ttvurderingBuilder.medGodkjent(false).medBegrunnelse("").build();

        // Hent endring på perioder
        var uttakPeriodeEndringer = uttakPeriodeEndringDtoTjeneste.hentEndringPåUttakPerioder(ttvurdering, behandling,
            Optional.empty());

        assertThat(uttakPeriodeEndringer).hasSize(1);
        assertThat(uttakPeriodeEndringer.get(0).getErLagtTil()).isTrue();
        assertThat(uttakPeriodeEndringer.get(0).getFom()).isEqualTo(LocalDate.of(2018, 8, 15));
        assertThat(uttakPeriodeEndringer.get(0).getTom()).isEqualTo(LocalDate.of(2018, 9, 15));

    }

    private UttakResultatPeriodeEntitet opprettUttakResultatPeriode(PeriodeResultatType resultat,
                                                                    LocalDate fom,
                                                                    LocalDate tom,
                                                                    StønadskontoType stønadskontoType,
                                                                    BigDecimal graderingArbeidsprosent,
                                                                    Utbetalingsgrad ubetalingsgrad) {
        var uttakAktivitet = new UttakAktivitetEntitet.Builder()
            .medArbeidsforhold(Arbeidsgiver.virksomhet(OrgNummer.KUNSTIG_ORG), InternArbeidsforholdRef.nyRef())
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .build();
        var periodeSøknad = new UttakResultatPeriodeSøknadEntitet.Builder()
            .medUttakPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medGraderingArbeidsprosent(graderingArbeidsprosent)
            .medSamtidigUttak(true)
            .medSamtidigUttaksprosent(SamtidigUttaksprosent.TEN)
            .build();
        var dokRegel = UttakResultatDokRegelEntitet.utenManuellBehandling()
            .medRegelInput(" ")
            .medRegelEvaluering(" ")
            .build();
        var uttakResultatPeriode = new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medDokRegel(dokRegel)
            .medResultatType(resultat, PeriodeResultatÅrsak.UKJENT)
            .medPeriodeSoknad(periodeSøknad)
            .build();
        var periodeAktivitet = UttakResultatPeriodeAktivitetEntitet.builder(uttakResultatPeriode, uttakAktivitet)
            .medTrekkonto(stønadskontoType)
            .medTrekkdager(new Trekkdager(10))
            .medArbeidsprosent(graderingArbeidsprosent)
            .medUtbetalingsgrad(ubetalingsgrad)
            .build();
        uttakResultatPeriode.leggTilAktivitet(periodeAktivitet);
        return uttakResultatPeriode;
    }

}
