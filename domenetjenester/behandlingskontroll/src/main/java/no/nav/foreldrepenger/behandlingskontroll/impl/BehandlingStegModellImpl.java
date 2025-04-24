package no.nav.foreldrepenger.behandlingskontroll.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;

/**
 * Modellerer ett behandlingssteg, inklusiv hvilke aksjonspunkter må løses
 * før/etter steget. Dersom det ved kjøring oppdages aksjonspunkter som ikke er
 * registrert må disse løses før utgang av et behandlingssteg.
 */
class BehandlingStegModellImpl implements BehandlingStegModell {
    private Instance<BehandlingSteg> stegInstances;
    private BehandlingSteg steg;
    private BehandlingStegType behandlingStegType;

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
    private BehandlingModellImpl behandlingModell;

    BehandlingStegModellImpl() {
    }

    /**
     * Holder for å referere til en konkret, men lazy-initialisert CDI
     * implementasjon av et {@link BehandlingSteg}.
     */
    BehandlingStegModellImpl(BehandlingModellImpl behandlingModell,
            @Any Instance<BehandlingSteg> bean,
            BehandlingStegType stegType) {
        Objects.requireNonNull(behandlingModell, "behandlingModell");
        Objects.requireNonNull(bean, "bean");
        Objects.requireNonNull(stegType, "stegType");
        this.stegInstances = bean;
        this.behandlingModell = behandlingModell;
        this.behandlingStegType = stegType;
    }

    /** Direkte injisering av {@link BehandlingSteg}. For testing. */
    BehandlingStegModellImpl(BehandlingModellImpl behandlingModell, BehandlingSteg steg, BehandlingStegType stegType) {
        Objects.requireNonNull(behandlingModell, "behandlingModell");
        Objects.requireNonNull(steg, "steg");
        Objects.requireNonNull(stegType, "stegType");
        this.steg = steg;
        this.behandlingModell = behandlingModell;
        this.behandlingStegType = stegType;
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

    protected void initSteg() {
        if (steg == null) {
            steg = BehandlingStegRef.Lookup
                    .find(BehandlingSteg.class, stegInstances, behandlingModell.getFagsakYtelseType(), behandlingModell.getBehandlingType(), behandlingStegType)
                    .orElseThrow(() -> new IllegalStateException(
                            "Mangler steg definert for stegKode=" + behandlingStegType + " [behandlingType="
                                    + behandlingModell.getBehandlingType() + ", fagsakYtelseType=" + behandlingModell.getFagsakYtelseType()
                                    + "]"));
        }
    }

    protected void leggTilAksjonspunktVurderingUtgang(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        behandlingModell.validerErIkkeAlleredeMappet(aksjonspunktDefinisjon);
        utgangAksjonpunkt.add(aksjonspunktDefinisjon);
    }

    protected void leggTilAksjonspunktVurderingInngang(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        behandlingModell.validerErIkkeAlleredeMappet(aksjonspunktDefinisjon);
        inngangAksjonpunkt.add(aksjonspunktDefinisjon);
    }

    void destroy() {
        if (stegInstances != null && steg != null) {
            stegInstances.destroy(steg);
        }
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
        initSteg();
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
