package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.fakta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

class FaktaUttakHistorikkinnslagTjenesteTest {

    private static final LocalDate FOM = LocalDate.of(2022,12,1);
    private static final LocalDate TOM = LocalDate.of(2022,12,7);

    private HistorikkTjenesteAdapter adapter;
    private FaktaUttakHistorikkinnslagTjeneste tjeneste;

    @BeforeEach
    public void setup() {
        adapter = new HistorikkTjenesteAdapter(mock(HistorikkRepository.class));
        tjeneste = new FaktaUttakHistorikkinnslagTjeneste(adapter);
    }

    @Test
    void skal_generere_historikkinnslag_uten_perioder_ved_ingen_endring() {

        //Scenario med avklar fakta uttak
        var før = OppgittPeriodeBuilder.ny().medPeriode(FOM, TOM)
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .build();
        var etter = OppgittPeriodeBuilder.fraEksisterende(før).build();

        // dto
        tjeneste.opprettHistorikkinnslag("Begrunnelse", false, List.of(før), List.of(etter));
        var historikkinnslag = opprettHistorikkInnslag(adapter.tekstBuilder());

        assertThat(historikkinnslag.getType()).isEqualTo(HistorikkinnslagType.FAKTA_ENDRET);
        assertThat(historikkinnslag.getAktør()).isEqualTo(HistorikkAktør.SAKSBEHANDLER);
        var del = historikkinnslag.getHistorikkinnslagDeler().get(0);
        assertThat(del.getSkjermlenke()).as("skjermlenke")
            .hasValueSatisfying(skjermlenke -> assertThat(skjermlenke).isEqualTo(SkjermlenkeType.FAKTA_UTTAK.getKode()));
        assertThat(del.getEndredeFelt()).isEmpty();
    }

    @Test
    void skal_generere_historikkinnslag_med_tillagt_periode_ved_utvidelse() {
        //Scenario med avklar fakta uttak
        var før = OppgittPeriodeBuilder.ny().medPeriode(FOM, TOM)
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .build();
        var etter = OppgittPeriodeBuilder.fraEksisterende(før).medPeriode(FOM, TOM.plusDays(1)).build();

        // dto
        tjeneste.opprettHistorikkinnslag("Begrunnelse", false, List.of(før), List.of(etter));
        var historikkinnslag = opprettHistorikkInnslag(adapter.tekstBuilder());

        assertThat(historikkinnslag.getType()).isEqualTo(HistorikkinnslagType.FAKTA_ENDRET);
        assertThat(historikkinnslag.getAktør()).isEqualTo(HistorikkAktør.SAKSBEHANDLER);
        var del = historikkinnslag.getHistorikkinnslagDeler().get(0);
        assertThat(del.getSkjermlenke()).as("skjermlenke")
            .hasValueSatisfying(skjermlenke -> assertThat(skjermlenke).isEqualTo(SkjermlenkeType.FAKTA_UTTAK.getKode()));
        assertThat(del.getEndredeFelt()).hasSize(1);
        var nyPeriode = del.getEndredeFelt().get(0);
        assertThat(nyPeriode.getNavn()).isEqualTo(HistorikkEndretFeltType.FAKTA_UTTAK_PERIODE.getKode());
        assertThat(nyPeriode.getNavnVerdi()).contains("2022");
        assertThat(nyPeriode.getTilVerdi()).contains("Lagt til");
        assertThat(nyPeriode.getTilVerdi()).contains("Uttak");
        assertThat(nyPeriode.getTilVerdi()).contains("Fedrekvoten");
    }

    @Test
    void skal_generere_historikkinnslag_med_slettet_periode_ved_krymping() {
        //Scenario med avklar fakta uttak
        var før = OppgittPeriodeBuilder.ny().medPeriode(FOM, TOM)
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .build();
        var etter = OppgittPeriodeBuilder.fraEksisterende(før).medPeriode(FOM, TOM.minusDays(1)).build();

        // dto
        tjeneste.opprettHistorikkinnslag("Begrunnelse", false, List.of(før), List.of(etter));
        var historikkinnslag = opprettHistorikkInnslag(adapter.tekstBuilder());

        assertThat(historikkinnslag.getType()).isEqualTo(HistorikkinnslagType.FAKTA_ENDRET);
        assertThat(historikkinnslag.getAktør()).isEqualTo(HistorikkAktør.SAKSBEHANDLER);
        var del = historikkinnslag.getHistorikkinnslagDeler().get(0);
        assertThat(del.getSkjermlenke()).as("skjermlenke")
            .hasValueSatisfying(skjermlenke -> assertThat(skjermlenke).isEqualTo(SkjermlenkeType.FAKTA_UTTAK.getKode()));
        assertThat(del.getEndredeFelt()).hasSize(1);
        var slettetPeriode = del.getEndredeFelt().get(0);
        assertThat(slettetPeriode.getNavn()).isEqualTo(HistorikkEndretFeltType.FAKTA_UTTAK_PERIODE.getKode());
        assertThat(slettetPeriode.getNavnVerdi()).contains("2022");
        assertThat(slettetPeriode.getTilVerdi()).contains("Slettet");
        assertThat(slettetPeriode.getTilVerdi()).contains("Uttak");
        assertThat(slettetPeriode.getTilVerdi()).contains("Fedrekvoten");
    }

