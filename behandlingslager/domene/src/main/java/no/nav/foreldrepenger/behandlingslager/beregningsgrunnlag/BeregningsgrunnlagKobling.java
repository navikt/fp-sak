package no.nav.foreldrepenger.behandlingslager.beregningsgrunnlag;


import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity(name = "BeregningsgrunnlagKobling")
@Table(name = "BG_EKSTERN_KOBLING")
public class BeregningsgrunnlagKobling extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BG_EKSTERN_KOBLING")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Long behandlingId;

    @Embedded
    @AttributeOverride(name = "verdi", column = @Column(name = "grunnbeloep"))
    @ChangeTracked
    private Beløp grunnbeløp;

    @Column(name = "skjaeringstidspunkt")
    private LocalDate skjæringstidspunkt;

    @Column(name = "kobling_uuid", nullable = false, updatable = false, unique = true)
    private UUID koblingUuid;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "reguleringsbehov")
    private Boolean reguleringsbehov;

    protected BeregningsgrunnlagKobling() {
        // Hibernate
    }

    public BeregningsgrunnlagKobling(Long behandlingId, UUID koblingUuid) {
        this.koblingUuid = koblingUuid;
        this.behandlingId = behandlingId;
    }

    BeregningsgrunnlagKobling(Long behandlingId, UUID koblingUuid, LocalDate skjæringstidspunkt, Beløp grunnbeløp, boolean reguleringsbehov) {
        this.koblingUuid = koblingUuid;
        this.behandlingId = behandlingId;
        this.skjæringstidspunkt = skjæringstidspunkt;
        this.grunnbeløp = grunnbeløp;
        this.reguleringsbehov = reguleringsbehov;
    }

    public Optional<Beløp> getGrunnbeløp() {
        return Optional.ofNullable(grunnbeløp);
    }

    public Optional<LocalDate> getSkjæringstidspunkt() {
        return Optional.ofNullable(skjæringstidspunkt);
    }

    public UUID getKoblingUuid() {
        return koblingUuid;
    }

    public Optional<Boolean> getReguleringsbehov() {
        return Optional.ofNullable(reguleringsbehov);
    }

    void oppdaterMedGrunnbeløp(Beløp grunnbeløp) {
        this.grunnbeløp = Objects.requireNonNull(grunnbeløp, "grunnbeløp");
    }

    void oppdaterMedSkjæringstidspunkt(LocalDate skjæringstidspunkt) {
        this.skjæringstidspunkt = Objects.requireNonNull(skjæringstidspunkt, "skjæringstidspunkt");
    }

    void oppdaterMedReguleringsbehov(boolean reguleringsbehov) {
        this.reguleringsbehov = reguleringsbehov;
    }

}
