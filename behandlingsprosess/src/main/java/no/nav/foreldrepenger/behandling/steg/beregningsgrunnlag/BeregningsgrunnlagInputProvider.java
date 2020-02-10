package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

@ApplicationScoped
public class BeregningsgrunnlagInputProvider {

    private Instance<BeregningsgrunnlagInputFelles> beregningsgrunnlagInputTjeneste;
    private Instance<BeregningsgrunnlagRestInputFelles> beregningsgrunnlagRestInputTjeneste;


    public BeregningsgrunnlagInputProvider() {
        //CDI proxy
    }

    @Inject
    public BeregningsgrunnlagInputProvider(@Any Instance<BeregningsgrunnlagInputFelles> beregningsgrunnlagInputTjeneste, @Any Instance<BeregningsgrunnlagRestInputFelles> beregningsgrunnlagRestInputTjeneste) {
        this.beregningsgrunnlagInputTjeneste = beregningsgrunnlagInputTjeneste;
        this.beregningsgrunnlagRestInputTjeneste = beregningsgrunnlagRestInputTjeneste;
    }

    public BeregningsgrunnlagInputFelles getTjeneste(FagsakYtelseType fagsakYtelseType) {
        return FagsakYtelseTypeRef.Lookup.find(beregningsgrunnlagInputTjeneste, fagsakYtelseType).orElseThrow();
    }

    public BeregningsgrunnlagRestInputFelles getRestInputTjeneste(FagsakYtelseType fagsakYtelseType) {
        return FagsakYtelseTypeRef.Lookup.find(beregningsgrunnlagRestInputTjeneste, fagsakYtelseType).orElseThrow();
    }

}
