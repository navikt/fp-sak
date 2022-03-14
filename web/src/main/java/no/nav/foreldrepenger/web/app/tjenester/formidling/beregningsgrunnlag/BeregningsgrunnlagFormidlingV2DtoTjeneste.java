package no.nav.foreldrepenger.web.app.tjenester.formidling.beregningsgrunnlag;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.domene.modell.AndelKilde;
import no.nav.foreldrepenger.domene.modell.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.kontrakter.fpsak.beregningsgrunnlag.v2.BeregningsgrunnlagAndelDto;
import no.nav.foreldrepenger.kontrakter.fpsak.beregningsgrunnlag.v2.BeregningsgrunnlagDto;
import no.nav.foreldrepenger.kontrakter.fpsak.beregningsgrunnlag.v2.BeregningsgrunnlagPeriodeDto;
import no.nav.foreldrepenger.kontrakter.fpsak.beregningsgrunnlag.v2.BgAndelArbeidsforholdDto;
import no.nav.foreldrepenger.kontrakter.fpsak.beregningsgrunnlag.v2.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.kontrakter.fpsak.beregningsgrunnlag.v2.kodeverk.Hjemmel;
import no.nav.foreldrepenger.kontrakter.fpsak.beregningsgrunnlag.v2.kodeverk.OpptjeningAktivitetType;
import no.nav.foreldrepenger.kontrakter.fpsak.beregningsgrunnlag.v2.kodeverk.PeriodeÅrsak;

public class BeregningsgrunnlagFormidlingV2DtoTjeneste {

    private final BeregningsgrunnlagGrunnlagEntitet grunnlag;

    public BeregningsgrunnlagFormidlingV2DtoTjeneste(BeregningsgrunnlagGrunnlagEntitet grunnlag) {
        this.grunnlag = Objects.requireNonNull(grunnlag, "beregningsgrunnlaggrunnlag");
    }

    public Optional<BeregningsgrunnlagDto> map() {
        var bgPerioder = grunnlag.getBeregningsgrunnlag()
            .map(BeregningsgrunnlagEntitet::getBeregningsgrunnlagPerioder)
            .orElse(Collections.emptyList())
            .stream()
            .map(this::mapPeriode)
            .collect(Collectors.toList());
        return grunnlag.getBeregningsgrunnlag().map(bg -> new BeregningsgrunnlagDto(mapAktivitetstatuser(bg.getAktivitetStatuser()),
            mapHjemmelTilDto(bg.getHjemmel()),
            bg.getGrunnbeløp().getVerdi(),
            bgPerioder,
            utledBesteberegning()));
    }

    private Hjemmel mapHjemmelTilDto(no.nav.foreldrepenger.domene.modell.Hjemmel hjemmel) {
        return switch (hjemmel) {
            case F_14_7 -> Hjemmel.F_14_7;
            case F_14_7_8_30 -> Hjemmel.F_14_7_8_30;
            case F_14_7_8_35 -> Hjemmel.F_14_7_8_35;
            case F_14_7_8_38 -> Hjemmel.F_14_7_8_38;
            case F_14_7_8_40 -> Hjemmel.F_14_7_8_40;
            case F_14_7_8_41 -> Hjemmel.F_14_7_8_41;
            case F_14_7_8_42 -> Hjemmel.F_14_7_8_42;
            case F_14_7_8_43 -> Hjemmel.F_14_7_8_43;
            case F_14_7_8_47 -> Hjemmel.F_14_7_8_47;
            case F_14_7_8_49 -> Hjemmel.F_14_7_8_49;
            case F_14_7_8_28_8_30 -> Hjemmel.F_14_7_8_28_8_30;
            case UDEFINERT -> null;
        };
    }

    private boolean utledBesteberegning() {
        // Automatisk besteberegnet
        var besteBeregningGrunnlag = grunnlag.getBeregningsgrunnlag().flatMap(BeregningsgrunnlagEntitet::getBesteberegninggrunnlag);

        // Manuelt besteberegnet
        var finnesBesteberegnetAndel = grunnlag.getBeregningsgrunnlag()
            .map(BeregningsgrunnlagEntitet::getBeregningsgrunnlagPerioder)
            .orElse(Collections.emptyList())
            .stream()
            .anyMatch(bgp -> finnesBesteberegnetAndel(bgp.getBeregningsgrunnlagPrStatusOgAndelList()));

        return besteBeregningGrunnlag.isPresent() || finnesBesteberegnetAndel;
    }

    private boolean finnesBesteberegnetAndel(List<BeregningsgrunnlagPrStatusOgAndel> beregningsgrunnlagPrStatusOgAndelList) {
        return beregningsgrunnlagPrStatusOgAndelList.stream().anyMatch(bga -> bga.getBesteberegningPrÅr() != null);
    }

