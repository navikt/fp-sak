package no.nav.foreldrepenger.behandlingskontroll;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

/**
 * Definerer metoder for å inspisere state-machine for en gitt behandling type.
 *
 * Hver behandling type er knyttet til en egen BehandlingModell.
 */
public interface BehandlingModell {

    /** Identifiserende parametre for modell */
    FagsakYtelseType getFagsakYtelseType();

    BehandlingType getBehandlingType();

    BehandlingStegModell getStartSteg();

    BehandlingStegModell getSluttSteg();

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

    boolean inneholderSteg(BehandlingStegType stegType);

    BehandlingStegModell finnForrigeSteg(BehandlingStegType stegType);

    BehandlingStegModell finnFørsteSteg(BehandlingStegType... behandlingStegTyper);

    BehandlingStegModell finnNesteSteg(BehandlingStegType stegType);

    BehandlingStegModell finnSteg(BehandlingStegType stegType);

    Optional<BehandlingStegModell> finnSenereSteg(BehandlingStegType førsteSteg, BehandlingStegType senereSteg);

    Optional<BehandlingStegStatus> finnStegStatusFor(BehandlingStegType stegType, Collection<AksjonspunktDefinisjon> aksjonspunktDefinisjoner);

    BehandlingStegModell finnTidligsteStegForAksjonspunktDefinisjon(Collection<AksjonspunktDefinisjon> aksjonspunktDefinisjoner);

    Stream<BehandlingStegModell> hvertSteg();

    Stream<BehandlingStegModell> hvertStegFra(BehandlingStegType fraSteg); // Eksklusiv fraSteg

    Stream<BehandlingStegModell> hvertStegFraOgMed(BehandlingStegType fraOgMedSteg);

    Stream<BehandlingStegModell> hvertStegFraOgMedTil(BehandlingStegType fraOgMedSteg, BehandlingStegType tilSteg, boolean tilOgMed);

    // To metoder for bedre lesbarhet og redusert kognitiv belastning
    boolean erStegAFørStegB(BehandlingStegType stegA, BehandlingStegType stegB);

    boolean erStegAEtterStegB(BehandlingStegType stegA, BehandlingStegType stegB);

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

}
