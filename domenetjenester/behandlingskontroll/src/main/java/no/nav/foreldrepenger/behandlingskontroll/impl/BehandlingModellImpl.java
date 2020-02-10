package no.nav.foreldrepenger.behandlingskontroll.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingModellVisitor;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegTilstandSnapshot;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegUtfall;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.StegProsesseringResultat;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStegOvergangEvent;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStegTilstandEndringEvent;
import no.nav.foreldrepenger.behandlingskontroll.impl.transisjoner.Transisjoner;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.StegTransisjon;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.TransisjonIdentifikator;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegTilstand;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.VurderingspunktType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

/**
 * Modell av behandlingssteg, vurderingspunkter og aksjonspunkter som brukes i evaluering av en prosess for behandling.
 */
public class BehandlingModellImpl implements AutoCloseable, BehandlingModell {

    private static final Logger logger = LoggerFactory.getLogger(BehandlingModellImpl.class);
    private static final String ER_IKKE_DEFINERT_BLANT = " er ikke definert blant: ";
    private final BehandlingType behandlingType;
    /**
     * Enkel sekvens av behandlingsteg. Støtter ikke branching p.t.
     */
    private List<BehandlingStegModellImpl> steg = new ArrayList<>();
    private TriFunction<BehandlingStegType, BehandlingType, FagsakYtelseType, BehandlingStegModellImpl> lookup;
    private boolean destroyOnClose;

    private FagsakYtelseType fagsakYtelseType;

    /**
     * Default modell bruker steg implementert vha. CDI.
     *
     * @param destroyOnClose - hvorvidt #close skal gjøre noe.
     */
    protected BehandlingModellImpl(BehandlingType behandlingType, FagsakYtelseType fagsakYtelseType, boolean destroyOnClose) {
        Objects.requireNonNull(behandlingType, "behandlingType"); //$NON-NLS-1$ // NOSONAR
        this.destroyOnClose = destroyOnClose;
        this.behandlingType = behandlingType;
        this.fagsakYtelseType = fagsakYtelseType;
        this.lookup = new CdiLookup(this);
    }

    /* for testing først og fremst. */
    protected BehandlingModellImpl(BehandlingType behandlingType,
                                   FagsakYtelseType fagsakYtelseType,
                                   TriFunction<BehandlingStegType, BehandlingType, FagsakYtelseType, BehandlingSteg> lookup) {
        Objects.requireNonNull(behandlingType, "behandlingType"); //$NON-NLS-1$ // NOSONAR
        Objects.requireNonNull(fagsakYtelseType, "fagsakYtelseType"); //$NON-NLS-1$ // NOSONAR
        this.fagsakYtelseType = fagsakYtelseType;
        this.behandlingType = behandlingType;
        this.lookup = (stegType, behType, ytType) -> new BehandlingStegModellImpl(this, lookup.apply(stegType, behType, ytType), stegType);
    }

    static BehandlingStegTilstandEndringEvent nyBehandlingStegTilstandEndring(BehandlingskontrollKontekst kontekst, BehandlingStegTilstandSnapshot fraTilstand,
                                                                              BehandlingStegTilstandSnapshot tilTilstand) {
        return new BehandlingStegTilstandEndringEvent(kontekst, fraTilstand, tilTilstand);
    }

    static BehandlingStegTilstandSnapshot tilBehandlingsStegSnapshot(Optional<BehandlingStegTilstand> tilstand) {
        BehandlingStegType stegType = tilstand.map(BehandlingStegTilstand::getBehandlingSteg).orElse(null);
        BehandlingStegStatus status = tilstand.map(BehandlingStegTilstand::getBehandlingStegStatus).orElse(null);
        return getBehandlingStegTilstandSnapshot(tilstand, stegType, status);
    }

    private static BehandlingStegTilstandSnapshot getBehandlingStegTilstandSnapshot(Optional<BehandlingStegTilstand> tilstand, BehandlingStegType stegType,
                                                                                    BehandlingStegStatus status) {
        if (stegType != null) {
            return new BehandlingStegTilstandSnapshot(tilstand.map(BehandlingStegTilstand::getId).orElse(null), stegType, status);
        }
        return null;
    }

