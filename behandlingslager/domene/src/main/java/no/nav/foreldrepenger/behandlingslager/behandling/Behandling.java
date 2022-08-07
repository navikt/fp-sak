package no.nav.foreldrepenger.behandlingslager.behandling;

import static java.util.Arrays.asList;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.Table;
import javax.persistence.Version;

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
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.pip.PipBehandlingsData;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@SqlResultSetMappings(value = {
        @SqlResultSetMapping(name = "PipDataResult", classes = {
                @ConstructorResult(targetClass = PipBehandlingsData.class, columns = {
                        @ColumnResult(name = "behandligStatus"),
                        @ColumnResult(name = "ansvarligSaksbehandler"),
                        @ColumnResult(name = "fagsakId"),
                        @ColumnResult(name = "fagsakStatus")
                })
        }),
        @SqlResultSetMapping(name = "BehandlingIdFagsakIdAktoerId", classes = {
                @ConstructorResult(targetClass = BehandlingIdFagsakIdAktorId.class, columns = {
                        @ColumnResult(name = "fagsakYtelseType", type = String.class),
                        @ColumnResult(name = "aktorId", type = String.class),
                        @ColumnResult(name = "saksnummer", type = Saksnummer.class),
                        @ColumnResult(name = "behandlingId", type = Long.class),
                        @ColumnResult(name = "behandlingUuid", type = UUID.class)
                })
        })
})
@Entity(name = "Behandling")
@Table(name = "BEHANDLING")
public class Behandling extends BaseEntitet {

    private static final Set<BehandlingÅrsakType> DØDSHENDELSER = Collections
            .unmodifiableSet(EnumSet.of(BehandlingÅrsakType.RE_HENDELSE_DØD_BARN, BehandlingÅrsakType.RE_HENDELSE_DØDFØDSEL));

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
    @Column(name = "uuid")
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

    @ChangeTracked
    @Convert(converter = Fagsystem.KodeverdiConverter.class)
    @Column(name = "migrert_kilde", nullable = false)
    private Fagsystem migrertKilde = Fagsystem.UDEFINERT;

    Behandling() {
        // Hibernate
    }

    private Behandling(Fagsak fagsak, BehandlingType type) {
        Objects.requireNonNull(fagsak, "Behandling må tilknyttes parent Fagsak"); //$NON-NLS-1$
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
        Objects.requireNonNull(stegTilstand, "behandlingStegTilstand"); //$NON-NLS-1$

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
                .collect(Collectors.toList());
        if (tilstander.size() > 1) {
            throw new IllegalStateException("Utvikler-feil: Kan ikke ha flere steg samtidig åpne: " + tilstander); //$NON-NLS-1$
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
        return behandlingStegTilstander.stream().sorted(comparator).findFirst();
    }

    public Optional<BehandlingStegTilstand> getBehandlingStegTilstand(BehandlingStegType stegType) {
        var tilstander = behandlingStegTilstander.stream()
                .filter(t -> !BehandlingStegStatus.erSluttStatus(t.getBehandlingStegStatus())
                        && Objects.equals(stegType, t.getBehandlingSteg()))
                .collect(Collectors.toList());
        if (tilstander.size() > 1) {
            throw new IllegalStateException(
                    "Utvikler-feil: Kan ikke ha flere steg samtidig åpne for stegType[" + stegType + "]: " + tilstander); //$NON-NLS-1$ //$NON-NLS-2$
        }

        return tilstander.isEmpty() ? Optional.empty() : Optional.of(tilstander.get(0));
    }

    /**
     * @deprecated bygg fortrinnsvis logikk rundt eksistens av stegresultater (fx
     *             vedtaksdato). Slik at man evt kan dekoble tabeller (evt behold en
     *             current her)
     */
    @Deprecated
    public Stream<BehandlingStegTilstand> getBehandlingStegTilstandHistorikk() {
        Comparator<BehandlingStegTilstand> comparator = compareOpprettetTid();
        return behandlingStegTilstander.stream().sorted(comparator);
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
        if (!(object instanceof Behandling)) {
            return false;
        }
        var other = (Behandling) object;
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
        return getClass().getSimpleName() + "<" //$NON-NLS-1$
                + (id != null ? "id=" + id + ", " : "") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                + "fagsak=" + fagsak + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "status=" + status + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "type=" + behandlingType + "," //$NON-NLS-1$ //$NON-NLS-2$
                + "steg=" + (getBehandlingStegTilstand().orElse(null)) + "," //$NON-NLS-1$ //$NON-NLS-2$
                + "opprettetTs=" + getOpprettetTidspunkt() //$NON-NLS-1$
                + ">"; //$NON-NLS-1$
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
                .collect(Collectors.toList());
    }

    public List<Aksjonspunkt> getÅpneAksjonspunkter(AksjonspunktType aksjonspunktType) {
        return getÅpneAksjonspunkterStream()
                .filter(ad -> Objects.equals(aksjonspunktType, ad.getAksjonspunktDefinisjon().getAksjonspunktType()))
                .collect(Collectors.toList());
    }

    public List<Aksjonspunkt> getÅpneAksjonspunkter(Collection<AksjonspunktDefinisjon> matchKriterier) {
        return getÅpneAksjonspunkterStream()
                .filter(a -> matchKriterier.contains(a.getAksjonspunktDefinisjon()))
                .collect(Collectors.toList());
    }

    public List<Aksjonspunkt> getAksjonspunkterMedTotrinnskontroll() {
        return getAksjonspunkterStream()
                .filter(a -> !a.erAvbrutt() && a.isToTrinnsBehandling())
                .collect(Collectors.toList());
    }

    public boolean harAksjonspunktMedType(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return getAksjonspunkterStream()
                .anyMatch(ap -> aksjonspunktDefinisjon.equals(ap.getAksjonspunktDefinisjon()));
    }

    public boolean harÅpentEllerLøstAksjonspunktMedType(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return getAksjonspunktMedDefinisjonOptional(aksjonspunktDefinisjon)
                .map(ap -> ap.getStatus().equals(AksjonspunktStatus.OPPRETTET) || ap.getStatus().equals(AksjonspunktStatus.UTFØRT)).orElse(false);
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
        var aksjonspunkt = getFørsteÅpneAutopunkt();
        LocalDateTime fristTid = null;
        if (aksjonspunkt.isPresent()) {
            fristTid = aksjonspunkt.get().getFristTid();
        }
        return fristTid == null ? null : fristTid.toLocalDate();
    }

    public AksjonspunktDefinisjon getBehandlingPåVentAksjonspunktDefinisjon() {
        var aksjonspunkt = getFørsteÅpneAutopunkt();
        if (aksjonspunkt.isPresent()) {
            return aksjonspunkt.get().getAksjonspunktDefinisjon();
        }
        return null;
    }

    public Venteårsak getVenteårsak() {
        var aksjonspunkt = getFørsteÅpneAutopunkt();
        if (aksjonspunkt.isPresent()) {
            return aksjonspunkt.get().getVenteårsak();
        }
        return null;
    }

    /**
     * @deprecated - fjernes når alle behandlinger har UUID og denne er satt NOT
     *             NULL i db. Inntil da sikrer denne lagring av UUID
     */
    @Deprecated
    @PreUpdate
    protected void onUpdateMigrerUuid() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
    }

