package no.nav.foreldrepenger.ytelse.beregning;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;

import no.nav.folketrygdloven.beregningsgrunnlag.RegelmodellOversetter;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.ytelse.beregning.adapter.MapBeregningsresultatFraRegelTilVL;
import no.nav.foreldrepenger.ytelse.beregning.adapter.MapBeregningsresultatFraVLTilRegel;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatRegelmodell;
import no.nav.foreldrepenger.ytelse.beregning.regler.RegelFastsettBeregningsresultat;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

@ApplicationScoped
public class FastsettBeregningsresultatTjeneste {

    private JacksonJsonConfig jacksonJsonConfig = new JacksonJsonConfig();
    private MapBeregningsresultatFraVLTilRegel mapBeregningsresultatFraVLTilRegel;
    private MapBeregningsresultatFraRegelTilVL mapBeregningsresultatFraRegelTilVL;

    FastsettBeregningsresultatTjeneste() {
        //NOSONAR
    }

    @Inject
    public FastsettBeregningsresultatTjeneste(MapBeregningsresultatFraVLTilRegel mapBeregningsresultatFraVLTilRegel,
                                              MapBeregningsresultatFraRegelTilVL mapBeregningsresultatFraRegelTilVL) {
        this.mapBeregningsresultatFraVLTilRegel = mapBeregningsresultatFraVLTilRegel;
        this.mapBeregningsresultatFraRegelTilVL = mapBeregningsresultatFraRegelTilVL;
    }

    public BeregningsresultatEntitet fastsettBeregningsresultat(BeregningsgrunnlagEntitet beregningsgrunnlag, UttakInput input) {
        // Map til regelmodell
        BeregningsresultatRegelmodell regelmodell = mapBeregningsresultatFraVLTilRegel.mapFra(beregningsgrunnlag, input);
        // Kalle regel
        RegelFastsettBeregningsresultat regel = new RegelFastsettBeregningsresultat();
        no.nav.foreldrepenger.ytelse.beregning.regelmodell.Beregningsresultat outputContainer = no.nav.foreldrepenger.ytelse.beregning.regelmodell.Beregningsresultat.builder().build();
        Evaluation evaluation = regel.evaluer(regelmodell, outputContainer);
        String sporing = RegelmodellOversetter.getSporing(evaluation);

        // Map tilbake til domenemodell fra regelmodell
        BeregningsresultatEntitet beregningsresultat = BeregningsresultatEntitet.builder()
            .medRegelInput(toJson(regelmodell))
            .medRegelSporing(sporing)
            .build();

        mapBeregningsresultatFraRegelTilVL.mapFra(outputContainer, beregningsresultat);

        return beregningsresultat;
    }

    private String toJson(BeregningsresultatRegelmodell grunnlag) {
        JacksonJsonConfig var10000 = this.jacksonJsonConfig;
        FastsettBeregningsresultatFeil var10002 = FastsettBeregningsresultatFeil.FACTORY;
        return var10000.toJson(grunnlag, var10002::jsonMappingFeilet);
    }

    interface FastsettBeregningsresultatFeil extends DeklarerteFeil {
        FastsettBeregningsresultatFeil FACTORY = FeilFactory.create(FastsettBeregningsresultatFeil.class); // NOSONAR ok med konstant

        @TekniskFeil(feilkode = "FP-563791",
            feilmelding = "JSON mapping feilet",
            logLevel = LogLevel.ERROR)
        Feil jsonMappingFeilet(JsonProcessingException var1);
    }
}
