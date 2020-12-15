package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import no.nav.folketrygdloven.kalkulator.output.BeregningAksjonspunktResultat;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningAktivitetAggregatEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagEntitet;

@ApplicationScoped
class KopierBeregningsgrunnlag {

    private KopierBeregningsgrunnlag() {
        // For CDI
    }

    /**
     * Sjekker om det er mulig å kopiere beregningsgrunnlagGrunnlaget som ble bekreftet ved forrige saksbehandling om det har oppstått aksjonspunkter.
     * @param aksjonspunkter Utledede aksjonspunkter for nytt beregningsgrunnlag
     * @param nyttGrunnlag Nytt beregningsgrunnlagGrunnlag
     * @param forrigeGrunnlag Forrige grunnlag som lagres i beregningsteget
     * @param forrigeBekreftetGrunnlag Forrige grunnlag som ble lagret etter saksbehandlers vurdering i steget
     */
    static boolean kanKopiereFraForrigeBekreftetGrunnlag(List<BeregningAksjonspunktResultat> aksjonspunkter,
                                                         BeregningsgrunnlagGrunnlagEntitet nyttGrunnlag,
                                                         Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag,
                                                         Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeBekreftetGrunnlag) {
        boolean kanKopiereFraBekreftet = kanKopiereAktiviteter(aksjonspunkter, nyttGrunnlag, forrigeGrunnlag, forrigeBekreftetGrunnlag);
        if (kanKopiereFraBekreftet) {
            return forrigeBekreftetGrunnlag.isPresent();
        } else {
            return false;
        }
    }

    /**
     * Sjekker om det er mulig å kopiere beregningsgrunnlaget som ble bekreftet ved forrige saksbehandling om det har oppstått aksjonspunkter.
     * @param aksjonspunkter Utledede aksjonspunkter for nytt beregningsgrunnlag
     * @param nyttBg Nytt beregningsgrunnlag
     * @param forrigeBeregningsgrunnlag Forrige beregningsgrunnlag som lagres i beregningsteget
     * @param forrigeBekreftetBeregningsgrunnlag Forrige beregningsgrunnlag som ble lagret etter saksbehandlers vurdering i steget
     */
    static boolean kanKopiereFraForrigeBekreftetGrunnlag(List<BeregningAksjonspunktResultat> aksjonspunkter,
                                                         BeregningsgrunnlagEntitet nyttBg,
                                                         Optional<BeregningsgrunnlagEntitet> forrigeBeregningsgrunnlag,
                                                         Optional<BeregningsgrunnlagEntitet> forrigeBekreftetBeregningsgrunnlag) {
        boolean kanKopiereFraBekreftet = kanKopiereBeregningsgrunnlag(
            aksjonspunkter,
            nyttBg,
            forrigeBeregningsgrunnlag);
        if (kanKopiereFraBekreftet) {
            return forrigeBekreftetBeregningsgrunnlag.isPresent() && forrigeBeregningsgrunnlag.isPresent()
                && forrigeBekreftetBeregningsgrunnlag.get().getOpprettetTidspunkt().isAfter(forrigeBeregningsgrunnlag.get().getOpprettetTidspunkt());
        } else {
            return false;
        }
    }

    private static boolean kanKopiereAktiviteter(List<BeregningAksjonspunktResultat> aksjonspunkter,
                                                 BeregningsgrunnlagGrunnlagEntitet nyttGrunnlag,
                                                 Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag,
                                                 Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeBekreftetGrunnlag) {
        BeregningAktivitetAggregatEntitet nyttRegister = nyttGrunnlag.getRegisterAktiviteter();
        Optional<BeregningAktivitetAggregatEntitet> forrigeRegister = forrigeGrunnlag.map(BeregningsgrunnlagGrunnlagEntitet::getRegisterAktiviteter);
        return forrigeRegister.map(aktivitetAggregatEntitet -> !BeregningsgrunnlagDiffSjekker.harSignifikantDiffIAktiviteter(nyttRegister, aktivitetAggregatEntitet)).orElse(false)
            && (!aksjonspunkter.isEmpty() || forrigeBekreftetGrunnlag.flatMap(BeregningsgrunnlagGrunnlagEntitet::getOverstyring).isPresent());
    }


    private static boolean kanKopiereBeregningsgrunnlag(List<BeregningAksjonspunktResultat> aksjonspunkter, BeregningsgrunnlagEntitet nyttBg, Optional<BeregningsgrunnlagEntitet> forrigeBeregningsgrunnlag) {
        return forrigeBeregningsgrunnlag.map(bg -> !BeregningsgrunnlagDiffSjekker.harSignifikantDiffIBeregningsgrunnlag(nyttBg, bg)).orElse(false) && !aksjonspunkter.isEmpty();
    }

}
