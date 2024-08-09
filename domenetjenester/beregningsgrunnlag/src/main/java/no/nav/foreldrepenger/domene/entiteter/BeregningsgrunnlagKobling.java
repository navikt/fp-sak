package no.nav.foreldrepenger.domene.entiteter;


import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.domene.typer.Beløp;

@Entity(name = "BeregningsgrunnlagKobling")
@Table(name = "BG_KOBLING")
public class BeregningsgrunnlagKobling extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BG_KOBLING")
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

    protected BeregningsgrunnlagKobling() {
        // Hibernate
    }

    public BeregningsgrunnlagKobling(Long behandlingId, UUID koblingUuid) {
        this.koblingUuid = koblingUuid;
        this.behandlingId = behandlingId;
    }

    public Beløp getGrunnbeløp() {
        return grunnbeløp;
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public UUID getKoblingUuid() {
        return koblingUuid;
    }

    void oppdaterMedGrunnbeløp(Beløp grunnbeløp) {
        if (this.grunnbeløp != null) {
            throw new IllegalArgumentException("Grunnbeløp er allerede satt, skal ikke oppdateres");
        }
        this.grunnbeløp = Objects.requireNonNull(grunnbeløp, "grunnbeløp");
    }

    void oppdaterMedSkjæringstidspunkt(LocalDate skjæringstidspunkt) {
        if (this.skjæringstidspunkt != null) {
            throw new IllegalArgumentException("Skjæringstidspunkt er allerede satt, skal ikke oppdateres");
        }
        this.skjæringstidspunkt = Objects.requireNonNull(skjæringstidspunkt, "skjæringstidspunkt");
    }

}
