package no.nav.foreldrepenger.behandlingslager.behandling;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.NaturalId;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity(name = "Behandling")
@Table(name = "BEHANDLING")
public class Behandling extends BaseEntitet {

    // Null safe
    private static final Comparator<? extends BaseEntitet> COMPARATOR_OPPRETTET_TID = Comparator
            .comparing(BaseEntitet::getOpprettetTidspunkt, (a, b) -> {
                if (a != null && b != null) {
                    return a.compareTo(b);
                }
                if (a == null && b == null) {
                    return 0;
                }
                return a == null ? -1 : 1;
            });

    // Null safe
    private static final Comparator<? extends BaseEntitet> COMPARATOR_ENDRET_TID = Comparator
            .comparing(BaseEntitet::getEndretTidspunkt, (a, b) -> {
                if (a != null && b != null) {
                    return a.compareTo(b);
                }
                if (a == null && b == null) {
                    return 0;
                }
                return a == null ? -1 : 1;
            });

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BEHANDLING")
    private Long id;

    @NaturalId
    @Column(name = "uuid", nullable = false)
    private UUID uuid;

    @ManyToOne(optional = false)
    @JoinColumn(name = "fagsak_id", nullable = false, updatable = false)
    private Fagsak fagsak;

    @Convert(converter = BehandlingStatus.KodeverdiConverter.class)
    @Column(name = "behandling_status", nullable = false)
    private BehandlingStatus status = BehandlingStatus.OPPRETTET;

    @OneToMany(cascade = { CascadeType.ALL }, orphanRemoval = true, mappedBy = "behandling")
    private List<BehandlingStegTilstand> behandlingStegTilstander = new ArrayList<>(1);

    @Convert(converter = BehandlingType.KodeverdiConverter.class)
    @Column(name = "behandling_type", nullable = false)
    private BehandlingType behandlingType = BehandlingType.UDEFINERT;

    /**
     * Er egentlig OneToOne, men må mappes slik da JPA/Hibernate ikke støtter
     * OneToOne på annet enn shared PK.
     */
    @OneToMany(mappedBy = "behandling")
    private Set<Behandlingsresultat> behandlingsresultat = new HashSet<>(1);

