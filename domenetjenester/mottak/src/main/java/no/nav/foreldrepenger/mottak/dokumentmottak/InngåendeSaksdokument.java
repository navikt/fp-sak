package no.nav.foreldrepenger.mottak.dokumentmottak;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentKategori;
import no.nav.foreldrepenger.domene.typer.JournalpostId;

public class InngåendeSaksdokument {

    private Long fagsakId;
    private JournalpostId journalpostId;
    private BehandlingTema behandlingTema;
    private String dokumentTypeId;
    private LocalDate forsendelseMottatt;
    private LocalDateTime forsendelseMottattTidspunkt;
    private Boolean elektroniskSøknad;
    private String payloadXml;
    private BehandlingÅrsakType behandlingÅrsakType;
    private UUID forsendelseId;
    private DokumentKategori dokumentKategori;
    private String kanalreferanse;
    private String journalEnhet;

    private InngåendeSaksdokument() {
        // Skjult.
    }

    public Long getFagsakId() {
        return fagsakId;
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    public BehandlingTema getBehandlingTema() {
        return behandlingTema;
    }

    public String getDokumentTypeId() {
        return dokumentTypeId;
    }

    public LocalDate getForsendelseMottatt() {
        return forsendelseMottatt;
    }

    public LocalDateTime getForsendelseMottattTidspunkt() {
        return forsendelseMottattTidspunkt;
    }

    public Boolean isElektroniskSøknad() {
        return elektroniskSøknad;
    }

    public String getPayloadXml() {
        return payloadXml;
    }

    public BehandlingÅrsakType getBehandlingÅrsakType() {
        return behandlingÅrsakType;
    }

    public UUID getForsendelseId() {
        return forsendelseId;
    }

    public DokumentKategori getDokumentKategori() { return dokumentKategori; }

    public String getKanalreferanse() {
        return kanalreferanse;
    }

    public String getJournalEnhet() {
        return journalEnhet;
    }

    public static Builder builder() {
        return new Builder(new InngåendeSaksdokument());
    }

    public static class Builder {
        private final InngåendeSaksdokument kladd;

        Builder(InngåendeSaksdokument kladd) {
            this.kladd = kladd;
            this.kladd.elektroniskSøknad = Boolean.TRUE;
        }

        public InngåendeSaksdokument.Builder medFagsakId(Long fagsakId) {
            this.kladd.fagsakId = fagsakId;
            return this;
        }

        public InngåendeSaksdokument.Builder medJournalpostId(JournalpostId journalpostId) {
            this.kladd.journalpostId = journalpostId;
            return this;
        }

        public InngåendeSaksdokument.Builder medForsendelseId(UUID forsendelseId) {
            this.kladd.forsendelseId = forsendelseId;
            return this;
        }

        public InngåendeSaksdokument.Builder medBehandlingTema(BehandlingTema behandlingTema) {
            this.kladd.behandlingTema = behandlingTema;
            return this;
        }

        public InngåendeSaksdokument.Builder medDokumentTypeId(String dokumentTypeId) {
            this.kladd.dokumentTypeId = dokumentTypeId;
            return this;
        }

        public InngåendeSaksdokument.Builder medDokumentKategori(DokumentKategori dokumentKategori) {
            this.kladd.dokumentKategori = dokumentKategori;
            return this;
        }

        public InngåendeSaksdokument.Builder medBehandlingÅrsak(BehandlingÅrsakType behandlingÅrsakType) {
            this.kladd.behandlingÅrsakType = behandlingÅrsakType;
            return this;
        }

        public InngåendeSaksdokument.Builder medForsendelseMottatt(LocalDate forsendelseMottatt) {
            this.kladd.forsendelseMottatt = forsendelseMottatt;
            return this;
        }

        public InngåendeSaksdokument.Builder medForsendelseMottatt(LocalDateTime forsendelseMottatt) {
            this.kladd.forsendelseMottattTidspunkt = forsendelseMottatt;
            return this;
        }

        public InngåendeSaksdokument.Builder medElektroniskSøknad(Boolean elektroniskSøknad) {
            this.kladd.elektroniskSøknad = elektroniskSøknad;
            return this;
        }

        public InngåendeSaksdokument.Builder medKanalreferanse(String kanalreferanse) {
            this.kladd.kanalreferanse = kanalreferanse;
            return this;
        }

        public InngåendeSaksdokument.Builder medJournalførendeEnhet(String journalEnhet) {
            this.kladd.journalEnhet = journalEnhet;
            return this;
        }

        public InngåendeSaksdokument.Builder medPayloadXml(String payloadXml) {
            this.kladd.payloadXml = payloadXml;
            return this;
        }

        public InngåendeSaksdokument build() {
            return kladd;
        }
    }
}