    @Test
    void skal_generere_historikkinnslag_med_endring_ved_ny_årsak() {

        //Scenario med avklar fakta uttak
        var før = OppgittPeriodeBuilder.ny().medPeriode(FOM, TOM)
            .medÅrsak(UtsettelseÅrsak.INSTITUSJON_BARN)
            .build();
        var etter = OppgittPeriodeBuilder.fraEksisterende(før)
            .medÅrsak(UtsettelseÅrsak.INSTITUSJON_SØKER)
            .build();

        // dto
        tjeneste.opprettHistorikkinnslag("Begrunnelse", false, List.of(før), List.of(etter));
        var historikkinnslag = opprettHistorikkInnslag(adapter.tekstBuilder());

        assertThat(historikkinnslag.getType()).isEqualTo(HistorikkinnslagType.FAKTA_ENDRET);
        assertThat(historikkinnslag.getAktør()).isEqualTo(HistorikkAktør.SAKSBEHANDLER);
        var del = historikkinnslag.getHistorikkinnslagDeler().get(0);
        assertThat(del.getSkjermlenke()).as("skjermlenke")
            .hasValueSatisfying(skjermlenke -> assertThat(skjermlenke).isEqualTo(SkjermlenkeType.FAKTA_UTTAK.getKode()));
        assertThat(del.getEndredeFelt()).hasSize(1);
        var endretPeriode = del.getEndredeFelt().get(0);
        assertThat(endretPeriode.getNavn()).isEqualTo(HistorikkEndretFeltType.FAKTA_UTTAK_PERIODE.getKode());
        assertThat(endretPeriode.getNavnVerdi()).contains("2022");
        assertThat(endretPeriode.getFraVerdi()).contains("Utsettelse");
        assertThat(endretPeriode.getFraVerdi()).contains("Barn");
        assertThat(endretPeriode.getTilVerdi()).contains("Utsettelse");
        assertThat(endretPeriode.getTilVerdi()).contains("Søker");
    }

    @Test
    void skal_generere_historikkinnslag_med_endring_type() {

        //Scenario med avklar fakta uttak
        var før = OppgittPeriodeBuilder.ny().medPeriode(FOM, TOM)
            .medÅrsak(UtsettelseÅrsak.INSTITUSJON_BARN)
            .build();
        var etter = OppgittPeriodeBuilder.ny().medPeriode(FOM, TOM)
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();

        // dto
        tjeneste.opprettHistorikkinnslag("Begrunnelse", false, List.of(før), List.of(etter));
        var historikkinnslag = opprettHistorikkInnslag(adapter.tekstBuilder());

        assertThat(historikkinnslag.getType()).isEqualTo(HistorikkinnslagType.FAKTA_ENDRET);
        assertThat(historikkinnslag.getAktør()).isEqualTo(HistorikkAktør.SAKSBEHANDLER);
        var del = historikkinnslag.getHistorikkinnslagDeler().get(0);
        assertThat(del.getSkjermlenke()).as("skjermlenke")
            .hasValueSatisfying(skjermlenke -> assertThat(skjermlenke).isEqualTo(SkjermlenkeType.FAKTA_UTTAK.getKode()));
        assertThat(del.getEndredeFelt()).hasSize(1);
        var endretPeriode = del.getEndredeFelt().get(0);
        assertThat(endretPeriode.getNavn()).isEqualTo(HistorikkEndretFeltType.FAKTA_UTTAK_PERIODE.getKode());
        assertThat(endretPeriode.getNavnVerdi()).contains("2022");
        assertThat(endretPeriode.getFraVerdi()).contains("Utsettelse");
        assertThat(endretPeriode.getFraVerdi()).contains("Barn");
        assertThat(endretPeriode.getTilVerdi()).contains("Uttak");
        assertThat(endretPeriode.getTilVerdi()).contains("Mødrekvoten");
    }

