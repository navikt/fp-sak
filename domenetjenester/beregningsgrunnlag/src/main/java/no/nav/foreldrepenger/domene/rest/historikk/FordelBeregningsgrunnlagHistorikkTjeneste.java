package no.nav.foreldrepenger.domene.rest.historikk;

import java.math.BigDecimal;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.rest.dto.fordeling.FordelBeregningsgrunnlagAndelDto;
import no.nav.foreldrepenger.domene.rest.dto.fordeling.FordelBeregningsgrunnlagDto;
import no.nav.foreldrepenger.domene.rest.dto.fordeling.FordelBeregningsgrunnlagPeriodeDto;
import no.nav.foreldrepenger.domene.rest.dto.fordeling.FordelFastsatteVerdierDto;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.Inntektskategori;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
public class FordelBeregningsgrunnlagHistorikkTjeneste {

    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste;
    private HistorikkTjenesteAdapter historikkTjenesteAdapter;
    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;


    FordelBeregningsgrunnlagHistorikkTjeneste() {
        // for CDI proxy
    }

    @Inject
    public FordelBeregningsgrunnlagHistorikkTjeneste(HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                                     ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste,
                                                     HistorikkTjenesteAdapter historikkTjenesteAdapter,
                                                     InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.arbeidsgiverHistorikkinnslagTjeneste = arbeidsgiverHistorikkinnslagTjeneste;
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public OppdateringResultat lagHistorikk(FordelBeregningsgrunnlagDto dto, AksjonspunktOppdaterParameter param) {
        Long behandlingId = param.getBehandlingId();
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagEntitetAggregatForBehandling(behandlingId);
        List<BeregningsgrunnlagPeriode> perioder = beregningsgrunnlag.getBeregningsgrunnlagPerioder();
        List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId).getArbeidsforholdOverstyringer();
        HistorikkInnslagTekstBuilder tekstBuilder = historikkTjenesteAdapter.tekstBuilder();
        for (FordelBeregningsgrunnlagPeriodeDto endretPeriode : dto.getEndretBeregningsgrunnlagPerioder()) {
            lagHistorikk(tekstBuilder, perioder, endretPeriode, arbeidsforholdOverstyringer);
        }

        lagHistorikkInnslag(dto, param, tekstBuilder);

        return OppdateringResultat.utenOveropp();
    }

    private void lagHistorikk(HistorikkInnslagTekstBuilder tekstBuilder, List<BeregningsgrunnlagPeriode> perioder,
                              FordelBeregningsgrunnlagPeriodeDto endretPeriode, List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer) {
        BeregningsgrunnlagPeriode korrektPeriode = getKorrektPeriode(perioder, endretPeriode);
        for (FordelBeregningsgrunnlagAndelDto endretAndel : endretPeriode.getAndeler()) {
            Lønnsendring endring = lagEndringsoppsummeringForHistorikk(endretAndel).build();
            leggTilArbeidsforholdHistorikkinnslag(tekstBuilder, endring, korrektPeriode, tekstBuilder, arbeidsforholdOverstyringer);
        }
    }

    private Lønnsendring.Builder lagEndringsoppsummeringForHistorikk(FordelBeregningsgrunnlagAndelDto endretAndel) {
        FordelFastsatteVerdierDto fastsatteVerdier = endretAndel.getFastsatteVerdier();
        Lønnsendring.Builder endring = new Lønnsendring.Builder()
            .medAktivitetStatus(endretAndel.getAktivitetStatus())
            .medNyInntektskategori(fastsatteVerdier.getInntektskategori())
            .medNyArbeidsinntektPrÅr(fastsatteVerdier.getFastsattÅrsbeløpInklNaturalytelse())
            .medNyAndel(endretAndel.getNyAndel());
        if (gjelderArbeidsforhold(endretAndel)) {
            settArbeidsforholdVerdier(endretAndel, endring);
        }
        if (!endretAndel.getNyAndel()) {
            settEndretFraVerdier(endretAndel, endring);
            endring.medNyTotalRefusjonPrÅr(fastsatteVerdier.getRefusjonPrÅr());
        }
        return endring;
    }

