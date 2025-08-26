package no.nav.foreldrepenger.domene.prosess;

import java.util.Optional;

import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.domene.aksjonspunkt.OppdaterBeregningsgrunnlagResultat;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;
import no.nav.foreldrepenger.domene.output.BeregningsgrunnlagVilkårOgAkjonspunktResultat;

interface BeregningAPI {

    Optional<BeregningsgrunnlagGrunnlag> hent(BehandlingReferanse referanse);

    BeregningsgrunnlagVilkårOgAkjonspunktResultat beregn(BehandlingReferanse behandlingReferanse, BehandlingStegType stegType);

    Optional<BeregningsgrunnlagDto> hentGUIDto(BehandlingReferanse referanse);

    void kopier(BehandlingReferanse revurdering, BehandlingReferanse originalbehandling, BehandlingStegType tilstand);

    Optional<OppdaterBeregningsgrunnlagResultat> oppdaterBeregning(BekreftetAksjonspunktDto oppdateringer, BehandlingReferanse referanse);

    Optional<OppdaterBeregningsgrunnlagResultat> overstyrBeregning(OverstyringAksjonspunktDto overstyring, BehandlingReferanse referanse);

    void avslutt(BehandlingReferanse referanse);

    boolean kanStartesISteg(BehandlingReferanse referanse, BehandlingStegType stegType);

    void kopierFastsatt(BehandlingReferanse revurdering, BehandlingReferanse originalbehandling);
}
