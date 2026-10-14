package no.nav.foreldrepenger.behandlingskontroll.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;

/**
 * Modellerer ett behandlingssteg, inklusiv hvilke aksjonspunkter må løses
 * før/etter steget. Dersom det ved kjøring oppdages aksjonspunkter som ikke er
 * registrert må disse løses før utgang av et behandlingssteg.
 */
class BehandlingStegModellImpl implements BehandlingStegModell {
    private final Supplier<BehandlingSteg> stegOppslag;
    private BehandlingSteg steg;
    private final BehandlingStegType behandlingStegType;

    /**
     * Aksjonspunkter som må løses ved inngang til behandlingsteg.
     */
    private final Set<AksjonspunktDefinisjon> inngangAksjonpunkt = new LinkedHashSet<>();

    /**
     * Aksjonspunkter som må løses ved utgang fra behandlingsteg.
     */
    private final Set<AksjonspunktDefinisjon> utgangAksjonpunkt = new LinkedHashSet<>();

    /**
     * Hver steg modell må tilhøre en BehandlingModell som beskriver hvordan de
     * henger sammen.
     */
    private final BehandlingModellImpl behandlingModell;

    /**
     * Lazy oppslag av {@link BehandlingSteg} ved første {@link #getSteg()}, slik at modeller kan bygges
     * i moduler/tester der steg-implementasjonene ikke finnes.
     */
    BehandlingStegModellImpl(BehandlingModellImpl behandlingModell, Supplier<BehandlingSteg> stegOppslag, BehandlingStegType stegType) {
        this.behandlingModell = Objects.requireNonNull(behandlingModell, "behandlingModell");
        this.stegOppslag = Objects.requireNonNull(stegOppslag, "stegOppslag");
        this.behandlingStegType = Objects.requireNonNull(stegType, "stegType");
    }

    /** Direkte referanse til {@link BehandlingSteg}. For testing. */
    BehandlingStegModellImpl(BehandlingModellImpl behandlingModell, BehandlingSteg steg, BehandlingStegType stegType) {
        Objects.requireNonNull(steg, "steg");
        this(behandlingModell, () -> steg, stegType);
    }

    @Override
    public BehandlingModell getBehandlingModell() {
        return behandlingModell;
    }

    @Override
    public BehandlingStegModell getNesteSteg() {
        return getBehandlingModell().finnNesteSteg(getBehandlingStegType());
    }

    @Override
    public BehandlingStegModell getSenereStegHvisFinnes(BehandlingStegType senereSteg) {
        return getBehandlingModell().finnSenereSteg(getBehandlingStegType(), senereSteg)
            .orElseThrow(() -> new IllegalStateException("Finnes ikke noe steg av type " + senereSteg + " etter " + getBehandlingStegType()));

    }

    Set<AksjonspunktDefinisjon> getInngangAksjonpunkt() {
        return Collections.unmodifiableSet(inngangAksjonpunkt);
    }

    Set<AksjonspunktDefinisjon> getUtgangAksjonpunkt() {
        return Collections.unmodifiableSet(utgangAksjonpunkt);
    }

    protected void leggTilAksjonspunktVurderingUtgang(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        behandlingModell.validerErIkkeAlleredeMappet(aksjonspunktDefinisjon);
        utgangAksjonpunkt.add(aksjonspunktDefinisjon);
    }

    protected void leggTilAksjonspunktVurderingInngang(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        behandlingModell.validerErIkkeAlleredeMappet(aksjonspunktDefinisjon);
        inngangAksjonpunkt.add(aksjonspunktDefinisjon);
    }

    /**
     * Type kode for dette steget.
     */
    @Override
    public BehandlingStegType getBehandlingStegType() {
        return behandlingStegType;
    }

    /**
     * Implementasjon av et gitt steg i behandlingen.
     */
    @Override
    public BehandlingSteg getSteg() {
        if (steg == null) {
            steg = stegOppslag.get();
        }
        return steg;
    }

    /**
     * Avleder status behandlingsteg bør settes i gitt et sett med aksjonpunkter.
     * Tar kun hensyn til aksjonpunkter som gjelder dette steget.
     */
    Optional<BehandlingStegStatus> avledStatus(Collection<AksjonspunktDefinisjon> aksjonspunkter) {

        if (!Collections.disjoint(aksjonspunkter, inngangAksjonpunkt)) {
            return Optional.of(BehandlingStegStatus.INNGANG);
        }
        if (!Collections.disjoint(aksjonspunkter, utgangAksjonpunkt)) {
            return Optional.of(BehandlingStegStatus.UTGANG);
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + behandlingStegType + ", "
                + "inngangAksjonspunkter=" + inngangAksjonpunkt + ", "
                + "utgangAksjonspunkter=" + utgangAksjonpunkt + ", "
                + "impl=" + steg
                + ">";
    }
}
