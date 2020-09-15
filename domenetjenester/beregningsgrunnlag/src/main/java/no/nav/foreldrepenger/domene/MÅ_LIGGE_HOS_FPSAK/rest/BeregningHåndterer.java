package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulator.KLASSER_MED_AVHENGIGHETER.aksjonspunkt.AvklarAktiviteterHåndterer;
import no.nav.folketrygdloven.kalkulator.KLASSER_MED_AVHENGIGHETER.aksjonspunkt.BeregningFaktaOgOverstyringHåndterer;
import no.nav.folketrygdloven.kalkulator.KLASSER_MED_AVHENGIGHETER.aksjonspunkt.FastsettBGTidsbegrensetArbeidsforholdHåndterer;
import no.nav.folketrygdloven.kalkulator.KLASSER_MED_AVHENGIGHETER.aksjonspunkt.FastsettBeregningsgrunnlagATFLHåndterer;
import no.nav.folketrygdloven.kalkulator.KLASSER_MED_AVHENGIGHETER.aksjonspunkt.FastsettBruttoBeregningsgrunnlagSNHåndterer;
import no.nav.folketrygdloven.kalkulator.KLASSER_MED_AVHENGIGHETER.aksjonspunkt.FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetHåndterer;
import no.nav.folketrygdloven.kalkulator.KLASSER_MED_AVHENGIGHETER.aksjonspunkt.FordelBeregningsgrunnlagHåndterer;
import no.nav.folketrygdloven.kalkulator.KLASSER_MED_AVHENGIGHETER.aksjonspunkt.VurderRefusjonBeregningsgrunnlagHåndterer;
import no.nav.folketrygdloven.kalkulator.KLASSER_MED_AVHENGIGHETER.aksjonspunkt.VurderVarigEndretNyoppstartetSNHåndterer;
import no.nav.folketrygdloven.kalkulator.KLASSER_MED_AVHENGIGHETER.aksjonspunkt.dto.AvklarteAktiviteterDto;
import no.nav.folketrygdloven.kalkulator.KLASSER_MED_AVHENGIGHETER.aksjonspunkt.dto.BeregningsaktivitetLagreDto;
import no.nav.folketrygdloven.kalkulator.KLASSER_MED_AVHENGIGHETER.aksjonspunkt.dto.FaktaBeregningLagreDto;
import no.nav.folketrygdloven.kalkulator.KLASSER_MED_AVHENGIGHETER.aksjonspunkt.dto.FastsettBGTidsbegrensetArbeidsforholdDto;
import no.nav.folketrygdloven.kalkulator.KLASSER_MED_AVHENGIGHETER.aksjonspunkt.dto.FastsettBeregningsgrunnlagATFLDto;
import no.nav.folketrygdloven.kalkulator.KLASSER_MED_AVHENGIGHETER.aksjonspunkt.dto.FastsettBruttoBeregningsgrunnlagSNDto;
import no.nav.folketrygdloven.kalkulator.KLASSER_MED_AVHENGIGHETER.aksjonspunkt.dto.FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto;
import no.nav.folketrygdloven.kalkulator.KLASSER_MED_AVHENGIGHETER.aksjonspunkt.dto.OverstyrBeregningsgrunnlagDto;
import no.nav.folketrygdloven.kalkulator.KLASSER_MED_AVHENGIGHETER.aksjonspunkt.dto.VurderRefusjonBeregningsgrunnlagDto;
import no.nav.folketrygdloven.kalkulator.KLASSER_MED_AVHENGIGHETER.aksjonspunkt.dto.VurderVarigEndringEllerNyoppstartetSNDto;
import no.nav.folketrygdloven.kalkulator.KLASSER_MED_AVHENGIGHETER.aksjonspunkt.fordeling.FordelBeregningsgrunnlagDto;
import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningsgrunnlagGrunnlagDto;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.BeregningTilInputTjeneste;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.HentOgLagreBeregningsgrunnlagTjeneste;

/**
 * Fasadetjeneste for håndtering av aksjonspunkt
 */
@ApplicationScoped
public class BeregningHåndterer {

    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private BeregningTilInputTjeneste beregningTilInputTjeneste;
    private BeregningFaktaOgOverstyringHåndterer beregningFaktaOgOverstyringHåndterer;

    public BeregningHåndterer() {
        // CDI
    }

    @Inject
    public BeregningHåndterer(HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste, BeregningTilInputTjeneste beregningTilInputTjeneste, BeregningFaktaOgOverstyringHåndterer beregningFaktaOgOverstyringHåndterer) {
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.beregningTilInputTjeneste = beregningTilInputTjeneste;
        this.beregningFaktaOgOverstyringHåndterer = beregningFaktaOgOverstyringHåndterer;
    }

