package no.nav.foreldrepenger.behandlingskontroll;

import java.time.LocalDateTime;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;

/**
 * Tjeneste for å opprette eller endre status på aksjonspunkt. Publiserer hendelser.
 */
public interface AksjonspunktkontrollTjeneste {

    /**
     * Signaliserer at aksjonspunkter er funnet eller har endret status. Bruk helst
     * lagre-metodene
     */
    void aksjonspunkterEndretStatus(BehandlingskontrollKontekst kontekst, BehandlingStegType behandlingStegType, List<Aksjonspunkt> aksjonspunkter);

    /**
     * Oppretter og håndterer nye aksjonspunkt
     */
    List<Aksjonspunkt> lagreAksjonspunkterFunnet(BehandlingskontrollKontekst kontekst, BehandlingStegType behandlingStegType,
            List<AksjonspunktDefinisjon> aksjonspunkter);

    /**
     * Oppretter og håndterer nye overstyringsaksjonspunkt
     */
    List<Aksjonspunkt> lagreAksjonspunkterFunnet(BehandlingskontrollKontekst kontekst, List<AksjonspunktDefinisjon> aksjonspunkter);

    /**
     * Lagrer og håndterer utførte aksjonspunkt uten begrunnelse. Dersom man skal
     * lagre begrunnelse - bruk apRepository + aksjonspunkterUtført
     */
    void lagreAksjonspunkterUtført(BehandlingskontrollKontekst kontekst, BehandlingStegType behandlingStegType,
            List<Aksjonspunkt> aksjonspunkter);

    /**
     * Lagrer og håndterer utførte aksjonspunkt uten begrunnelse. Dersom man skal
     * lagre begrunnelse - bruk apRepository + aksjonspunkterUtført
     */
    void lagreAksjonspunkterUtført(BehandlingskontrollKontekst kontekst, BehandlingStegType behandlingStegType,
            Aksjonspunkt aksjonspunkt, String begrunnelse);

    /**
     * Lagrer og håndterer avbrutte aksjonspunkt
     */
    void lagreAksjonspunkterAvbrutt(BehandlingskontrollKontekst kontekst, BehandlingStegType behandlingStegType,
            List<Aksjonspunkt> aksjonspunkter);

    /**
     * Lagrer og håndterer reåpning av aksjonspunkt
     */
    void lagreAksjonspunkterReåpnet(BehandlingskontrollKontekst kontekst, List<Aksjonspunkt> aksjonspunkter, boolean beholdToTrinnVurdering,
            boolean setTotrinn);

    /**
     * Lagrer og håndterer aksjonspunktresultater fra utledning utenom steg
     */
    void lagreAksjonspunktResultat(BehandlingskontrollKontekst kontekst, BehandlingStegType behandlingStegType,
            List<AksjonspunktResultat> aksjonspunktResultater);

    /**
     * Setter behandlingen på vent. NB IKKE BRUK FRA STEG
     *
     * @param behandling
     * @param aksjonspunktDefinisjon hvilket Aksjonspunkt skal holde i 'ventingen'
     * @param fristTid               Frist før Behandlingen å adresseres
     * @param venteårsak             Årsak til ventingen.
     *
     */
    Aksjonspunkt settBehandlingPåVentUtenSteg(Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon, LocalDateTime fristTid,
            Venteårsak venteårsak);

    /**
     * Setter behandlingen på vent med angitt hvilket steg det står i. NB IKKE BRUK
     * FRA STEG
     *
     * @param behandling
     * @param aksjonspunktDefinisjon hvilket Aksjonspunkt skal holde i 'ventingen'
     * @param BehandlingStegType     stegType aksjonspunktet står i.
     * @param fristTid               Frist før Behandlingen å adresseres
     * @param venteårsak             Årsak til ventingen.
     *
     */
    Aksjonspunkt settBehandlingPåVent(Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon, BehandlingStegType stegType,
            LocalDateTime fristTid,
            Venteårsak venteårsak);

    /**
     * Setter autopunkter av en spesifikk aksjonspunktdefinisjon til utført. Dette
     * klargjør kun behandligen for prosessering, men vil ikke drive prosessen
     * videre.
     *
     * @param aksjonspunktDefinisjon Aksjonspunktdefinisjon til de aksjonspunktene
     *                               som skal lukkes Bruk
     *                               {@link #prosesserBehandling(BehandlingskontrollKontekst)}
     *                               el. tilsvarende for det.
     */
    void settAutopunktTilUtført(Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon, BehandlingskontrollKontekst kontekst);

    /**
     * Ny metode som forbereder en behandling for prosessering - setter autopunkt
     * til utført og evt tilbakeføring ved gjenopptak. Behandlingen skal være klar
     * til prosessering uten åpne autopunkt når kallet er ferdig.
     */
    void taBehandlingAvVentSetAlleAutopunktUtført(Behandling behandling, BehandlingskontrollKontekst kontekst);

    void taBehandlingAvVentSetAlleAutopunktUtførtForHenleggelse(Behandling behandling, BehandlingskontrollKontekst kontekst);


}