    // CascadeType.ALL + orphanRemoval=true må til for at aksjonspunkter skal bli
    // slettet fra databasen ved fjerning fra HashSet
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "behandling", orphanRemoval = true, cascade = CascadeType.ALL, targetEntity = Aksjonspunkt.class)
    private Set<Aksjonspunkt> aksjonspunkter = new HashSet<>();

    @OneToMany(mappedBy = "behandling")
    private Set<BehandlingÅrsak> behandlingÅrsaker = new HashSet<>(1);

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Convert(converter = StartpunktType.KodeverdiConverter.class)
    @Column(name = "startpunkt_type", nullable = false)
    private StartpunktType startpunkt = StartpunktType.UDEFINERT;

    /**
     * -------------------------------------------------------------- FIXME:
     * Produksjonstyringsinformasjon bør flyttes ut av Behandling klassen. Gjelder
     * feltene under --------------------------------------------------------------
     */
    @Column(name = "opprettet_dato", nullable = false, updatable = false)
    private LocalDateTime opprettetDato;

    @Column(name = "avsluttet_dato")
    private LocalDateTime avsluttetDato;

    @Column(name = "totrinnsbehandling", nullable = false)
    @Convert(converter = BooleanToStringConverter.class)
    private boolean toTrinnsBehandling = false;

    @Column(name = "ansvarlig_saksbehandler")
    private String ansvarligSaksbehandler;

    @Column(name = "ansvarlig_beslutter")
    private String ansvarligBeslutter;

    @Column(name = "behandlende_enhet")
    private String behandlendeEnhet;

    @Column(name = "behandlende_enhet_navn")
    private String behandlendeEnhetNavn;

    @Column(name = "behandlende_enhet_arsak")
    private String behandlendeEnhetÅrsak;

    @Column(name = "behandlingstid_frist", nullable = false)
    private LocalDate behandlingstidFrist;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "aapnet_for_endring", nullable = false)
    private boolean åpnetForEndring = false;

    // Kolonnen SIST_OPPDATERT_TIDSPUNKT aksesseres via 2 direkte queries - søk i koden etter kolonnenavn

    Behandling() {
        // Hibernate
    }

    private Behandling(Fagsak fagsak, BehandlingType type) {
        Objects.requireNonNull(fagsak, "Behandling må tilknyttes parent Fagsak");
        this.fagsak = fagsak;
        if (type != null) {
            this.behandlingType = type;
        }

        // generer ny behandling uuid
        this.uuid = UUID.randomUUID();
    }

    /**
     * Skal kun brukes av BehandlingskontrollTjeneste for prod kode slik at events
     * fyres.
     * <p>
     * Denne oppretter en Builder for å bygge en {@link Behandling}.
     *
     * <h4>NB! BRUKES VED FØRSTE FØRSTEGANGSBEHANDLING</h4>
     * <h4>NB2! FOR TESTER - FORTREKK (ScenarioMorSøkerEngangsstønad) eller
     * (ScenarioFarSøkerEngangsstønad). De forenkler test oppsett</h4>
     * <p>
     * Ved senere behandlinger på samme Fagsak, bruk
     * {@link #fraTidligereBehandling(Behandling, BehandlingType)}.
     */
    public static Behandling.Builder forFørstegangssøknad(Fagsak fagsak) {
        return nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD);
    }

    /**
     * @deprecated Ikke bruk, man klager kun på behandling - ikke direkte på fagsak.
     */
    @Deprecated
    public static Behandling.Builder forKlage(Fagsak fagsak) {
        return nyBehandlingFor(fagsak, BehandlingType.KLAGE);
    }

    /**
     * @deprecated Ikke bruk, man anke kun på behandling - ikke direkte på fagsak.
     */
    @Deprecated
    public static Behandling.Builder forAnke(Fagsak fagsak) {
        return nyBehandlingFor(fagsak, BehandlingType.ANKE);
    }

    /**
     * Skal kun brukes av BehandlingskontrollTjeneste for prod kode slik at events
     * fyres.
     *
     * @see #forFørstegangssøknad(Fagsak)
     */
    public static Builder nyBehandlingFor(Fagsak fagsak, BehandlingType behandlingType) {
        return new Builder(fagsak, behandlingType);
    }

    /**
     * Skal kun brukes av BehandlingskontrollTjeneste for prod kode slik at events
     * fyres.
     * <p>
     * Denne oppretter en Builder for å bygge en {@link Behandling} basert på et
     * eksisterende behandling.
     * <p>
     * Ved Endringssøknad eller REVURD_OPPR er det normalt DENNE som skal brukes.
     * <p>
     * NB! FOR TESTER - FORTREKK (ScenarioMorSøkerEngangsstønad) eller
     * (ScenarioFarSøkerEngangsstønad). De forenkler test oppsett basert på vanlige
     * defaults.
     */
    public static Behandling.Builder fraTidligereBehandling(Behandling forrigeBehandling, BehandlingType behandlingType) {
        return new Builder(forrigeBehandling, behandlingType);
    }

    @SuppressWarnings("unchecked")
    private static <V extends BaseEntitet> Comparator<V> compareOpprettetTid() {
        return (Comparator<V>) COMPARATOR_OPPRETTET_TID;
    }

    @SuppressWarnings("unchecked")
    private static <V extends BaseEntitet> Comparator<V> compareEndretTid() {
        return (Comparator<V>) COMPARATOR_ENDRET_TID;
    }

    /**
     * @deprecated FIXME PFP-1131 Fjern direkte kobling
     *             Behandling->Behandlingsresultat fra entiteter/jpa modell
     */
    @Deprecated
    // (FC) støtter bare ett Behandlingsresultat for en Behandling - JPA har ikke
    // støtte for OneToOne på non-PK
    // kolonne, så emuleres her ved å tømme listen.
    public Behandlingsresultat getBehandlingsresultat() {
        if (this.behandlingsresultat.size() > 1) {
            throw new TekniskException("FP-918665",
                "Ugyldig antall behandlingsresultat, forventer maks 1 per behandling, men har "
                    + behandlingsresultat.size());
        }
        return this.behandlingsresultat.isEmpty() ? null : this.behandlingsresultat.iterator().next();
    }

    public List<BehandlingÅrsak> getBehandlingÅrsaker() {
        return new ArrayList<>(behandlingÅrsaker);
    }

    void leggTilBehandlingÅrsaker(List<BehandlingÅrsak> behandlingÅrsaker) {
        if (erAvsluttet() && erHenlagt()) {
            throw new IllegalStateException("Utvikler-feil: kan ikke legge til årsaker på en behandling som er avsluttet.");
        }
        behandlingÅrsaker.forEach(bå -> {
            bå.setBehandling(this);
            this.behandlingÅrsaker.add(bå);
        });
    }

    public boolean harBehandlingÅrsak(BehandlingÅrsakType behandlingÅrsak) {
        return getBehandlingÅrsaker().stream()
                .map(BehandlingÅrsak::getBehandlingÅrsakType)
                .anyMatch(behandlingÅrsak::equals);
    }

    public boolean harNoenBehandlingÅrsaker(Set<BehandlingÅrsakType> behandlingÅrsaker) {
        return getBehandlingÅrsaker().stream()
            .map(BehandlingÅrsak::getBehandlingÅrsakType)
            .anyMatch(behandlingÅrsaker::contains);
    }

    public Optional<Long> getOriginalBehandlingId() {
        return getBehandlingÅrsaker().stream()
                .map(BehandlingÅrsak::getOriginalBehandlingId)
                .filter(Objects::nonNull)
                .findFirst();
    }

    public boolean erManueltOpprettet() {
        return getBehandlingÅrsaker().stream()
                .anyMatch(BehandlingÅrsak::erManueltOpprettet);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Long getFagsakId() {
        return getFagsak().getId();
    }

    public Saksnummer getSaksnummer() {
        return getFagsak().getSaksnummer();
    }

    public AktørId getAktørId() {
        return getFagsak().getNavBruker().getAktørId();
    }

    public BehandlingStatus getStatus() {
        return status;
    }

    /**
     * Oppdater behandlingssteg og tilhørende status.
     * <p>
     * NB::NB::NB Dette skal normalt kun gjøres fra Behandlingskontroll slik at
     * bokføring og events blir riktig. Er ikke en del av offentlig API.
     *
     * @param stegTilstand - tilstand for steg behandlingen er i
     */
    void oppdaterBehandlingStegOgStatus(BehandlingStegTilstand stegTilstand) {
        Objects.requireNonNull(stegTilstand, "behandlingStegTilstand");

        // legg til ny
        this.behandlingStegTilstander.add(stegTilstand);
        var behandlingSteg = stegTilstand.getBehandlingSteg();
        this.status = behandlingSteg.getDefinertBehandlingStatus();
    }

    /**
     * Marker behandling som avsluttet.
     */
    public void avsluttBehandling() {
        lukkBehandlingStegStatuser(this.behandlingStegTilstander, BehandlingStegStatus.UTFØRT);
        this.status = BehandlingStatus.AVSLUTTET;
        this.avsluttetDato = LocalDateTime.now();
    }

    private void lukkBehandlingStegStatuser(Collection<BehandlingStegTilstand> stegTilstander, BehandlingStegStatus sluttStatusForSteg) {
        stegTilstander.stream()
                .filter(t -> !BehandlingStegStatus.erSluttStatus(t.getBehandlingStegStatus()))
                .forEach(t -> t.setBehandlingStegStatus(sluttStatusForSteg));
    }

    public BehandlingType getType() {
        return behandlingType;
    }

    public LocalDateTime getOpprettetDato() {
        return opprettetDato;
    }

    public LocalDateTime getAvsluttetDato() {
        return avsluttetDato;
    }

    public LocalDate getBehandlingstidFrist() {
        return behandlingstidFrist;
    }

    public void setBehandlingstidFrist(LocalDate behandlingstidFrist) {
        guardTilstandPåBehandling();
        this.behandlingstidFrist = behandlingstidFrist;
    }

    public Optional<BehandlingStegTilstand> getBehandlingStegTilstand() {
        var tilstander = behandlingStegTilstander.stream()
                .filter(t -> !BehandlingStegStatus.erSluttStatus(t.getBehandlingStegStatus()))
                .toList();
        if (tilstander.size() > 1) {
            throw new IllegalStateException("Utvikler-feil: Kan ikke ha flere steg samtidig åpne: " + tilstander);
        }

        return tilstander.isEmpty() ? Optional.empty() : Optional.of(tilstander.get(0));
    }

    public Optional<BehandlingStegTilstand> getSisteBehandlingStegTilstand() {
        // sjekk "ikke-sluttstatuser" først
        var sisteAktive = getBehandlingStegTilstand();

        if (sisteAktive.isPresent()) {
            return sisteAktive;
        }

        Comparator<BehandlingStegTilstand> comparatorOpprettet = compareOpprettetTid();
        Comparator<BehandlingStegTilstand> comparatorEndret = compareEndretTid();
        var comparator = comparatorOpprettet.reversed()
                .thenComparing(Comparator.nullsLast(comparatorEndret).reversed());

        // tar nyeste.
        return behandlingStegTilstander.stream().min(comparator);
    }

    public Optional<BehandlingStegTilstand> getBehandlingStegTilstand(BehandlingStegType stegType) {
        var tilstander = behandlingStegTilstander.stream()
                .filter(t -> !BehandlingStegStatus.erSluttStatus(t.getBehandlingStegStatus())
                        && Objects.equals(stegType, t.getBehandlingSteg()))
                .toList();
        if (tilstander.size() > 1) {
            throw new IllegalStateException(
                    "Utvikler-feil: Kan ikke ha flere steg samtidig åpne for stegType[" + stegType + "]: " + tilstander);
        }

        return tilstander.isEmpty() ? Optional.empty() : Optional.of(tilstander.get(0));
    }

    // Test use only
    public boolean harBehandlingStegTilstandHistorikk(int antall) {
        return behandlingStegTilstander.size() == antall;
    }

    // Test use only
    public List<BehandlingStegStatus> getHistoriskBehandlingStegStatus(BehandlingStegType stegType) {
        return behandlingStegTilstander.stream()
            .filter(bst -> stegType == null || Objects.equals(bst.getBehandlingSteg(), stegType))
            .map(BehandlingStegTilstand::getBehandlingStegStatus)
            .toList();
    }

    public BehandlingStegType getAktivtBehandlingSteg() {
        var stegTilstand = getBehandlingStegTilstand().orElse(null);
        return stegTilstand == null ? null : stegTilstand.getBehandlingSteg();
    }

    /**
     * @deprecated FIXME skal ikke ha public settere, og heller ikke setter for
     *             behandlingsresultat her. Bør gå via repository.
     */
    @Deprecated
    public void setBehandlingresultat(Behandlingsresultat behandlingsresultat) {
        // (FC) støtter bare ett Behandlingsresultat for en Behandling - JPA har ikke
        // støtte for OneToOne på non-PK
        // kolonne, så emuleres her ved å tømme listen.

        this.behandlingsresultat.clear();
        behandlingsresultat.setBehandling(this);
        // kun ett om gangen, mappet på annet enn pk
        this.behandlingsresultat.add(behandlingsresultat);
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof Behandling other)) {
            return false;
        }
        return Objects.equals(getFagsak(), other.getFagsak())
                && Objects.equals(getType(), other.getType())
                && Objects.equals(getOpprettetTidspunkt(), other.getOpprettetTidspunkt());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFagsak(), getType(), getOpprettetTidspunkt());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + (id != null ? "id=" + id + ", " : "") + "fagsak=" + fagsak + ", " + "status=" + status + ", "
            + "type=" + behandlingType + "," + "steg=" + getBehandlingStegTilstand().orElse(null) + "," + "opprettetTs=" + getOpprettetTidspunkt()
            + ">";
    }

    public NavBruker getNavBruker() {
        return getFagsak().getNavBruker();
    }

    public RelasjonsRolleType getRelasjonsRolleType() {
        return getFagsak().getRelasjonsRolleType();
    }

    public String getBehandlendeEnhetÅrsak() {
        return behandlendeEnhetÅrsak;
    }

    public void setBehandlendeEnhetÅrsak(String behandlendeEnhetÅrsak) {
        guardTilstandPåBehandling();
        this.behandlendeEnhetÅrsak = behandlendeEnhetÅrsak;
    }

    public String getBehandlendeEnhet() {
        return behandlendeEnhet;
    }

    public void setBehandlendeEnhet(OrganisasjonsEnhet enhet) {
        guardTilstandPåBehandling();
        this.behandlendeEnhet = enhet.enhetId();
        this.behandlendeEnhetNavn = enhet.enhetNavn();
    }

    public OrganisasjonsEnhet getBehandlendeOrganisasjonsEnhet() {
        return new OrganisasjonsEnhet(behandlendeEnhet, behandlendeEnhetNavn);
    }

    public Fagsak getFagsak() {
        return fagsak;
    }

    /**
     * Internt API, IKKE BRUK.
     */
    void addAksjonspunkt(Aksjonspunkt aksjonspunkt) {
        aksjonspunkter.add(aksjonspunkt);
    }

    public Set<Aksjonspunkt> getAksjonspunkter() {
        return Collections.unmodifiableSet(aksjonspunkter);
    }

    public Optional<Aksjonspunkt> getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon definisjon) {
        return getAksjonspunkterStream()
                .filter(a -> a.getAksjonspunktDefinisjon().equals(definisjon))
                .findFirst();
    }

    public Optional<Aksjonspunkt> getÅpentAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon definisjon) {
        return getÅpneAksjonspunkterStream()
                .filter(a -> a.getAksjonspunktDefinisjon().equals(definisjon))
                .findFirst();
    }

    public Aksjonspunkt getAksjonspunktFor(AksjonspunktDefinisjon definisjon) {
        return getAksjonspunkterStream()
                .filter(a -> a.getAksjonspunktDefinisjon().equals(definisjon))
                .findFirst()
                .orElseThrow(
                    () -> new TekniskException("FP-138032", "Behandling har ikke aksjonspunkt for definisjon " + definisjon.getKode()));
    }

    public List<Aksjonspunkt> getÅpneAksjonspunkter() {
        return getÅpneAksjonspunkterStream()
                .toList();
    }

    public List<Aksjonspunkt> getÅpneAksjonspunkter(AksjonspunktType aksjonspunktType) {
        return getÅpneAksjonspunkterStream()
                .filter(ad -> Objects.equals(aksjonspunktType, ad.getAksjonspunktDefinisjon().getAksjonspunktType()))
                .toList();
    }

    public List<Aksjonspunkt> getÅpneAksjonspunkter(Collection<AksjonspunktDefinisjon> matchKriterier) {
        return getÅpneAksjonspunkterStream()
                .filter(a -> matchKriterier.contains(a.getAksjonspunktDefinisjon()))
                .toList();
    }

    public List<Aksjonspunkt> getAksjonspunkterMedTotrinnskontroll() {
        return getAksjonspunkterStream()
                .filter(a -> !a.erAvbrutt() && a.isToTrinnsBehandling())
                .toList();
    }

    public boolean harAksjonspunktMedType(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return getAksjonspunkterStream()
                .anyMatch(ap -> aksjonspunktDefinisjon.equals(ap.getAksjonspunktDefinisjon()));
    }

    public boolean harAvbruttAksjonspunktMedType(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return getAksjonspunkterStream()
            .filter(ap -> aksjonspunktDefinisjon.equals(ap.getAksjonspunktDefinisjon()))
            .anyMatch(Aksjonspunkt::erAvbrutt);
    }

    public boolean harAvbruttAlleAksjonspunktAvTyper(Set<AksjonspunktDefinisjon> aksjonspunktTyper) {
        var relevanteAksjonspunkter = getAksjonspunkter().stream()
            .filter(ap -> aksjonspunktTyper.contains(ap.getAksjonspunktDefinisjon()))
            .toList();
        if (relevanteAksjonspunkter.isEmpty()) {
            return false;
        }
        return relevanteAksjonspunkter.stream().allMatch(Aksjonspunkt::erAvbrutt);
    }

    public boolean harUtførtAksjonspunktMedType(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return getAksjonspunktMedDefinisjonOptional(aksjonspunktDefinisjon)
            .map(ap -> ap.getStatus().equals(AksjonspunktStatus.UTFØRT)).orElse(false);
    }

    public boolean harÅpentAksjonspunktMedType(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return getÅpneAksjonspunkterStream().map(Aksjonspunkt::getAksjonspunktDefinisjon)
                .anyMatch(aksjonspunktDefinisjon::equals);
    }

    public boolean harAksjonspunktMedTotrinnskontroll() {
        return getAksjonspunkterStream()
                .anyMatch(a -> !a.erAvbrutt() && a.isToTrinnsBehandling());
    }

    private Optional<Aksjonspunkt> getFørsteÅpneAutopunkt() {
        return getÅpneAksjonspunkter(AksjonspunktType.AUTOPUNKT).stream()
                .findFirst();
    }

    public boolean isBehandlingPåVent() {
        return !getÅpneAksjonspunkter(AksjonspunktType.AUTOPUNKT).isEmpty();
    }

    public boolean erKøet() {
        return this.getÅpneAksjonspunkterStream()
                .anyMatch(ap -> AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING.equals(ap.getAksjonspunktDefinisjon()));
    }

    private Stream<Aksjonspunkt> getAksjonspunkterStream() {
        return aksjonspunkter.stream();
    }

    private Stream<Aksjonspunkt> getÅpneAksjonspunkterStream() {
        return getAksjonspunkterStream()
                .filter(Aksjonspunkt::erÅpentAksjonspunkt);
    }

    public Long getVersjon() {
        return versjon;
    }

    public BehandlingStegStatus getBehandlingStegStatus() {
        var stegTilstand = getBehandlingStegTilstand().orElse(null);
        return stegTilstand == null ? null : stegTilstand.getBehandlingStegStatus();
    }

    public boolean isToTrinnsBehandling() {
        return toTrinnsBehandling;
    }

    public void setToTrinnsBehandling() {
        guardTilstandPåBehandling();
        this.toTrinnsBehandling = true;
    }

    public void nullstillToTrinnsBehandling() {
        guardTilstandPåBehandling();
        this.toTrinnsBehandling = false;
    }

    public String getAnsvarligSaksbehandler() {
        return ansvarligSaksbehandler;
    }

    public void setAnsvarligSaksbehandler(String ansvarligSaksbehandler) {
        guardTilstandPåBehandling();
        this.ansvarligSaksbehandler = ansvarligSaksbehandler;
    }

    public String getAnsvarligBeslutter() {
        return ansvarligBeslutter;
    }

    public void setAnsvarligBeslutter(String ansvarligBeslutter) {
        guardTilstandPåBehandling();
        this.ansvarligBeslutter = ansvarligBeslutter;
    }

    public FagsakYtelseType getFagsakYtelseType() {
        return getFagsak().getYtelseType();
    }

    public LocalDate getFristDatoBehandlingPåVent() {
        return getFørsteÅpneAutopunkt().map(Aksjonspunkt::getFristTid).map(LocalDateTime::toLocalDate).orElse(null);
    }

    public AksjonspunktDefinisjon getBehandlingPåVentAksjonspunktDefinisjon() {
        return getFørsteÅpneAutopunkt().map(Aksjonspunkt::getAksjonspunktDefinisjon).orElse(null);
    }

    public Venteårsak getVenteårsak() {
        return getFørsteÅpneAutopunkt().map(Aksjonspunkt::getVenteårsak).orElse(null);
    }

    public boolean erSaksbehandlingAvsluttet() {
        return erAvsluttet() || erUnderIverksettelse() || erHenlagt();
    }

    public boolean erOrdinærSaksbehandlingAvsluttet() {
        return erAvsluttet() || erUnderIverksettelse() || erTilBeslutter();
    }

    private boolean erHenlagt() {
        if (behandlingsresultat == null || behandlingsresultat.isEmpty()) {
            return false;
        }
        return getBehandlingsresultat().isBehandlingHenlagt();
    }

    public boolean erTilBeslutter() {
        return Objects.equals(BehandlingStatus.FATTER_VEDTAK, getStatus());
    }

    public boolean erUnderIverksettelse() {
        return Objects.equals(BehandlingStatus.IVERKSETTER_VEDTAK, getStatus());
    }

    public boolean erAvsluttet() {
        return Objects.equals(BehandlingStatus.AVSLUTTET, getStatus());
    }

    public boolean erStatusFerdigbehandlet() {
        return getStatus().erFerdigbehandletStatus();
    }

    public boolean erRevurdering() {
        return BehandlingType.REVURDERING.equals(getType());
    }

    public boolean erYtelseBehandling() {
        return getType().erYtelseBehandlingType();
    }

    public boolean harSattStartpunkt() {
        return !StartpunktType.UDEFINERT.equals(startpunkt);
    }

    public StartpunktType getStartpunkt() {
        return startpunkt;
    }

    public void setStartpunkt(StartpunktType startpunkt) {
        guardTilstandPåBehandling();
        this.startpunkt = startpunkt;
    }

    public boolean erÅpnetForEndring() {
        return åpnetForEndring;
    }

    public void setÅpnetForEndring(boolean åpnetForEndring) {
        guardTilstandPåBehandling();
        this.åpnetForEndring = åpnetForEndring;
    }

    private void guardTilstandPåBehandling() {
        if (erSaksbehandlingAvsluttet()) {
            throw new IllegalStateException("Utvikler-feil: kan ikke endre tilstand på en behandling som er avsluttet.");
        }
    }

    public static class Builder {

        private final BehandlingType behandlingType;
        private Fagsak fagsak;
        private Behandling forrigeBehandling;
        /**
         * optional
         */
        private Behandlingsresultat.Builder resultatBuilder;

        private LocalDateTime opprettetDato;
        private LocalDateTime avsluttetDato;

        private String behandlendeEnhet;
        private String behandlendeEnhetNavn;
        private String behandlendeEnhetÅrsak;

        private LocalDate behandlingstidFrist = LocalDate.now().plusWeeks(6);

        private BehandlingÅrsak.Builder behandlingÅrsakBuilder;

        private Builder(Fagsak fagsak, BehandlingType behandlingType) {
            this(behandlingType);
            Objects.requireNonNull(fagsak, "fagsak");
            this.fagsak = fagsak;
        }

        private Builder(Behandling forrigeBehandling, BehandlingType behandlingType) {
            this(behandlingType);
            this.forrigeBehandling = forrigeBehandling;
        }

        private Builder(BehandlingType behandlingType) {
            Objects.requireNonNull(behandlingType, "behandlingType");
            this.behandlingType = behandlingType;
        }

        public Builder medKopiAvForrigeBehandlingsresultat() {
            var behandlingsresultatForrige = forrigeBehandling.getBehandlingsresultat();
            this.resultatBuilder = Behandlingsresultat.builderFraEksisterende(behandlingsresultatForrige);
            return this;
        }

        public Builder medBehandlingÅrsak(BehandlingÅrsak.Builder årsakBuilder) {
            this.behandlingÅrsakBuilder = årsakBuilder;
            return this;
        }

        /**
         * Fix opprettet dato.
         */
        public Builder medOpprettetDato(LocalDateTime tid) {
            this.opprettetDato = tid;
            return this;
        }

        /**
         * Fix avsluttet dato.
         */
        public Builder medAvsluttetDato(LocalDateTime tid) {
            this.avsluttetDato = tid;
            return this;
        }

        public Builder medBehandlendeEnhet(OrganisasjonsEnhet enhet) {
            this.behandlendeEnhet = enhet.enhetId();
            this.behandlendeEnhetNavn = enhet.enhetNavn();
            return this;
        }

        public Builder medBehandlendeEnhetÅrsak(String behandlendeEnhetÅrsak) {
            this.behandlendeEnhetÅrsak = behandlendeEnhetÅrsak;
            return this;
        }

        public Builder medBehandlingstidFrist(LocalDate frist) {
            this.behandlingstidFrist = frist;
            return this;
        }

        /**
         * Bygger en Behandling.
         * <p>
         * Husk: Har du brukt riktig Factory metode for å lage en Builder? :
         * <ul>
         * <li>{@link Behandling#fraTidligereBehandling(Behandling, BehandlingType)}
         * (&lt;- BRUK DENNE HVIS DET ER TIDLIGERE BEHANDLINGER PÅ SAMME FAGSAK)</li>
         * <li>{@link Behandling#forFørstegangssøknad(Fagsak)}</li>
         * </ul>
         */
        public Behandling build() {
            Behandling behandling;

            if (forrigeBehandling != null) {
                behandling = new Behandling(forrigeBehandling.getFagsak(), behandlingType);
                if (behandlingstidFrist != null) {
                    behandling.behandlingstidFrist = behandlingstidFrist;
                } else {
                    behandling.behandlingstidFrist = forrigeBehandling.behandlingstidFrist;
                }
            } else {
                behandling = new Behandling(fagsak, behandlingType);
                behandling.behandlingstidFrist = behandlingstidFrist;
            }

            if (behandlendeEnhet != null) {
                behandling.behandlendeEnhet = behandlendeEnhet;
                behandling.behandlendeEnhetNavn = behandlendeEnhetNavn;
                behandling.behandlendeEnhetÅrsak = behandlendeEnhetÅrsak;
            } else if (forrigeBehandling != null) {
                behandling.behandlendeEnhet = forrigeBehandling.behandlendeEnhet;
                behandling.behandlendeEnhetNavn = forrigeBehandling.behandlendeEnhetNavn;
                behandling.behandlendeEnhetÅrsak = forrigeBehandling.behandlendeEnhetÅrsak;
            }

            behandling.opprettetDato = LocalDateTime.now();
            if (opprettetDato != null) {
                behandling.opprettetDato = opprettetDato;
            }
            if (avsluttetDato != null) {
                behandling.avsluttetDato = avsluttetDato;
            }
            if (resultatBuilder != null) {
                var behandlingsresultat = resultatBuilder.buildFor(behandling);
                behandling.setBehandlingresultat(behandlingsresultat);
            }

            if (behandlingÅrsakBuilder != null) {
                behandlingÅrsakBuilder.buildFor(behandling);
            }

            return behandling;
        }
    }

    /*
     * FØLGENDE ER KUN TIL TESTFORMÅL !!!
     */
    public void setStatus(BehandlingStatus status) {
        this.status = status;
    }

    public void setBehandlingType(BehandlingType type) {
        this.behandlingType = type;
    }

    public void setBehandlingStegTilstander(List<BehandlingStegTilstand> tilstander) {
        this.behandlingStegTilstander = tilstander;

    }

    public void setAvsluttetDato(LocalDateTime dato) {
        this.avsluttetDato = dato;

    }

}
