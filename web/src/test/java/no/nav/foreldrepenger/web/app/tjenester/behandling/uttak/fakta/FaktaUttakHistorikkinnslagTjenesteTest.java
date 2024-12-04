package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.fakta;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;

class FaktaUttakHistorikkinnslagTjenesteTest {

    private static final LocalDate FOM = LocalDate.of(2022,12,1);
    private static final LocalDate TOM = LocalDate.of(2022,12,7);
    private static final String BEGRUNNELSE_TEKST = "Begrunnelse";

    private FaktaUttakHistorikkinnslagTjeneste tjeneste;
    private Historikkinnslag2Repository historikkinnslagRepository;
    private Behandling behandling;

    @BeforeEach
    public void setup() {
        var scenarioFarSøkerForeldrepenger = ScenarioFarSøkerForeldrepenger.forFødsel();
        behandling = scenarioFarSøkerForeldrepenger.lagMocked();
        historikkinnslagRepository = scenarioFarSøkerForeldrepenger.mockBehandlingRepositoryProvider().getHistorikkinnslag2Repository();
        tjeneste = new FaktaUttakHistorikkinnslagTjeneste(historikkinnslagRepository);
    }

    @Test
    void skal_generere_historikkinnslag_uten_perioder_ved_ingen_endring() {
        //Scenario med avklar fakta uttak
        var før = OppgittPeriodeBuilder.ny().medPeriode(FOM, TOM)
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .build();
        var etter = OppgittPeriodeBuilder.fraEksisterende(før).build();

        // dto
        tjeneste.opprettHistorikkinnslag(behandling.getId(), behandling.getFagsakId(), List.of(før), List.of(etter), false, BEGRUNNELSE_TEKST);

        var historikkinnslag = historikkinnslagRepository.hent(behandling.getId());
        assertThat(historikkinnslag).hasSize(1);
        assertThat(historikkinnslag.getFirst().getAktør()).isEqualTo(HistorikkAktør.SAKSBEHANDLER);
        assertThat(historikkinnslag.getFirst().getSkjermlenke()).isEqualTo(SkjermlenkeType.FAKTA_UTTAK);
        assertThat(historikkinnslag.getFirst().getLinjer()).hasSize(1);
        assertThat(historikkinnslag.getFirst().getLinjer().getFirst().getTekst()).contains(BEGRUNNELSE_TEKST);
    }

    @Test
    void skal_generere_historikkinnslag_med_tillagt_periode_ved_utvidelse_overtyrt() {
        //Scenario med avklar fakta uttak
        var før = OppgittPeriodeBuilder.ny().medPeriode(FOM, TOM)
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .build();
        var etter = OppgittPeriodeBuilder.fraEksisterende(før).medPeriode(FOM, TOM.plusDays(1)).build();

        // dto
        tjeneste.opprettHistorikkinnslag(behandling.getId(), behandling.getFagsakId(), List.of(før), List.of(etter), true, BEGRUNNELSE_TEKST);

        var historikkinnslag = historikkinnslagRepository.hent(behandling.getId());
        assertThat(historikkinnslag).hasSize(1);
        assertThat(historikkinnslag.getFirst().getAktør()).isEqualTo(HistorikkAktør.SAKSBEHANDLER);
        assertThat(historikkinnslag.getFirst().getSkjermlenke()).isEqualTo(SkjermlenkeType.FAKTA_UTTAK);
        assertThat(historikkinnslag.getFirst().getLinjer()).hasSize(3);
        assertThat(historikkinnslag.getFirst().getLinjer().get(0).getTekst()).contains("Overstyrt vurdering:");
        assertThat(historikkinnslag.getFirst().getLinjer().get(1).getTekst()).contains("Perioden", ".2022", "er satt til", "Uttak", "Fedrekvoten");
        assertThat(historikkinnslag.getFirst().getLinjer().get(2).getTekst()).contains(BEGRUNNELSE_TEKST);
    }

