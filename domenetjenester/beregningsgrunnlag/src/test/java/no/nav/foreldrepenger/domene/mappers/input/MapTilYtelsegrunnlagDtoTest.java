package no.nav.foreldrepenger.domene.mappers.input;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.YtelseBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YtelseFilter;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.Arbeidskategori;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

class MapTilYtelsegrunnlagDtoTest {
    private static final LocalDate STP = LocalDate.of(2021,1,1);

    @Test
    void skal_mappe_sykepenger() {
        var sykeBuilder = lagSykepenger(DatoIntervallEntitet.fraOgMedTilOgMed(månederFørStp(15), månederFørStp(6)));
        lagYtelseGrunnlag(sykeBuilder, Arbeidskategori.DAGPENGER);
        var filter = new YtelseFilter(Collections.singletonList(sykeBuilder.build()));
        var mappetGrunnlag = MapTilYtelsegrunnlagDto.mapEksterneYtelserTilBesteberegningYtelsegrunnlag(DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(12), STP), filter, RelatertYtelseType.SYKEPENGER);
        assertThat(mappetGrunnlag).isPresent();
        assertThat(mappetGrunnlag.get().getPerioder()).hasSize(1);
        assertThat(mappetGrunnlag.get().getPerioder().getFirst().getPeriode().getFom()).isEqualTo(månederFørStp(15));
        assertThat(mappetGrunnlag.get().getPerioder().getFirst().getPeriode().getTom()).isEqualTo(månederFørStp(6));
        assertThat(mappetGrunnlag.get().getPerioder().getFirst().getAndeler()).hasSize(1);
        assertThat(mappetGrunnlag.get().getPerioder().getFirst().getAndeler().getFirst().getArbeidskategori().getKode()).isEqualTo(Arbeidskategori.DAGPENGER.getKode());
    }

    @Test
    void skal_ikke_mappe_ubehandlede_ytelser_med_ugyldig_kategori() {
        var sykeBuilder = lagSykepenger(DatoIntervallEntitet.fraOgMedTilOgMed(månederFørStp(15), månederFørStp(6)), RelatertYtelseTilstand.ÅPEN);
        lagYtelseGrunnlag(sykeBuilder, Arbeidskategori.UGYLDIG);
        var filter = new YtelseFilter(Collections.singletonList(sykeBuilder.build()));
        var mappetGrunnlag = MapTilYtelsegrunnlagDto.mapEksterneYtelserTilBesteberegningYtelsegrunnlag(DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(12), STP), filter, RelatertYtelseType.SYKEPENGER);
        assertThat(mappetGrunnlag).isEmpty();
    }

    @Test
    void skal_mappe_foreldrepenger() {
        var brres = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .build();
        lagFpsakResultatAndel(lagFpsakResultatPeriode(brres, månederFørStp(12), månederFørStp(7)), AktivitetStatus.ARBEIDSTAKER, 350);
        lagFpsakResultatAndel(lagFpsakResultatPeriode(brres, månederFørStp(5), månederFørStp(2)), AktivitetStatus.ARBEIDSTAKER, 700);
        var mappetGrunnlag = MapTilYtelsegrunnlagDto.mapFpsakYtelseTilYtelsegrunnlag(brres, FagsakYtelseType.FORELDREPENGER);
        assertThat(mappetGrunnlag).isPresent();
        var sortertePerioder = mappetGrunnlag.get().getPerioder().stream().sorted(Comparator.comparing(yp -> yp.getPeriode().getFom())).toList();
        assertThat(sortertePerioder).hasSize(2);
        assertThat(sortertePerioder.getFirst().getPeriode().getFom()).isEqualTo(månederFørStp(12));
        assertThat(sortertePerioder.getFirst().getPeriode().getTom()).isEqualTo(månederFørStp(7));
        assertThat(sortertePerioder.getFirst().getAndeler()).hasSize(1);
        assertThat(sortertePerioder.get(0).getAndeler().getFirst().getAktivitetStatus().getKode()).isEqualTo(AktivitetStatus.ARBEIDSTAKER.getKode());
        assertThat(sortertePerioder.get(0).getAndeler().getFirst().getDagsats()).isEqualTo(350L);


        assertThat(sortertePerioder.get(1).getPeriode().getFom()).isEqualTo(månederFørStp(5));
        assertThat(sortertePerioder.get(1).getPeriode().getTom()).isEqualTo(månederFørStp(2));
        assertThat(sortertePerioder.get(1).getAndeler()).hasSize(1);
        assertThat(sortertePerioder.get(1).getAndeler().getFirst().getAktivitetStatus().getKode()).isEqualTo(AktivitetStatus.ARBEIDSTAKER.getKode());
        assertThat(sortertePerioder.get(1).getAndeler().getFirst().getDagsats()).isEqualTo(700L);
    }


    private LocalDate månederFørStp(int måneder) {
        return STP.minusDays(måneder);
    }

    private void lagYtelseGrunnlag(YtelseBuilder builder, Arbeidskategori arbeidskategori) {
        builder.medYtelseGrunnlag(builder.getGrunnlagBuilder()
            .medArbeidskategori(arbeidskategori)
            .medDekningsgradProsent(BigDecimal.ZERO)
            .medVedtaksDagsats(Beløp.ZERO)
            .medInntektsgrunnlagProsent(BigDecimal.ZERO)
            .build());
    }

    private YtelseBuilder lagSykepenger(DatoIntervallEntitet periode) {
        return lagSykepenger(periode, RelatertYtelseTilstand.AVSLUTTET);
    }

    private YtelseBuilder lagSykepenger(DatoIntervallEntitet periode, RelatertYtelseTilstand tilstand) {
        return YtelseBuilder.oppdatere(Optional.empty())
            .medPeriode(periode)
            .medYtelseType(RelatertYtelseType.SYKEPENGER)
            .medStatus(tilstand)
            .medKilde(Fagsystem.INFOTRYGD)
            .medSaksnummer(new Saksnummer("3339"));
    }

    private BeregningsresultatPeriode lagFpsakResultatPeriode(BeregningsresultatEntitet beregningsresultat, LocalDate fom, LocalDate tom) {
        return BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(fom, tom)
            .build(beregningsresultat);
    }

    private BeregningsresultatAndel lagFpsakResultatAndel(BeregningsresultatPeriode beregningsresultatPeriode,
                                                          AktivitetStatus aktivitetStatus, int dagsats) {
        var arbeidsgiver = Arbeidsgiver.virksomhet("999999999");
        return BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdRef(InternArbeidsforholdRef.nullRef())
            .medAktivitetStatus(aktivitetStatus)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medStillingsprosent(BigDecimal.ZERO)
            .medUtbetalingsgrad(BigDecimal.ZERO)
            .medDagsats(dagsats)
            .medDagsatsFraBg(dagsats)
            .medArbeidsforholdType(OpptjeningAktivitetType.ARBEID)
            .build(beregningsresultatPeriode);
    }
}