    private List<AktivitetStatus> mapAktivitetstatuser(List<BeregningsgrunnlagAktivitetStatus> aktivitetStatuser) {
        return aktivitetStatuser.stream()
            .map(BeregningsgrunnlagAktivitetStatus::getAktivitetStatus)
            .map(this::mapAktivitetStatusTilDto)
            .collect(Collectors.toList());
    }

    private AktivitetStatus mapAktivitetStatusTilDto(no.nav.foreldrepenger.domene.modell.AktivitetStatus aktivitetStatus) {
        return switch(aktivitetStatus) {
            case ARBEIDSAVKLARINGSPENGER -> AktivitetStatus.ARBEIDSAVKLARINGSPENGER;
            case ARBEIDSTAKER -> AktivitetStatus.ARBEIDSTAKER;
            case DAGPENGER -> AktivitetStatus.DAGPENGER;
            case FRILANSER -> AktivitetStatus.FRILANSER;
            case MILITÆR_ELLER_SIVIL -> AktivitetStatus.MILITÆR_ELLER_SIVIL;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE;
            case KOMBINERT_AT_FL -> AktivitetStatus.KOMBINERT_AT_FL;
            case KOMBINERT_AT_SN -> AktivitetStatus.KOMBINERT_AT_SN;
            case KOMBINERT_FL_SN -> AktivitetStatus.KOMBINERT_AT_SN;
            case KOMBINERT_AT_FL_SN -> AktivitetStatus.KOMBINERT_AT_FL_SN;
            case BRUKERS_ANDEL -> AktivitetStatus.BRUKERS_ANDEL;
            case KUN_YTELSE -> AktivitetStatus.KUN_YTELSE;
            case TTLSTØTENDE_YTELSE -> AktivitetStatus.TTLSTØTENDE_YTELSE;
            case VENTELØNN_VARTPENGER -> AktivitetStatus.VENTELØNN_VARTPENGER;
            case UDEFINERT -> null;
        };
    }

    private BeregningsgrunnlagPeriodeDto mapPeriode(BeregningsgrunnlagPeriode bgPeriode) {
        var andeler = bgPeriode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .map(this::mapAndel)
            .collect(Collectors.toList());
        var bruttoInkludertBortfaltNaturalytelsePrAar = bgPeriode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .map(BeregningsgrunnlagPrStatusOgAndel::getBruttoInkludertNaturalYtelser)
            .filter(Objects::nonNull)
            .reduce(BigDecimal::add)
            .orElse(null);
        return new BeregningsgrunnlagPeriodeDto(bgPeriode.getDagsats(),
            bgPeriode.getBruttoPrÅr(),
            bgPeriode.getAvkortetPrÅr() == null
                ? null
                : finnAvkortetUtenGraderingPrÅr(bruttoInkludertBortfaltNaturalytelsePrAar,
                grunnlag.getBeregningsgrunnlag().orElseThrow().getGrunnbeløp()),
            mapPeriodeÅrsakerTilDto(bgPeriode.getPeriodeÅrsaker()),
            bgPeriode.getBeregningsgrunnlagPeriodeFom(),
            bgPeriode.getBeregningsgrunnlagPeriodeTom(),
            andeler);
    }

    private List<PeriodeÅrsak> mapPeriodeÅrsakerTilDto(List<no.nav.foreldrepenger.domene.modell.PeriodeÅrsak> periodeÅrsaker) {
        return periodeÅrsaker.stream().map(this::mapPeriodeÅrsakTilDto).toList();
    }

    private PeriodeÅrsak mapPeriodeÅrsakTilDto(no.nav.foreldrepenger.domene.modell.PeriodeÅrsak periodeÅrsak) {
        return switch (periodeÅrsak) {
            case GRADERING -> PeriodeÅrsak.GRADERING;
            case REFUSJON_AVSLÅTT -> PeriodeÅrsak.REFUSJON_AVSLÅTT;
            case GRADERING_OPPHØRER -> PeriodeÅrsak.GRADERING_OPPHØRER;
            case REFUSJON_OPPHØRER -> PeriodeÅrsak.REFUSJON_OPPHØRER;
            case NATURALYTELSE_BORTFALT -> PeriodeÅrsak.NATURALYTELSE_BORTFALT;
            case ENDRING_I_REFUSJONSKRAV -> PeriodeÅrsak.ENDRING_I_REFUSJONSKRAV;
            case NATURALYTELSE_TILKOMMER -> PeriodeÅrsak.NATURALYTELSE_TILKOMMER;
            case ARBEIDSFORHOLD_AVSLUTTET -> PeriodeÅrsak.ARBEIDSFORHOLD_AVSLUTTET;
            case ENDRING_I_AKTIVITETER_SØKT_FOR -> PeriodeÅrsak.ENDRING_I_AKTIVITETER_SØKT_FOR;
            case UDEFINERT -> null;
        };
    }

