package no.nav.foreldrepenger.web.app.tjenester.dokument.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.dokumentarkiv.ArkivDokument;
import no.nav.foreldrepenger.dokumentarkiv.ArkivJournalPost;
import no.nav.foreldrepenger.dokumentarkiv.Kommunikasjonsretning;
import no.nav.foreldrepenger.domene.typer.JournalpostId;

public class DokumentDto {
    @NotNull private JournalpostId journalpostId;
    @NotNull private String dokumentId;
    private List<Long> behandlinger;
    private List<UUID> behandlingUuidList;
    private LocalDateTime tidspunkt;
    private String tittel;
    @NotNull private Kommunikasjonsretning kommunikasjonsretning;
    private String gjelderFor;
    private String arbeidsgiverReferanse;

    public DokumentDto() {
        // trengs for deserialisering av JSON
    }

    public DokumentDto(ArkivJournalPost arkivJournalPost, ArkivDokument arkivDokument) {
        this.journalpostId = arkivJournalPost.getJournalpostId();
        this.dokumentId = arkivDokument.getDokumentId();
        this.tittel = arkivDokument.getTittel();
        this.kommunikasjonsretning = arkivJournalPost.getKommunikasjonsretning();
        this.behandlinger = new ArrayList<>();
        this.behandlingUuidList = new ArrayList<>();
        this.tidspunkt = arkivJournalPost.getTidspunkt();
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    public void setJournalpostId(JournalpostId journalpostId) {
        this.journalpostId = journalpostId;
    }

    public String getDokumentId() {
        return dokumentId;
    }

    public void setDokumentId(String dokumentId) {
        this.dokumentId = dokumentId;
    }

    public String getTittel() {
        return tittel;
    }

    public void setTittel(String tittel) {
        this.tittel = tittel;
    }

    public LocalDateTime getTidspunkt() {
        return tidspunkt;
    }

    public void setTidspunkt(LocalDateTime tidspunkt) {
        this.tidspunkt = tidspunkt;
    }

    public Kommunikasjonsretning getKommunikasjonsretning() {
        return kommunikasjonsretning;
    }

    public void setKommunikasjonsretning(Kommunikasjonsretning kommunikasjonsretning) {
        this.kommunikasjonsretning = kommunikasjonsretning;
    }

    public List<Long> getBehandlinger() {
        return behandlinger;
    }

    public void setBehandlinger(List<Long> behandlinger) {
        this.behandlinger = behandlinger;
    }

    public List<UUID> getBehandlingUuidList() {
        return behandlingUuidList;
    }

    public void setBehandlingUuidList(List<UUID> behandlingUuidList) {
        this.behandlingUuidList = behandlingUuidList;
    }

    public String getGjelderFor() {
        return gjelderFor;
    }

    public void setGjelderFor(String gjelderFor) {
        this.gjelderFor = gjelderFor;
    }

    public String getArbeidsgiverReferanse() {
        return arbeidsgiverReferanse;
    }

    public void setArbeidsgiverReferanse(String arbeidsgiverReferanse) {
        this.arbeidsgiverReferanse = arbeidsgiverReferanse;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (DokumentDto) o;
        return kommunikasjonsretning == that.kommunikasjonsretning &&
            Objects.equals(journalpostId, that.journalpostId) &&
            Objects.equals(dokumentId, that.dokumentId) &&
            Objects.equals(behandlinger, that.behandlinger) &&
            Objects.equals(tidspunkt, that.tidspunkt) &&
            Objects.equals(tittel, that.tittel) &&
            Objects.equals(gjelderFor, that.gjelderFor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(journalpostId, dokumentId, behandlinger, tidspunkt, tidspunkt, tittel, kommunikasjonsretning, gjelderFor);
    }
}
