package no.nav.foreldrepenger.domene.rest;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulator.avklaringsbehov.AvklarAktiviteterHåndterer;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.BeregningFaktaOgOverstyringHåndterer;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.FastsettBGTidsbegrensetArbeidsforholdHåndterer;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.FastsettBeregningsgrunnlagATFLHåndterer;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.FastsettBruttoBeregningsgrunnlagSNHåndterer;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetHåndterer;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.FordelBeregningsgrunnlagHåndterer;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.VurderVarigEndretNyoppstartetSNHåndterer;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.AvklarteAktiviteterDto;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.BeregningsaktivitetLagreDto;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.FaktaBeregningLagreDto;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.FastsettBGTidsbegrensetArbeidsforholdDto;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.FastsettBeregningsgrunnlagATFLDto;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.FastsettBruttoBeregningsgrunnlagSNDto;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.OverstyrBeregningsgrunnlagDto;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.VurderRefusjonBeregningsgrunnlagDto;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.VurderVarigEndringEllerNyoppstartetSNDto;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.fordeling.FordelBeregningsgrunnlagDto;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.refusjon.VurderRefusjonBeregningsgrunnlagHåndterer;
import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.domene.input.KalkulatorHåndteringInputTjeneste;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;

/**
 * Fasadetjeneste for håndtering av aksjonspunkt
 */
@ApplicationScoped
public class BeregningHåndterer {

    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private BeregningFaktaOgOverstyringHåndterer beregningFaktaOgOverstyringHåndterer;
    private KalkulatorHåndteringInputTjeneste kalkulatorHåndteringInputTjeneste;

    public BeregningHåndterer() {
        // CDI
    }

    @Inject
    public BeregningHåndterer(HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                              BeregningFaktaOgOverstyringHåndterer beregningFaktaOgOverstyringHåndterer,
                              KalkulatorHåndteringInputTjeneste kalkulatorHåndteringInputTjeneste) {
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.beregningFaktaOgOverstyringHåndterer = beregningFaktaOgOverstyringHåndterer;
        this.kalkulatorHåndteringInputTjeneste = kalkulatorHåndteringInputTjeneste;
    }

