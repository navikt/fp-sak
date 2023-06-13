package no.nav.foreldrepenger.behandlingslager.behandling.pleiepenger;

import java.util.Objects;
import java.util.Optional;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@Entity(name = "PleiepengerInnleggelseEntitet")
@Table(name = "PSB_INNLAGT_PERIODE")
public class PleiepengerInnleggelseEntitet extends BaseEntitet implements IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PSB_INNLAGT_PERIODE")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "PSB_PERIODER_ID", nullable = false, updatable = false, unique = true)
    private PleiepengerPerioderEntitet pleiepengerPerioder;

    @Embedded
    @ChangeTracked
    private DatoIntervallEntitet periode;

    @Embedded
    @ChangeTracked
    @AttributeOverrides(@AttributeOverride(name = "saksnummer", column = @Column(name = "psb_saksnummer")))
    private Saksnummer pleiepengerSaksnummer;

    @Embedded
    @ChangeTracked
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "PLEIETRENGENDE_AKTOER_ID", nullable = false, updatable = false)))
    private AktørId pleietrengendeAktørId;


    public PleiepengerInnleggelseEntitet() {
        //jaja
    }

    public PleiepengerInnleggelseEntitet(PleiepengerInnleggelseEntitet innleggelse) {
        this.periode = innleggelse.getPeriode();
        innleggelse.getPleiepengerSaksnummer().ifPresent(s -> this.pleiepengerSaksnummer = s);
        this.pleietrengendeAktørId = innleggelse.getPleietrengendeAktørId();
    }



    public Long getId() {
        return id;
    }

    public PleiepengerPerioderEntitet getPleiepengerPerioder() {
        return pleiepengerPerioder;
    }

    void setPleiepengerPerioder(PleiepengerPerioderEntitet pleiepengerPerioder) {
        this.pleiepengerPerioder = pleiepengerPerioder;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public Optional<Saksnummer> getPleiepengerSaksnummer() {
        return Optional.ofNullable(pleiepengerSaksnummer);
    }

    public AktørId getPleietrengendeAktørId() {
        return pleietrengendeAktørId;
    }

    @Override
    public String getIndexKey() {
        return IndexKey.createKey(periode, pleiepengerSaksnummer, pleietrengendeAktørId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (PleiepengerInnleggelseEntitet) o;
        return Objects.equals(periode, that.periode) &&
            Objects.equals(pleiepengerSaksnummer, that.pleiepengerSaksnummer) &&
            Objects.equals(pleietrengendeAktørId, that.pleietrengendeAktørId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, pleiepengerSaksnummer, pleietrengendeAktørId);
    }

    public static class Builder {

        private PleiepengerInnleggelseEntitet kladd;

        public Builder() {
            this.kladd = new PleiepengerInnleggelseEntitet();
        }

        public Builder medPeriode(DatoIntervallEntitet periode) {
            this.kladd.periode = periode;
            return this;
        }

        public Builder medPleietrengendeAktørId(AktørId aktørId) {
            this.kladd.pleietrengendeAktørId = aktørId;
            return this;
        }

        public Builder medPleiepengerSaksnummer(Saksnummer saksnummer) {
            this.kladd.pleiepengerSaksnummer = saksnummer;
            return this;
        }

        public PleiepengerInnleggelseEntitet build() {
            return this.kladd;
        }
    }
}
