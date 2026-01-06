package no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.InternalUtil;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity(name = "Aksjonspunkt")
@Table(name = "AKSJONSPUNKT")
public class Aksjonspunkt extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_AKSJONSPUNKT")
    private Long id;

    @Column(name = "frist_tid")
    private LocalDateTime fristTid;

    @Enumerated(EnumType.STRING)
    @Column(name = "aksjonspunkt_def", nullable = false, updatable = false)
    private AksjonspunktDefinisjon aksjonspunktDefinisjon;

    @Convert(converter = BehandlingStegType.KodeverdiConverter.class)
    @Column(name = "behandling_steg_funnet")
    private BehandlingStegType behandlingSteg;

    @ManyToOne(optional = false)
    @JoinColumn(name = "behandling_id", nullable = false, updatable = false)
    private Behandling behandling;

    @Convert(converter = AksjonspunktStatus.KodeverdiConverter.class)
    @Column(name = "aksjonspunkt_status", nullable = false)
    private AksjonspunktStatus status;

    @Convert(converter = Venteårsak.KodeverdiConverter.class)
    @Column(name = "vent_aarsak", nullable = false)
    private Venteårsak venteårsak = Venteårsak.UDEFINERT;

    @Version
    @Column(name = "versjon", nullable = false)
    private Long versjon;

    /**
     * Saksbehandler begrunnelse som settes ifm at et aksjonspunkt settes til
     * utført.
     */
    @Column(name = "begrunnelse")
    private String begrunnelse;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "TOTRINN_BEHANDLING", nullable = false)
    private boolean toTrinnsBehandling;

    Aksjonspunkt() {
        // for hibernate
    }

    protected Aksjonspunkt(AksjonspunktDefinisjon aksjonspunktDef, BehandlingStegType behandlingStegFunnet) {
        Objects.requireNonNull(behandlingStegFunnet, "behandlingStegFunnet");
        Objects.requireNonNull(aksjonspunktDef, "aksjonspunktDef");
        this.behandlingSteg = behandlingStegFunnet;
        this.aksjonspunktDefinisjon = aksjonspunktDef;
        this.toTrinnsBehandling = aksjonspunktDef.getDefaultTotrinnBehandling();
        this.status = AksjonspunktStatus.OPPRETTET;
    }

    protected Aksjonspunkt(AksjonspunktDefinisjon aksjonspunktDef) {
        Objects.requireNonNull(aksjonspunktDef, "aksjonspunktDef");
        this.aksjonspunktDefinisjon = aksjonspunktDef;
        this.toTrinnsBehandling = aksjonspunktDef.getDefaultTotrinnBehandling();
        this.status = AksjonspunktStatus.OPPRETTET;
    }

    public Long getId() {
        return id;
    }

    /**
     * Hvorvidt et Aksjonspunkt er av typen Autopunkt.
     * <p>
     * NB: Ikke bruk dette til å styre på vent eller lignende. Bruk egenskapene til
     * Aksjonspunktet i stedet (eks. hvorvidt det har en frist).
     */
    public boolean erAutopunkt() {
        return getAksjonspunktDefinisjon() != null && getAksjonspunktDefinisjon().erAutopunkt();
    }

    public boolean erManueltOpprettet() {
        return this.aksjonspunktDefinisjon.getAksjonspunktType() != null && this.aksjonspunktDefinisjon.getAksjonspunktType().erOverstyringpunkt();
    }

    void setBehandling(Behandling behandling) {
        // brukes kun internt for å koble sammen aksjonspunkt og behandling
        this.behandling = behandling;
    }

    public AksjonspunktDefinisjon getAksjonspunktDefinisjon() {
        return aksjonspunktDefinisjon;
    }

    public AksjonspunktStatus getStatus() {
        return status;
    }

    /**
     * Sett til utført med gitt begrunnelse. Returner true dersom ble endret, false
     * dersom allerede var utfør og hadde samme begrunnelse.
     *
     * @return true hvis status eller begrunnelse er endret.
     */
    public boolean setStatus(AksjonspunktStatus nyStatus, String begrunnelse) {
        var statusEndret = !Objects.equals(getStatus(), nyStatus);

        if (statusEndret) {
            if (Objects.equals(nyStatus, AksjonspunktStatus.UTFØRT)) {
                validerIkkeAvbruttAllerede();
            }

            this.status = nyStatus;
        }

        var begrunnelseEndret = !Objects.equals(getBegrunnelse(), begrunnelse);
        if (begrunnelseEndret) {
            setBegrunnelse(begrunnelse);
        }

        return begrunnelseEndret || statusEndret;
    }

    public BehandlingStegType getBehandlingStegFunnet() {
        return behandlingSteg;
    }

    public LocalDateTime getFristTid() {
        return fristTid;
    }

    void setFristTid(LocalDateTime fristTid) {
        this.fristTid = fristTid;
    }

    public boolean erOpprettet() {
        return Objects.equals(getStatus(), AksjonspunktStatus.OPPRETTET);
    }

    public boolean erÅpentAksjonspunkt() {
        return status.erÅpentAksjonspunkt();
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof Aksjonspunkt kontrollpunkt)) {
            return false;
        }
        return Objects.equals(getAksjonspunktDefinisjon(), kontrollpunkt.getAksjonspunktDefinisjon())
                && Objects.equals(behandling, kontrollpunkt.behandling)
                && Objects.equals(getStatus(), kontrollpunkt.getStatus())
                && Objects.equals(getFristTid(), kontrollpunkt.getFristTid());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAksjonspunktDefinisjon(), behandling, getStatus(), getFristTid());
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public boolean isToTrinnsBehandling() {
        return toTrinnsBehandling || aksjonspunktDefinisjon.getDefaultTotrinnBehandling();
    }

    public boolean kanSetteToTrinnsbehandling() {
        return aksjonspunktDefinisjon.kanSetteTotrinnBehandling();
    }

    void setToTrinnsBehandling(boolean setToTrinnsBehandling) {
        validerIkkeUtførtAvbruttAllerede();
        this.toTrinnsBehandling = aksjonspunktDefinisjon.getDefaultTotrinnBehandling() || setToTrinnsBehandling;
    }

    private void validerIkkeUtførtAvbruttAllerede() {
        if (erUtført() || erAvbrutt()) {
            // TODO (FC): håndteres av låsing allerede? Kaster exception nå for å se om GUI
            // kan være ute av synk.
            throw new IllegalStateException("Forsøkte å bekrefte et allerede lukket aksjonspunkt:" + this);
        }
    }

    private void validerIkkeAvbruttAllerede() {
        if (erAvbrutt()) {
            throw new IllegalStateException("Forsøkte å bekrefte et allerede lukket aksjonspunkt:" + this);
        }
    }

    public Venteårsak getVenteårsak() {
        return venteårsak;
    }

    void setVenteårsak(Venteårsak venteårsak) {
        this.venteårsak = venteårsak;
    }

    /**
     * Intern Builder. Bruk Repository-klasser til å legge til og endre
     * {@link Aksjonspunkt}.
     */
    static class Builder {
        private final Aksjonspunkt aksjonspunkt;

        Builder(AksjonspunktDefinisjon aksjonspunktDefinisjon, BehandlingStegType behandlingStegFunnet) {
            this.aksjonspunkt = new Aksjonspunkt(aksjonspunktDefinisjon, behandlingStegFunnet);
        }

        Builder(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
            this.aksjonspunkt = new Aksjonspunkt(aksjonspunktDefinisjon);
        }

        Aksjonspunkt buildFor(Behandling behandling) {
            var ap = this.aksjonspunkt;
            var eksisterende = behandling.getAksjonspunktMedDefinisjonOptional(ap.aksjonspunktDefinisjon).orElse(null);
            if (eksisterende != null) {
                // Oppdater eksisterende.
                kopierBasisfelter(ap, eksisterende);
                return eksisterende;
            }
            // Opprett ny og knytt til behandling
            ap.setBehandling(behandling);
            InternalUtil.leggTilAksjonspunkt(behandling, ap);
            return ap;
        }

        private void kopierBasisfelter(Aksjonspunkt fra, Aksjonspunkt til) {
            til.setBegrunnelse(fra.getBegrunnelse());
            til.setVenteårsak(fra.getVenteårsak());
            til.setFristTid(fra.getFristTid());
            til.setStatus(fra.getStatus(), fra.getBegrunnelse());
        }

        Aksjonspunkt.Builder medFristTid(LocalDateTime fristTid) {
            aksjonspunkt.setFristTid(fristTid);
            return this;
        }

        Aksjonspunkt.Builder medVenteårsak(Venteårsak venteårsak) {
            aksjonspunkt.setVenteårsak(venteårsak);
            return this;
        }
    }

    public boolean erUtført() {
        return Objects.equals(status, AksjonspunktStatus.UTFØRT);
    }

    public boolean erAvbrutt() {
        return Objects.equals(status, AksjonspunktStatus.AVBRUTT);
    }

    @Override
    public String toString() {
        return "Aksjonspunkt{" +
                "id=" + id +
                ", aksjonspunktDefinisjon=" + getAksjonspunktDefinisjon() +
                ", status=" + status +
                ", behandlingStegFunnet=" + getBehandlingStegFunnet() +
                ", versjon=" + versjon +
                ", toTrinnsBehandling=" + isToTrinnsBehandling() +
                ", fristTid=" + getFristTid() +
                '}';
    }

    void setBehandlingSteg(BehandlingStegType stegType) {
        this.behandlingSteg = stegType;
    }

    public void setId(long id) {
        this.id = id;
    }

}
