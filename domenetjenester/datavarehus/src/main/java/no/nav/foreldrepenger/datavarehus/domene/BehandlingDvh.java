package no.nav.foreldrepenger.datavarehus.domene;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.NaturalId;

@Entity(name = "BehandlingDvh")
@Table(name = "BEHANDLING_DVH")
public class BehandlingDvh extends DvhBaseEntitet {

    private static final String PAPIR_SØKNAD = "1";
    private static final String DIGITAL_SØKNAD = "0";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BEHANDLING_DVH")
    @Column(name="TRANS_ID")
    private Long id;

    @Column(name = "BEHANDLING_ID", nullable = false)
    private Long behandlingId;

    @Column(name = "FAGSAK_ID", nullable = false)
    private Long fagsakId;

    @Column(name = "SAKSNUMMER")
    private String saksnummer;

    @Column(name = "AKTOER_ID")
    private String aktørId;

    @Column(name = "YTELSE_TYPE")
    private String ytelseType;

    @Column(name = "BEHANDLING_RESULTAT_TYPE")
    private String behandlingResultatType;

    @Column(name = "BEHANDLING_TYPE", nullable = false)
    private String behandlingType;

    @Column(name = "BEHANDLING_STATUS", nullable = false)
    private String behandlingStatus;

    @Column(name = "BEHANDLENDE_ENHET", nullable = false)
    private String behandlendeEnhet;

    @Column(name = "UTLANDSTILSNITT", nullable = false)
    private String utlandstilsnitt;

    @Column(name = "ANSVARLIG_SAKSBEHANDLER")
    private String ansvarligSaksbehandler;

    @Column(name = "ANSVARLIG_BESLUTTER")
    private String ansvarligBeslutter;

    @Column(name = "RELATERT_TIL")
    private Long relatertBehandling;

    @Column(name = "FAMILIE_HENDELSE_TYPE")
    private String familieHendelseType;

    @Column(name = "FOERSTE_STOENADSDAG")
    private LocalDate foersteStoenadsdag;

    @NaturalId
    @Column(name = "uuid")
    private UUID uuid;

    @Column(name = "PAPIR_SOKNAD")
    private String papirSøknad;

    @Column(name = "BEHANDLING_METODE")
    private String behandlingMetode;

    @Column(name = "REVURDERING_AARSAK")
    private String revurderingÅrsak;

    @Column(name = "MOTTATT_TID")
    private LocalDateTime mottattTid;

    @Column(name = "REGISTRERT_TID")
    private LocalDateTime registrertTid;

    @Column(name = "KAN_BEHANDLES_TID")
    private LocalDateTime kanBehandlesTid;

    @Column(name = "FERDIG_BEHANDLET_TID")
    private LocalDateTime ferdigBehandletTid;

    @Column(name = "FORVENTET_OPPSTART_TID")
    private LocalDate forventetOppstartTid;

    @Column(name = "VEDTAK_TID")
    private LocalDateTime vedtakTid;

    @Column(name = "VEDTAK_RESULTAT_TYPE")
    private String vedtakResultatType;

    @Column(name = "UTBETALT_TID")
    private LocalDate utbetaltTid;

    @Column(name = "VILKAR_IKKE_OPPFYLT")
    private String vilkårIkkeOppfylt;


    BehandlingDvh() {
        // Hibernate
    }

