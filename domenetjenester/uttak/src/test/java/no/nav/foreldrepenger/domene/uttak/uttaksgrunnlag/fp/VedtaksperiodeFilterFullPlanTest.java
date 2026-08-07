package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeSøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakUtsettelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

/**
 * Probesuite for scenariet der endringssøknaden inneholder <em>hele</em> uttaksplanen, altså at
 * frontend ikke lenger avkorter planen fra egen-detektert endringsdato, men overlater
 * endringsdato-deteksjonen til {@link VedtaksperiodeFilter}.
 * <p>
 * Hver test asserterer <b>dagens</b> oppførsel. Der dagens oppførsel avviker fra ønsket oppførsel
 * ved full plan, er målbildet dokumentert i en MÅLBILDE-kommentar rett over assert'en. Testene som
 * da feiler ved en omlegging er nøyaktig de scenariene som endrer semantikk.
 * <p>
 * Forutsetninger som ligger til grunn for målbildet:
 * <ul>
 *   <li>Full plan rendres fra det <em>innvilgede</em> vedtaket. Avslåtte perioder blir hull i planen,
 *       og et hull som gjensøkes er en reell endring (skal vurderes på nytt).</li>
 *   <li>Fjerning av uttak uttrykkes med en utsettelse av type {@link UtsettelseÅrsak#FRI}, ikke med hull.</li>
 * </ul>
 */
class VedtaksperiodeFilterFullPlanTest {

    private static final long BEHANDLING_ID = 1L;

    private static final LocalDate FØDSEL = LocalDate.of(2024, 1, 1); // mandag
    private static final LocalDate FPFF_FOM = LocalDate.of(2023, 12, 11);
    private static final LocalDate FPFF_TOM = LocalDate.of(2023, 12, 31);
    private static final LocalDate MK_FOM = FØDSEL;
    private static final LocalDate MK_TOM = LocalDate.of(2024, 3, 10);
    private static final LocalDate FP_FOM = LocalDate.of(2024, 3, 11);
    private static final LocalDate FP_TOM = LocalDate.of(2024, 6, 30);
    private static final LocalDate HALE_FOM = FP_TOM.plusDays(3); // 2024-07-03, mandag

    private static final UttakAktivitetEntitet AKTIVITET = new UttakAktivitetEntitet.Builder()
        .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
        .medArbeidsforhold(Arbeidsgiver.virksomhet(OrgNummer.KUNSTIG_ORG), InternArbeidsforholdRef.nyRef())
        .build();

    /**
     * Endringsdatoen slik den faller ut av filteret: tidligste fom blant periodene som beholdes.
     * {@code null} betyr at hele planen ble filtrert bort, dvs. ingen endring.
     */
    private static LocalDate endringsdato(List<OppgittPeriodeEntitet> plan, UttakResultatEntitet vedtak, boolean beholdSenestePeriode) {
        return VedtaksperiodeFilter.filtrerVekkPerioderSomErLikeInnvilgetUttak(BEHANDLING_ID, plan, vedtak, beholdSenestePeriode).stream()
            .map(OppgittPeriodeEntitet::getFom)
            .min(Comparator.naturalOrder())
            .orElse(null);
    }

    private static List<OppgittPeriodeEntitet> filtrer(List<OppgittPeriodeEntitet> plan, UttakResultatEntitet vedtak, boolean beholdSeneste) {
        return VedtaksperiodeFilter.filtrerVekkPerioderSomErLikeInnvilgetUttak(BEHANDLING_ID, plan, vedtak, beholdSeneste);
    }

    @Nested
    class EndringerSomSkalDetekteres {

        @Test
        void ny_utsettelse_midt_i_plan_gir_endringsdato_fra_utsettelsen() {
            var utsFom = LocalDate.of(2024, 5, 1);
            var utsTom = LocalDate.of(2024, 5, 15);
            var plan = List.of(
                søkt(FPFF_FOM, FPFF_TOM, UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL),
                søkt(MK_FOM, MK_TOM, UttakPeriodeType.MØDREKVOTE),
                søkt(FP_FOM, utsFom.minusDays(1), UttakPeriodeType.FELLESPERIODE),
                søktUtsettelse(utsFom, utsTom, UtsettelseÅrsak.FERIE),
                søkt(utsTom.plusDays(1), FP_TOM.plusWeeks(3), UttakPeriodeType.FELLESPERIODE));

            assertThat(endringsdato(plan, uttak(normaltVedtak()), false)).isEqualTo(utsFom);
        }