    private BigDecimal finnAvkortetUtenGraderingPrÅr(BigDecimal bruttoInkludertBortfaltNaturalytelsePrAar, Beløp grunnbeløp) {
        if (bruttoInkludertBortfaltNaturalytelsePrAar == null) {
            return null;
        }
        var seksG = grunnbeløp.multipliser(6).getVerdi();
        return bruttoInkludertBortfaltNaturalytelsePrAar.compareTo(seksG) > 0 ? seksG : bruttoInkludertBortfaltNaturalytelsePrAar;
    }

    private BeregningsgrunnlagAndelDto mapAndel(BeregningsgrunnlagPrStatusOgAndel andel) {
        var arbeidsforholdDto = andel.getBgAndelArbeidsforhold().map(this::mapArbeidsforhold);
        return new BeregningsgrunnlagAndelDto(andel.getDagsats(),
            mapAktivitetStatusTilDto(andel.getAktivitetStatus()),
            andel.getBruttoPrÅr(), andel.getAvkortetPrÅr(),
            andel.getNyIArbeidslivet(),
            mapOpptjeningAktivitetsTypeTilDto(andel.getArbeidsforholdType()),
            andel.getBeregningsperiodeFom(),
            andel.getBeregningsperiodeTom(),
            arbeidsforholdDto.orElse(null),
            erTilkommetAndel(andel.getKilde()));
    }

    private OpptjeningAktivitetType mapOpptjeningAktivitetsTypeTilDto(no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType arbeidsforholdType) {
        return switch (arbeidsforholdType) {
            case DAGPENGER -> OpptjeningAktivitetType.DAGPENGER;
            case VENTELØNN_VARTPENGER -> OpptjeningAktivitetType.VENTELØNN_VARTPENGER;
            case SVANGERSKAPSPENGER -> OpptjeningAktivitetType.SVANGERSKAPSPENGER;
            case ARBEID -> OpptjeningAktivitetType.ARBEID;
            case NÆRING -> OpptjeningAktivitetType.NÆRING;
            case FRILANS -> OpptjeningAktivitetType.FRILANS;
            case FRISINN -> OpptjeningAktivitetType.FRISINN;
            case SYKEPENGER -> OpptjeningAktivitetType.SYKEPENGER;
            case PLEIEPENGER -> OpptjeningAktivitetType.PLEIEPENGER;
            case OMSORGSPENGER -> OpptjeningAktivitetType.OMSORGSPENGER;
            case FORELDREPENGER -> OpptjeningAktivitetType.FORELDREPENGER;
            case ARBEIDSAVKLARING -> OpptjeningAktivitetType.ARBEIDSAVKLARING;
            case OPPLÆRINGSPENGER -> OpptjeningAktivitetType.OPPLÆRINGSPENGER;
            case UTDANNINGSPERMISJON -> OpptjeningAktivitetType.UTDANNINGSPERMISJON;
            case ETTERLØNN_SLUTTPAKKE -> OpptjeningAktivitetType.ETTERLØNN_SLUTTPAKKE;
            case VIDERE_ETTERUTDANNING -> OpptjeningAktivitetType.VIDERE_ETTERUTDANNING;
            case UTENLANDSK_ARBEIDSFORHOLD -> OpptjeningAktivitetType.UTENLANDSK_ARBEIDSFORHOLD;
            case MILITÆR_ELLER_SIVILTJENESTE -> OpptjeningAktivitetType.MILITÆR_ELLER_SIVILTJENESTE;
            case UDEFINERT -> null;
            case FRILOPP -> throw new IllegalArgumentException("Argumentet støttes ikke: FRILOPP");
        };
    }

    private BgAndelArbeidsforholdDto mapArbeidsforhold(BGAndelArbeidsforhold bga) {
        // BGAndelArbeidsforhold skal alltid ha en arbeidsgiver satt
        return new BgAndelArbeidsforholdDto(bga.getArbeidsgiver().getIdentifikator(),
            bga.getArbeidsforholdRef().getReferanse(),
            bga.getNaturalytelseBortfaltPrÅr().orElse(null),
            bga.getNaturalytelseTilkommetPrÅr().orElse(null));
    }

    private boolean erTilkommetAndel(AndelKilde kilde) {
        return !kilde.equals(AndelKilde.PROSESS_START);
    }

}