    static BehandlingStegOvergangEvent nyBehandlingStegOvergangEvent(BehandlingModell modell,
                                                                     BehandlingStegTilstandSnapshot forrigeTilstand,
                                                                     BehandlingStegTilstandSnapshot nyTilstand, BehandlingskontrollKontekst kontekst) {

        BehandlingStegType stegFørType = forrigeTilstand != null ? forrigeTilstand.getSteg() : null;
        BehandlingStegType stegEtterType = nyTilstand != null ? nyTilstand.getSteg() : null;

        int relativForflytning = modell.relativStegForflytning(stegFørType, stegEtterType);

        return BehandlingStegOvergangEvent.nyEvent(kontekst, forrigeTilstand, nyTilstand, relativForflytning);

    }

    @Override
    public BehandlingType getBehandlingType() {
        return behandlingType;
    }

    @Override
    public FagsakYtelseType getFagsakYtelseType() {
        return fagsakYtelseType;
    }

    @Override
    public BehandlingStegModell finnSteg(BehandlingStegType stegType) {
        return internFinnSteg(stegType);
    }

    @Override
    public BehandlingStegModell finnSteg(String stegKode) {
        return steg.get(indexOf(stegKode));
    }

    @Override
    public BehandlingStegModell finnNesteSteg(String stegKode) {
        int idx = indexOf(stegKode);
        if (idx >= (steg.size() - 1) || idx < 0) {
            return null;
        } else {
            return steg.get(idx + 1);
        }
    }

    @Override
    public BehandlingStegModell finnNesteSteg(BehandlingStegType stegType) {
        Objects.requireNonNull(stegType, "stegType"); //$NON-NLS-1$ // NOSONAR
        return finnNesteSteg(stegType.getKode());
    }

    @Override
    public BehandlingStegModell finnForrigeSteg(BehandlingStegType stegType) {
        Objects.requireNonNull(stegType, "stegType"); //$NON-NLS-1$ // NOSONAR
        return finnForrigeSteg(stegType.getKode());
    }

    @Override
    public BehandlingStegModell finnForrigeSteg(String stegKode) {

        int idx = indexOf(stegKode);
        if (idx > 0 && idx < steg.size()) {
            return steg.get(idx - 1);
        } else {
            return null;
        }
    }

    @Override
    public BehandlingStegModell finnTidligsteStegFor(AksjonspunktDefinisjon aksjonspunkt) {
        return finnTidligsteStegFor(Arrays.asList(aksjonspunkt));
    }

    @Override
    public BehandlingStegModell finnTidligsteStegFor(Collection<AksjonspunktDefinisjon> aksjonspunkter) {
        for (BehandlingStegModellImpl stegModell : steg) {
            boolean hørerTilSteget = aksjonspunkter.stream().map(AksjonspunktDefinisjon::getBehandlingSteg)
                .anyMatch(s -> s.getKode().equals(stegModell.getBehandlingStegType().getKode()));
            if (hørerTilSteget) {
                return stegModell;
            }
        }
        return null;
    }

    @Override
    public BehandlingStegModell finnTidligsteStegForAksjonspunktDefinisjon(Collection<String> aksjonspunktDefinisjoner) {
        for (BehandlingStegModellImpl stegModell : steg) {
            boolean hørerTilSteget = aksjonspunktDefinisjoner.stream().map(AksjonspunktDefinisjon::fraKode)
                .map(AksjonspunktDefinisjon::getBehandlingSteg)
                .anyMatch(s -> s.getKode().equals(stegModell.getBehandlingStegType().getKode()));
            if (hørerTilSteget) {
                return stegModell;
            }
        }
        return null;
    }

    @Override
    public BehandlingStegModell finnFørsteSteg(BehandlingStegType... behandlingStegTyper) {
        Set<BehandlingStegType> stegTyper = new LinkedHashSet<>(Arrays.asList(behandlingStegTyper).stream().filter(bs -> bs != null)
            .collect(Collectors.toList()));

        for (BehandlingStegModellImpl stegModell : steg) {
            BehandlingStegType sjekkSteg = stegModell.getBehandlingStegType();
            if (stegTyper.contains(sjekkSteg)) {
                return stegModell;
            }
        }

        throw new IllegalArgumentException(
            "Utvikler-feil: Ingen av forespurte steg er kjent i BehandlingModell: behandlingType=" + behandlingType //$NON-NLS-1$
                + ", forspurteSteg=" + Arrays.asList(stegTyper) // NOSONAR //$NON-NLS-1$
        );
    }

