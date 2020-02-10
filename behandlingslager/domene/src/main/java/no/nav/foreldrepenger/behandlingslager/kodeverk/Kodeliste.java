package no.nav.foreldrepenger.behandlingslager.kodeverk;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedSubgraph;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.BatchSize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.DiffIgnore;

/**
 * Et innslag i en liste av koder tilgjengelig for et Kodeverk.
 * Koder kan legges til og oppdateres, men tracker ikke endringer over tid (kun av om de er tilgjengelig).
 * <p>
 * Koder skal ikke gjenbrukes, i tråd med anbefalinger fra Kodeverkforvaltningen.Derfor vil kun en
 * gyldighetsperiode vedlikeholdes per kode.
 */
@MappedSuperclass
@Table(name = "KODELISTE")
@DiscriminatorColumn(name = "kodeverk")
@NamedEntityGraph(name = "KodelistMedNavn", attributeNodes = {
        @NamedAttributeNode(value = "kode"),
        @NamedAttributeNode(value = "kodeverk"),
        @NamedAttributeNode(value = "offisiellKode"),
        @NamedAttributeNode(value = "beskrivelse"),
        @NamedAttributeNode(value = "kodelisteNavnI18NList", subgraph = "kodelistNavn")
}, subgraphs = {
        @NamedSubgraph(name = "kodelistNavn", attributeNodes = {
                @NamedAttributeNode(value = "id"),
                @NamedAttributeNode(value = "kodeliste"),
                @NamedAttributeNode(value = "navn"),
                @NamedAttributeNode(value = "språk")
        })
})
public abstract class Kodeliste extends KodeverkBaseEntitet implements Comparable<Kodeliste>, BasisKodeverdi {
    private static final Logger LOG = LoggerFactory.getLogger(Kodeliste.class);
    /**
     * Default fil er samme som property key navn.
     */
    @DiffIgnore // gitt av path
    @Id
    @Column(name = "kodeverk", nullable = false, updatable = false, insertable = false)
    private String kodeverk;

    @DiffIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kodeverk", referencedColumnName = "kode", insertable = false, updatable = false, nullable = false)
    private Kodeverk kodeverkEntitet;

    @ChangeTracked
    @Id
    @Column(name = "kode", nullable = false, updatable = false, insertable = false)
    private String kode;

    /**
     * Kode bestemt av kodeeier. Kan avvike fra intern kodebruk
     */
    @DiffIgnore
    @Column(name = "offisiell_kode", updatable = false, insertable = false)
    private String offisiellKode;

    @DiffIgnore
    @Column(name = "beskrivelse", updatable = false, insertable = false)
    private String beskrivelse;

    /**
     * Når koden gjelder fra og med.
     */
    @DiffIgnore
    @Column(name = "gyldig_fom", nullable = false, updatable = false, insertable = false)
    private LocalDate gyldigFraOgMed = LocalDate.of(2000, 01, 01); // NOSONAR

    /**
     * Når koden gjelder til og med.
     */
    @DiffIgnore
    @Column(name = "gyldig_tom", nullable = false, updatable = false, insertable = false)
    private LocalDate gyldigTilOgMed = LocalDate.of(9999, 12, 31); // NOSONAR

    @DiffIgnore
    @JsonManagedReference
    @OneToMany(mappedBy = "kodeliste", fetch = FetchType.EAGER, cascade = CascadeType.DETACH)
    @BatchSize(size = 1000)
    private List<KodelisteNavnI18N> kodelisteNavnI18NList;

    protected Kodeliste() {
        // proxy for hibernate
    }

    public Kodeliste(String kode, String kodeverk) {
        Objects.requireNonNull(kode, "kode"); //$NON-NLS-1$
        Objects.requireNonNull(kodeverk, "kodeverk"); //$NON-NLS-1$
        this.kode = kode;
        this.kodeverk = kodeverk;
    }

    public Kodeliste(String kode, String kodeverk, String offisiellKode, LocalDate fom, LocalDate tom) {
        this(kode, kodeverk);
        this.offisiellKode = offisiellKode;
        this.gyldigFraOgMed = fom;
        this.gyldigTilOgMed = tom;
    }

    static final String hentLoggedInBrukerSpråk() {
        return "NB"; // TODO(HUMLE): må utvidere til å finne språk til bruker som er logged inn.
    }

    public static List<String> kodeVerdier(Kodeliste... entries) {
        return kodeVerdier(Arrays.asList(entries));
    }

    public static List<String> kodeVerdier(Collection<? extends Kodeliste> entries) {
        return entries.stream().map(k -> k.getKode()).collect(Collectors.toList());
    }

    @Override
    public String getIndexKey() {
        return kodeverk + ":" + kode;
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return offisiellKode;
    }

    public boolean erLikOffisiellKode(String annenOffisiellKode) {
        if (offisiellKode == null) {
            throw new IllegalArgumentException("Har ikke offisiellkode for, Kodeverk=" + getKodeverk() + ", kode=" + getKode()); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return offisiellKode.equals(annenOffisiellKode);
    }

    @Override
    public String getNavn() {
        String navn = null;
        if (kodelisteNavnI18NList != null) {
            String brukerSpråk = hentLoggedInBrukerSpråk();
            for (KodelisteNavnI18N kodelisteNavnI18N : kodelisteNavnI18NList) {
                if (brukerSpråk.equals(kodelisteNavnI18N.getSpråk())) {
                    navn = kodelisteNavnI18N.getNavn();
                    break;
                }
            }
        }

        if (navn == null) {
            LOG.warn("Kodeliste(kode={}, kodeverk={}) mangler navn. Prøver sannsynligvis å hente navn fra konstant.", kode, kodeverk);
        }
        return navn;
    }

    public LocalDate getGyldigFraOgMed() {
        return gyldigFraOgMed;
    }

    public LocalDate getGyldigTilOgMed() {
        return gyldigTilOgMed;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj==null ||!(obj instanceof BasisKodeverdi)) {
            return false;
        }
        BasisKodeverdi other = (BasisKodeverdi) obj;
        return Objects.equals(getKode(), other.getKode())
            && Objects.equals(getKodeverk(), other.getKodeverk());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKode(), getKodeverk());
    }

    @Override
    public int compareTo(Kodeliste that) {
        return that.getKode().compareTo(this.getKode());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
            + "<" //$NON-NLS-1$
            + "kode=" + getKode() //$NON-NLS-1$
            + (offisiellKode == null ? "" : ", offisiellKode=" + offisiellKode) //$NON-NLS-1$
            + ">"; //$NON-NLS-1$
    }

    @Override
    public String getKodeverk() {
        if (kodeverk == null) {
            DiscriminatorValue dc = getClass().getDeclaredAnnotation(DiscriminatorValue.class);
            if (dc != null) {
                kodeverk = dc.value();
            }
        }
        return kodeverk;
    }
}