        @Test
        void plan_utvidet_bakover_foran_vedtaket_gir_endringsdato_fra_ny_start() {
            List<OppgittPeriodeEntitet> plan = new ArrayList<>();
            plan.add(søkt(FPFF_FOM.minusWeeks(2), FPFF_FOM.minusWeeks(2).plusDays(1), UttakPeriodeType.FELLESPERIODE));
            plan.addAll(fullPlan());

            assertThat(endringsdato(plan, uttak(normaltVedtak()), false)).isEqualTo(FPFF_FOM.minusWeeks(2));
        }

        @Test
        void utsettelse_erstatter_vedtatt_uttak_midt_i_modrekvoten() {
            var plan = List.of(
                søkt(FPFF_FOM, FPFF_TOM, UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL),
                søkt(MK_FOM, LocalDate.of(2024, 2, 9), UttakPeriodeType.MØDREKVOTE),
                søktUtsettelse(LocalDate.of(2024, 2, 12), LocalDate.of(2024, 3, 8), UtsettelseÅrsak.ARBEID),
                søkt(LocalDate.of(2024, 3, 11), FP_TOM.plusWeeks(4), UttakPeriodeType.FELLESPERIODE));

            assertThat(endringsdato(plan, uttak(normaltVedtak()), false)).isEqualTo(LocalDate.of(2024, 2, 12));
        }

        @Test
        void fri_utsettelse_forst_i_plan_fjerner_forste_vedtatte_uttak() {
            // Konvensjonen for å fjerne innvilget uttak: send FRI-utsettelse i stedet for hull
            var plan = List.of(
                søktUtsettelse(FPFF_FOM, LocalDate.of(2024, 1, 12), UtsettelseÅrsak.FRI),
                søkt(LocalDate.of(2024, 1, 15), MK_TOM, UttakPeriodeType.MØDREKVOTE),
                søkt(FP_FOM, FP_TOM, UttakPeriodeType.FELLESPERIODE));

            assertThat(endringsdato(plan, uttak(normaltVedtak()), false)).isEqualTo(FPFF_FOM);
        }

        @Test
        void avslatt_periode_i_vedtak_som_gjensokes_er_en_reell_endring() {
            var avslagFom = LocalDate.of(2024, 2, 1);
            var avslagTom = LocalDate.of(2024, 2, 15);
            var vedtak = List.of(
                vedtaksperiode(FPFF_FOM, FPFF_TOM, UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL, PeriodeResultatType.INNVILGET, true),
                vedtaksperiode(MK_FOM, avslagFom.minusDays(1), UttakPeriodeType.MØDREKVOTE, PeriodeResultatType.INNVILGET, true),
                vedtaksperiode(avslagFom, avslagTom, UttakPeriodeType.MØDREKVOTE, PeriodeResultatType.AVSLÅTT, true),
                vedtaksperiode(avslagTom.plusDays(1), MK_TOM, UttakPeriodeType.MØDREKVOTE, PeriodeResultatType.INNVILGET, true),
                vedtaksperiode(FP_FOM, FP_TOM, UttakPeriodeType.FELLESPERIODE, PeriodeResultatType.INNVILGET, true));
            var plan = planMedHale();

            assertThat(endringsdato(plan, uttak(vedtak), false)).isEqualTo(avslagFom);
        }

