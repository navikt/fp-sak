package no.nav.foreldrepenger.domene.rest.historikk;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstBuilderFormater.formatDate;
import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstBuilderFormater.formatString;
import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder.fraTilEquals;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.entiteter.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.entiteter.BeregningRefusjonOverstyringEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningRefusjonOverstyringerEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningRefusjonPeriodeEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.rest.dto.VurderRefusjonAndelBeregningsgrunnlagDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderRefusjonBeregningsgrunnlagDto;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

@ApplicationScoped
public class VurderRefusjonBeregningsgrunnlagHistorikkTjeneste {
    private static final BigDecimal MÅNEDER_I_ÅR = BigDecimal.valueOf(12);
    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private Historikkinnslag2Repository historikkinnslagRepository;


    VurderRefusjonBeregningsgrunnlagHistorikkTjeneste() {
        // for CDI proxy
    }

    @Inject
    public VurderRefusjonBeregningsgrunnlagHistorikkTjeneste(ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste,
                                                             InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                                             Historikkinnslag2Repository historikkinnslagRepository) {
        this.arbeidsgiverHistorikkinnslagTjeneste = arbeidsgiverHistorikkinnslagTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    public void lagHistorikk(VurderRefusjonBeregningsgrunnlagDto dto,
                             AksjonspunktOppdaterParameter param,
                             Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag) {
        var behandlingId = param.getBehandlingId();
        var arbeidsforholdOverstyringer = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId).getArbeidsforholdOverstyringer();
        var historikkinnslagBuilder = new Historikkinnslag2.Builder();
        List<HistorikkinnslagTekstlinjeBuilder> tekstlinjerBuilder = new ArrayList<>();
        var forrigeOverstyringer = forrigeGrunnlag.flatMap(BeregningsgrunnlagGrunnlagEntitet::getRefusjonOverstyringer)
            .map(BeregningRefusjonOverstyringerEntitet::getRefusjonOverstyringer)
            .orElse(Collections.emptyList());
        var forrigeBeregningsgrunnlag = forrigeGrunnlag.flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag);
        for (var fastsattAndel : dto.getFastsatteAndeler()) {
            var forrigeRefusjonsstart = finnForrigeRefusjonsstartForArbeidsforhold(fastsattAndel, forrigeOverstyringer);
            Optional<BigDecimal> forrigeDelvisRefusjonPrÅr = forrigeRefusjonsstart.isEmpty() ? Optional.empty() : finnForrigeDelvisRefusjon(
                fastsattAndel, forrigeRefusjonsstart.get(), forrigeBeregningsgrunnlag);
            tekstlinjerBuilder.add(new HistorikkinnslagTekstlinjeBuilder().t(dto.getBegrunnelse()));
            tekstlinjerBuilder = leggTilArbeidsforholdHistorikkinnslag(fastsattAndel, forrigeRefusjonsstart, forrigeDelvisRefusjonPrÅr,
                arbeidsforholdOverstyringer);
        }