    @Override
    public Stream<BehandlingStegModell> hvertSteg() {
        return steg.stream().map(m -> (BehandlingStegModell) m);
    }

    @Override
    public List<BehandlingStegType> getAlleBehandlingStegTyper() {
        return steg.stream().map(s -> s.getBehandlingStegType()).collect(Collectors.toList());
    }

    /**
     * Siden CDI kan ha blitt benyttet programmatisk i oppslag må modellen også avsluttes når den ikke lenger er i bruk.
     */
    @Override
    public void close() {
        if (destroyOnClose) {
            for (BehandlingStegModellImpl stegModellImpl : steg) {
                stegModellImpl.destroy();
            }
        }

    }

    @Override
    public Stream<BehandlingStegModell> hvertStegFraOgMed(BehandlingStegType fraOgMedSteg) {
        return hvertStegFraOgMedTil(fraOgMedSteg, steg.get(steg.size() - 1).getBehandlingStegType(), true);
    }

    @Override
    public Stream<BehandlingStegModell> hvertStegFraOgMedTil(BehandlingStegType fraOgMedSteg, BehandlingStegType tilSteg,
                                                             boolean inklusivTil) {
        if (fraOgMedSteg == null) {
            return Stream.empty();
        }
        int idx = indexOf(fraOgMedSteg.getKode());
        if (idx < 0) {
            throw new IllegalStateException("BehandlingSteg (fraogmed) " + fraOgMedSteg + ER_IKKE_DEFINERT_BLANT + steg); //$NON-NLS-1$ //$NON-NLS-2$
        }

        int idxEnd = tilSteg == null ? steg.size() - 1 : indexOf(tilSteg.getKode());
        if (idxEnd < 0) {
            throw new IllegalStateException("BehandlingSteg (til) " + tilSteg + ER_IKKE_DEFINERT_BLANT + steg); //$NON-NLS-1$ //$NON-NLS-2$
        }
        if (idx <= idxEnd) {
            return steg.subList(idx, idxEnd + (inklusivTil ? 1 : 0)).stream().map(e -> e);
        } else {
            return steg.subList(idxEnd + (inklusivTil ? 1 : 0), idx).stream().map(e -> e);
        }

    }

    @Override
    public Stream<BehandlingStegModell> hvertStegEtter(BehandlingStegType stegType) {
        return internHvertStegEtter(stegType).stream().map(e -> e);
    }

    @Override
    public Optional<BehandlingStegStatus> finnStegStatusFor(BehandlingStegType stegType, Collection<String> aksjonspunkter) {
        BehandlingStegModellImpl stegModell = internFinnSteg(stegType);
        return stegModell.avledStatus(aksjonspunkter);
    }

    @Override
    public Set<String> finnAksjonspunktDefinisjonerEtter(BehandlingStegType steg) {
        Set<String> set = new LinkedHashSet<>();
        internHvertStegEtter(steg).forEach(s -> {
            set.addAll(s.getInngangAksjonpunktKoder());
            set.addAll(s.getUtgangAksjonpunktKoder());
        });
        return Collections.unmodifiableSet(set);
    }

    @Override
    public Set<String> finnAksjonspunktDefinisjonerFraOgMed(BehandlingStegType steg, boolean medInngangOgså) {
        if (steg == null) {
            return Collections.emptySet();
        }

        Set<String> set = new LinkedHashSet<>();

        if (medInngangOgså) {
            set.addAll(finnAksjonspunktDefinisjoner(steg));
        } else {
            set.addAll(finnAksjonspunktDefinisjonerUtgang(steg));
        }

        set.addAll(finnAksjonspunktDefinisjonerEtter(steg));

        return Collections.unmodifiableSet(set);
    }

    @Override
    public Set<String> finnAksjonspunktDefinisjoner(BehandlingStegType stegType) {
        Set<String> set = new LinkedHashSet<>();
        BehandlingStegModellImpl stegModell = internFinnSteg(stegType);
        set.addAll(stegModell.getInngangAksjonpunktKoder());
        set.addAll(stegModell.getUtgangAksjonpunktKoder());
        return set;
    }

    @Override
    public Set<String> finnAksjonspunktDefinisjonerInngang(BehandlingStegType steg) {
        return internFinnSteg(steg).getInngangAksjonpunktKoder();
    }

