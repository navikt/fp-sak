package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.totrinn.Totrinnsvurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
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
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.app.UttakPeriodeEndringDtoTjeneste;

@CdiDbAwareTest
class UttakPeriodeEndringDtoTjenesteTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Inject
    private UttakPeriodeEndringDtoTjeneste uttakPeriodeEndringDtoTjeneste;

    private Behandling behandling;
    private LocalDate dato;
    private FpUttakRepository fpUttakRepository;

    @BeforeEach
    public void setUp() {
        fpUttakRepository = repositoryProvider.getFpUttakRepository();
        dato = LocalDate.of(2018, 8, 1);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        behandling = scenario.lagre(repositoryProvider);
    }

    @Test
    void hent_endring_på_uttak_perioder_med_aksjonspunkt_fastsett_uttakperioder_finn_endret_utakk_resultat_periode() {

        // Legg til opprinnelig periode
        var opprinneligPeriode = opprettUttakResultatPeriode(PeriodeResultatType.MANUELL_BEHANDLING, dato,
            dato.plusMonths(1), UttakPeriodeType.FORELDREPENGER, new BigDecimal("100"), new Utbetalingsgrad(100));
        var opprinneligFordeling = new UttakResultatPerioderEntitet();
        opprinneligFordeling.leggTilPeriode(opprinneligPeriode);
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandling.getId(), opprinneligFordeling);

        // Legg til overstyrende periode
        var overstyrendePeriode = opprettUttakResultatPeriode(PeriodeResultatType.INNVILGET, dato, dato.plusMonths(1),
            UttakPeriodeType.FORELDREPENGER, new BigDecimal("100"), new Utbetalingsgrad(100));
        var overstyrendeFordeling = new UttakResultatPerioderEntitet();
        overstyrendeFordeling.leggTilPeriode(overstyrendePeriode);
        fpUttakRepository.lagreOverstyrtUttakResultatPerioder(behandling.getId(), overstyrendeFordeling);

        // Legg til data i totrinnsvurdering.
        var ttvurderingBuilder = new Totrinnsvurdering.Builder(behandling,
            AksjonspunktDefinisjon.FASTSETT_UTTAKPERIODER);
        var ttvurdering = ttvurderingBuilder.medGodkjent(false).medBegrunnelse("").build();

        // Hent endring på perioder
        var uttakPeriodeEndringer = uttakPeriodeEndringDtoTjeneste.hentEndringPåUttakPerioder(ttvurdering, behandling);

        assertThat(uttakPeriodeEndringer).hasSize(1);
        assertThat(uttakPeriodeEndringer.get(0).getErEndret()).isTrue();
        assertThat(uttakPeriodeEndringer.get(0).getFom()).isEqualTo(LocalDate.of(2018, 8, 1));
        assertThat(uttakPeriodeEndringer.get(0).getTom()).isEqualTo(LocalDate.of(2018, 9, 1));

    }

    @Test
    void hent_endring_på_uttak_perioder_med_aksjonspunkt_overstyring_av_uttakperioder_finn_lagt_til_utakk_resultat_periode() {

        // Legg til opprinnelig periode
        var opprinneligPeriode = opprettUttakResultatPeriode(PeriodeResultatType.MANUELL_BEHANDLING, dato,
            dato.plusMonths(1), UttakPeriodeType.FORELDREPENGER, new BigDecimal("100"), new Utbetalingsgrad(100));
        var opprinneligFordeling = new UttakResultatPerioderEntitet();
        opprinneligFordeling.leggTilPeriode(opprinneligPeriode);
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandling.getId(), opprinneligFordeling);

        // Legg til overstyrende periode
        var overstyrendePeriode = opprettUttakResultatPeriode(PeriodeResultatType.INNVILGET, dato.plusWeeks(2),
            dato.plusMonths(1).plusWeeks(2), UttakPeriodeType.FORELDREPENGER, new BigDecimal("100"),
            new Utbetalingsgrad(100));
        var overstyrendeFordeling = new UttakResultatPerioderEntitet();
        overstyrendeFordeling.leggTilPeriode(overstyrendePeriode);
        fpUttakRepository.lagreOverstyrtUttakResultatPerioder(behandling.getId(), overstyrendeFordeling);

        // Legg til data i totrinnsvurdering.
        var ttvurderingBuilder = new Totrinnsvurdering.Builder(behandling,
            AksjonspunktDefinisjon.OVERSTYRING_AV_UTTAKPERIODER);
        var ttvurdering = ttvurderingBuilder.medGodkjent(false).medBegrunnelse("").build();

        // Hent endring på perioder
        var uttakPeriodeEndringer = uttakPeriodeEndringDtoTjeneste.hentEndringPåUttakPerioder(ttvurdering, behandling);

        assertThat(uttakPeriodeEndringer).hasSize(1);
        assertThat(uttakPeriodeEndringer.get(0).getErLagtTil()).isTrue();
        assertThat(uttakPeriodeEndringer.get(0).getFom()).isEqualTo(LocalDate.of(2018, 8, 15));
        assertThat(uttakPeriodeEndringer.get(0).getTom()).isEqualTo(LocalDate.of(2018, 9, 15));

    }

    private UttakResultatPeriodeEntitet opprettUttakResultatPeriode(PeriodeResultatType resultat,
                                                                    LocalDate fom,
                                                                    LocalDate tom,
                                                                    UttakPeriodeType stønadskontoType,
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
        var dokRegel = new UttakResultatDokRegelEntitet.Builder()
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
