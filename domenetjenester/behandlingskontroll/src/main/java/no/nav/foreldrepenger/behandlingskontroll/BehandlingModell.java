package no.nav.foreldrepenger.behandlingskontroll;

import no.nav.foreldrepenger.behandlingskontroll.transisjoner.StegTransisjon;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.TransisjonIdentifikator;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Definerer metoder for å inspisere state-machine for en gitt behandling type.
 *
 * Hver behandling type er knyttet til en egen BehandlingModell.
 */
public interface BehandlingModell {

    /** Gjelder kun steg ETTER angitt steg (eksklusv angitt steg). */
    Set<AksjonspunktDefinisjon> finnAksjonspunktDefinisjonerEtter(BehandlingStegType steg);

    /**
     * Gjelder kun steg ETTER angitt steg (inklusiv angitt steg). Dersom
     * medInngangOgså tas også aksjonspunt som skal være løst også ved Inngang med,
     * ellers kun ved Utgang av steget
     */
    Set<AksjonspunktDefinisjon> finnAksjonspunktDefinisjonerFraOgMed(BehandlingStegType steg, boolean medInngangOgså);

    Set<AksjonspunktDefinisjon> finnAksjonspunktDefinisjoner(BehandlingStegType stegType);

    Set<AksjonspunktDefinisjon> finnAksjonspunktDefinisjonerInngang(BehandlingStegType steg);

    Set<AksjonspunktDefinisjon> finnAksjonspunktDefinisjonerUtgang(BehandlingStegType steg);

    BehandlingStegModell finnForrigeSteg(BehandlingStegType stegType);

    BehandlingStegModell finnFørsteSteg(BehandlingStegType... behandlingStegTyper);

    BehandlingStegModell finnNesteSteg(BehandlingStegType stegType);

    BehandlingStegModell finnSteg(BehandlingStegType stegType);

    Optional<BehandlingStegStatus> finnStegStatusFor(BehandlingStegType stegType, Collection<AksjonspunktDefinisjon> aksjonspunktDefinisjoner);

    BehandlingStegModell finnTidligsteStegFor(Collection<AksjonspunktDefinisjon> aksjonspunkter);

    BehandlingStegModell finnTidligsteStegFor(AksjonspunktDefinisjon aksjonspunkt);

    BehandlingStegModell finnTidligsteStegForAksjonspunktDefinisjon(Collection<AksjonspunktDefinisjon> aksjonspunktDefinisjoner);

    /** Behandling type modellen gjelder for. */
    BehandlingType getBehandlingType();

    Stream<BehandlingStegModell> hvertSteg();

    Stream<BehandlingStegModell> hvertStegEtter(BehandlingStegType stegType);

    Stream<BehandlingStegModell> hvertStegFraOgMed(BehandlingStegType fraOgMedSteg);

    Stream<BehandlingStegModell> hvertStegFraOgMedTil(BehandlingStegType fraOgMedSteg, BehandlingStegType tilSteg, boolean inklusivTil);

    boolean erStegAFørStegB(BehandlingStegType stegA, BehandlingStegType stegB);

    /**
     * Beregn relativ forflytning mellom to steg.
     *
     * @param stegFørType
     * @param stegEtterType
     * @return 1 (normalt fremover), mindre enn 0 (tilbakeføring), større enn 1
     *         (overhopp/framføring)
     */
    int relativStegForflytning(BehandlingStegType stegFørType, BehandlingStegType stegEtterType);

    /**
     * Kjør behandling fra angitt steg, med angitt visitor. Stopper når visitor ikke
     * kan kjøre lenger.
     *
     * @param startFraBehandlingStegType
     * @param visitor
     *
     * @return
     */
    BehandlingStegUtfall prosesserFra(BehandlingStegType startFraBehandlingStegType, BehandlingModellVisitor visitor);

    FagsakYtelseType getFagsakYtelseType();

    StegTransisjon finnTransisjon(TransisjonIdentifikator transisjonIdentifikator);

    List<BehandlingStegType> getAlleBehandlingStegTyper();

}