    @Override
    public Set<String> finnAksjonspunktDefinisjonerUtgang(BehandlingStegType steg) {
        return internFinnSteg(steg).getUtgangAksjonpunktKoder();
    }

    protected BehandlingStegModellImpl internFinnSteg(BehandlingStegType stegType) {
        Objects.requireNonNull(stegType, "stegType"); //$NON-NLS-1$ // NOSONAR
        return steg.get(indexOf(stegType.getKode()));
    }

    protected List<BehandlingStegModellImpl> internHvertStegEtter(BehandlingStegType stegType) {
        int idx;
        if (stegType == null) {
            idx = 1;
        } else {
            idx = indexOf(stegType.getKode());
            if (idx < 0) {
                throw new IllegalStateException("BehandlingSteg " + stegType + ER_IKKE_DEFINERT_BLANT + steg); //$NON-NLS-1$ //$NON-NLS-2$
            } else if (idx == (steg.size() - 1)) {
                return Collections.emptyList();
            }
        }
        List<BehandlingStegModellImpl> subList = steg.subList(idx + 1, steg.size());
        return subList;
    }

    /**
     * Visit alle steg definert i denne modellen.
     *
     * @param førsteSteg - Kode for første steg vi starter fra. Hvis null, begynn fra begynnelsen.
     * @param visitor - kalles for hvert steg definert
     * @return null hvis alle steg til slutt ble kalt. Eller siste stegType som ble forsøkt.
     */
    @Override
    public BehandlingStegUtfall prosesserFra(BehandlingStegType førsteSteg, BehandlingModellVisitor visitor) {
        Objects.requireNonNull(visitor, "visitor"); //$NON-NLS-1$ // NOSONAR

        int idx = førsteSteg == null ? 0 : indexOf(førsteSteg.getKode());
        BehandlingStegModellImpl entry = steg.get(idx);
        while (entry != null) {
            logger.debug("Prosesserer steg: {}", entry);
            StegProsesseringResultat resultat = visitor.prosesser(entry);
            BehandlingStegStatus nyStegStatus = resultat.getNyStegStatus();

            if (!nyStegStatus.kanFortsetteTilNeste()) {
                // bryt flyten, og bli stående på dette steget
                logger.debug("Avbryter etter steg: {}, transisjon={}", entry, resultat);
                return new BehandlingStegUtfall(entry.getBehandlingStegType(), resultat.getNyStegStatus());
            }

            StegTransisjon transisjon = finnTransisjon(resultat.getTransisjon());
            entry = (BehandlingStegModellImpl) transisjon.nesteSteg(entry);
        }

        // avslutter med null når ikke flere steg igjen.
        logger.debug("Avslutter, ingen flere steg");
        return null;

    }

    void leggTil(BehandlingStegType stegType, BehandlingType behandlingType, FagsakYtelseType ytelseType) {
        Objects.requireNonNull(stegType, "stegType"); //$NON-NLS-1$ // NOSONAR

        BehandlingStegModellImpl entry = lookup.apply(stegType, behandlingType, ytelseType);
        if (entry == null) {
            throw new IllegalArgumentException(
                "Fant ikke steg for kode: " + //$NON-NLS-1$
                    stegType.getKode() +
                    ", [behandlingType=" + behandlingType + "]"); // NOSONAR //$NON-NLS-1$ //$NON-NLS-2$
        }
        this.steg.add(entry);
        leggTilAksjonspunktDefinisjoner(stegType, entry);
    }

    protected void leggTilAksjonspunktDefinisjoner(BehandlingStegType stegType, BehandlingStegModellImpl entry) {
        AksjonspunktDefinisjon.finnAksjonspunktDefinisjoner(stegType, VurderingspunktType.INN)
            .forEach(ad -> entry.leggTilAksjonspunktVurderingInngang(ad.getKode()));
        
        AksjonspunktDefinisjon.finnAksjonspunktDefinisjoner(stegType, VurderingspunktType.UT)
            .forEach(ad -> entry.leggTilAksjonspunktVurderingUtgang(ad.getKode()));
    }

