package no.nav.foreldrepenger.domene.rest.historikk.kalkulus;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.fraTilEquals;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.aksjonspunkt.BeløpEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.DatoEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.OppdaterBeregningsgrunnlagResultat;
import no.nav.foreldrepenger.domene.aksjonspunkt.RefusjonoverstyringEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.RefusjonoverstyringPeriodeEndring;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.rest.dto.VurderRefusjonAndelBeregningsgrunnlagDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderRefusjonBeregningsgrunnlagDto;
import no.nav.foreldrepenger.domene.rest.historikk.ArbeidsgiverHistorikkinnslag;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

@ApplicationScoped
public class VurderRefusjonBeregningsgrunnlagHistorikkKalkulusTjeneste {
    private static final BigDecimal MÅNEDER_I_ÅR = BigDecimal.valueOf(12);
    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private Historikkinnslag2Repository historikkinnslagRepository;


    VurderRefusjonBeregningsgrunnlagHistorikkKalkulusTjeneste() {
        // for CDI proxy
    }

    @Inject
    public VurderRefusjonBeregningsgrunnlagHistorikkKalkulusTjeneste(ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste,
                                                                     InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                                                     Historikkinnslag2Repository historikkinnslagRepository) {
        this.arbeidsgiverHistorikkinnslagTjeneste = arbeidsgiverHistorikkinnslagTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    public void lagHistorikk(VurderRefusjonBeregningsgrunnlagDto dto,
                             AksjonspunktOppdaterParameter param,
                             OppdaterBeregningsgrunnlagResultat endringsaggregat) {
        var behandlingId = param.getBehandlingId();
        var arbeidsforholdOverstyringer = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId).getArbeidsforholdOverstyringer();
        var historikkinnslagBuilder = new Historikkinnslag2.Builder();
        List<HistorikkinnslagLinjeBuilder> linjeBuilder = new ArrayList<>();
        for (var fastsattAndel : dto.getFastsatteAndeler()) {
            var forrigeOverstyringer = endringsaggregat.getRefusjonoverstyringEndring()
                .map(RefusjonoverstyringEndring::getRefusjonperiodeEndringer)
                .orElse(List.of());
            var forrigeFastsattAndel = forrigeOverstyringer.stream()
                .filter(os -> matcherAG(os.getArbeidsgiver(), fastsattAndel))
                .filter(os -> matcherReferanse(os.getArbeidsforholdRef(), fastsattAndel))
                .findFirst();
            var forrigeRefusjonsstart = finnForrigeRefusjonsstartForArbeidsforhold(forrigeFastsattAndel);
            Optional<BigDecimal> forrigeDelvisRefusjonPrÅr = finnForrigeDelvisRefusjon(forrigeFastsattAndel);
            linjeBuilder.addAll(leggTilArbeidsforholdHistorikkinnslag(fastsattAndel, forrigeRefusjonsstart, forrigeDelvisRefusjonPrÅr,
                arbeidsforholdOverstyringer));
            linjeBuilder.add(new HistorikkinnslagLinjeBuilder().tekst(dto.getBegrunnelse()));
        }

        if (!linjeBuilder.isEmpty()) {
            historikkinnslagBuilder.medAktør(HistorikkAktør.SAKSBEHANDLER)
                .medBehandlingId(param.getBehandlingId())
                .medFagsakId(param.getFagsakId())
                .medTittel(SkjermlenkeType.FAKTA_OM_FORDELING)
                .medLinjer(linjeBuilder);
            historikkinnslagRepository.lagre(historikkinnslagBuilder.build());
        }
    }

    private Optional<BigDecimal> finnForrigeDelvisRefusjon(Optional<RefusjonoverstyringPeriodeEndring> forrigeMatchendeAndel) {
        // Hvis saksbehandletRefusjonPrÅr var > 0 i denne andelen som ligger i en periode før forrige startdato for refusjon
        // betyr det at det var tidligere innvilget delvis refusjon
        var forrigeSaksbehandletRefusjonPrÅr = forrigeMatchendeAndel.map(RefusjonoverstyringPeriodeEndring::getFastsattDelvisRefusjonFørDatoEndring)
            .flatMap(BeløpEndring::getFraBeløp);
        if (forrigeSaksbehandletRefusjonPrÅr.isPresent() && forrigeSaksbehandletRefusjonPrÅr.get().compareTo(BigDecimal.ZERO) > 0) {
            return forrigeSaksbehandletRefusjonPrÅr;
        }
        return Optional.empty();
    }

    private Optional<LocalDate> finnForrigeRefusjonsstartForArbeidsforhold(Optional<RefusjonoverstyringPeriodeEndring> refusjonoverstyringEndring) {
        var refusjonsendringHosSammeArbeidsforhold = refusjonoverstyringEndring.map(RefusjonoverstyringPeriodeEndring::getFastsattRefusjonFomEndring);
        return refusjonsendringHosSammeArbeidsforhold.map(DatoEndring::getFraVerdi);
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

    private List<HistorikkinnslagLinjeBuilder> leggTilArbeidsforholdHistorikkinnslag(VurderRefusjonAndelBeregningsgrunnlagDto fastsattAndel,
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
        List<HistorikkinnslagLinjeBuilder> linjeBuilder = new ArrayList<>();
        linjeBuilder.add(fraTilEquals("Startdato for refusjon til " + arbeidsforholdInfo, fraStartdato, tilStartdato));
        if (fastsattAndel.getDelvisRefusjonPrMndFørStart() != null && fastsattAndel.getDelvisRefusjonPrMndFørStart() != 0) {
            var fraBeløpPrMnd = forrigeDelvisRefusjonPrÅr.map(forrigeDelvisRef -> forrigeDelvisRef.divide(MÅNEDER_I_ÅR, RoundingMode.HALF_EVEN))
                .map(BigDecimal::intValue)
                .orElse(null);
            var tilBeløpPrMnd = fastsattAndel.getDelvisRefusjonPrMndFørStart();
            linjeBuilder.add(
                fraTilEquals("Delvis refusjon før " + arbeidsforholdInfo, fraBeløpPrMnd, tilBeløpPrMnd));
        }

        return linjeBuilder;
    }
}