        @Test
        void avslag_med_null_trekkdager_pga_annenpart_som_gjensokes_er_en_reell_endring() {
            var avslagFom = LocalDate.of(2024, 2, 1);
            var avslagTom = LocalDate.of(2024, 2, 15);
            var avslag = new UttakResultatPeriodeEntitet.Builder(avslagFom, avslagTom)
                .medResultatType(PeriodeResultatType.AVSLÅTT,
                    PeriodeResultatÅrsak.DEN_ANDRE_PART_OVERLAPPENDE_UTTAK_IKKE_SØKT_INNVILGET_SAMTIDIG_UTTAK)
                .medPeriodeSoknad(new UttakResultatPeriodeSøknadEntitet.Builder().medUttakPeriodeType(UttakPeriodeType.MØDREKVOTE).build())
                .build();
            UttakResultatPeriodeAktivitetEntitet.builder(avslag, AKTIVITET)
                .medTrekkdager(Trekkdager.ZERO)
                .medTrekkonto(UttakPeriodeType.MØDREKVOTE)
                .medUtbetalingsgrad(Utbetalingsgrad.ZERO)
                .medArbeidsprosent(BigDecimal.ZERO)
                .build();
            var vedtak = List.of(
                vedtaksperiode(FPFF_FOM, FPFF_TOM, UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL, PeriodeResultatType.INNVILGET, true),
                vedtaksperiode(MK_FOM, avslagFom.minusDays(1), UttakPeriodeType.MØDREKVOTE, PeriodeResultatType.INNVILGET, true),
                avslag,
                vedtaksperiode(avslagTom.plusDays(1), MK_TOM, UttakPeriodeType.MØDREKVOTE, PeriodeResultatType.INNVILGET, true),
                vedtaksperiode(FP_FOM, FP_TOM, UttakPeriodeType.FELLESPERIODE, PeriodeResultatType.INNVILGET, true));

            assertThat(endringsdato(planMedHale(), uttak(vedtak), false)).isEqualTo(avslagFom);
        }
    }

    @Nested
    class UendretPrefiksMedNyHale {

        @Test
        void ny_periode_bakerst_gir_endringsdato_fra_halen() {
            assertThat(endringsdato(planMedHale(), uttak(normaltVedtak()), false)).isEqualTo(HALE_FOM);
        }

        @Test
        void tidligere_innvilget_ferieutsettelse_gjentatt_i_planen_gir_ikke_endring() {
            var utsFom = LocalDate.of(2024, 5, 1);
            var utsTom = LocalDate.of(2024, 5, 15);
            var vedtak = List.of(
                vedtaksperiode(FPFF_FOM, FPFF_TOM, UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL, PeriodeResultatType.INNVILGET, true),
                vedtaksperiode(MK_FOM, MK_TOM, UttakPeriodeType.MØDREKVOTE, PeriodeResultatType.INNVILGET, true),
                vedtaksperiode(FP_FOM, utsFom.minusDays(1), UttakPeriodeType.FELLESPERIODE, PeriodeResultatType.INNVILGET, true),
                utsettelsesperiodeVedtak(utsFom, utsTom, UttakUtsettelseType.FERIE),
                vedtaksperiode(utsTom.plusDays(1), FP_TOM, UttakPeriodeType.FELLESPERIODE, PeriodeResultatType.INNVILGET, true));
            var plan = List.of(
                søkt(FPFF_FOM, FPFF_TOM, UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL),
                søkt(MK_FOM, MK_TOM, UttakPeriodeType.MØDREKVOTE),
                søkt(FP_FOM, utsFom.minusDays(1), UttakPeriodeType.FELLESPERIODE),
                søktUtsettelse(utsFom, utsTom, UtsettelseÅrsak.FERIE),
                søkt(utsTom.plusDays(1), FP_TOM, UttakPeriodeType.FELLESPERIODE),
                søkt(HALE_FOM, FP_TOM.plusWeeks(3), UttakPeriodeType.FELLESPERIODE));

            assertThat(endringsdato(plan, uttak(vedtak), false)).isEqualTo(HALE_FOM);
        }