    public boolean erSaksbehandlingAvsluttet() {
        return (erAvsluttet() || erUnderIverksettelse() || erHenlagt());
    }

    private boolean erHenlagt() {
        if (behandlingsresultat == null || behandlingsresultat.isEmpty()) {
            return false;
        }
        return getBehandlingsresultat().isBehandlingHenlagt();
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

    public Optional<VilkårType> getVilkårTypeForRelasjonTilBarnet() {
        var resultat = getBehandlingsresultat();
        if (resultat == null) {
            return Optional.empty();
        }
        var vilkårResultat = resultat.getVilkårResultat();
        if (vilkårResultat == null) {
            return Optional.empty();
        }
        var vilkårTyper = asList(VilkårType.FØDSELSVILKÅRET_MOR, VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR,
                VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD, VilkårType.ADOPSJONSVILKARET_FORELDREPENGER,
                VilkårType.OMSORGSVILKÅRET, VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD, VilkårType.FORELDREANSVARSVILKÅRET_4_LEDD);

        return vilkårResultat.getVilkårene().stream()
                .filter(v -> vilkårTyper.contains(v.getVilkårType()))
                .findFirst()
                .map(Vilkår::getVilkårType);
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

    public boolean behandlingSkyldesDødsfall() {
        return behandlingÅrsaker.stream().anyMatch(årsak -> DØDSHENDELSER.contains(årsak.getBehandlingÅrsakType()));
    }

    public void setÅpnetForEndring(boolean åpnetForEndring) {
        guardTilstandPåBehandling();
        this.åpnetForEndring = åpnetForEndring;
    }

    public Fagsystem getMigrertKilde() {
        return migrertKilde;
    }

    public void setMigrertKilde(Fagsystem migrertKilde) {
        guardTilstandPåBehandling();
        this.migrertKilde = migrertKilde;
    }

    private void guardTilstandPåBehandling() {
        if (erSaksbehandlingAvsluttet()) {
            throw new IllegalStateException("Utvikler-feil: kan ikke endre tilstand på en behandling som er avsluttet.");
        }
    }

    @PreRemove
    protected void onDelete() {
        // FIXME: FPFEIL-2799 (FrodeC): Fjern denne når FPFEIL-2799 er godkjent
        throw new IllegalStateException("Skal aldri kunne slette behandling. [id=" + id + ", status=" + getStatus() + ", type=" + getType() + "]");
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
            Objects.requireNonNull(fagsak, "fagsak"); //$NON-NLS-1$
            this.fagsak = fagsak;
        }

        private Builder(Behandling forrigeBehandling, BehandlingType behandlingType) {
            this(behandlingType);
            this.forrigeBehandling = forrigeBehandling;
        }

        private Builder(BehandlingType behandlingType) {
            Objects.requireNonNull(behandlingType, "behandlingType"); //$NON-NLS-1$
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

    public void setBehandlingStegTilstander(List<BehandlingStegTilstand> tilstander) {
        this.behandlingStegTilstander = tilstander;

    }

    public void setAvsluttetDato(LocalDateTime dato) {
        this.avsluttetDato = dato;

    }

}
