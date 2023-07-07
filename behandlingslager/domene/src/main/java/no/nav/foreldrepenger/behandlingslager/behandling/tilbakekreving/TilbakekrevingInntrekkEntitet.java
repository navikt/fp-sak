package no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Table(name = "TILBAKEKREVING_INNTREKK")
@Entity
public class TilbakekrevingInntrekkEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_TILBAKEKREVING_INNTREKK")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "avslaatt_inntrekk", nullable = false)
    private boolean avslåttInntrekk;

    TilbakekrevingInntrekkEntitet() {
        // For hibernate
    }

    public Long getId() {
        return id;
    }

    public boolean isAvslåttInntrekk() {
        return avslåttInntrekk;
    }

    void deaktiver() {
        aktiv = false;
    }

    public static class Builder {
        private TilbakekrevingInntrekkEntitet kladd = new TilbakekrevingInntrekkEntitet();

        public Builder medBehandling(Behandling behandling) {
            kladd.behandlingId = behandling.getId();
            return this;
        }

        public Builder medAvslåttInntrekk(boolean avslåttInntrekk) {
            kladd.avslåttInntrekk = avslåttInntrekk;
            return this;
        }

        public TilbakekrevingInntrekkEntitet build() {
            return kladd;
        }

    }
}