    private boolean gjelderArbeidsforhold(FordelBeregningsgrunnlagAndelDto endretAndel) {
        return endretAndel.getArbeidsgiverId() != null;
    }


    private void settArbeidsforholdVerdier(FordelBeregningsgrunnlagAndelDto endretAndel, Lønnsendring.Builder endring) {
        endring
            .medArbeidsforholdRef(endretAndel.getArbeidsforholdId())
            .medArbeidsgiver(finnArbeidsgiver(endretAndel));
    }

    private Arbeidsgiver finnArbeidsgiver(FordelBeregningsgrunnlagAndelDto endretAndel) {
        Arbeidsgiver arbeidsgiver;
        if (OrgNummer.erGyldigOrgnr(endretAndel.getArbeidsgiverId())) {
            arbeidsgiver = Arbeidsgiver.virksomhet(endretAndel.getArbeidsgiverId());
        } else {
            arbeidsgiver = Arbeidsgiver.person(new AktørId(endretAndel.getArbeidsgiverId()));
        }
        return arbeidsgiver;
    }


    private void settEndretFraVerdier(FordelBeregningsgrunnlagAndelDto endretAndel, Lønnsendring.Builder endring) {
        endring
            .medGammelArbeidsinntektPrÅr(endretAndel.getForrigeArbeidsinntektPrÅr())
            .medGammelInntektskategori(endretAndel.getForrigeInntektskategori())
            .medGammelRefusjonPrÅr(endretAndel.getForrigeRefusjonPrÅr());
    }


    private void leggTilArbeidsforholdHistorikkinnslag(HistorikkInnslagTekstBuilder historikkBuilder,
                                                       Lønnsendring endring,
                                                       BeregningsgrunnlagPeriode korrektPeriode,
                                                       HistorikkInnslagTekstBuilder tekstBuilder,
                                                       List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer) {

        if (!harEndringSomGirHistorikk(endring)) {
            return;
        }
        String arbeidsforholdInfo = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningsgrunnlag(endring.getAktivitetStatus(), endring.getArbeidsgiver(), endring.getArbeidsforholdRef(), arbeidsforholdOverstyringer);
        HistorikkEndretFeltType endretFeltType = finnEndretFeltType(endring);
        historikkBuilder.medNavnOgGjeldendeFra(endretFeltType, arbeidsforholdInfo, korrektPeriode.getBeregningsgrunnlagPeriodeFom());
        lagHistorikkForRefusjon(historikkBuilder, endring);
        lagHistorikkForInntekt(historikkBuilder, endring);
        lagHistorikkForInntektskategori(historikkBuilder, endring);
        if (!tekstBuilder.erSkjermlenkeSatt()) {
            historikkBuilder.medSkjermlenke(SkjermlenkeType.FAKTA_OM_FORDELING);
        }
        historikkBuilder.ferdigstillHistorikkinnslagDel();
    }


    private HistorikkEndretFeltType finnEndretFeltType(Lønnsendring endring) {
        return endring.isNyAndel() ? HistorikkEndretFeltType.NY_AKTIVITET : HistorikkEndretFeltType.NY_FORDELING;
    }

    private void lagHistorikkForInntekt(HistorikkInnslagTekstBuilder historikkBuilder, Lønnsendring endring) {
        historikkBuilder.medEndretFelt(HistorikkEndretFeltType.INNTEKT, endring.getGammelArbeidsinntektPrÅr(), endring.getNyArbeidsinntektPrÅr());
    }

    private void lagHistorikkForInntektskategori(HistorikkInnslagTekstBuilder historikkBuilder, Lønnsendring endring) {
        Inntektskategori nyInntektskategori = endring.getNyInntektskategori();
        if (nyInntektskategori != null) {
            historikkBuilder.medEndretFelt(HistorikkEndretFeltType.INNTEKTSKATEGORI, null, nyInntektskategori);
        }
    }

