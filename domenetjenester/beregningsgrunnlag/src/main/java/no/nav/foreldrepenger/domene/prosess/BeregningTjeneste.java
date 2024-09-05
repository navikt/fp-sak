package no.nav.foreldrepenger.domene.prosess;

import java.util.Optional;

import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.domene.aksjonspunkt.OppdaterBeregningsgrunnlagResultat;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.output.BeregningsgrunnlagVilkårOgAkjonspunktResultat;

public interface BeregningTjeneste {

    Optional<BeregningsgrunnlagGrunnlag> hent(BehandlingReferanse referanse);

    Optional<BeregningsgrunnlagDto> hentGuiDto(BehandlingReferanse referanse);

    BeregningsgrunnlagVilkårOgAkjonspunktResultat beregn(BehandlingReferanse referanse, BehandlingStegType stegType);

    void lagre(BeregningsgrunnlagGrunnlag beregningsgrunnlagGrunnlag, BehandlingReferanse referanse);

    void kopier(BehandlingReferanse revurdering, BehandlingReferanse originalbehandling, BeregningsgrunnlagTilstand tilstand);

    /**
     * @param oppdatering - Dto som spesifiserer hvilken oppdatering som skal gjøres på grunnlaget
     * @param referanse - Behandlingsreferansen
     * @return - Hvis oppdateringen gjøres i kalkulus returneres et OppdaterBeregningsgrunnlagResultat, hvis oppdateringen skjer i fpsak returneres Optional.empty da det håndteres i oppdatererne
     */
    Optional<OppdaterBeregningsgrunnlagResultat> oppdaterBeregning(BekreftetAksjonspunktDto oppdatering, BehandlingReferanse referanse);

    /**
     * @param overstyring - Dto som spesifiserer hvilken overstyring som skal gjøres på grunnlaget
     * @param referanse - Behandlingsreferansen
     * @return - Hvis oppdateringen gjøres i kalkulus returneres et OppdaterBeregningsgrunnlagResultat, hvis oppdateringen skjer i fpsak returneres Optional.empty da det håndteres i oppdatererne
     */
    Optional<OppdaterBeregningsgrunnlagResultat> overstyrBeregning(OverstyringAksjonspunktDto overstyring, BehandlingReferanse referanse);


}