    public void håndterAvklarAktiviteter(BeregningsgrunnlagInput input, AvklarteAktiviteterDto dto) {
        var håndterBeregningsgrunnlagInput = kalkulatorHåndteringInputTjeneste.lagInput(
            input.getKoblingReferanse().getKoblingId(), input, AksjonspunktKodeDefinisjon.AVKLAR_AKTIVITETER_KODE);
        var resultatFraKalkulus = AvklarAktiviteterHåndterer.håndter(dto, håndterBeregningsgrunnlagInput);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultatFraKalkulus);
    }

    public void håndterFastsettBGTidsbegrensetArbeidsforhold(BeregningsgrunnlagInput input,
                                                             FastsettBGTidsbegrensetArbeidsforholdDto dto) {
        var håndterBeregningsgrunnlagInput = kalkulatorHåndteringInputTjeneste.lagInput(
            input.getKoblingReferanse().getKoblingId(), input,
            AksjonspunktKodeDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_TIDSBEGRENSET_ARBEIDSFORHOLD_KODE);
        var resultatFraKalkulus = FastsettBGTidsbegrensetArbeidsforholdHåndterer.håndter(håndterBeregningsgrunnlagInput,
            dto);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultatFraKalkulus);
    }

    public void håndterFastsettBeregningsgrunnlagATFL(BeregningsgrunnlagInput input,
                                                      FastsettBeregningsgrunnlagATFLDto dto) {
        var håndterBeregningsgrunnlagInput = kalkulatorHåndteringInputTjeneste.lagInput(
            input.getKoblingReferanse().getKoblingId(), input,
            AksjonspunktKodeDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS_KODE);
        var resultatFraKalkulus = FastsettBeregningsgrunnlagATFLHåndterer.håndter(håndterBeregningsgrunnlagInput, dto);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultatFraKalkulus);
    }

    public void håndterBeregningAktivitetOverstyring(BeregningsgrunnlagInput input,
                                                     List<BeregningsaktivitetLagreDto> overstyrAktiviteter) {
        var håndterBeregningsgrunnlagInput = kalkulatorHåndteringInputTjeneste.lagInput(
            input.getKoblingReferanse().getKoblingId(), input,
            AksjonspunktKodeDefinisjon.OVERSTYRING_AV_BEREGNINGSAKTIVITETER_KODE);
        var grunnlag = AvklarAktiviteterHåndterer.håndterOverstyring(overstyrAktiviteter,
            håndterBeregningsgrunnlagInput);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), grunnlag);
    }

    public void håndterBeregningsgrunnlagOverstyring(BeregningsgrunnlagInput input,
                                                     OverstyrBeregningsgrunnlagDto overstyrBeregningsgrunnlagDto) {
        var håndterBeregningsgrunnlagInput = kalkulatorHåndteringInputTjeneste.lagInput(
            input.getKoblingReferanse().getKoblingId(), input,
            AksjonspunktKodeDefinisjon.OVERSTYRING_AV_BEREGNINGSGRUNNLAG_KODE);
        var resultat = beregningFaktaOgOverstyringHåndterer.håndterMedOverstyring(håndterBeregningsgrunnlagInput,
            overstyrBeregningsgrunnlagDto);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultat);
    }

    public void håndterFastsettBruttoForSNNyIArbeidslivet(BeregningsgrunnlagInput input,
                                                          FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto dto) {
        var håndterBeregningsgrunnlagInput = kalkulatorHåndteringInputTjeneste.lagInput(
            input.getKoblingReferanse().getKoblingId(), input,
            AksjonspunktKodeDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_FOR_SN_NY_I_ARBEIDSLIVET_KODE);
        var resultat = FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetHåndterer.oppdater(
            håndterBeregningsgrunnlagInput, dto);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultat);
    }

    public void håndterFastsettBeregningsgrunnlagSN(BeregningsgrunnlagInput input,
                                                    FastsettBruttoBeregningsgrunnlagSNDto dto) {
        var håndterBeregningsgrunnlagInput = kalkulatorHåndteringInputTjeneste.lagInput(
            input.getKoblingReferanse().getKoblingId(), input,
            AksjonspunktKodeDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_SELVSTENDIG_NÆRINGSDRIVENDE_KODE);
        var resultat = FastsettBruttoBeregningsgrunnlagSNHåndterer.håndter(håndterBeregningsgrunnlagInput, dto);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultat);
    }

    public void håndterFordelBeregningsgrunnlag(BeregningsgrunnlagInput input, FordelBeregningsgrunnlagDto dto) {
        var håndterBeregningsgrunnlagInput = kalkulatorHåndteringInputTjeneste.lagInput(
            input.getKoblingReferanse().getKoblingId(), input,
            AksjonspunktKodeDefinisjon.FORDEL_BEREGNINGSGRUNNLAG_KODE);
        var resultat = FordelBeregningsgrunnlagHåndterer.håndter(dto, håndterBeregningsgrunnlagInput);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultat);
    }

    public void håndterVurderRefusjonBeregningsgrunnlag(BeregningsgrunnlagInput input,
                                                        VurderRefusjonBeregningsgrunnlagDto dto) {
        var håndterBeregningsgrunnlagInput = kalkulatorHåndteringInputTjeneste.lagInput(
            input.getKoblingReferanse().getKoblingId(), input, AksjonspunktKodeDefinisjon.VURDER_REFUSJON_BERGRUNN);
        var resultat = VurderRefusjonBeregningsgrunnlagHåndterer.håndter(dto, håndterBeregningsgrunnlagInput);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultat);
    }


    public void håndterVurderFaktaOmBeregning(BeregningsgrunnlagInput input, FaktaBeregningLagreDto dto) {
        var håndterBeregningsgrunnlagInput = kalkulatorHåndteringInputTjeneste.lagInput(
            input.getKoblingReferanse().getKoblingId(), input,
            AksjonspunktKodeDefinisjon.VURDER_FAKTA_FOR_ATFL_SN_KODE);
        var resultat = beregningFaktaOgOverstyringHåndterer.håndter(håndterBeregningsgrunnlagInput, dto);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultat);
    }

    public void håndterVurderVarigEndretNyoppstartetSN(BeregningsgrunnlagInput input,
                                                       VurderVarigEndringEllerNyoppstartetSNDto dto) {
        var håndterBeregningsgrunnlagInput = kalkulatorHåndteringInputTjeneste.lagInput(
            input.getKoblingReferanse().getKoblingId(), input,
            AksjonspunktKodeDefinisjon.VURDER_VARIG_ENDRET_ELLER_NYOPPSTARTET_NÆRING_SELVSTENDIG_NÆRINGSDRIVENDE_KODE);
        var resultat = VurderVarigEndretNyoppstartetSNHåndterer.håndter(håndterBeregningsgrunnlagInput, dto);
        //TODO(OJR) hvorfor kan denne gi null????
        if (resultat != null) {
            beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultat);
        }
    }

    private Long getBehandlingId(BeregningsgrunnlagInput input) {
        return input.getKoblingReferanse().getKoblingId();
    }

}
