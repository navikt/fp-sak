package no.nav.foreldrepenger.datavarehus.domene;

import java.time.LocalDateTime;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

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

    private AnkeVurderingResultatDvh() {
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

    public LocalDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AnkeVurderingResultatDvh that = (AnkeVurderingResultatDvh) o;
        return Objects.equals(ankeBehandlingId, that.ankeBehandlingId) &&
            Objects.equals(ankeVurdering, that.ankeVurdering) &&
            Objects.equals(ankeOmgjørÅrsak, that.ankeOmgjørÅrsak) &&
            Objects.equals(ankeVurderingOmgjør, that.ankeVurderingOmgjør) &&
            Objects.equals(erMerknaderMottatt, that.erMerknaderMottatt) &&
            Objects.equals(gjelderVedtak, that.gjelderVedtak) &&
            Objects.equals(erSubsidiartRealitetsbehandles, that.erSubsidiartRealitetsbehandles) &&
            Objects.equals(erAnkerIkkePart, that.erAnkerIkkePart) &&
            Objects.equals(erFristIkkeOverholdt, that.erFristIkkeOverholdt) &&
            Objects.equals(erIkkeKonkret, that.erIkkeKonkret) &&
            Objects.equals(erIkkeSignert, that.erIkkeSignert) &&
            Objects.equals(opprettetTidspunkt, that.opprettetTidspunkt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), ankeBehandlingId, ankeVurdering, ankeOmgjørÅrsak, ankeVurderingOmgjør, erMerknaderMottatt,
            gjelderVedtak, erSubsidiartRealitetsbehandles, erAnkerIkkePart, erFristIkkeOverholdt, erIkkeKonkret, erIkkeSignert, opprettetTidspunkt);
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

        public AnkeVurderingResultatDvh build() {
            return this.ankeVurderingResultatDvh;
        }
    }
}
