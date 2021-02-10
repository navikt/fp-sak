package no.nav.foreldrepenger.dokumentarkiv;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;

public class ArkivDokument {
    private String dokumentId;
    private String tittel;
    private DokumentTypeId dokumentTypeId;
    private List<ArkivDokumentHentbart> tilgjengeligSom; // hvilke formater som er tilgjengelig fra joark
    private Set<DokumentTypeId> alleDokumenttyper = new HashSet<>(); // sammensatt dokument der vedlegg er scannet inn i ett dokument

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

    public DokumentTypeId getDokumentType() {
        return dokumentTypeId;
    }

    public void setDokumentType(DokumentTypeId dokumentTypeId) {
        this.dokumentTypeId = dokumentTypeId;
    }

    public Set<DokumentTypeId> getAlleDokumenttyper() {
        return alleDokumenttyper;
    }

    public List<ArkivDokumentHentbart> getTilgjengeligSom() {
        return tilgjengeligSom != null ? tilgjengeligSom : List.of();
    }

    public void setTilgjengeligSom(List<ArkivDokumentHentbart> tilgjengeligSom) {
        this.tilgjengeligSom = tilgjengeligSom;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArkivDokument that = (ArkivDokument) o;
        return Objects.equals(dokumentId, that.dokumentId) &&
            Objects.equals(tittel, that.tittel) &&
            Objects.equals(dokumentTypeId, that.dokumentTypeId);
    }

    @Override
    public int hashCode() {

        return Objects.hash(dokumentId, tittel, dokumentTypeId);
    }

    public static class Builder {
        private final ArkivDokument arkivDokument;

        private Builder() {
            this.arkivDokument = new ArkivDokument();
            this.arkivDokument.setDokumentType(DokumentTypeId.UDEFINERT);
            this.arkivDokument.setTilgjengeligSom(new ArrayList<>());
        }

        public static Builder ny() {
            return new Builder();
        }

        public Builder medDokumentId(String dokumentId) {
            this.arkivDokument.setDokumentId(dokumentId);
            return this;
        }

        public Builder medTittel(String tittel) {
            this.arkivDokument.setTittel(tittel);
            return this;
        }

        public Builder medDokumentTypeId(DokumentTypeId dokumentTypeId) {
            this.arkivDokument.setDokumentType(dokumentTypeId);
            this.arkivDokument.getAlleDokumenttyper().add(dokumentTypeId);
            return this;
        }

        public Builder medAlleDokumenttyper(Set<DokumentTypeId> dokumentTypeId){
            this.arkivDokument.getAlleDokumenttyper().addAll(dokumentTypeId);
            return this;
        }

        public Builder leggTilTilgjengeligFormat(ArkivDokumentHentbart arkivVariant){
            this.arkivDokument.getTilgjengeligSom().add(arkivVariant);
            return this;
        }

        public ArkivDokument build() {
            return this.arkivDokument;
        }

    }
}
