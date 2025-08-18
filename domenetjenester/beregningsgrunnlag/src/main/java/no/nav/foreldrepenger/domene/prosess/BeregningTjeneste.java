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

    /**
     * Kopieringen er normalt sett "til og med", men pga spesialbehandling for g-regulering kopierer "FORESLÅTT" bare til dette steget.
     * FORESLÅTT steget må deretter kjøres av fpsak. For alle andre steg er kopieringen til og med angitt tilstand.
     * @param revurdering - behandlingen vi skal kopiere et grunnlag til
     * @param originalbehandling - behandlingen vi skal kopiere et grunnlag fra
     * @param tilstand tilstanden vi skal kopiere
     */
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

    /**
     * Markerer en kobling som avsluttet i fp-kalkulus. Dette betyr at koblingen og dens grunnlag ikke lenger skal endres, og inaktive data på sikt kan ryddes.
     * @param referanse
     */
    void avslutt(BehandlingReferanse referanse);

    /**
     *
     * @param referanse referanse til behandlingen som sjekkes
     * @param stegType steg vi ønsker å se om behandlingen kan startes i
     * @return en boolean for om steget er et gyldig startpunkt eller ikke.
     */
    boolean kanStartesISteg(BehandlingReferanse referanse, BehandlingStegType stegType);
}
