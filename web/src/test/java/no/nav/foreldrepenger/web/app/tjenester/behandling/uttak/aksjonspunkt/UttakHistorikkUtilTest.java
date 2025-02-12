package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeType;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.ArbeidsgiverLagreDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPeriodeAktivitetLagreDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPeriodeLagreDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.overstyring.UttakHistorikkUtil;

class UttakHistorikkUtilTest {

    private static final BehandlingReferanse BEHANDLING = mockBehandling();
    private static final LocalDate DEFAULT_FOM = LocalDate.now();
    private static final LocalDate DEFAULT_TOM = LocalDate.now().plusWeeks(1);
    private static final String ORGNR = KUNSTIG_ORG;
    private static final InternArbeidsforholdRef ARBEIDSFORHOLD_REF = InternArbeidsforholdRef.namedRef("TEST-REF");

    @Test
    void skalLageHistorikkInnslagForPeriodeResultatTypeHvisEndring() {
        var manuell = PeriodeResultatType.MANUELL_BEHANDLING;
        var gjeldende = enkeltPeriode(manuell);

        var perioder = nyMedResultatType(PeriodeResultatType.INNVILGET, new ArbeidsgiverLagreDto(ORGNR));

        var historikkinnslag = UttakHistorikkUtil.forOverstyring().lagHistorikkinnslag(BEHANDLING, perioder, List.of(gjeldende));
        assertThat(historikkinnslag).isPresent();
        var innslag = historikkinnslag.get();
        assertThat(innslag.getBehandlingId()).isEqualTo(BEHANDLING.behandlingId());
        assertThat(innslag.getAktør()).isEqualTo(HistorikkAktør.SAKSBEHANDLER);
        assertThat(innslag.getLinjer()).hasSize(5);
        assertThat(innslag.getLinjer().get(0).getTekst()).isEqualTo("__Overstyring av uttak__.");
        assertThat(innslag.getLinjer().get(1).getType()).isEqualTo(HistorikkinnslagLinjeType.LINJESKIFT);
        assertThat(innslag.getLinjer().get(2).getTekst()).contains("Vurdering av perioden");
        assertThat(innslag.getLinjer().get(3).getTekst()).isEqualTo("__Resultatet__ er endret fra Til manuell behandling til __Innvilget__.");
        assertThat(innslag.getLinjer().get(4).getTekst()).isEqualTo(perioder.getFirst().getBegrunnelse());
    }

    @Test
    void skalIkkeLageHistorikkInnslagForPeriodeResultatTypeHvisIngenEndring() {
        var gjeldende = enkeltPeriode(PeriodeResultatType.INNVILGET);

        var perioder = nyMedResultatType(PeriodeResultatType.INNVILGET, new ArbeidsgiverLagreDto(ORGNR));

        var historikkinnslag = UttakHistorikkUtil.forOverstyring().lagHistorikkinnslag(BEHANDLING, perioder, List.of(gjeldende));

        assertThat(historikkinnslag).isEmpty();
    }


        @Test
        void skalLageHistorikkInnslagForPerioderMedPrivatpersonSomArbeidsgiver() {
            var manuell = PeriodeResultatType.MANUELL_BEHANDLING;
            var arbeidsgiverAktørId = AktørId.dummy();
            var privateperson = Arbeidsgiver.person(arbeidsgiverAktørId);
            var gjeldende = enkeltPeriode(manuell, privateperson);

            var perioder = nyMedResultatType(PeriodeResultatType.INNVILGET, new ArbeidsgiverLagreDto(arbeidsgiverAktørId));

            var historikk = UttakHistorikkUtil.forFastsetting().lagHistorikkinnslag(BEHANDLING,
                    perioder, List.of(gjeldende));

            assertThat(historikk).isPresent();
            var historikkinnslag = historikk.get();
            assertThat(historikkinnslag.getBehandlingId()).isEqualTo(BEHANDLING.behandlingId());
            assertThat(historikkinnslag.getAktør()).isEqualTo(HistorikkAktør.SAKSBEHANDLER);
            assertThat(historikkinnslag.getLinjer()).hasSize(5);
            assertThat(historikkinnslag.getLinjer().get(0).getTekst()).isEqualTo("__Manuell vurdering av uttak__.");
            assertThat(historikkinnslag.getLinjer().get(1).getType()).isEqualTo(HistorikkinnslagLinjeType.LINJESKIFT);
            assertThat(historikkinnslag.getLinjer().get(2).getTekst()).contains("Vurdering av perioden");
            assertThat(historikkinnslag.getLinjer().get(3).getTekst()).contains(gjeldende.getResultatType().getNavn());
            assertThat(historikkinnslag.getLinjer().get(3).getTekst()).contains(perioder.getFirst().getPeriodeResultatType().getNavn());
            assertThat(historikkinnslag.getLinjer().get(4).getTekst()).isEqualTo(perioder.getFirst().getBegrunnelse());
        }