    public Long getId() {
        return id;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public UUID getBehandlingUuid() { return uuid; }

    public Long getFagsakId() {
        return fagsakId;
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public String getAktørId() {
        return aktørId;
    }

    public String getYtelseType() {
        return ytelseType;
    }

    public String getBehandlingResultatType() {
        return behandlingResultatType;
    }

    public String getBehandlingType() {
        return behandlingType;
    }

    public String getBehandlingStatus() {
        return behandlingStatus;
    }

    public String getBehandlendeEnhet() {
        return behandlendeEnhet;
    }

    public String getUtlandstilsnitt() {
        return utlandstilsnitt;
    }

    public String getAnsvarligSaksbehandler() {
        return ansvarligSaksbehandler;
    }

    public String getAnsvarligBeslutter() {
        return ansvarligBeslutter;
    }

    public Long getRelatertBehandling() {
        return relatertBehandling;
    }

    public String getFamilieHendelseType() {
        return familieHendelseType;
    }

    public LocalDate getFoersteStoenadsdag() {
        return foersteStoenadsdag;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Boolean getPapirSøknad() {
        return papirSøknad != null ? PAPIR_SØKNAD.equals(papirSøknad) : null;
    }

    public String getBehandlingMetode() {
        return behandlingMetode;
    }

    public String getRevurderingÅrsak() {
        return revurderingÅrsak;
    }

    public LocalDateTime getMottattTid() {
        return mottattTid;
    }

    public LocalDateTime getRegistrertTid() {
        return registrertTid;
    }

    public LocalDateTime getKanBehandlesTid() {
        return kanBehandlesTid;
    }

    public LocalDateTime getFerdigBehandletTid() {
        return ferdigBehandletTid;
    }

    public LocalDate getForventetOppstartTid() {
        return forventetOppstartTid;
    }

    public LocalDateTime getVedtakTid() {
        return vedtakTid;
    }

    public String getVedtakResultatType() {
        return vedtakResultatType;
    }

    public LocalDate getUtbetaltTid() {
        return utbetaltTid;
    }

    public String getVilkårIkkeOppfylt() {
        return vilkårIkkeOppfylt;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BehandlingDvh other)) {
            return false;
        }
        if (!super.equals(obj)) {
            return false;
        }
        return Objects.equals(behandlingId, other.behandlingId)
                && Objects.equals(fagsakId, other.fagsakId)
                && Objects.equals(saksnummer, other.saksnummer)
                && Objects.equals(aktørId, other.aktørId)
                && Objects.equals(ytelseType, other.ytelseType)
                && Objects.equals(behandlingResultatType, other.behandlingResultatType)
                && Objects.equals(behandlingType, other.behandlingType)
                && Objects.equals(behandlingStatus, other.behandlingStatus)
                && Objects.equals(behandlendeEnhet, other.behandlendeEnhet)
                && Objects.equals(utlandstilsnitt, other.utlandstilsnitt)
                && Objects.equals(ansvarligSaksbehandler, other.ansvarligSaksbehandler)
                && Objects.equals(ansvarligBeslutter, other.ansvarligBeslutter)
                && Objects.equals(relatertBehandling, other.relatertBehandling)
                && Objects.equals(familieHendelseType, other.familieHendelseType)
                && Objects.equals(foersteStoenadsdag, other.foersteStoenadsdag)
                && Objects.equals(uuid, other.uuid)
                && Objects.equals(papirSøknad, other.papirSøknad)
                && Objects.equals(behandlingMetode, other.behandlingMetode)
                && Objects.equals(revurderingÅrsak, other.revurderingÅrsak)
                && Objects.equals(mottattTid, other.mottattTid)
                && Objects.equals(registrertTid, other.registrertTid)
                && Objects.equals(kanBehandlesTid, other.kanBehandlesTid)
                && Objects.equals(ferdigBehandletTid, other.ferdigBehandletTid)
                && Objects.equals(forventetOppstartTid, other.forventetOppstartTid)
                && Objects.equals(vedtakTid, other.vedtakTid)
                && Objects.equals(utbetaltTid, other.utbetaltTid)
                && Objects.equals(vedtakResultatType, other.vedtakResultatType)
                && Objects.equals(vilkårIkkeOppfylt, other.vilkårIkkeOppfylt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), behandlingId, fagsakId, behandlingResultatType, behandlingType,
            behandlingStatus, behandlendeEnhet, utlandstilsnitt, ansvarligSaksbehandler, ansvarligBeslutter,
            familieHendelseType, foersteStoenadsdag,papirSøknad,
            behandlingMetode, revurderingÅrsak, mottattTid, registrertTid, kanBehandlesTid, ferdigBehandletTid, forventetOppstartTid,
            vedtakTid, utbetaltTid, vedtakResultatType, vilkårIkkeOppfylt, saksnummer, aktørId, ytelseType);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long behandlingId;
        private Long fagsakId;
        private String saksnummer;
        private String aktørId;
        private String ytelseType;
        private String behandlingResultatType;
        private String behandlingType;
        private String behandlingStatus;
        private String behandlendeEnhet;
        private String utlandstilsnitt;
        private String ansvarligSaksbehandler;
        private String ansvarligBeslutter;
        private LocalDateTime funksjonellTid;
        private String endretAv;
        private Long relatertBehandling;
        private String familieHendelseType;
        private LocalDate foersteStoenadsdag;
        private UUID uuid;
        private String papirSøknad;
        private String behandlingMetode;
        private String revurderingÅrsak;
        private LocalDateTime mottattTid;
        private LocalDateTime registrertTid;
        private LocalDateTime kanBehandlesTid;
        private LocalDateTime ferdigBehandletTid;
        private LocalDate forventetOppstartTid;
        private LocalDateTime vedtakTid;
        private String vedtakResultatType;
        private LocalDate utbetaltTid;
        private String vilkårIkkeOppfylt;

        public Builder behandlingId(Long behandlingId) {
            this.behandlingId = behandlingId;
            return this;
        }


        public Builder behandlingUuid(UUID uuid) {
            this.uuid = uuid;
            return this;
        }

        public Builder fagsakId(Long fagsakId) {
            this.fagsakId = fagsakId;
            return this;
        }

        public Builder saksnummer(String saksnummer) {
            this.saksnummer = saksnummer;
            return this;
        }

        public Builder aktørId(String aktørId) {
            this.aktørId = aktørId;
            return this;
        }

        public Builder ytelseType(String ytelseType) {
            this.ytelseType = ytelseType;
            return this;
        }

        public Builder behandlingResultatType(String behandlingResultatType) {
            this.behandlingResultatType = behandlingResultatType;
            return this;
        }

        public Builder behandlingType(String behandlingType) {
            this.behandlingType = behandlingType;
            return this;
        }

        public Builder behandlingStatus(String behandlingStatus) {
            this.behandlingStatus = behandlingStatus;
            return this;
        }

        public Builder behandlendeEnhet(String behandlendeEnhet) {
            this.behandlendeEnhet = behandlendeEnhet;
            return this;
        }

        public Builder utlandstilsnitt(String utlandstilsnitt) {
            this.utlandstilsnitt = utlandstilsnitt;
            return this;
        }

        public Builder ansvarligSaksbehandler(String ansvarligSaksbehandler) {
            this.ansvarligSaksbehandler = ansvarligSaksbehandler;
            return this;
        }

        public Builder ansvarligBeslutter(String ansvarligBeslutter) {
            this.ansvarligBeslutter = ansvarligBeslutter;
            return this;
        }

        public Builder funksjonellTid(LocalDateTime funksjonellTid) {
            this.funksjonellTid = funksjonellTid;
            return this;
        }

        public Builder endretAv(String endretAv) {
            this.endretAv = endretAv;
            return this;
        }

        public Builder relatertBehandling(Long behandlingId) {
            this.relatertBehandling = behandlingId;
            return this;
        }

        public Builder familieHendelseType(String familieHendelseType) {
            this.familieHendelseType = familieHendelseType;
            return this;
        }

        public Builder medFoersteStoenadsdag(LocalDate foersteStoenadsdag) {
            this.foersteStoenadsdag = foersteStoenadsdag;
            return this;
        }

        public Builder medPapirSøknad(Boolean papirSøknad) {
            this.papirSøknad = papirSøknad != null ? (papirSøknad ? PAPIR_SØKNAD : DIGITAL_SØKNAD) : null;
            return this;
        }

        public Builder medBehandlingMetode(BehandlingMetode behandlingMetode) {
            this.behandlingMetode = behandlingMetode != null ? behandlingMetode.name() : null;
            return this;
        }

        public Builder medRevurderingÅrsak(RevurderingÅrsak revurderingÅrsak) {
            this.revurderingÅrsak = revurderingÅrsak != null ? revurderingÅrsak.name() : null;
            return this;
        }

        public Builder medMottattTid(LocalDateTime mottattTid) {
            this.mottattTid = mottattTid;
            return this;
        }

        public Builder medRegistrertTid(LocalDateTime registrertTid) {
            this.registrertTid = registrertTid;
            return this;
        }

        public Builder medKanBehandlesTid(LocalDateTime kanBehandlesTid) {
            this.kanBehandlesTid = kanBehandlesTid;
            return this;
        }

        public Builder medFerdigBehandletTid(LocalDateTime ferdigBehandletTid) {
            this.ferdigBehandletTid = ferdigBehandletTid;
            return this;
        }

        public Builder medForventetOppstartTid(LocalDate forventetOppstartTid) {
            this.forventetOppstartTid = forventetOppstartTid;
            return this;
        }

        public Builder vedtakTid(LocalDateTime vedtakTid) {
            this.vedtakTid = vedtakTid;
            return this;
        }

        public Builder vedtakResultatType(String vedtakResultatType) {
            this.vedtakResultatType = vedtakResultatType;
            return this;
        }

        public Builder vilkårIkkeOppfylt(VilkårIkkeOppfylt vilkårIkkeOppfylt) {
            this.vilkårIkkeOppfylt = vilkårIkkeOppfylt != null ? vilkårIkkeOppfylt.name() : null;
            return this;
        }

        public Builder utbetaltTid(LocalDate utbetaltTid) {
            this.utbetaltTid = utbetaltTid;
            return this;
        }

        public BehandlingDvh build() {
            var behandlingDvh = new BehandlingDvh();
            behandlingDvh.behandlingId = behandlingId;
            behandlingDvh.fagsakId = fagsakId;
            behandlingDvh.saksnummer = saksnummer;
            behandlingDvh.aktørId = aktørId;
            behandlingDvh.ytelseType = ytelseType;
            behandlingDvh.behandlingResultatType = behandlingResultatType;
            behandlingDvh.behandlingType = behandlingType;
            behandlingDvh.behandlingStatus = behandlingStatus;
            behandlingDvh.behandlendeEnhet = behandlendeEnhet;
            behandlingDvh.utlandstilsnitt = utlandstilsnitt;
            behandlingDvh.ansvarligSaksbehandler = ansvarligSaksbehandler;
            behandlingDvh.ansvarligBeslutter = ansvarligBeslutter;
            behandlingDvh.relatertBehandling = relatertBehandling;
            behandlingDvh.setFunksjonellTid(funksjonellTid);
            behandlingDvh.setEndretAv(endretAv);
            behandlingDvh.familieHendelseType = familieHendelseType;
            behandlingDvh.foersteStoenadsdag = foersteStoenadsdag;
            behandlingDvh.uuid = uuid;
            behandlingDvh.papirSøknad = papirSøknad;
            behandlingDvh.behandlingMetode = behandlingMetode;
            behandlingDvh.revurderingÅrsak = revurderingÅrsak;
            behandlingDvh.mottattTid = mottattTid;
            behandlingDvh.registrertTid = registrertTid;
            behandlingDvh.kanBehandlesTid = kanBehandlesTid;
            behandlingDvh.ferdigBehandletTid = ferdigBehandletTid;
            behandlingDvh.forventetOppstartTid = forventetOppstartTid;
            behandlingDvh.vedtakTid = vedtakTid;
            behandlingDvh.utbetaltTid = utbetaltTid;
            behandlingDvh.vedtakResultatType = vedtakResultatType;
            behandlingDvh.vilkårIkkeOppfylt = vilkårIkkeOppfylt;
            return behandlingDvh;
        }
    }
}