        if (!tekstlinjerBuilder.isEmpty()) {
            historikkinnslagBuilder.medAktør(HistorikkAktør.SAKSBEHANDLER)
                .medBehandlingId(param.getBehandlingId())
                .medFagsakId(param.getRef().fagsakId())
                .medTittel(SkjermlenkeType.FAKTA_OM_FORDELING)
                .medTekstlinjer(tekstlinjerBuilder);
            historikkinnslagRepository.lagre(historikkinnslagBuilder.build());
        }
    }

    private Optional<BigDecimal> finnForrigeDelvisRefusjon(VurderRefusjonAndelBeregningsgrunnlagDto fastsattAndel,
                                                           LocalDate forrigeRefusjonsstart,
                                                           Optional<BeregningsgrunnlagEntitet> forrigeBeregningsgrunnlag) {
        var forrigeBGPerioder = forrigeBeregningsgrunnlag.map(BeregningsgrunnlagEntitet::getBeregningsgrunnlagPerioder)
            .orElse(Collections.emptyList());
        var andelerIForrugeGrunnlagFørRefusjonstart = forrigeBGPerioder.stream()
            .filter(bgp -> bgp.getBeregningsgrunnlagPeriodeFom().isBefore(forrigeRefusjonsstart))
            .findFirst()
            .map(BeregningsgrunnlagPeriode::getBeregningsgrunnlagPrStatusOgAndelList)
            .orElse(Collections.emptyList());
        var forrigeMatchendeAndel = andelerIForrugeGrunnlagFørRefusjonstart.stream()
            .filter(andel -> andel.getArbeidsgiver().isPresent() && matcherAG(andel.getArbeidsgiver().get(), fastsattAndel) && matcherReferanse(
                andel.getArbeidsforholdRef().orElse(InternArbeidsforholdRef.nullRef()), fastsattAndel))
            .findFirst();

        // Hvis saksbehandletRefusjonPrÅr var > 0 i denne andelen som ligger i en periode før forrige startdato for refusjon
        // betyr det at det var tidligere innvilget delvis refusjon
        var forrigeSaksbehandletRefusjonPrÅr = forrigeMatchendeAndel.flatMap(BeregningsgrunnlagPrStatusOgAndel::getBgAndelArbeidsforhold)
            .map(BGAndelArbeidsforhold::getSaksbehandletRefusjonPrÅr);
        if (forrigeSaksbehandletRefusjonPrÅr.isPresent() && forrigeSaksbehandletRefusjonPrÅr.get().compareTo(BigDecimal.ZERO) > 0) {
            return forrigeSaksbehandletRefusjonPrÅr;
        }
        return Optional.empty();
    }

    private Optional<LocalDate> finnForrigeRefusjonsstartForArbeidsforhold(VurderRefusjonAndelBeregningsgrunnlagDto fastsattAndel,
                                                                           List<BeregningRefusjonOverstyringEntitet> forrigeOverstyringer) {
        var refusjonsperioderHosSammeAG = forrigeOverstyringer.stream()
            .filter(os -> matcherAG(os.getArbeidsgiver(), fastsattAndel))
            .findFirst()
            .map(BeregningRefusjonOverstyringEntitet::getRefusjonPerioder)
            .orElse(Collections.emptyList());
        var first = refusjonsperioderHosSammeAG.stream().filter(rp -> matcherReferanse(rp.getArbeidsforholdRef(), fastsattAndel)).findFirst();
        return first.map(BeregningRefusjonPeriodeEntitet::getStartdatoRefusjon);
    }

    private boolean matcherReferanse(InternArbeidsforholdRef arbeidsforholdRef, VurderRefusjonAndelBeregningsgrunnlagDto fastsattAndel) {
        return Objects.equals(arbeidsforholdRef.getReferanse(), fastsattAndel.getInternArbeidsforholdRef());
    }

    private boolean matcherAG(Arbeidsgiver arbeidsgiver, VurderRefusjonAndelBeregningsgrunnlagDto fastsattAndel) {
        if (fastsattAndel.getArbeidsgiverOrgnr() != null) {
            return fastsattAndel.getArbeidsgiverOrgnr().equals(arbeidsgiver.getIdentifikator());
        }
        return Objects.equals(fastsattAndel.getArbeidsgiverAktoerId(), arbeidsgiver.getIdentifikator());
    }

    private List<HistorikkinnslagTekstlinjeBuilder> leggTilArbeidsforholdHistorikkinnslag(VurderRefusjonAndelBeregningsgrunnlagDto fastsattAndel,
                                                                                          Optional<LocalDate> forrigeRefusjonsstart,
                                                                                          Optional<BigDecimal> forrigeDelvisRefusjonPrÅr,
                                                                                          List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer) {
        Arbeidsgiver ag;
        if (fastsattAndel.getArbeidsgiverAktoerId() != null) {
            ag = Arbeidsgiver.person(new AktørId(fastsattAndel.getArbeidsgiverAktoerId()));
        } else {
            ag = Arbeidsgiver.virksomhet(fastsattAndel.getArbeidsgiverOrgnr());
        }
        Optional<InternArbeidsforholdRef> ref = fastsattAndel.getInternArbeidsforholdRef() == null ? Optional.empty() : Optional.of(
            InternArbeidsforholdRef.ref(fastsattAndel.getInternArbeidsforholdRef()));
        var arbeidsforholdInfo = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningsgrunnlag(AktivitetStatus.ARBEIDSTAKER,
            Optional.of(ag), ref, arbeidsforholdOverstyringer);
        var fraStartdato = forrigeRefusjonsstart.orElse(null);
        var tilStartdato = fastsattAndel.getFastsattRefusjonFom();

        List<HistorikkinnslagTekstlinjeBuilder> tekstlinjerBuilder = new ArrayList<>();
        tekstlinjerBuilder.add(fraTilEquals("Startdato for refusjon til " + arbeidsforholdInfo, formatDate(fraStartdato), formatDate(tilStartdato)));

        if (fastsattAndel.getDelvisRefusjonPrMndFørStart() != null && fastsattAndel.getDelvisRefusjonPrMndFørStart() != 0) {
            var fraBeløpPrMnd = forrigeDelvisRefusjonPrÅr.map(forrigeDelvisRef -> forrigeDelvisRef.divide(MÅNEDER_I_ÅR, RoundingMode.HALF_EVEN))
                .map(BigDecimal::intValue)
                .orElse(null);
            var tilBeløpPrMnd = fastsattAndel.getDelvisRefusjonPrMndFørStart();
            tekstlinjerBuilder.add(
                fraTilEquals("Delvis refusjon før " + arbeidsforholdInfo, formatString(fraBeløpPrMnd), formatString(tilBeløpPrMnd)));
        }
        tekstlinjerBuilder.removeIf(Objects::isNull);

        return tekstlinjerBuilder;
    }
}
