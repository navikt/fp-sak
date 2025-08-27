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
     * Kopierer det som skal til for å starte beregningen i det angitte steget.
     * Eksempel: Hvis FORESLÅ_BEREGNINGSGRUNNLAG er angitt vil siste steg før FORESLÅ_BEREGNINGSGRUNNLAG bli kopiert, men ikke FORESLÅ_BEREGNINGSGRUNNLAG.
     * Beregningen er da klar for å kjøres videre fra FORESLÅ_BEREGNINGSGRUNNLAG steget.
     * OBS: Hvis FORESLÅ_BEREGNINGSGRUNNLAG er angitt som steg vil det bli gjort g-regulering av grunnlaget hvis input tilsier at dette er nødvendig.
     * @param revurdering - behandlingen vi skal kopiere et grunnlag til
     * @param originalbehandling - behandlingen vi skal kopiere et grunnlag fra
     * @param stegType steget vi skal kopiere til (OBS: Om det er til og med eller til avhenger foreløpig av implementasjon)
     */
    void kopier(BehandlingReferanse revurdering, BehandlingReferanse originalbehandling, BehandlingStegType stegType);

    /**
     * Kopierer et fastsatt grunnlag til en ny behandling. Til bruk i revurderinger når behandlingen skal starte etter beregning.
     * Forutsetter at originalbehandling har et fastsatt (ferdig beregnet) beregningsgrunnlag
     * @param revurdering - behandlingen vi skal kopiere et grunnlag til
     * @param originalbehandling - behandlingen vi skal kopiere et grunnlag fra
     */
    void kopierFastsatt(BehandlingReferanse revurdering, BehandlingReferanse originalbehandling);

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