        @Test
        void plan_oppdelt_per_uke_uten_helg_gir_ikke_falsk_endring() {
            List<OppgittPeriodeEntitet> plan = new ArrayList<>();
            plan.add(søkt(FPFF_FOM, FPFF_TOM, UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL));
            for (var uke = MK_FOM; !uke.isAfter(MK_TOM.minusDays(2)); uke = uke.plusWeeks(1)) {
                plan.add(søkt(uke, uke.plusDays(4), UttakPeriodeType.MØDREKVOTE));
            }
            plan.add(søkt(FP_FOM, FP_TOM, UttakPeriodeType.FELLESPERIODE));
            plan.add(søkt(HALE_FOM, FP_TOM.plusWeeks(3), UttakPeriodeType.FELLESPERIODE));

            assertThat(endringsdato(plan, uttak(normaltVedtak()), false)).isEqualTo(HALE_FOM);
        }

        @Test
        void plan_som_hopper_over_avslatt_periode_gir_ikke_falsk_endring() {
            var avslagFom = LocalDate.of(2024, 2, 1);
            var avslagTom = LocalDate.of(2024, 2, 15);
            var vedtak = List.of(
                vedtaksperiode(FPFF_FOM, FPFF_TOM, UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL, PeriodeResultatType.INNVILGET, true),
                vedtaksperiode(MK_FOM, avslagFom.minusDays(1), UttakPeriodeType.MØDREKVOTE, PeriodeResultatType.INNVILGET, true),
                vedtaksperiode(avslagFom, avslagTom, UttakPeriodeType.MØDREKVOTE, PeriodeResultatType.AVSLÅTT, true),
                vedtaksperiode(avslagTom.plusDays(1), MK_TOM, UttakPeriodeType.MØDREKVOTE, PeriodeResultatType.INNVILGET, true),
                vedtaksperiode(FP_FOM, FP_TOM, UttakPeriodeType.FELLESPERIODE, PeriodeResultatType.INNVILGET, true));
            var plan = List.of(
                søkt(FPFF_FOM, FPFF_TOM, UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL),
                søkt(MK_FOM, avslagFom.minusDays(1), UttakPeriodeType.MØDREKVOTE),
                søkt(avslagTom.plusDays(1), MK_TOM, UttakPeriodeType.MØDREKVOTE),
                søkt(FP_FOM, FP_TOM, UttakPeriodeType.FELLESPERIODE),
                søkt(HALE_FOM, FP_TOM.plusWeeks(3), UttakPeriodeType.FELLESPERIODE));

            assertThat(endringsdato(plan, uttak(vedtak), false)).isEqualTo(HALE_FOM);
        }
    }

    /**
     * Hull i planen tolkes i dag som fjerning av uttak. Ved full plan er hull normalt bare dager det
     * ikke søkes uttak for, og fjerning uttrykkes med FRI-utsettelse. Alle testene her har derfor
     * målbilde "ingen endring".
     */
    @Nested
    class HullOgUendretPlanTolkesSomEndringIDag {

        @Test
        void plan_helt_lik_vedtak_gir_syntetisk_fri_etter_siste_vedtaksdag() {
            // MÅLBILDE ved full plan: null (ingen endring)
            assertThat(endringsdato(fullPlan(), uttak(normaltVedtak()), false)).isEqualTo(FP_TOM.plusDays(1));

            var filtrert = filtrer(fullPlan(), uttak(normaltVedtak()), false);
            assertThat(filtrert).hasSize(1);
            assertThat(filtrert.getFirst().getÅrsak()).isEqualTo(UtsettelseÅrsak.FRI);
        }

        @Test
        void plan_lik_vedtak_med_beholdSenestePeriode_beholder_siste_vedtaksperiode() {
            // MÅLBILDE ved full plan: null (ingen endring)
            assertThat(endringsdato(fullPlan(), uttak(normaltVedtak()), true)).isEqualTo(FP_FOM);
        }

        @Test
        void plan_forkortet_bakerst_gir_fri_fra_forste_fjernede_dag() {
            var plan = List.of(
                søkt(FPFF_FOM, FPFF_TOM, UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL),
                søkt(MK_FOM, MK_TOM, UttakPeriodeType.MØDREKVOTE),
                søkt(FP_FOM, LocalDate.of(2024, 5, 3), UttakPeriodeType.FELLESPERIODE));

            // MÅLBILDE ved full plan: null - fjerning av halen må uttrykkes med FRI-utsettelse
            assertThat(endringsdato(plan, uttak(normaltVedtak()), false)).isEqualTo(LocalDate.of(2024, 5, 6));
        }

