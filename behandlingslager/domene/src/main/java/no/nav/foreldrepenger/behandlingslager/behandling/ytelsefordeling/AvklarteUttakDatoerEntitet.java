package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;

@Entity(name = "AvklarteUttakDatoerEntitet")
@Table(name = "YF_AVKLART_DATO")
public class AvklarteUttakDatoerEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_YF_AVKLART_DATO")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Column(name = "forste_uttaksdato")
    @ChangeTracked
    private LocalDate førsteUttaksdato;

    @Column(name = "endringsdato")
    @ChangeTracked
    private LocalDate endringsdato;

    @Column(name = "justert_endringsdato")
    private LocalDate justertEndringsdato;

    AvklarteUttakDatoerEntitet() {
    }

    public LocalDate getFørsteUttaksdato() {
        return førsteUttaksdato;
    }

    public LocalDate getGjeldendeEndringsdato() {
        return getJustertEndringsdato() == null ? getOpprinneligEndringsdato() : getJustertEndringsdato();
    }

    public LocalDate getOpprinneligEndringsdato() {
        return endringsdato;
    }

    public LocalDate getJustertEndringsdato() {
        return justertEndringsdato;
    }

    boolean harVerdier() {
        return !(førsteUttaksdato == null && endringsdato == null && justertEndringsdato == null);
    }

    public static class Builder {

        private AvklarteUttakDatoerEntitet kladd = new AvklarteUttakDatoerEntitet();

        public Builder() {
        }

        public Builder(Optional<AvklarteUttakDatoerEntitet> avklarteDatoer) {
            avklarteDatoer.ifPresent(ad -> {
                medFørsteUttaksdato(ad.getFørsteUttaksdato());
                medOpprinneligEndringsdato(ad.getOpprinneligEndringsdato());
                medJustertEndringsdato(ad.getJustertEndringsdato());
            });
        }

        public Builder medFørsteUttaksdato(LocalDate førsteUttaksdato) {
            kladd.førsteUttaksdato = førsteUttaksdato;
            return this;
        }

        public Builder medOpprinneligEndringsdato(LocalDate endringsdato) {
            kladd.endringsdato = endringsdato;
            return this;
        }

        public Builder medJustertEndringsdato(LocalDate justertEndringsdato) {
            kladd.justertEndringsdato = justertEndringsdato;
            return this;
        }

        public AvklarteUttakDatoerEntitet build() {
            return kladd;
        }
    }
}
