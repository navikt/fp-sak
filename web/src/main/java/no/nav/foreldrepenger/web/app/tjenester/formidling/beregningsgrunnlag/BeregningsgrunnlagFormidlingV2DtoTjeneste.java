package no.nav.foreldrepenger.web.app.tjenester.formidling.beregningsgrunnlag;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.domene.modell.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.modell.FaktaAggregat;
import no.nav.foreldrepenger.domene.modell.FaktaAktør;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.kodeverk.AndelKilde;
import no.nav.foreldrepenger.domene.modell.kodeverk.Hjemmel;
import no.nav.foreldrepenger.domene.modell.kodeverk.PeriodeÅrsak;
import no.nav.foreldrepenger.domene.modell.FaktaVurdering;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.kontrakter.fpsak.beregningsgrunnlag.v2.BeregningsgrunnlagAndelDto;
import no.nav.foreldrepenger.kontrakter.fpsak.beregningsgrunnlag.v2.BeregningsgrunnlagDto;
import no.nav.foreldrepenger.kontrakter.fpsak.beregningsgrunnlag.v2.BeregningsgrunnlagPeriodeDto;
import no.nav.foreldrepenger.kontrakter.fpsak.beregningsgrunnlag.v2.BgAndelArbeidsforholdDto;
import no.nav.foreldrepenger.kontrakter.fpsak.beregningsgrunnlag.v2.kodeverk.AktivitetStatusDto;
import no.nav.foreldrepenger.kontrakter.fpsak.beregningsgrunnlag.v2.kodeverk.HjemmelDto;
import no.nav.foreldrepenger.kontrakter.fpsak.beregningsgrunnlag.v2.kodeverk.OpptjeningAktivitetDto;
import no.nav.foreldrepenger.kontrakter.fpsak.beregningsgrunnlag.v2.kodeverk.PeriodeÅrsakDto;

public class BeregningsgrunnlagFormidlingV2DtoTjeneste {

    private final BeregningsgrunnlagGrunnlag grunnlag;
    private static final Logger LOG = LoggerFactory.getLogger(BeregningsgrunnlagFormidlingV2DtoTjeneste.class);

    public BeregningsgrunnlagFormidlingV2DtoTjeneste(BeregningsgrunnlagGrunnlag grunnlag) {
        this.grunnlag = Objects.requireNonNull(grunnlag, "beregningsgrunnlaggrunnlag");
    }

    public Optional<BeregningsgrunnlagDto> map() {
        var bgPerioder = grunnlag.getBeregningsgrunnlag()
            .map(Beregningsgrunnlag::getBeregningsgrunnlagPerioder)
            .orElse(List.of())
            .stream()
            .map(this::mapPeriode)
            .toList();
        var erBesteberegnet = utledBesteberegning();
        return grunnlag.getBeregningsgrunnlag().map(bg -> new BeregningsgrunnlagDto(mapAktivitetstatuser(bg.getAktivitetStatuser()),
            mapHjemmelTilDto(bg.getHjemmel()),
            bg.getGrunnbeløp().getVerdi(),
            bgPerioder,
            erBesteberegnet,
            erBesteberegnet && erSeksAvDeTiBeste()));
    }

    private HjemmelDto mapHjemmelTilDto(Hjemmel hjemmel) {
        return switch (hjemmel) {
            case F_14_7 -> HjemmelDto.F_14_7;
            case F_14_7_8_30 -> HjemmelDto.F_14_7_8_30;
            case F_14_7_8_35 -> HjemmelDto.F_14_7_8_35;
            case F_14_7_8_38 -> HjemmelDto.F_14_7_8_38;
            case F_14_7_8_40 -> HjemmelDto.F_14_7_8_40;
            case F_14_7_8_41 -> HjemmelDto.F_14_7_8_41;
            case F_14_7_8_42 -> HjemmelDto.F_14_7_8_42;
            case F_14_7_8_43 -> HjemmelDto.F_14_7_8_43;
            case F_14_7_8_47 -> HjemmelDto.F_14_7_8_47;
            case F_14_7_8_49 -> HjemmelDto.F_14_7_8_49;
            case F_14_7_8_28_8_30 -> HjemmelDto.F_14_7_8_28_8_30;
            case UDEFINERT -> null;
        };
    }

    private boolean utledBesteberegning() {
        // Automatisk besteberegnet
        var besteBeregningGrunnlag = grunnlag.getBeregningsgrunnlag().flatMap(Beregningsgrunnlag::getBesteberegningGrunnlag);
        // Manuelt besteberegnet
        var finnesBesteberegnetAndel = grunnlag.getBeregningsgrunnlag()
            .map(Beregningsgrunnlag::getBeregningsgrunnlagPerioder)
            .orElse(Collections.emptyList())
            .stream()
            .anyMatch(bgp -> finnesBesteberegnetAndel(bgp.getBeregningsgrunnlagPrStatusOgAndelList()));

        return besteBeregningGrunnlag.isPresent() || finnesBesteberegnetAndel;
    }