        @Test
        void hull_midt_i_plan_gir_endring_fra_hullets_start() {
            var plan = List.of(
                søkt(FPFF_FOM, FPFF_TOM, UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL),
                søkt(MK_FOM, MK_TOM, UttakPeriodeType.MØDREKVOTE),
                søkt(FP_FOM, LocalDate.of(2024, 4, 30), UttakPeriodeType.FELLESPERIODE),
                søkt(LocalDate.of(2024, 5, 16), FP_TOM, UttakPeriodeType.FELLESPERIODE));

            // MÅLBILDE ved full plan: null - fjerning må uttrykkes med FRI-utsettelse
            assertThat(endringsdato(plan, uttak(normaltVedtak()), false)).isEqualTo(LocalDate.of(2024, 5, 1));
        }

        @Test
        void internt_hull_i_starten_av_modrekvoten_gir_endring_fra_hullets_start() {
            var plan = List.of(
                søkt(FPFF_FOM, FPFF_TOM, UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL),
                søkt(LocalDate.of(2024, 1, 15), MK_TOM, UttakPeriodeType.MØDREKVOTE),
                søkt(FP_FOM, FP_TOM, UttakPeriodeType.FELLESPERIODE));

            // MÅLBILDE ved full plan: null - fjerning må uttrykkes med FRI-utsettelse
            assertThat(endringsdato(plan, uttak(normaltVedtak()), false)).isEqualTo(MK_FOM);
        }

        @Test
        void plan_avkortet_i_starten_kan_ikke_skilles_fra_legacy_delsoknad() {
            // Samme plan som testen over, men uten periodene før avkortingen. Dagens algoritme
            // tolker den som en legacy delsøknad; målbildet er at begge tolkninger gir samme utfall.
            var plan = List.of(
                søkt(LocalDate.of(2024, 1, 15), MK_TOM, UttakPeriodeType.MØDREKVOTE),
                søkt(FP_FOM, FP_TOM, UttakPeriodeType.FELLESPERIODE));

            // MÅLBILDE ved full plan: null (ingen endring)
            assertThat(endringsdato(plan, uttak(normaltVedtak()), false)).isEqualTo(FP_TOM.plusDays(1));
        }
    }

    /**
     * Vedtakssiden projiseres fra <em>resultatet</em> (trekkonto, innvilget samtidig uttak), ikke fra
     * de søkte verdiene. Det er tilsiktet: {@link UttakResultatPeriodeEntitet} er det som gjelder og
     * som presenteres for bruker, og den fulle planen rendres fra det. Sender søknaden tilbake den
     * opprinnelig søkte verdien der regelen har overstyrt, er det en <em>reell</em> endring som skal
     * behandles på nytt.
     */
    @Nested
    class VedtakOverstyrerSøkteVerdier {

        @Test
        void plan_som_gjentar_sokt_konto_der_vedtaket_omfordelte_trekket_er_en_endring() {
            var vedtak = List.of(
                vedtaksperiode(FPFF_FOM, FPFF_TOM, UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL, PeriodeResultatType.INNVILGET, true),
                vedtaksperiode(MK_FOM, MK_TOM, UttakPeriodeType.MØDREKVOTE, PeriodeResultatType.INNVILGET, true),
                vedtaksperiodeSøktAnnenKonto(FP_FOM, FP_TOM, UttakPeriodeType.FELLESPERIODE, UttakPeriodeType.MØDREKVOTE));

            // Søkt FELLESPERIODE, innvilget med trekk fra MØDREKVOTE. Planen ber om FELLESPERIODE igjen,
            // altså en omgjøring av det som er vedtatt og presentert for bruker.
            assertThat(endringsdato(planMedHale(), uttak(vedtak), false)).isEqualTo(FP_FOM);
        }

