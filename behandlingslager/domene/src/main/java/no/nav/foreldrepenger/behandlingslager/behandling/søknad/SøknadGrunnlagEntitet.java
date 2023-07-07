package no.nav.foreldrepenger.behandlingslager.behandling.søknad;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity(name = "SøknadGrunnlag")
@Table(name = "GR_SOEKNAD")
class SøknadGrunnlagEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_SOEKNAD")
    private Long id;

    @OneToOne
    @JoinColumn(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Behandling behandling;

    @OneToOne
    @JoinColumn(name = "soeknad_id", nullable = false, updatable = false, unique = true)
    private SøknadEntitet søknad;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    SøknadGrunnlagEntitet() {
    }

    SøknadGrunnlagEntitet(Behandling behandling, SøknadEntitet søknad) {
        this.behandling = behandling;
        this.søknad = søknad;
    }

    void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    public SøknadEntitet getSøknad() {
        return søknad;
    }
}