    private boolean erSeksAvDeTiBeste() {
        var besteBeregningGrunnlag = grunnlag.getBeregningsgrunnlag().flatMap(Beregningsgrunnlag::getBesteberegningGrunnlag);
        if (besteBeregningGrunnlag.isEmpty()){
            //manuelt besteberegnet er alltid seks av de ti beste månedene
            return true;
        }

        var besteBeregnetBeløp = grunnlag.getBeregningsgrunnlag()
            .map(Beregningsgrunnlag::getBeregningsgrunnlagPerioder)
            .orElse(Collections.emptyList())
            .stream()
            .map(BeregningsgrunnlagPeriode::getBeregningsgrunnlagPrStatusOgAndelList)
            .flatMap(Collection::stream)
            .map(andel -> andel.getBesteberegnetPrÅr() != null ? andel.getBesteberegnetPrÅr() : BigDecimal.ZERO)
            .reduce(BigDecimal::add)
            .orElse(BigDecimal.ZERO);

        List<BeregningsgrunnlagPeriode> beregningsGrunnlagPerioder = grunnlag.getBeregningsgrunnlag()
            .map(Beregningsgrunnlag::getBeregningsgrunnlagPerioder)
            .orElse(Collections.emptyList());

        var ordinærtBeregnetBeløp = beregningsGrunnlagPerioder.get(0).getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .map(this::hentOverstyrtEllerBeregnetHvisFinnes)
            .reduce(BigDecimal::add)
            .orElse(BigDecimal.ZERO);

        LOG.info("BesteBeregnetBeløp fra getBesteberegnetPrÅr: {}  Ordinært eller overstyrt beregnet beløp: {}",  besteBeregnetBeløp, ordinærtBeregnetBeløp );

        return besteBeregnetBeløp.compareTo(ordinærtBeregnetBeløp) > 0;
    }

    private BigDecimal hentOverstyrtEllerBeregnetHvisFinnes(BeregningsgrunnlagPrStatusOgAndel andel) {
        return Optional.ofNullable(andel.getOverstyrtPrÅr())
            .or(() -> Optional.ofNullable(andel.getBeregnetPrÅr()))
            .orElse(BigDecimal.ZERO);
    }

    private boolean finnesBesteberegnetAndel(List<BeregningsgrunnlagPrStatusOgAndel> beregningsgrunnlagPrStatusOgAndelList) {
        return beregningsgrunnlagPrStatusOgAndelList.stream().anyMatch(bga -> bga.getBesteberegnetPrÅr() != null);
    }

    private List<AktivitetStatusDto> mapAktivitetstatuser(List<BeregningsgrunnlagAktivitetStatus> aktivitetStatuser) {
        return aktivitetStatuser.stream()
            .map(BeregningsgrunnlagAktivitetStatus::getAktivitetStatus)
            .map(this::mapAktivitetStatusTilDto)
            .toList();
    }

    private AktivitetStatusDto mapAktivitetStatusTilDto(AktivitetStatus aktivitetStatus) {
        return switch(aktivitetStatus) {
            case ARBEIDSAVKLARINGSPENGER -> AktivitetStatusDto.ARBEIDSAVKLARINGSPENGER;
            case ARBEIDSTAKER -> AktivitetStatusDto.ARBEIDSTAKER;
            case DAGPENGER -> AktivitetStatusDto.DAGPENGER;
            case FRILANSER -> AktivitetStatusDto.FRILANSER;
            case MILITÆR_ELLER_SIVIL -> AktivitetStatusDto.MILITÆR_ELLER_SIVIL;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> AktivitetStatusDto.SELVSTENDIG_NÆRINGSDRIVENDE;
            case KOMBINERT_AT_FL -> AktivitetStatusDto.KOMBINERT_AT_FL;
            case KOMBINERT_AT_SN -> AktivitetStatusDto.KOMBINERT_AT_SN;
            case KOMBINERT_FL_SN -> AktivitetStatusDto.KOMBINERT_FL_SN;
            case KOMBINERT_AT_FL_SN -> AktivitetStatusDto.KOMBINERT_AT_FL_SN;
            case BRUKERS_ANDEL -> AktivitetStatusDto.BRUKERS_ANDEL;
            case KUN_YTELSE -> AktivitetStatusDto.KUN_YTELSE;
            case TTLSTØTENDE_YTELSE -> AktivitetStatusDto.TTLSTØTENDE_YTELSE;
            case VENTELØNN_VARTPENGER -> AktivitetStatusDto.VENTELØNN_VARTPENGER;
            case UDEFINERT -> throw new IllegalArgumentException("AKtivitetStatus udefinert");
        };
    }

