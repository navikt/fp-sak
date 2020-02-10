package no.nav.foreldrepenger.behandlingskontroll.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;

/**
 * Modellerer ett behandlingssteg, inklusiv hvilke aksjonspunkter må løses før/etter steget.
 * Dersom det ved kjøring oppdages aksjonspunkter som ikke er registrert må disse løses før utgang av et
 * behandlingssteg.
 */
class BehandlingStegModellImpl implements BehandlingStegModell {
    private Instance<BehandlingSteg> stegInstances;
    private BehandlingSteg steg;
    private BehandlingStegType behandlingStegType;

    /**
     * Aksjonspunkter som må løses ved inngang til behandlingsteg.
     */
    private final Set<String> inngangAksjonpunktKoder = new LinkedHashSet<>();

    /**
     * Aksjonspunkter som må løses ved utgang fra behandlingsteg.
     */
    private final Set<String> utgangAksjonpunktKoder = new LinkedHashSet<>();

    /** Hver steg modell må tilhøre en BehandlingModell som beskriver hvordan de henger sammen. */
    private BehandlingModellImpl behandlingModell;

    /**
     * Holder for å referere til en konkret, men lazy-initialisert CDI implementasjon av et {@link BehandlingSteg}.
     */
    BehandlingStegModellImpl(BehandlingModellImpl behandlingModell, 
        @Any Instance<BehandlingSteg> bean, 
        BehandlingStegType stegType) {
        Objects.requireNonNull(behandlingModell, "behandlingModell"); //$NON-NLS-1$
        Objects.requireNonNull(bean, "bean"); //$NON-NLS-1$
        Objects.requireNonNull(stegType, "stegType"); //$NON-NLS-1$
        this.stegInstances = bean;
        this.behandlingModell = behandlingModell;
        this.behandlingStegType = stegType;
    }

    /** Direkte injisering av {@link BehandlingSteg}. For testing. */
    BehandlingStegModellImpl(BehandlingModellImpl behandlingModell, BehandlingSteg steg, BehandlingStegType stegType) {
        Objects.requireNonNull(behandlingModell, "behandlingModell"); //$NON-NLS-1$
        Objects.requireNonNull(steg, "steg"); //$NON-NLS-1$ // NOSONAR
        Objects.requireNonNull(stegType, "stegType"); //$NON-NLS-1$ // NOSONAR
        this.steg = steg;
        this.behandlingModell = behandlingModell;
        this.behandlingStegType = stegType;
    }

    @Override
    public BehandlingModell getBehandlingModell() {
        return behandlingModell;
    }

    Set<String> getInngangAksjonpunktKoder() {
        return Collections.unmodifiableSet(inngangAksjonpunktKoder);
    }

    Set<String> getUtgangAksjonpunktKoder() {
        return Collections.unmodifiableSet(utgangAksjonpunktKoder);
    }

    protected void initSteg() {
        if (steg == null) {
            steg = BehandlingTypeRef.Lookup
                .find(BehandlingSteg.class, stegInstances, behandlingModell.getFagsakYtelseType(), behandlingModell.getBehandlingType())
                .orElseThrow(() -> {
                    return new IllegalStateException(
                        "Mangler steg definert for stegKode=" + behandlingStegType + " [behandlingType=" //$NON-NLS-1$ //$NON-NLS-2$
                            + behandlingModell.getBehandlingType() + ", fagsakYtelseType=" + behandlingModell.getFagsakYtelseType() //$NON-NLS-1$ //$NON-NLS-2$
                            + "]");
                });
        }
    }

    protected void leggTilAksjonspunktVurderingUtgang(String kode) {
        behandlingModell.validerErIkkeAlleredeMappet(kode);
        utgangAksjonpunktKoder.add(kode);
    }

    protected void leggTilAksjonspunktVurderingInngang(String kode) {
        behandlingModell.validerErIkkeAlleredeMappet(kode);
        inngangAksjonpunktKoder.add(kode);
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
     * Forventet status når behandling er i steget.
     */
    @Override
    public String getForventetStatus() {
        return behandlingStegType.getDefinertBehandlingStatus().getKode();
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
     * Avleder status behandlingsteg bør settes i gitt et sett med aksjonpunkter. Tar kun hensyn til aksjonpunkter
     * som gjelder dette steget.
     */
    Optional<BehandlingStegStatus> avledStatus(Collection<String> aksjonspunkter) {

        if (!Collections.disjoint(aksjonspunkter, inngangAksjonpunktKoder)) { // NOSONAR
            return Optional.of(BehandlingStegStatus.INNGANG);
        } else if (!Collections.disjoint(aksjonspunkter, utgangAksjonpunktKoder)) { // NOSONAR
            return Optional.of(BehandlingStegStatus.UTGANG);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + behandlingStegType.getKode() + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "inngangAksjonspunkter=" + inngangAksjonpunktKoder + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "utgangAksjonspunkter=" + utgangAksjonpunktKoder + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "impl=" + steg //$NON-NLS-1$
            + ">"; //$NON-NLS-1$
    }
}
