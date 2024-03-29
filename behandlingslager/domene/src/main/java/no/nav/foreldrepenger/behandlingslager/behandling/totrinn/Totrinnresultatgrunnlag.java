package no.nav.foreldrepenger.behandlingslager.behandling.totrinn;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity(name = "Totrinnresultatgrunnlag")
@Table(name = "TOTRINNRESULTATGRUNNLAG")
public class Totrinnresultatgrunnlag extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_TOTRINNRESULTATGRUNNLAG")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "behandling_id", nullable = false, updatable = false)
    private Behandling behandling;

    @Column(name = "ytelses_fordeling_grunnlag_id", updatable = false)
    private Long ytelseFordelingGrunnlagEntitetId;

    @Column(name = "uttak_resultat_id", updatable = false)
    private Long uttakResultatEntitetId;

    @Column(name = "beregningsgrunnlag_id", updatable = false)
    private Long beregningsgrunnlagId;

    @Column(name = "iay_grunnlag_uuid", updatable = false)
    private UUID iayGrunnlagUuid;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;


    Totrinnresultatgrunnlag() {
        // for hibernate
    }


    public Totrinnresultatgrunnlag(Behandling behandling,
                                   Long ytelseFordelingGrunnlagEntitetId,
                                   Long uttakResultatEntitetId,
                                   Long beregningsgrunnlagId,
                                   UUID iayGrunnlagUuid) {
        Objects.requireNonNull(behandling);
        this.behandling = behandling;
        this.ytelseFordelingGrunnlagEntitetId = ytelseFordelingGrunnlagEntitetId;
        this.uttakResultatEntitetId = uttakResultatEntitetId;
        this.beregningsgrunnlagId = beregningsgrunnlagId;
        this.iayGrunnlagUuid = iayGrunnlagUuid;
    }

    public Long getId() {
        return id;
    }

    public Optional<Long> getYtelseFordelingGrunnlagEntitetId() {
        return Optional.ofNullable(ytelseFordelingGrunnlagEntitetId);
    }

    public Optional<Long> getUttakResultatEntitetId() {
        return Optional.ofNullable(uttakResultatEntitetId);
    }

    public boolean isAktiv() {
        return aktiv;
    }

    public void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    public Optional<Long> getBeregningsgrunnlagId() {
        return Optional.ofNullable(beregningsgrunnlagId);
    }

    public void setBeregningsgrunnlag(Long beregningsgrunnlagId) {
        this.beregningsgrunnlagId = beregningsgrunnlagId;
    }

    public Optional<UUID> getGrunnlagUuid() {
        return Optional.ofNullable(this.iayGrunnlagUuid);
    }

    public void setGrunnlagUuid(UUID iayGrunnlagUuid) {
        this.iayGrunnlagUuid = iayGrunnlagUuid;
    }

    public Long getBehandlingId() {
        return behandling.getId();
    }
}