    public void håndterAvklarAktiviteter(BeregningsgrunnlagInput input, AvklarteAktiviteterDto dto) {
        BeregningsgrunnlagInput inputMedBeregningsgrunnlag = beregningTilInputTjeneste.lagInputMedVerdierFraBeregning(input);
        BeregningsgrunnlagGrunnlagDto resultatFraKalkulus = AvklarAktiviteterHåndterer.håndter(dto, inputMedBeregningsgrunnlag);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultatFraKalkulus);
    }

    public void håndterFastsettBGTidsbegrensetArbeidsforhold(BeregningsgrunnlagInput input, FastsettBGTidsbegrensetArbeidsforholdDto dto) {
        BeregningsgrunnlagInput inputMedBeregningsgrunnlag = beregningTilInputTjeneste.lagInputMedVerdierFraBeregning(input);
        BeregningsgrunnlagGrunnlagDto resultatFraKalkulus = FastsettBGTidsbegrensetArbeidsforholdHåndterer.håndter(inputMedBeregningsgrunnlag, dto);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultatFraKalkulus);
    }

    public void håndterFastsettBeregningsgrunnlagATFL(BeregningsgrunnlagInput inputUtenBeregningsgrunnlag, FastsettBeregningsgrunnlagATFLDto dto) {
        BeregningsgrunnlagInput inputMedBeregningsgrunnlag = beregningTilInputTjeneste.lagInputMedVerdierFraBeregning(inputUtenBeregningsgrunnlag);
        BeregningsgrunnlagGrunnlagDto resultatFraKalkulus = FastsettBeregningsgrunnlagATFLHåndterer.håndter(inputMedBeregningsgrunnlag, dto);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(inputUtenBeregningsgrunnlag), resultatFraKalkulus);
    }

    public void håndterBeregningAktivitetOverstyring(BeregningsgrunnlagInput input, List<BeregningsaktivitetLagreDto> overstyrAktiviteter) {
        BeregningsgrunnlagInput inputMedBeregningsgrunnlag = beregningTilInputTjeneste.lagInputMedVerdierFraBeregning(input);
        BeregningsgrunnlagGrunnlagDto grunnlag = AvklarAktiviteterHåndterer.håndterOverstyring(overstyrAktiviteter, inputMedBeregningsgrunnlag);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), grunnlag);
    }

    public void håndterBeregningsgrunnlagOverstyring(BeregningsgrunnlagInput input, OverstyrBeregningsgrunnlagDto overstyrBeregningsgrunnlagDto) {
        BeregningsgrunnlagInput inputMedVerdierFraBeregning = beregningTilInputTjeneste.lagInputMedVerdierFraBeregning(input);
        BeregningsgrunnlagGrunnlagDto resultat = beregningFaktaOgOverstyringHåndterer.håndterMedOverstyring(inputMedVerdierFraBeregning, overstyrBeregningsgrunnlagDto);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultat);
    }

    public void håndterFastsettBruttoForSNNyIArbeidslivet(BeregningsgrunnlagInput input, FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto dto) {
        BeregningsgrunnlagInput inputMedVerdierFraBeregning = beregningTilInputTjeneste.lagInputMedVerdierFraBeregning(input);
        BeregningsgrunnlagGrunnlagDto resultat = FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetHåndterer.oppdater(inputMedVerdierFraBeregning, dto);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultat);
    }

    public void håndterFastsettBeregningsgrunnlagSN(BeregningsgrunnlagInput input, FastsettBruttoBeregningsgrunnlagSNDto dto) {
        BeregningsgrunnlagInput inputMedVerdierFraBeregning = beregningTilInputTjeneste.lagInputMedVerdierFraBeregning(input);
        BeregningsgrunnlagGrunnlagDto resultat = FastsettBruttoBeregningsgrunnlagSNHåndterer.håndter(inputMedVerdierFraBeregning, dto);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultat);
    }

    public void håndterFordelBeregningsgrunnlag(BeregningsgrunnlagInput input, FordelBeregningsgrunnlagDto dto) {
        BeregningsgrunnlagInput inputMedVerdierFraBeregning = beregningTilInputTjeneste.lagInputMedVerdierFraBeregning(input);
        BeregningsgrunnlagGrunnlagDto resultat = FordelBeregningsgrunnlagHåndterer.håndter(dto, inputMedVerdierFraBeregning);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultat);
    }

    public void håndterVurderRefusjonBeregningsgrunnlag(BeregningsgrunnlagInput input, VurderRefusjonBeregningsgrunnlagDto dto) {
        BeregningsgrunnlagInput inputMedVerdierFraBeregning = beregningTilInputTjeneste.lagInputMedVerdierFraBeregning(input);
        BeregningsgrunnlagGrunnlagDto resultat = VurderRefusjonBeregningsgrunnlagHåndterer.håndter(dto, inputMedVerdierFraBeregning);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultat);
    }


    public void håndterVurderFaktaOmBeregning(BeregningsgrunnlagInput input, FaktaBeregningLagreDto dto) {
        BeregningsgrunnlagInput inputMedBeregningsgrunnlag = beregningTilInputTjeneste.lagInputMedVerdierFraBeregning(input);
        BeregningsgrunnlagGrunnlagDto resultat = beregningFaktaOgOverstyringHåndterer.håndter(inputMedBeregningsgrunnlag, dto);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultat);
    }

    public void håndterVurderVarigEndretNyoppstartetSN(BeregningsgrunnlagInput input, VurderVarigEndringEllerNyoppstartetSNDto dto) {
        BeregningsgrunnlagInput inputMedVerdierFraBeregning = beregningTilInputTjeneste.lagInputMedVerdierFraBeregning(input);
        BeregningsgrunnlagGrunnlagDto resultat = VurderVarigEndretNyoppstartetSNHåndterer.håndter(inputMedVerdierFraBeregning, dto);
        //TODO(OJR) hvorfor kan denne gi null????
        if (resultat != null) {
            beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultat);
        }
    }

    private Long getBehandlingId(BeregningsgrunnlagInput input) {
        return input.getKoblingReferanse().getKoblingId();
    }

}
