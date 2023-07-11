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
import java.util.stream.Stream;

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
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
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
 * Modell av behandlingssteg, vurderingspunkter og aksjonspunkter som brukes i
 * evaluering av en prosess for behandling.
 */
public class BehandlingModellImpl implements AutoCloseable, BehandlingModell {

    private static final Logger LOG = LoggerFactory.getLogger(BehandlingModellImpl.class);
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
        Objects.requireNonNull(behandlingType, "behandlingType");
        this.destroyOnClose = destroyOnClose;
        this.behandlingType = behandlingType;
        this.fagsakYtelseType = fagsakYtelseType;
        this.lookup = new CdiLookup(this);
    }

    /* for testing først og fremst. */
    protected BehandlingModellImpl(BehandlingType behandlingType,
            FagsakYtelseType fagsakYtelseType,
            TriFunction<BehandlingStegType, BehandlingType, FagsakYtelseType, BehandlingSteg> lookup) {
        Objects.requireNonNull(behandlingType, "behandlingType");
        Objects.requireNonNull(fagsakYtelseType, "fagsakYtelseType");
        this.fagsakYtelseType = fagsakYtelseType;
        this.behandlingType = behandlingType;
        this.lookup = (stegType, behType, ytType) -> new BehandlingStegModellImpl(this, lookup.apply(stegType, behType, ytType), stegType);
    }

    static BehandlingStegTilstandEndringEvent nyBehandlingStegTilstandEndring(BehandlingskontrollKontekst kontekst,
            BehandlingStegTilstandSnapshot fraTilstand,
            BehandlingStegTilstandSnapshot tilTilstand) {
        return new BehandlingStegTilstandEndringEvent(kontekst, fraTilstand, tilTilstand);
    }

    static BehandlingStegTilstandSnapshot tilBehandlingsStegSnapshot(Optional<BehandlingStegTilstand> tilstand) {
        var stegType = tilstand.map(BehandlingStegTilstand::getBehandlingSteg).orElse(null);
        var status = tilstand.map(BehandlingStegTilstand::getBehandlingStegStatus).orElse(null);
        return getBehandlingStegTilstandSnapshot(tilstand, stegType, status);
    }

    private static BehandlingStegTilstandSnapshot getBehandlingStegTilstandSnapshot(Optional<BehandlingStegTilstand> tilstand,
            BehandlingStegType stegType,
            BehandlingStegStatus status) {
        if (stegType != null) {
            return new BehandlingStegTilstandSnapshot(tilstand.map(BehandlingStegTilstand::getId).orElse(null), stegType, status);
        }
        return null;
    }

    static BehandlingStegOvergangEvent nyBehandlingStegOvergangEvent(BehandlingModell modell,
            BehandlingStegTilstandSnapshot forrigeTilstand,
            BehandlingStegTilstandSnapshot nyTilstand, BehandlingskontrollKontekst kontekst) {

        var stegFørType = forrigeTilstand != null ? forrigeTilstand.getSteg() : null;
        var stegEtterType = nyTilstand != null ? nyTilstand.getSteg() : null;

        var relativForflytning = modell.relativStegForflytning(stegFørType, stegEtterType);

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
    public BehandlingStegModell finnNesteSteg(BehandlingStegType stegType) {
        Objects.requireNonNull(stegType, "stegType");
        var idx = indexOf(stegType);
        if (idx >= steg.size() - 1 || idx < 0) {
            return null;
        }
        return steg.get(idx + 1);
    }

    @Override
    public BehandlingStegModell finnForrigeSteg(BehandlingStegType stegType) {
        Objects.requireNonNull(stegType, "stegType");
        var idx = indexOf(stegType);
        if (idx > 0 && idx < steg.size()) {
            return steg.get(idx - 1);
        }
        return null;
    }

    @Override
    public BehandlingStegModell finnTidligsteStegFor(AksjonspunktDefinisjon aksjonspunkt) {
        return finnTidligsteStegFor(List.of(aksjonspunkt));
    }

    @Override
    public BehandlingStegModell finnTidligsteStegFor(Collection<AksjonspunktDefinisjon> aksjonspunkter) {
        for (var stegModell : steg) {
            var hørerTilSteget = aksjonspunkter.stream().map(AksjonspunktDefinisjon::getBehandlingSteg)
                    .anyMatch(s -> s.getKode().equals(stegModell.getBehandlingStegType().getKode()));
            if (hørerTilSteget) {
                return stegModell;
            }
        }
        return null;
    }

    @Override
    public BehandlingStegModell finnTidligsteStegForAksjonspunktDefinisjon(Collection<AksjonspunktDefinisjon> aksjonspunktDefinisjoner) {
        for (var stegModell : steg) {
            var hørerTilSteget = aksjonspunktDefinisjoner.stream()
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
        Set<BehandlingStegType> stegTyper = new LinkedHashSet<>(Arrays.stream(behandlingStegTyper).filter(Objects::nonNull)
                .toList());

        for (var stegModell : steg) {
            var sjekkSteg = stegModell.getBehandlingStegType();
            if (stegTyper.contains(sjekkSteg)) {
                return stegModell;
            }
        }

        throw new IllegalArgumentException(
                "Utvikler-feil: Ingen av forespurte steg er kjent i BehandlingModell: behandlingType=" + behandlingType
                        + ", forspurteSteg=" + List.of(stegTyper)
        );
    }

    @Override
    public Stream<BehandlingStegModell> hvertSteg() {
        return steg.stream().map(m -> m);
    }

    @Override
    public List<BehandlingStegType> getAlleBehandlingStegTyper() {
        return steg.stream().map(BehandlingStegModellImpl::getBehandlingStegType).toList();
    }

    /**
     * Siden CDI kan ha blitt benyttet programmatisk i oppslag må modellen også
     * avsluttes når den ikke lenger er i bruk.
     */
    @Override
    public void close() {
        if (destroyOnClose) {
            for (var stegModellImpl : steg) {
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
        var idx = indexOf(fraOgMedSteg);
        if (idx < 0) {
            throw new IllegalStateException("BehandlingSteg (fraogmed) " + fraOgMedSteg + ER_IKKE_DEFINERT_BLANT + steg);
        }

        var idxEnd = tilSteg == null ? steg.size() - 1 : indexOf(tilSteg);
        if (idxEnd < 0) {
            throw new IllegalStateException("BehandlingSteg (til) " + tilSteg + ER_IKKE_DEFINERT_BLANT + steg);
        }
        if (idx <= idxEnd) {
            return steg.subList(idx, idxEnd + (inklusivTil ? 1 : 0)).stream().map(e -> e);
        }
        return steg.subList(idxEnd + (inklusivTil ? 1 : 0), idx).stream().map(e -> e);

    }

    @Override
    public Stream<BehandlingStegModell> hvertStegEtter(BehandlingStegType stegType) {
        return internHvertStegEtter(stegType).stream().map(e -> e);
    }

    @Override
    public Optional<BehandlingStegStatus> finnStegStatusFor(BehandlingStegType stegType, Collection<AksjonspunktDefinisjon> aksjonspunkter) {
        var stegModell = internFinnSteg(stegType);
        return stegModell.avledStatus(aksjonspunkter);
    }

    @Override
    public Set<AksjonspunktDefinisjon> finnAksjonspunktDefinisjonerEtter(BehandlingStegType steg) {
        Set<AksjonspunktDefinisjon> set = new LinkedHashSet<>();
        internHvertStegEtter(steg).forEach(s -> {
            set.addAll(s.getInngangAksjonpunkt());
            set.addAll(s.getUtgangAksjonpunkt());
        });
        return Collections.unmodifiableSet(set);
    }

    @Override
    public Set<AksjonspunktDefinisjon> finnAksjonspunktDefinisjonerFraOgMed(BehandlingStegType steg, boolean medInngangOgså) {
        if (steg == null) {
            return Collections.emptySet();
        }

        Set<AksjonspunktDefinisjon> set = new LinkedHashSet<>();

        if (medInngangOgså) {
            set.addAll(finnAksjonspunktDefinisjoner(steg));
        } else {
            set.addAll(finnAksjonspunktDefinisjonerUtgang(steg));
        }

        set.addAll(finnAksjonspunktDefinisjonerEtter(steg));

        return Collections.unmodifiableSet(set);
    }

    @Override
    public Set<AksjonspunktDefinisjon> finnAksjonspunktDefinisjoner(BehandlingStegType stegType) {
        Set<AksjonspunktDefinisjon> set = new LinkedHashSet<>();
        var stegModell = internFinnSteg(stegType);
        set.addAll(stegModell.getInngangAksjonpunkt());
        set.addAll(stegModell.getUtgangAksjonpunkt());
        return set;
    }

    @Override
    public Set<AksjonspunktDefinisjon> finnAksjonspunktDefinisjonerInngang(BehandlingStegType steg) {
        return internFinnSteg(steg).getInngangAksjonpunkt();
    }

    @Override
    public Set<AksjonspunktDefinisjon> finnAksjonspunktDefinisjonerUtgang(BehandlingStegType steg) {
        return internFinnSteg(steg).getUtgangAksjonpunkt();
    }

    protected BehandlingStegModellImpl internFinnSteg(BehandlingStegType stegType) {
        Objects.requireNonNull(stegType, "stegType");
        return steg.get(indexOf(stegType));
    }

    protected List<BehandlingStegModellImpl> internHvertStegEtter(BehandlingStegType stegType) {
        int idx;
        if (stegType == null) {
            idx = 1;
        } else {
            idx = indexOf(stegType);
            if (idx < 0) {
                throw new IllegalStateException("BehandlingSteg " + stegType + ER_IKKE_DEFINERT_BLANT + steg);
            }
            if (idx == steg.size() - 1) {
                return Collections.emptyList();
            }
        }
        return steg.subList(idx + 1, steg.size());
    }

    /**
     * Visit alle steg definert i denne modellen.
     *
     * @param førsteSteg - Kode for første steg vi starter fra. Hvis null, begynn
     *                   fra begynnelsen.
     * @param visitor    - kalles for hvert steg definert
     * @return null hvis alle steg til slutt ble kalt. Eller siste stegType som ble
     *         forsøkt.
     */
    @Override
    public BehandlingStegUtfall prosesserFra(BehandlingStegType førsteSteg, BehandlingModellVisitor visitor) {
        Objects.requireNonNull(visitor, "visitor");

        var idx = førsteSteg == null ? 0 : indexOf(førsteSteg);
        var entry = steg.get(idx);
        while (entry != null) {
            LOG.debug("Prosesserer steg: {}", entry);
            var resultat = visitor.prosesser(entry);

            if (!kanFortsetteTilNeste(resultat)) {
                // bryt flyten, og bli stående på dette steget
                LOG.debug("Avbryter etter steg: {}, transisjon={}", entry, resultat);
                return new BehandlingStegUtfall(entry.getBehandlingStegType(), resultat.getNyStegStatus());
            }

            var transisjon = finnTransisjon(resultat.getTransisjon());
            entry = (BehandlingStegModellImpl) transisjon.nesteSteg(entry);
        }

        // avslutter med null når ikke flere steg igjen.
        LOG.debug("Avslutter, ingen flere steg");
        return null;

    }

    boolean kanFortsetteTilNeste(StegProsesseringResultat resultat) {
        var transisjon = finnTransisjon(resultat.getTransisjon()); // TODO (jol) rydd opp henleggelse. HENLEGGELSE er avbrutt, ikke
                                                                              // framoverført
        return resultat.getNyStegStatus().kanFortsetteTilNeste() || transisjon.getMålstegHvisHopp().isPresent() && !FellesTransisjoner.HENLAGT.getId()
            .equals(transisjon.getId());
    }

    void leggTil(BehandlingStegType stegType, BehandlingType behandlingType, FagsakYtelseType ytelseType) {
        Objects.requireNonNull(stegType, "stegType");

        var entry = lookup.apply(stegType, behandlingType, ytelseType);
        if (entry == null) {
            throw new IllegalArgumentException(
                    "Fant ikke steg for kode: " +
                            stegType.getKode() +
                            ", [behandlingType=" + behandlingType + "]");
        }
        this.steg.add(entry);
        leggTilAksjonspunktDefinisjoner(stegType, entry);
    }

    protected void leggTilAksjonspunktDefinisjoner(BehandlingStegType stegType, BehandlingStegModellImpl entry) {
        AksjonspunktDefinisjon.finnAksjonspunktDefinisjoner(stegType, VurderingspunktType.INN)
                .forEach(entry::leggTilAksjonspunktVurderingInngang);

        AksjonspunktDefinisjon.finnAksjonspunktDefinisjoner(stegType, VurderingspunktType.UT)
                .forEach(entry::leggTilAksjonspunktVurderingUtgang);
    }

    void validerErIkkeAlleredeMappet(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        Objects.requireNonNull(aksjonspunktDefinisjon, "aksjonspunktDefinisjon");

        for (var bsm : this.steg) {
            if (bsm.getInngangAksjonpunkt().contains(aksjonspunktDefinisjon)) {
                throw new IllegalStateException("Aksjonpunktkode [" + aksjonspunktDefinisjon + "] allerede mappet til inngang av " +
                        bsm.getBehandlingStegType().getKode()
                        + " [behandlingType=" + behandlingType + "]");
                // //

            }
            if (bsm.getUtgangAksjonpunkt().contains(aksjonspunktDefinisjon)) {
                throw new IllegalStateException("Aksjonpunktkode [" + aksjonspunktDefinisjon + "] allerede mappet til utgang av " +
                        bsm.getBehandlingStegType().getKode()
                        + " [behandlingType=" + behandlingType + "]");
                // //

            }
        }
    }

    @Override
    public int relativStegForflytning(BehandlingStegType stegFørType, BehandlingStegType stegEtterType) {
        return indexOfNullable(stegEtterType) - indexOfNullable(stegFørType);
    }

    private int indexOfNullable(BehandlingStegType stegType) {
        return stegType == null ? -1 : indexOf(stegType);
    }

    private int indexOf(BehandlingStegType stegType) {
        Objects.requireNonNull(stegType, "stegKode");
        for (int i = 0, max = steg.size(); i < max; i++) {
            BehandlingStegModell bsModell = steg.get(i);
            if (Objects.equals(stegType, bsModell.getBehandlingStegType())) {
                return i;
            }
        }
        throw new IllegalArgumentException("Ukjent behandlingssteg: " + stegType.getKode() +
            ", [behandlingType=" + behandlingType + "]");
    }

    @Override
    public boolean erStegAFørStegB(BehandlingStegType stegA, BehandlingStegType stegB) {
        return indexOfNullable(stegA) < indexOfNullable(stegB);
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

            var instance = CDI.current().select(BehandlingSteg.class, annotationLiteral);
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