    @Test
    void skal_generere_historikkinnslag_med_slettet_periode_ved_krymping() {
        //Scenario med avklar fakta uttak
        var før = OppgittPeriodeBuilder.ny().medPeriode(FOM, TOM)
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .build();
        var etter = OppgittPeriodeBuilder.fraEksisterende(før).medPeriode(FOM, TOM.minusDays(1)).build();

        // dto
        tjeneste.opprettHistorikkinnslag(behandling.getId(), behandling.getFagsakId(), List.of(før), List.of(etter), false, BEGRUNNELSE_TEKST);

        var historikkinnslag = historikkinnslagRepository.hent(behandling.getId());
        assertThat(historikkinnslag).hasSize(1);
        assertThat(historikkinnslag.getFirst().getAktør()).isEqualTo(HistorikkAktør.SAKSBEHANDLER);
        assertThat(historikkinnslag.getFirst().getSkjermlenke()).isEqualTo(SkjermlenkeType.FAKTA_UTTAK);
        assertThat(historikkinnslag.getFirst().getLinjer()).hasSize(2);
        assertThat(historikkinnslag.getFirst().getLinjer().get(0).getTekst()).contains("Perioden", ".2022", "Uttak", "Fedrekvoten", "er fjernet");
        assertThat(historikkinnslag.getFirst().getLinjer().get(1).getTekst()).contains(BEGRUNNELSE_TEKST);
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
        tjeneste.opprettHistorikkinnslag(behandling.getId(), behandling.getFagsakId(), List.of(før), List.of(etter), false, BEGRUNNELSE_TEKST);

        var historikkinnslag = historikkinnslagRepository.hent(behandling.getId());
        assertThat(historikkinnslag).hasSize(1);
        assertThat(historikkinnslag.getFirst().getAktør()).isEqualTo(HistorikkAktør.SAKSBEHANDLER);
        assertThat(historikkinnslag.getFirst().getSkjermlenke()).isEqualTo(SkjermlenkeType.FAKTA_UTTAK);
        assertThat(historikkinnslag.getFirst().getLinjer()).hasSize(2);
        assertThat(historikkinnslag.getFirst().getLinjer().get(0).getTekst()).contains("Perioden", ".2022", "er endret fra", "Utsettelse", "Barn", "Søker");
        assertThat(historikkinnslag.getFirst().getLinjer().get(1).getTekst()).contains(BEGRUNNELSE_TEKST);
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
        tjeneste.opprettHistorikkinnslag(behandling.getId(), behandling.getFagsakId(), List.of(før), List.of(etter), false, BEGRUNNELSE_TEKST);

        var historikkinnslag = historikkinnslagRepository.hent(behandling.getId());
        assertThat(historikkinnslag).hasSize(1);
        assertThat(historikkinnslag.getFirst().getAktør()).isEqualTo(HistorikkAktør.SAKSBEHANDLER);
        assertThat(historikkinnslag.getFirst().getSkjermlenke()).isEqualTo(SkjermlenkeType.FAKTA_UTTAK);
        assertThat(historikkinnslag.getFirst().getLinjer()).hasSize(2);
        assertThat(historikkinnslag.getFirst().getLinjer().get(0).getTekst()).contains("Perioden", ".2022", "er endret fra", "Utsettelse", "Barn", "til" , "Uttak", "Mødrekvoten");
        assertThat(historikkinnslag.getFirst().getLinjer().get(1).getTekst()).contains(BEGRUNNELSE_TEKST);
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
        tjeneste.opprettHistorikkinnslag(behandling.getId(), behandling.getFagsakId(), List.of(før), List.of(etter1, etter2, etter3, etter4, etter5, etter6), false,
            BEGRUNNELSE_TEKST);

        var historikkinnslag = historikkinnslagRepository.hent(behandling.getId());
        assertThat(historikkinnslag).hasSize(1);
        assertThat(historikkinnslag.getFirst().getAktør()).isEqualTo(HistorikkAktør.SAKSBEHANDLER);
        assertThat(historikkinnslag.getFirst().getSkjermlenke()).isEqualTo(SkjermlenkeType.FAKTA_UTTAK);

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

        assertThat(historikkinnslag.getFirst().getLinjer()).hasSize(7);
        assertThat(historikkinnslag.getFirst().getLinjer().get(0).getTekst()).contains("Perioden", "er satt til", "Overføring", "Mødrekvoten");
        assertThat(historikkinnslag.getFirst().getLinjer().get(1).getTekst()).contains("Perioden", "er endret fra", "Uttak", "til", "Utsettelse", "Barn");
        assertThat(historikkinnslag.getFirst().getLinjer().get(2).getTekst()).contains("Perioden", "er endret fra", "Uttak", "til", "Uttak", "Samtidig");
        assertThat(historikkinnslag.getFirst().getLinjer().get(3).getTekst()).contains("Perioden", "er endret fra", "Uttak", "til", "Gradering", "10");
        assertThat(historikkinnslag.getFirst().getLinjer().get(4).getTekst()).contains("Perioden", "er endret fra", "Uttak", "Fedrekvoten", "til",  "Fellesperioden");
        assertThat(historikkinnslag.getFirst().getLinjer().get(5).getTekst()).contains("Perioden", "Uttak", "Fedrekvoten", "er fjernet");
        assertThat(historikkinnslag.getFirst().getLinjer().get(6).getTekst()).contains(BEGRUNNELSE_TEKST);
    }
}
