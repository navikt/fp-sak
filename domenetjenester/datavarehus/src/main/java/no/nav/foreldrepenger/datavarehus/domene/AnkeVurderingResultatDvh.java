package no.nav.foreldrepenger.datavarehus.domene;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity(name = "AnkeVurderingResultatDvh")
@Table(name = "ANKE_VURDERING_RESULTAT_DVH")
public class AnkeVurderingResultatDvh extends DvhBaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_ANKE_VURDERING_RES_DVH")
    @Column(name = "TRANS_ID")
    private Long id;

    @Column(name = "anke_behandling_id", nullable = false, updatable = false)
    private Long ankeBehandlingId;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "gjelder_vedtak", nullable = false)
    private boolean gjelderVedtak;

    @Column(name = "ankevurdering")
    private String ankeVurdering;

    @Column(name = "anke_omgjoer_aarsak")
    private String ankeOmgjørÅrsak;

    @Column(name = "anke_vurdering_omgjoer")
    private String ankeVurderingOmgjør;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "er_merknader_mottatt", nullable = false)
    private boolean erMerknaderMottatt;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "er_anker_ikke_part", nullable = false)
    private boolean erAnkerIkkePart;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "er_frist_ikke_overholdt", nullable = false)
    private boolean erFristIkkeOverholdt;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "er_ikke_konkret", nullable = false)
    private boolean erIkkeKonkret;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "er_ikke_signert", nullable = false)
    private boolean erIkkeSignert;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "er_subsidiart_realitet_beh", nullable = false)
    private boolean erSubsidiartRealitetsbehandles;

    @Column(name = "opprettet_tid", nullable = false, updatable = false)
    private LocalDateTime opprettetTidspunkt;

    @Column(name="tr_vurdering")
    private String trygderettVurdering;

    @Column(name="tr_omgjoer_aarsak")
    private String trygderettOmgjørÅrsak;

    @Column(name="tr_vurdering_omgjoer")
    private String trygderettVurderingOmgjør;

    @Column(name = "tr_kjennelse_dato")
    private LocalDate trygderettKjennelseDato;

    AnkeVurderingResultatDvh() {
        // hibernate
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() {
        return id;
    }

    public Long getAnkeBehandlingId() {
        return ankeBehandlingId;
    }

    public boolean gjelderVedtak() {
        return gjelderVedtak;
    }

    public String getAnkeVurdering() {
        return ankeVurdering;
    }

    public String getAnkeOmgjørÅrsak() {
        return ankeOmgjørÅrsak;
    }

    public String getAnkeVurderingOmgjør() {
        return ankeVurderingOmgjør;
    }

    public boolean erMerknaderMottatt() {
        return erMerknaderMottatt;
    }

    public boolean erAnkerIkkePart() {
        return erAnkerIkkePart;
    }

    public boolean erFristIkkeOverholdt() {
        return erFristIkkeOverholdt;
    }

    public boolean erIkkeKonkret() {
        return erIkkeKonkret;
    }

    public boolean erIkkeSignert() {
        return erIkkeSignert;
    }

    public boolean erSubsidiartRealitetsbehandles() {
        return erSubsidiartRealitetsbehandles;
    }

    public String getTrygderettVurdering() {
        return trygderettVurdering;
    }

    public String getTrygderettOmgjørÅrsak() {
        return trygderettOmgjørÅrsak;
    }

    public String getTrygderettVurderingOmgjør() {
        return trygderettVurderingOmgjør;
    }

    public LocalDate getTrygderettKjennelseDato() {
        return trygderettKjennelseDato;
    }

    public LocalDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        var that = (AnkeVurderingResultatDvh) o;
        return gjelderVedtak == that.gjelderVedtak &&
            erMerknaderMottatt == that.erMerknaderMottatt &&
            erAnkerIkkePart == that.erAnkerIkkePart &&
            erFristIkkeOverholdt == that.erFristIkkeOverholdt &&
            erIkkeKonkret == that.erIkkeKonkret &&
            erIkkeSignert == that.erIkkeSignert &&
            erSubsidiartRealitetsbehandles == that.erSubsidiartRealitetsbehandles &&
            Objects.equals(id, that.id) &&
            Objects.equals(ankeBehandlingId, that.ankeBehandlingId) &&
            Objects.equals(ankeVurdering, that.ankeVurdering) &&
            Objects.equals(ankeOmgjørÅrsak, that.ankeOmgjørÅrsak) &&
            Objects.equals(ankeVurderingOmgjør, that.ankeVurderingOmgjør) &&
            Objects.equals(opprettetTidspunkt, that.opprettetTidspunkt) &&
            Objects.equals(trygderettVurdering, that.trygderettVurdering) &&
            Objects.equals(trygderettOmgjørÅrsak, that.trygderettOmgjørÅrsak) &&
            Objects.equals(trygderettVurderingOmgjør, that.trygderettVurderingOmgjør) &&
            Objects.equals(trygderettKjennelseDato, that.trygderettKjennelseDato);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, ankeBehandlingId, gjelderVedtak, ankeVurdering, ankeOmgjørÅrsak, ankeVurderingOmgjør, erMerknaderMottatt,
            erAnkerIkkePart, erFristIkkeOverholdt, erIkkeKonkret, erIkkeSignert, erSubsidiartRealitetsbehandles, opprettetTidspunkt,
            trygderettVurdering, trygderettOmgjørÅrsak, trygderettVurderingOmgjør, trygderettKjennelseDato);
    }

    public static class Builder {
        private AnkeVurderingResultatDvh ankeVurderingResultatDvh = new AnkeVurderingResultatDvh();

        public Builder medAnkeBehandlingId(Long ankeBehandlingId) {
            ankeVurderingResultatDvh.ankeBehandlingId = ankeBehandlingId;
            return this;
        }

        public Builder medAnkeVurdering(String ankeVurdering) {
            ankeVurderingResultatDvh.ankeVurdering = ankeVurdering;
            return this;
        }

        public Builder medAnkeOmgjørÅrsak(String ankeOmgjørÅrsak) {
            ankeVurderingResultatDvh.ankeOmgjørÅrsak = ankeOmgjørÅrsak;
            return this;
        }

        public Builder medAnkeVurderingOmgjør(String ankeVurderingOmgjør) {
            ankeVurderingResultatDvh.ankeVurderingOmgjør = ankeVurderingOmgjør;
            return this;
        }

        public Builder medGjelderVedtak(boolean gjelderVedtak) {
            ankeVurderingResultatDvh.gjelderVedtak = gjelderVedtak;
            return this;
        }

        public Builder medErMerknaderMottatt(boolean erMerknaderMottatt) {
            ankeVurderingResultatDvh.erMerknaderMottatt = erMerknaderMottatt;
            return this;
        }

        public Builder medErSubsidiartRealitetsbehandles(boolean erSubsidiartRealitetsbehandles) {
            ankeVurderingResultatDvh.erSubsidiartRealitetsbehandles = erSubsidiartRealitetsbehandles;
            return this;
        }

        public Builder medErAnkerIkkePart(boolean erAnkerIkkePart) {
            ankeVurderingResultatDvh.erAnkerIkkePart = erAnkerIkkePart;
            return this;
        }

        public Builder medErFristIkkeOverholdt(boolean erFristIkkeOverholdt) {
            ankeVurderingResultatDvh.erFristIkkeOverholdt = erFristIkkeOverholdt;
            return this;
        }

        public Builder medErIkkeKonkret(boolean erIkkeKonkret) {
            ankeVurderingResultatDvh.erIkkeKonkret = erIkkeKonkret;
            return this;
        }

        public Builder medErIkkeSignert(boolean erIkkeSignert) {
            ankeVurderingResultatDvh.erIkkeSignert = erIkkeSignert;
            return this;
        }

        public Builder medOpprettetTidspunkt(LocalDateTime opprettetTidspunkt) {
            ankeVurderingResultatDvh.opprettetTidspunkt = opprettetTidspunkt;
            return this;
        }

        public Builder medTrygderettVurdering(String ankeVurdering) {
            ankeVurderingResultatDvh.trygderettVurdering = ankeVurdering;
            return this;
        }

        public Builder medTrygderettOmgjørÅrsak(String ankeOmgjørÅrsak) {
            ankeVurderingResultatDvh.trygderettOmgjørÅrsak = ankeOmgjørÅrsak;
            return this;
        }

        public Builder medTrygderettVurderingOmgjør(String ankeVurderingOmgjør) {
            ankeVurderingResultatDvh.trygderettVurderingOmgjør = ankeVurderingOmgjør;
            return this;
        }

        public Builder medTrygderettKjennelseDato(LocalDate kjennelseDato) {
            ankeVurderingResultatDvh.trygderettKjennelseDato = kjennelseDato;
            return this;
        }

        public AnkeVurderingResultatDvh build() {
            return this.ankeVurderingResultatDvh;
        }
    }
}
