package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import jakarta.enterprise.context.ApplicationScoped;

import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.foreldrepenger.domene.modell.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.modell.kodeverk.AndelKilde;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class BeregningOversiktDtoTjeneste {

    private BehandlingRepository behandlingRepository;
    private BeregningTjeneste beregningTjeneste;
    private FagsakRepository fagsakRepository;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;


    @Inject
    public BeregningOversiktDtoTjeneste(BehandlingRepository behandlingRepository,
                                        BeregningTjeneste beregningTjeneste,
                                        FagsakRepository fagsakRepository,
                                        InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.beregningTjeneste = beregningTjeneste;
        this.fagsakRepository = fagsakRepository;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public BeregningOversiktDtoTjeneste() {}

    public FpSakBeregningDto hentBeregningForSak(Saksnummer saksnummer) {
        var fagsakId = fagsakRepository.hentSakGittSaksnummer(saksnummer).map(Fagsak::getId).orElseThrow(() -> new IllegalStateException("Fikk saksnummer som ikke finnes"));
        behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId).map(this::mapBeregningsgrunnlag);
    }

    private Optional<FpSakBeregningDto> mapBeregningsgrunnlag(Behandling behandling) {
        var grBeregningsgrunnlag = beregningTjeneste.hent(BehandlingReferanse.fra(behandling));
        var inntektsmeldinger = inntektArbeidYtelseTjeneste.finnGrunnlag(behandling.getId())
            .flatMap(InntektArbeidYtelseGrunnlag::getInntektsmeldinger)
            .map(InntektsmeldingAggregat::getAlleInntektsmeldinger)
            .orElse(List.of());
        return grBeregningsgrunnlag.flatMap(BeregningsgrunnlagGrunnlag::getBeregningsgrunnlag).map(bg -> mapBeregning(bg, inntektsmeldinger));
    }

    private Optional<FpSakBeregningDto> mapBeregning(Beregningsgrunnlag beregningsgrunnlag, List<Inntektsmelding> inntektsmeldinger) {
        if (gjelderBesteberegning(beregningsgrunnlag)) {
            return Optional.empty();
        }
        var aktivitetStatuser = beregningsgrunnlag.getAktivitetStatuser().stream().map(this::mapAktivitetStatus).toList();
        førsteBeregningsperiode(beregningsgrunnlag).map(førstePeriode -> mapAndeler(førstePeriode.getBeregningsgrunnlagPrStatusOgAndelList(), inntektsmeldinger));
        return new FpSakBeregningDto(beregningsgrunnlag.getSkjæringstidspunkt(), null, aktivitetStatuser);
    }

    private static Optional<BeregningsgrunnlagPeriode> førsteBeregningsperiode(Beregningsgrunnlag beregningsgrunnlag) {
        return beregningsgrunnlag.getBeregningsgrunnlagPerioder()
            .stream()
            .filter(bgp -> bgp.getPeriode().getFomDato().equals(beregningsgrunnlag.getSkjæringstidspunkt()))
            .findFirst();
    }

    private boolean gjelderBesteberegning(Beregningsgrunnlag beregningsgrunnlag) {
        return førsteBeregningsperiode(beregningsgrunnlag).map(BeregningsgrunnlagPeriode::getBeregningsgrunnlagPrStatusOgAndelList)
            .orElse(List.of())
            .stream()
            .anyMatch(andel -> andel.getBesteberegnetPrÅr() != null);
    }

    private Object mapAndeler(List<BeregningsgrunnlagPrStatusOgAndel> beregningsgrunnlagPrStatusOgAndelList, List<Inntektsmelding> inntektsmeldinger) {
        var andelerFraStart = beregningsgrunnlagPrStatusOgAndelList.stream()
            .filter(a -> a.getKilde().equals(AndelKilde.PROSESS_START))
            .toList();
        var andelerUtenArbeidsforhold = andelerFraStart.stream()
            .filter(a -> !a.getAktivitetStatus().equals(AktivitetStatus.ARBEIDSTAKER))
            .map(this::mapAndelUtenArbeidsforhold)
            .toList();
        var arbeidsandeler = andelerFraStart.stream().filter(andel -> andel.getAktivitetStatus().equals(AktivitetStatus.ARBEIDSTAKER)).toList();
        mapAndelerMedArbeidsforhold(arbeidsandeler, inntektsmeldinger))

    }

    private void mapAndelerMedArbeidsforhold(List<BeregningsgrunnlagPrStatusOgAndel> arbeidsandeler, List<Inntektsmelding> inntektsmeldinger) {
        var finnesArbeidsandelUtenArbeidstaker = arbeidsandeler.stream().anyMatch(a -> a.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsgiver).isEmpty());
        if (finnesArbeidsandelUtenArbeidstaker) {
            throw new IllegalStateException("Støttes ikke ennå");
        }
        Map<Arbeidsgiver, List<BeregningsgrunnlagPrStatusOgAndel>> andelerPrArbeidsgiver = arbeidsandeler.stream()
            .collect(Collectors.groupingBy(
                andel -> andel.getBgAndelArbeidsforhold()
                    .map(BGAndelArbeidsforhold::getArbeidsgiver)
                    .orElseThrow()
            ));
        andelerPrArbeidsgiver.entrySet().stream().map(entry -> {
            var erSkjønnsfastsatt = entry.getValue().stream().anyMatch(a -> a.getOverstyrtPrÅr() != null);
            var finnesIM = inntektsmeldinger.stream().anyMatch(im -> im.getArbeidsgiver().equals(entry.getKey()));
            // TODO fullfør her, trenger samlet refusjon, samlet inntekt
        })
    }

    private FpSakBeregningDto.BeregningsAndel mapAndelUtenArbeidsforhold(BeregningsgrunnlagPrStatusOgAndel andel) {
        var erSkjønsfastsatt = andel.getOverstyrtPrÅr() != null;
        var fastsattPrÅr = erSkjønsfastsatt ? andel.getOverstyrtPrÅr() : andel.getBeregnetPrÅr();
        var fastsattPrMnd = fastsattPrÅr.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_EVEN);
        if (andel.getAktivitetStatus().equals(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)) {
            var inntektsKilde = erSkjønsfastsatt ? FpSakBeregningDto.InntektsKilde.SKJØNNSFASTSATT : FpSakBeregningDto.InntektsKilde.PGI;
            return new FpSakBeregningDto.BeregningsAndel(andel.getAktivitetStatus(), fastsattPrMnd, inntektsKilde, null, BigDecimal.valueOf(andel.getDagsats()));
        }
        if (andel.getAktivitetStatus().equals(AktivitetStatus.FRILANSER)) {
            var inntektsKilde = erSkjønsfastsatt ? FpSakBeregningDto.InntektsKilde.SKJØNNSFASTSATT : FpSakBeregningDto.InntektsKilde.A_INNTEKT;
            return new FpSakBeregningDto.BeregningsAndel(andel.getAktivitetStatus(), fastsattPrMnd, inntektsKilde, null, BigDecimal.valueOf(andel.getDagsats()));
        }
        if (andel.getAktivitetStatus().equals(AktivitetStatus.ARBEIDSAVKLARINGSPENGER)) {
            return new FpSakBeregningDto.BeregningsAndel(andel.getAktivitetStatus(), fastsattPrMnd, FpSakBeregningDto.InntektsKilde.VEDTAK_ANNEN_YTELSE, null, BigDecimal.valueOf(andel.getDagsats()));
        }
        if (andel.getAktivitetStatus().equals(AktivitetStatus.DAGPENGER)) {
            return new FpSakBeregningDto.BeregningsAndel(andel.getAktivitetStatus(), fastsattPrMnd, FpSakBeregningDto.InntektsKilde.VEDTAK_ANNEN_YTELSE, null, BigDecimal.valueOf(andel.getDagsats()));
        }
        if (andel.getAktivitetStatus().equals(AktivitetStatus.MILITÆR_ELLER_SIVIL)) {
            throw new IllegalStateException("Støttes ikke ennå");
        }
        throw new IllegalStateException("Ukjent aktivitetstatus uten arbeidsforhold: " + andel.getAktivitetStatus());
    }

    private Object mapAndel(BeregningsgrunnlagPrStatusOgAndel andel) {
        var erSkjønsfastsatt = andel.getOverstyrtPrÅr() != null;
        var fastsattPrÅr = erSkjønsfastsatt ? andel.getOverstyrtPrÅr() : andel.getBeregnetPrÅr();
        var fastsattPrMnd = fastsattPrÅr.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_EVEN);

        return FpSakBeregningDto.BeregningsAndel(andel.getAktivitetStatus(), fastsattPrMnd, )
        return null;
    }

    private FpSakBeregningDto.BeregningAktivitetStatus mapAktivitetStatus(BeregningsgrunnlagAktivitetStatus aks) {
        return new FpSakBeregningDto.BeregningAktivitetStatus(aks.getAktivitetStatus(), aks.getHjemmel());
    }


}