    private BeregningsgrunnlagPeriodeDto mapPeriode(BeregningsgrunnlagPeriode bgPeriode) {
        var andeler = bgPeriode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .map(this::mapAndel)
            .toList();
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

    private List<PeriodeÅrsakDto> mapPeriodeÅrsakerTilDto(List<PeriodeÅrsak> periodeÅrsaker) {
        return periodeÅrsaker.stream().map(this::mapPeriodeÅrsakTilDto).toList();
    }

    private PeriodeÅrsakDto mapPeriodeÅrsakTilDto(PeriodeÅrsak periodeÅrsak) {
        return switch (periodeÅrsak) {
            case GRADERING -> PeriodeÅrsakDto.GRADERING;
            case REFUSJON_AVSLÅTT -> PeriodeÅrsakDto.REFUSJON_AVSLÅTT;
            case GRADERING_OPPHØRER -> PeriodeÅrsakDto.GRADERING_OPPHØRER;
            case REFUSJON_OPPHØRER -> PeriodeÅrsakDto.REFUSJON_OPPHØRER;
            case NATURALYTELSE_BORTFALT -> PeriodeÅrsakDto.NATURALYTELSE_BORTFALT;
            case ENDRING_I_REFUSJONSKRAV -> PeriodeÅrsakDto.ENDRING_I_REFUSJONSKRAV;
            case NATURALYTELSE_TILKOMMER -> PeriodeÅrsakDto.NATURALYTELSE_TILKOMMER;
            case ARBEIDSFORHOLD_AVSLUTTET -> PeriodeÅrsakDto.ARBEIDSFORHOLD_AVSLUTTET;
            case ENDRING_I_AKTIVITETER_SØKT_FOR -> PeriodeÅrsakDto.ENDRING_I_AKTIVITETER_SØKT_FOR;
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
        var faktaVurdering = finnFaktaavklaringForGrunnlag().map(FaktaAktør::getErNyIArbeidslivetSN);
        var arbeidsforholdDto = andel.getBgAndelArbeidsforhold().map(this::mapArbeidsforhold);
        var aktivitetStatus = mapAktivitetStatusTilDto(andel.getAktivitetStatus());
        return new BeregningsgrunnlagAndelDto(andel.getDagsats(), aktivitetStatus,
            andel.getBruttoPrÅr(), andel.getAvkortetPrÅr(),
            faktaVurdering.map(FaktaVurdering::getVurdering).orElse(null),
            mapOpptjeningAktivitetsTypeTilDto(andel.getArbeidsforholdType()),
            andel.getBeregningsperiodeFom(),
            andel.getBeregningsperiodeTom(),
            arbeidsforholdDto.orElse(null),
            erTilkommetAndel(andel.getKilde()));
    }

    private Optional<FaktaAktør> finnFaktaavklaringForGrunnlag() {
        return grunnlag.getFaktaAggregat().flatMap(FaktaAggregat::getFaktaAktør);
    }

    private OpptjeningAktivitetDto mapOpptjeningAktivitetsTypeTilDto(OpptjeningAktivitetType arbeidsforholdType) {
        return switch (arbeidsforholdType) {
            case DAGPENGER -> OpptjeningAktivitetDto.DAGPENGER;
            case VENTELØNN_VARTPENGER -> OpptjeningAktivitetDto.VENTELØNN_VARTPENGER;
            case SVANGERSKAPSPENGER -> OpptjeningAktivitetDto.SVANGERSKAPSPENGER;
            case ARBEID -> OpptjeningAktivitetDto.ARBEID;
            case NÆRING -> OpptjeningAktivitetDto.NÆRING;
            case FRILANS -> OpptjeningAktivitetDto.FRILANS;
            case FRISINN -> OpptjeningAktivitetDto.FRISINN;
            case SYKEPENGER -> OpptjeningAktivitetDto.SYKEPENGER;
            case PLEIEPENGER -> OpptjeningAktivitetDto.PLEIEPENGER;
            case OMSORGSPENGER -> OpptjeningAktivitetDto.OMSORGSPENGER;
            case FORELDREPENGER -> OpptjeningAktivitetDto.FORELDREPENGER;
            case ARBEIDSAVKLARING -> OpptjeningAktivitetDto.ARBEIDSAVKLARING;
            case OPPLÆRINGSPENGER -> OpptjeningAktivitetDto.OPPLÆRINGSPENGER;
            case UTDANNINGSPERMISJON -> OpptjeningAktivitetDto.UTDANNINGSPERMISJON;
            case ETTERLØNN_SLUTTPAKKE -> OpptjeningAktivitetDto.ETTERLØNN_SLUTTPAKKE;
            case VIDERE_ETTERUTDANNING -> OpptjeningAktivitetDto.VIDERE_ETTERUTDANNING;
            case UTENLANDSK_ARBEIDSFORHOLD -> OpptjeningAktivitetDto.UTENLANDSK_ARBEIDSFORHOLD;
            case MILITÆR_ELLER_SIVILTJENESTE -> OpptjeningAktivitetDto.MILITÆR_ELLER_SIVILTJENESTE;
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