    void validerErIkkeAlleredeMappet(String aksjonspunktKode) {
        Objects.requireNonNull(aksjonspunktKode, "aksjonspunktKode"); //$NON-NLS-1$

        for (BehandlingStegModellImpl bsm : this.steg) {
            if (bsm.getInngangAksjonpunktKoder().contains(aksjonspunktKode)) {
                throw new IllegalStateException("Aksjonpunktkode [" + aksjonspunktKode + "] allerede mappet til inngang av " + //$NON-NLS-1$ //$NON-NLS-2$
                    bsm.getBehandlingStegType().getKode()
                    + " [behandlingType=" + behandlingType + "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                // //
                // NOSONAR
            }
            if (bsm.getUtgangAksjonpunktKoder().contains(aksjonspunktKode)) {
                throw new IllegalStateException("Aksjonpunktkode [" + aksjonspunktKode + "] allerede mappet til utgang av " + //$NON-NLS-1$ //$NON-NLS-2$
                    bsm.getBehandlingStegType().getKode()
                    + " [behandlingType=" + behandlingType + "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                // //
                // NOSONAR
            }
        }
    }

    @Override
    public int relativStegForflytning(BehandlingStegType stegFørType, BehandlingStegType stegEtterType) {
        return indexOf(stegEtterType) - indexOf(stegFørType);
    }

    private int indexOf(BehandlingStegType stegType) {
        return stegType == null ? -1 : indexOf(stegType.getKode());
    }

    private int indexOf(String stegKode) {
        Objects.requireNonNull(stegKode, "stegKode"); //$NON-NLS-1$ // NOSONAR
        for (int i = 0, max = steg.size(); i < max; i++) {
            BehandlingStegModell bsModell = steg.get(i);
            if (Objects.equals(stegKode, bsModell.getBehandlingStegType().getKode())) {
                return i;
            }
        }
        throw new IllegalArgumentException("Ukjent behandlingssteg: " + stegKode + ", [behandlingType=" + behandlingType + "]"); //$NON-NLS-1$ //$NON-NLS-2$ //
                                                                                                                                 // NOSONAR
    }

    @Override
    public boolean erStegAFørStegB(BehandlingStegType stegA, BehandlingStegType stegB) {
        return indexOf(stegA) < indexOf(stegB);
    }

    /** Legger til default. */
    protected void leggTil(BehandlingStegType... stegTyper) {
        List.of(stegTyper).forEach(s -> leggTil(s, behandlingType, fagsakYtelseType));
    }

    @Override
    public StegTransisjon finnTransisjon(TransisjonIdentifikator transisjonIdentifikator) {
        return Transisjoner.finnTransisjon(transisjonIdentifikator);
    }

    @FunctionalInterface
    public interface TriFunction<T, U, V, R> {
        /**
         * Applies this function to the given arguments.
         *
         * @param t the first function argument
         * @param u the second function argument
         * @param v the third function argument
         * @return the function result
         */
        R apply(T t, U u, V v);

    }

    private static class CdiLookup implements TriFunction<BehandlingStegType, BehandlingType, FagsakYtelseType, BehandlingStegModellImpl> {
        private BehandlingModellImpl behandlingModell;

        CdiLookup(BehandlingModellImpl behandlingModell) {
            this.behandlingModell = behandlingModell;
        }

        @Override
        public BehandlingStegModellImpl apply(BehandlingStegType stegType, BehandlingType behandlingType, FagsakYtelseType ytelseType) {
            var annotationLiteral = new BehandlingStegRef.BehandlingStegRefLiteral(stegType);

            Instance<BehandlingSteg> instance = CDI.current().select(BehandlingSteg.class, annotationLiteral);
            return new BehandlingStegModellImpl(behandlingModell, instance, stegType);
        }

    }

    @SuppressWarnings("resource")
    public static BehandlingModellBuilder builder(BehandlingType behandlingType, FagsakYtelseType fagsakYtelseType) {
        return new BehandlingModellBuilder(new BehandlingModellImpl(behandlingType, fagsakYtelseType, false));
    }

    public static class BehandlingModellBuilder {
        private BehandlingModellImpl modell;

        public BehandlingModellBuilder(BehandlingModellImpl modell) {
            this.modell = modell;
        }

        public BehandlingModell build() {
            var b = modell;
            modell = null;
            return b;
        }

        public BehandlingModellBuilder medSteg(BehandlingStegType... stegTyper) {
            modell.leggTil(stegTyper);
            return this;
        }
    }
}