    private void lagHistorikkForRefusjon(HistorikkInnslagTekstBuilder historikkBuilder, Lønnsendring endring) {
        if (endring.getNyTotalRefusjonPrÅr() != null && endring.getArbeidsgiver().isPresent() && endring.getArbeidsforholdRef().isPresent()) {
            Integer forrigeRefusjon = endring.getGammelRefusjonPrÅr();
            if (!endring.getNyTotalRefusjonPrÅr().equals(forrigeRefusjon)) {
                historikkBuilder.medEndretFelt(HistorikkEndretFeltType.NYTT_REFUSJONSKRAV,
                    BigDecimal.valueOf(forrigeRefusjon),
                    endring.getNyTotalRefusjonPrÅr());
            }
        }
    }

    private boolean harEndringSomGirHistorikk(Lønnsendring endring) {
        boolean harEndringIRefusjon = endring.getNyTotalRefusjonPrÅr() != null && !endring.getNyTotalRefusjonPrÅr().equals(endring.getGammelRefusjonPrÅr());
        boolean harEndringIInntektskategori = endring.getNyInntektskategori() != null && !endring.getNyInntektskategori().equals(endring.getGammelInntektskategori());
        boolean harEndringIInntekt = endring.getGammelArbeidsinntekt() == null || !endring.getGammelArbeidsinntekt().equals(endring.getNyArbeidsinntektPrÅr());
        return harEndringIInntekt || harEndringIRefusjon || harEndringIInntektskategori || endring.isNyAndel();
    }

    private BeregningsgrunnlagPeriode getKorrektPeriode(List<BeregningsgrunnlagPeriode> perioder,
                                                        FordelBeregningsgrunnlagPeriodeDto endretPeriode) {
        return perioder.stream()
            .filter(periode -> periode.getBeregningsgrunnlagPeriodeFom().equals(endretPeriode.getFom()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Finner ikke periode"));
    }

    private void lagHistorikkInnslag(FordelBeregningsgrunnlagDto dto, AksjonspunktOppdaterParameter param, HistorikkInnslagTekstBuilder tekstBuilder) {
        tekstBuilder.ferdigstillHistorikkinnslagDel();
        List<HistorikkinnslagDel> historikkDeler = tekstBuilder.getHistorikkinnslagDeler();
        settBegrunnelse(param, historikkDeler, tekstBuilder, dto.getBegrunnelse());
        settSkjermlenkeOmIkkeSatt(historikkDeler, tekstBuilder);
    }

    private void settBegrunnelse(AksjonspunktOppdaterParameter param, List<HistorikkinnslagDel> historikkDeler, HistorikkInnslagTekstBuilder tekstBuilder, String begrunnelse) {
        boolean erBegrunnelseSatt = historikkDeler.stream()
            .anyMatch(historikkDel -> historikkDel.getBegrunnelse().isPresent());
        if (!erBegrunnelseSatt) {
            boolean erBegrunnelseEndret = param.erBegrunnelseEndret();
            if (erBegrunnelseEndret) {
                boolean erSkjermlenkeSatt = historikkDeler.stream().anyMatch(historikkDel -> historikkDel.getSkjermlenke().isPresent());
                tekstBuilder.medBegrunnelse(begrunnelse, true);
                if (!erSkjermlenkeSatt) {
                    tekstBuilder.medSkjermlenke(SkjermlenkeType.FAKTA_OM_FORDELING);
                }
                tekstBuilder.ferdigstillHistorikkinnslagDel();
            }
        }
    }

    private void settSkjermlenkeOmIkkeSatt(List<HistorikkinnslagDel> historikkDeler, HistorikkInnslagTekstBuilder tekstBuilder) {
        boolean erSkjermlenkeSatt = historikkDeler.stream()
            .anyMatch(historikkDel -> historikkDel.getSkjermlenke().isPresent());
        if (!erSkjermlenkeSatt && !historikkDeler.isEmpty()) {
            tekstBuilder.medSkjermlenke(SkjermlenkeType.FAKTA_OM_FORDELING);
        }
    }

}