        @Test
        void plan_som_gjentar_sokt_samtidig_uttaksprosent_der_vedtaket_justerte_den_er_en_endring() {
            var vedtakPeriode = new UttakResultatPeriodeEntitet.Builder(FP_FOM, FP_TOM)
                .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
                .medSamtidigUttak(true)
                .medSamtidigUttaksprosent(new SamtidigUttaksprosent(50))
                .medPeriodeSoknad(new UttakResultatPeriodeSøknadEntitet.Builder()
                    .medUttakPeriodeType(UttakPeriodeType.FELLESPERIODE)
                    .medSamtidigUttak(true)
                    .medSamtidigUttaksprosent(new SamtidigUttaksprosent(40))
                    .build())
                .build();
            UttakResultatPeriodeAktivitetEntitet.builder(vedtakPeriode, AKTIVITET)
                .medTrekkdager(new Trekkdager(10))
                .medTrekkonto(UttakPeriodeType.FELLESPERIODE)
                .medUtbetalingsgrad(Utbetalingsgrad.HUNDRED)
                .medArbeidsprosent(BigDecimal.ZERO)
                .build();
            var vedtak = List.of(
                vedtaksperiode(FPFF_FOM, FPFF_TOM, UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL, PeriodeResultatType.INNVILGET, true),
                vedtaksperiode(MK_FOM, MK_TOM, UttakPeriodeType.MØDREKVOTE, PeriodeResultatType.INNVILGET, true),
                vedtakPeriode);
            var plan = List.of(
                søkt(FPFF_FOM, FPFF_TOM, UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL),
                søkt(MK_FOM, MK_TOM, UttakPeriodeType.MØDREKVOTE),
                OppgittPeriodeBuilder.ny()
                    .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
                    .medPeriode(FP_FOM, FP_TOM)
                    .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
                    .medSamtidigUttak(true)
                    .medSamtidigUttaksprosent(new SamtidigUttaksprosent(40))
                    .build(),
                søkt(HALE_FOM, FP_TOM.plusWeeks(3), UttakPeriodeType.FELLESPERIODE));

            // Søkt 40 %, innvilget 50 %. Planen ber om 40 % igjen - reell endring fra vedtatt uttak.
            assertThat(endringsdato(plan, uttak(vedtak), false)).isEqualTo(FP_FOM);
        }

        @Test
        void innvilget_vedtaksperiode_uten_periodesoknad_tas_med_riktig_endring() {
            // En periode er innvilget manuelt uten søknad (typisk manglende søkt).
            // Planen gjentar den uendret og utvider med ekstra dager.
            var systemFom = LocalDate.of(2024, 2, 1);
            var systemTom = LocalDate.of(2024, 2, 15);
            var vedtak = List.of(
                vedtaksperiode(FPFF_FOM, FPFF_TOM, UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL, PeriodeResultatType.INNVILGET, true),
                vedtaksperiode(MK_FOM, systemFom.minusDays(1), UttakPeriodeType.MØDREKVOTE, PeriodeResultatType.INNVILGET, true),
                vedtaksperiode(systemFom, systemTom, UttakPeriodeType.MØDREKVOTE, PeriodeResultatType.INNVILGET, false),
                vedtaksperiode(systemTom.plusDays(1), MK_TOM, UttakPeriodeType.MØDREKVOTE, PeriodeResultatType.INNVILGET, true),
                vedtaksperiode(FP_FOM, FP_TOM, UttakPeriodeType.FELLESPERIODE, PeriodeResultatType.INNVILGET, true));

            assertThat(endringsdato(planMedHale(), uttak(vedtak), false)).isEqualTo(HALE_FOM);
        }
    }

    private static List<UttakResultatPeriodeEntitet> normaltVedtak() {
        return List.of(
            vedtaksperiode(FPFF_FOM, FPFF_TOM, UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL, PeriodeResultatType.INNVILGET, true),
            vedtaksperiode(MK_FOM, MK_TOM, UttakPeriodeType.MØDREKVOTE, PeriodeResultatType.INNVILGET, true),
            vedtaksperiode(FP_FOM, FP_TOM, UttakPeriodeType.FELLESPERIODE, PeriodeResultatType.INNVILGET, true));
    }