    private ForeldrepengerUttakPeriode periode(PeriodeResultatType type,
                                               LocalDate fom,
                                               LocalDate tom,
                                               List<ForeldrepengerUttakPeriodeAktivitet> aktiviteter) {
        return new ForeldrepengerUttakPeriode.Builder().medTidsperiode(fom, tom)
            .medResultatType(type)
            .medAktiviteter(aktiviteter)
            .medResultatÅrsak(PeriodeResultatÅrsak.UKJENT)
            .medBegrunnelse("abc.")
            .build();
    }

    private ForeldrepengerUttakPeriode enkeltPeriode(PeriodeResultatType type, LocalDate fom, LocalDate tom, Arbeidsgiver arbeidsgiver) {
        var periodeAktivitet = new ForeldrepengerUttakPeriodeAktivitet.Builder().medAktivitet(
                new ForeldrepengerUttakAktivitet(UttakArbeidType.ORDINÆRT_ARBEID, arbeidsgiver, ARBEIDSFORHOLD_REF))
            .medArbeidsprosent(BigDecimal.ZERO)
            .medUtbetalingsgrad(Utbetalingsgrad.TEN)
            .build();
        return periode(type, fom, tom, List.of(periodeAktivitet));
    }

    private List<UttakResultatPeriodeLagreDto> nyMedResultatType(PeriodeResultatType type, ArbeidsgiverLagreDto arbeidsgiver) {
        var nyPeriode = nyPeriodeMedType(type, arbeidsgiver);
        return Collections.singletonList(nyPeriode);
    }

    private UttakResultatPeriodeLagreDto nyPeriodeMedType(PeriodeResultatType type, ArbeidsgiverLagreDto arbeidsgiver) {
        return nyPeriodeMedType(type, DEFAULT_FOM, DEFAULT_TOM, arbeidsgiver);
    }

    private UttakResultatPeriodeLagreDto nyPeriodeMedType(PeriodeResultatType resultatType,
                                                          LocalDate fom,
                                                          LocalDate tom,
                                                          ArbeidsgiverLagreDto arbeidsgiver) {
        List<UttakResultatPeriodeAktivitetLagreDto> aktiviteter = new ArrayList<>();
        var aktivitetLagreDto = new UttakResultatPeriodeAktivitetLagreDto.Builder().medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdId(ARBEIDSFORHOLD_REF)
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .medTrekkdager(BigDecimal.ZERO)
            .medUtbetalingsgrad(Utbetalingsgrad.TEN)
            .build();
        aktiviteter.add(aktivitetLagreDto);
        return new UttakResultatPeriodeLagreDto.Builder().medTidsperiode(fom, tom)
            .medAktiviteter(aktiviteter)
            .medPeriodeResultatType(resultatType)
            .medPeriodeResultatÅrsak(PeriodeResultatÅrsak.UKJENT)
            .medFlerbarnsdager(false)
            .medSamtidigUttak(false)
            .medBegrunnelse("abc.")
            .build();
    }

    private ForeldrepengerUttakPeriode enkeltPeriode(PeriodeResultatType type) {
        return enkeltPeriode(type, Arbeidsgiver.virksomhet(ORGNR));
    }

    private ForeldrepengerUttakPeriode enkeltPeriode(PeriodeResultatType type, Arbeidsgiver virksomhet) {
        return enkeltPeriode(type, DEFAULT_FOM, DEFAULT_TOM, virksomhet);
    }

    private static BehandlingReferanse mockBehandling() {
        var mock = mock(BehandlingReferanse.class);
        when(mock.behandlingId()).thenReturn(123L);
        return mock;
    }

}
