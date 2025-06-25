package no.nav.foreldrepenger.domene.prosess;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.domene.aksjonspunkt.OppdaterBeregningsgrunnlagResultat;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.output.BeregningsgrunnlagVilkårOgAkjonspunktResultat;

@ApplicationScoped
public class BeregningTjenesteImpl implements BeregningTjeneste {

    private BeregningKalkulus kalkulusBeregner;

    BeregningTjenesteImpl() {
        // CDI
    }

    @Inject
    public BeregningTjenesteImpl(BeregningKalkulus kalkulusBeregner) {
        this.kalkulusBeregner = kalkulusBeregner;
    }

    @Override
    public Optional<BeregningsgrunnlagGrunnlag> hent(BehandlingReferanse referanse) {
        return kalkulusBeregner.hent(referanse);
    }

    @Override
    public Optional<BeregningsgrunnlagDto> hentGuiDto(BehandlingReferanse referanse) {
        return kalkulusBeregner.hentGUIDto(referanse);
    }

    @Override
    public BeregningsgrunnlagVilkårOgAkjonspunktResultat beregn(BehandlingReferanse referanse, BehandlingStegType stegType) {
        return kalkulusBeregner.beregn(referanse, stegType);
    }

    @Override
    public void lagre(BeregningsgrunnlagGrunnlag beregningsgrunnlagGrunnlag, BehandlingReferanse referanse) {
        throw new IllegalStateException("Skal kun kalles i test, bruk heller #beregn");
    }

    @Override
    public void kopier(BehandlingReferanse revurdering, BehandlingReferanse originalbehandling, BeregningsgrunnlagTilstand tilstand) {
        kalkulusBeregner.kopier(revurdering, originalbehandling, tilstand);

    }

    @Override
    public Optional<OppdaterBeregningsgrunnlagResultat> oppdaterBeregning(BekreftetAksjonspunktDto oppdatering, BehandlingReferanse referanse) {
        return kalkulusBeregner.oppdaterBeregning(oppdatering, referanse);
    }

    @Override
    public Optional<OppdaterBeregningsgrunnlagResultat> overstyrBeregning(OverstyringAksjonspunktDto overstyring, BehandlingReferanse referanse) {
        return kalkulusBeregner.overstyrBeregning(overstyring, referanse);
    }

    @Override
    public void avslutt(BehandlingReferanse referanse) {
        kalkulusBeregner.avslutt(referanse);
    }

    @Override
    public boolean kanStartesISteg(BehandlingReferanse referanse, BehandlingStegType stegType) {
        return kalkulusBeregner.kanStartesISteg(referanse, stegType);
    }
}