    private static List<OppgittPeriodeEntitet> fullPlan() {
        return List.of(
            søkt(FPFF_FOM, FPFF_TOM, UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL),
            søkt(MK_FOM, MK_TOM, UttakPeriodeType.MØDREKVOTE),
            søkt(FP_FOM, FP_TOM, UttakPeriodeType.FELLESPERIODE));
    }

    private static List<OppgittPeriodeEntitet> planMedHale() {
        List<OppgittPeriodeEntitet> plan = new ArrayList<>(fullPlan());
        plan.add(søkt(HALE_FOM, FP_TOM.plusWeeks(3), UttakPeriodeType.FELLESPERIODE));
        return plan;
    }

    private static OppgittPeriodeEntitet søkt(LocalDate fom, LocalDate tom, UttakPeriodeType type) {
        return OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom, tom)
            .medPeriodeType(type)
            .build();
    }

    private static OppgittPeriodeEntitet søktUtsettelse(LocalDate fom, LocalDate tom, UtsettelseÅrsak årsak) {
        return OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom, tom)
            .medPeriodeType(UttakPeriodeType.UDEFINERT)
            .medÅrsak(årsak)
            .build();
    }

    private static UttakResultatPeriodeEntitet vedtaksperiode(LocalDate fom, LocalDate tom, UttakPeriodeType konto,
                                                             PeriodeResultatType resultat, boolean fraSøknad) {
        var builder = new UttakResultatPeriodeEntitet.Builder(fom, tom).medResultatType(resultat, PeriodeResultatÅrsak.UKJENT);
        if (fraSøknad) {
            builder.medPeriodeSoknad(new UttakResultatPeriodeSøknadEntitet.Builder().medUttakPeriodeType(konto).build());
        }
        var periode = builder.build();
        UttakResultatPeriodeAktivitetEntitet.builder(periode, AKTIVITET)
            .medTrekkdager(new Trekkdager(10))
            .medTrekkonto(konto)
            .medUtbetalingsgrad(PeriodeResultatType.INNVILGET.equals(resultat) ? Utbetalingsgrad.HUNDRED : Utbetalingsgrad.ZERO)
            .medArbeidsprosent(BigDecimal.ZERO)
            .build();
        return periode;
    }

    private static UttakResultatPeriodeEntitet vedtaksperiodeSøktAnnenKonto(LocalDate fom, LocalDate tom, UttakPeriodeType søkt,
                                                                           UttakPeriodeType trukket) {
        var periode = new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .medPeriodeSoknad(new UttakResultatPeriodeSøknadEntitet.Builder().medUttakPeriodeType(søkt).build())
            .build();
        UttakResultatPeriodeAktivitetEntitet.builder(periode, AKTIVITET)
            .medTrekkdager(new Trekkdager(10))
            .medTrekkonto(trukket)
            .medUtbetalingsgrad(Utbetalingsgrad.HUNDRED)
            .medArbeidsprosent(BigDecimal.ZERO)
            .build();
        return periode;
    }

    private static UttakResultatPeriodeEntitet utsettelsesperiodeVedtak(LocalDate fom, LocalDate tom, UttakUtsettelseType type) {
        var periode = new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .medUtsettelseType(type)
            .medPeriodeSoknad(new UttakResultatPeriodeSøknadEntitet.Builder().medUttakPeriodeType(UttakPeriodeType.UDEFINERT).build())
            .build();
        UttakResultatPeriodeAktivitetEntitet.builder(periode, AKTIVITET)
            .medTrekkdager(Trekkdager.ZERO)
            .medTrekkonto(UttakPeriodeType.UDEFINERT)
            .medUtbetalingsgrad(Utbetalingsgrad.ZERO)
            .medArbeidsprosent(BigDecimal.ZERO)
            .build();
        return periode;
    }

    private static UttakResultatEntitet uttak(List<UttakResultatPeriodeEntitet> perioder) {
        var p = new UttakResultatPerioderEntitet();
        perioder.forEach(p::leggTilPeriode);
        return new UttakResultatEntitet.Builder(Behandlingsresultat.builder().build()).medOpprinneligPerioder(p).build();
    }
}
