package no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving;

import jakarta.persistence.*;
import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

import java.util.Objects;

@Table(name = "TILBAKEKREVING_VALG")
@Entity(name = "TilbakekrevingValgEntitet")
class TilbakekrevingValgEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_TILBAKEKREVING_VALG")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private Long versjon;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "tbk_vilkaar_oppfylt")
    private Boolean vilkarOppfylt;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "grunn_til_reduksjon")
    private Boolean grunnTilReduksjon;

    @Column(name = "varseltekst")
    private String varseltekst;

    @Convert(converter = TilbakekrevingVidereBehandling.KodeverdiConverter.class)
    @Column(name="videre_behandling")
    private TilbakekrevingVidereBehandling tilbakekrevningsVidereBehandling;

    TilbakekrevingValgEntitet() {
        // For hibernate
    }

    public Boolean erVilkarOppfylt() {
        return vilkarOppfylt;
    }

    public Boolean erGrunnTilReduksjon() {
        return grunnTilReduksjon;
    }

    public TilbakekrevingVidereBehandling getTilbakekrevningsVidereBehandling() {
        return tilbakekrevningsVidereBehandling;
    }

    public String getVarseltekst() {
        return varseltekst;
    }

    void deaktiver() {
        this.aktiv = false;
    }

    public static Builder builder() {
        return new Builder();
    }

    static class Builder {
        private TilbakekrevingValgEntitet kladd = new TilbakekrevingValgEntitet();

        public Builder medBehandling(Behandling behandling) {
            kladd.behandlingId = behandling.getId();
            return this;
        }

        public Builder medVilkarOppfylt(Boolean vilkarOppfylt) {
            kladd.vilkarOppfylt = vilkarOppfylt;
            return this;
        }

        public Builder medGrunnTilReduksjon(Boolean grunnTilReduksjon) {
            kladd.grunnTilReduksjon = grunnTilReduksjon;
            return this;
        }

        public Builder medTilbakekrevningsVidereBehandling(TilbakekrevingVidereBehandling tilbakekrevningsVidereBehandling) {
            kladd.tilbakekrevningsVidereBehandling = tilbakekrevningsVidereBehandling;
            return this;
        }

        public Builder medVarseltekst(String varseltekst) {
            kladd.varseltekst = varseltekst;
            return this;
        }

        public TilbakekrevingValgEntitet build() {
            Objects.requireNonNull(kladd.behandlingId, "behandlingId");
            Objects.requireNonNull(kladd.tilbakekrevningsVidereBehandling, "tilbakekrevningsVidereBehandling");
            return kladd;
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<id=" + id
            + ", behandling=" + behandlingId
            + ", aktiv=" + aktiv
            + ">";

    }

}
