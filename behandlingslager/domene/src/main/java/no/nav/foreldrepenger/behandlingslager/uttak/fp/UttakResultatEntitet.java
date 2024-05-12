package no.nav.foreldrepenger.behandlingslager.uttak.fp;

import java.util.Objects;

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
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Table(name = "UTTAK_RESULTAT")
@Entity
public class UttakResultatEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UTTAK_RESULTAT")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "opprinnelig_perioder_id", updatable = false, unique = true)
    private UttakResultatPerioderEntitet opprinneligPerioder;

    @ManyToOne
    @JoinColumn(name = "overstyrt_perioder_id", updatable = false, unique = true)
    private UttakResultatPerioderEntitet overstyrtPerioder;

    @ManyToOne
    @JoinColumn(name = "behandling_resultat_id", nullable = false, updatable = false)
    private Behandlingsresultat behandlingsresultat;

    @ManyToOne
    @JoinColumn(name = "konto_beregning_id")
    private Stønadskontoberegning stønadskontoberegning;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    public Long getId() {
        return id;
    }

    public Behandlingsresultat getBehandlingsresultat() {
        return behandlingsresultat;
    }

    public UttakResultatPerioderEntitet getOpprinneligPerioder() {
        return opprinneligPerioder;
    }

    public UttakResultatPerioderEntitet getGjeldendePerioder() {
        if (overstyrtPerioder == null && opprinneligPerioder == null) {
            throw new IllegalStateException("Ingen uttaksperioder er satt");
        }
        return overstyrtPerioder != null ? overstyrtPerioder : opprinneligPerioder;
    }

    public UttakResultatPerioderEntitet getOverstyrtPerioder() {
        return overstyrtPerioder;
    }

    public Stønadskontoberegning getStønadskontoberegning() {
        return stønadskontoberegning;
    }

    public void deaktiver() {
        aktiv = false;
    }

    public static class Builder {
        private UttakResultatEntitet kladd;

        public Builder(Behandlingsresultat behandlingsresultat) {
            Objects.requireNonNull(behandlingsresultat,
                "Må ha behandlingsresultat for å opprette UttakResultatEntitet");
            kladd = new UttakResultatEntitet();
            kladd.behandlingsresultat = behandlingsresultat;
        }

        public Builder medOpprinneligPerioder(UttakResultatPerioderEntitet opprinneligPerioder) {
            Objects.requireNonNull(opprinneligPerioder);
            kladd.opprinneligPerioder = opprinneligPerioder;
            return this;
        }

        public Builder medOverstyrtPerioder(UttakResultatPerioderEntitet overstyrtPerioder) {
            kladd.overstyrtPerioder = overstyrtPerioder;
            return this;
        }

        public Builder medStønadskontoberegning(Stønadskontoberegning stønadskontoberegning) {
            kladd.stønadskontoberegning = stønadskontoberegning;
            return this;
        }

        public Builder nullstill() {
            kladd.opprinneligPerioder = null;
            kladd.overstyrtPerioder = null;
            return this;
        }

        public UttakResultatEntitet build() {
            if (kladd.getOverstyrtPerioder() != null && kladd.getOpprinneligPerioder() == null) {
                var msg = "Behandling må ha eksisterende uttaksresultat ved lagring av manuelt fastsatte perioder. "
                    + "Behandling id " + kladd.behandlingsresultat.getBehandlingId();
                throw new TekniskException("FP-661902", msg);
            }
            return kladd;
        }
    }
}
