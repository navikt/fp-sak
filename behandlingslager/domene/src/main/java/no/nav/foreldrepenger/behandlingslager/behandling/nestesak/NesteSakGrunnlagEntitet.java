package no.nav.foreldrepenger.behandlingslager.behandling.nestesak;

import jakarta.persistence.*;
import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/*
 * Grunnlag med livssyklus gitt av saker som innvilger ny stønadsperiode som påvirker aktuell saks stønadsperiode
 */
@Entity(name = NesteSakGrunnlagEntitet.GRUNNLAG_NAME)
@Table(name = "GR_NESTESAK")
public class NesteSakGrunnlagEntitet extends BaseEntitet {

    public static final String GRUNNLAG_NAME = "NestesakGrunnlag";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_NESTESAK")
    private Long id;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;

    @Embedded
    @AttributeOverride(name = "saksnummer", column = @Column(name = "saksnummer", updatable = false, nullable=false))
    private Saksnummer saksnummer;

    @Column(name = "startdato", nullable = false)
    @ChangeTracked
    private LocalDate startdato;

    @Column(name = "hendelsedato")
    @ChangeTracked
    private LocalDate hendelsedato;

    NesteSakGrunnlagEntitet() {
    }

    NesteSakGrunnlagEntitet(NesteSakGrunnlagEntitet grunnlag) {
        this.saksnummer = grunnlag.saksnummer;
        this.startdato = grunnlag.startdato;
        this.hendelsedato = grunnlag.hendelsedato;
    }

    public Long getId() {
        return id;
    }

    public long getBehandlingId() {
        return behandlingId;
    }

    public boolean isAktiv() {
        return aktiv;
    }

    public void deaktiver() {
        this.aktiv = false;
    }

    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    public LocalDate getStartdato() {
        return startdato;
    }

    public LocalDate getHendelsedato() {
        return hendelsedato;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NesteSakGrunnlagEntitet that)) return false;
        return Objects.equals(behandlingId, that.behandlingId) &&
            Objects.equals(saksnummer, that.saksnummer) &&
            Objects.equals(startdato, that.startdato) &&
            Objects.equals(hendelsedato, that.hendelsedato);
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandlingId, saksnummer, startdato, hendelsedato);
    }

    public static class Builder {

        private NesteSakGrunnlagEntitet kladd;

        private Builder(NesteSakGrunnlagEntitet kladd) {
            this.kladd = kladd;
        }

        private static Builder nytt() {
            return new Builder(new NesteSakGrunnlagEntitet());
        }

        private static Builder oppdatere(NesteSakGrunnlagEntitet kladd) {
            return new Builder(new NesteSakGrunnlagEntitet(kladd));
        }

        public static Builder oppdatere(Optional<NesteSakGrunnlagEntitet> kladd) {
            return kladd.map(Builder::oppdatere).orElseGet(Builder::nytt);
        }

        public Builder medBehandlingId(Long behandlingId) {
            this.kladd.behandlingId = behandlingId;
            return this;
        }

        public Builder medSaksnummer(Saksnummer saksnummer) {
            this.kladd.saksnummer = saksnummer;
            return this;
        }

        public Builder medStartdato(LocalDate startdato) {
            this.kladd.startdato = startdato;
            return this;
        }

        public Builder medHendelsedato(LocalDate hendelsedato) {
            this.kladd.hendelsedato = hendelsedato;
            return this;
        }

        public NesteSakGrunnlagEntitet build() {
            Objects.requireNonNull(this.kladd.behandlingId, "Utviklerfeil: behandlingId skal være satt");
            Objects.requireNonNull(this.kladd.saksnummer, "Utviklerfeil: saksnummer skal være satt");
            Objects.requireNonNull(this.kladd.startdato, "Utviklerfeil: startdato skal være satt");
            return this.kladd;
        }
    }
}