    @Test
    void skal_generere_historikkinnslag_med_splitt() {

        //Scenario med avklar fakta uttak
        var før = OppgittPeriodeBuilder.ny().medPeriode(FOM, TOM.plusWeeks(5).minusDays(1))
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .build();
        var etter1 = OppgittPeriodeBuilder.ny().medPeriode(FOM.minusWeeks(1), FOM.minusDays(1))
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medÅrsak(OverføringÅrsak.SYKDOM_ANNEN_FORELDER)
            .build();
        var etter2 = OppgittPeriodeBuilder.ny().medPeriode(FOM, TOM.minusDays(1))
            .medÅrsak(UtsettelseÅrsak.INSTITUSJON_BARN)
            .build();
        var etter3 = OppgittPeriodeBuilder.ny().medPeriode(TOM, TOM.plusWeeks(1).minusDays(1))
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medSamtidigUttak(true)
            .medSamtidigUttaksprosent(new SamtidigUttaksprosent(50))
            .build();
        var etter4 = OppgittPeriodeBuilder.ny().medPeriode(TOM.plusWeeks(1), TOM.plusWeeks(2).minusDays(1))
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medGraderingAktivitetType(GraderingAktivitetType.SELVSTENDIG_NÆRINGSDRIVENDE)
            .medArbeidsprosent(BigDecimal.TEN)
            .build();
        var etter5 = OppgittPeriodeBuilder.ny().medPeriode(TOM.plusWeeks(2), TOM.plusWeeks(3).minusDays(1))
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medMorsAktivitet(MorsAktivitet.TRENGER_HJELP)
            .build();
        var etter6 = OppgittPeriodeBuilder.ny().medPeriode(TOM.plusWeeks(3), TOM.plusWeeks(4).minusDays(1))
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .build();

        // dto
        tjeneste.opprettHistorikkinnslag("Begrunnelse", false, List.of(før), List.of(etter1, etter2, etter3, etter4, etter5, etter6));
        var historikkinnslag = opprettHistorikkInnslag(adapter.tekstBuilder());

        assertThat(historikkinnslag.getType()).isEqualTo(HistorikkinnslagType.FAKTA_ENDRET);
        assertThat(historikkinnslag.getAktør()).isEqualTo(HistorikkAktør.SAKSBEHANDLER);
        var del = historikkinnslag.getHistorikkinnslagDeler().get(0);
        assertThat(del.getSkjermlenke()).as("skjermlenke")
            .hasValueSatisfying(skjermlenke -> assertThat(skjermlenke).isEqualTo(SkjermlenkeType.FAKTA_UTTAK.getKode()));
        /**
         * Venter disse 5 endringene av de 6 ukene
         * - Lagt til en uke overført MK før opprinnelig
         * - Endret 1 uke fra FK til utsettelse innleggelse barn
         * - Endret 1 uke FK til 50% samtidig uttak
         * - Endret en uke uttak FK fra full til gradering med 10% arbeid
         * - Endret en FK til fellesperiode
         * - Ikke endring av nest siste uke
         * - Slettet siste uke FK
         */
        assertThat(del.getEndredeFelt()).hasSize(6);
        var endretPeriode = del.getEndredeFelt().get(0);
        assertThat(del.getEndredeFelt().stream().allMatch(f -> HistorikkEndretFeltType.FAKTA_UTTAK_PERIODE.getKode().equals(f.getNavn()))).isTrue();
        assertThat(del.getEndredeFelt().stream().anyMatch(f -> f.getTilVerdi().contains("Lagt til") && f.getTilVerdi().contains("Overføring") && f.getTilVerdi().contains("Mødrekvoten"))).isTrue();
        assertThat(del.getEndredeFelt().stream().anyMatch(f -> f.getTilVerdi().contains("Utsettelse")  && f.getTilVerdi().contains("Barn"))).isTrue();
        assertThat(del.getEndredeFelt().stream().anyMatch(f -> f.getFraVerdi() != null && f.getFraVerdi().contains("Uttak") && f.getTilVerdi().contains("Samtidig"))).isTrue();
        assertThat(del.getEndredeFelt().stream().anyMatch(f -> f.getTilVerdi().contains("Gradering")  && f.getTilVerdi().contains("10"))).isTrue();
        assertThat(del.getEndredeFelt().stream().anyMatch(f -> f.getTilVerdi().contains("Uttak")  && f.getTilVerdi().contains("Fellesperioden"))).isTrue();
        assertThat(del.getEndredeFelt().stream().anyMatch(f -> f.getTilVerdi().contains("Slettet")  && f.getTilVerdi().contains("Fedrekvoten"))).isTrue();
    }

    public Historikkinnslag opprettHistorikkInnslag(HistorikkInnslagTekstBuilder builder) {
        if (!builder.getHistorikkinnslagDeler().isEmpty() || builder.antallEndredeFelter() > 0 || builder.getErBegrunnelseEndret()
            || builder.getErGjeldendeFraSatt()) {

            var innslag = new Historikkinnslag();

            builder.medHendelse(HistorikkinnslagType.FAKTA_ENDRET);
            innslag.setAktør(HistorikkAktør.SAKSBEHANDLER);
            innslag.setType(HistorikkinnslagType.FAKTA_ENDRET);
            innslag.setBehandlingId(123L);
            builder.build(innslag);

            return innslag;
        }
        return null;
    }
}
