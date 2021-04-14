package no.nav.foreldrepenger.domene.iay.modell;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Convert;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.behandlingslager.ytelse.TemaUnderkategori;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

public class Ytelse extends BaseEntitet implements IndexKey {

    private YtelseGrunnlag ytelseGrunnlag;

    @Convert(converter = RelatertYtelseType.KodeverdiConverter.class)
    private RelatertYtelseType relatertYtelseType = RelatertYtelseType.UDEFINERT;

    @ChangeTracked
    private DatoIntervallEntitet periode;

    @ChangeTracked
    private RelatertYtelseTilstand status;

    /**
     * Saksnummer (fra Arena, Infotrygd, ..).
     */
    private Saksnummer saksnummer;

    @ChangeTracked
    @Convert(converter = Fagsystem.KodeverdiConverter.class)
    private Fagsystem kilde;

    @ChangeTracked
    private TemaUnderkategori temaUnderkategori = TemaUnderkategori.UDEFINERT;

    @ChangeTracked
    private Set<YtelseAnvist> ytelseAnvist = new LinkedHashSet<>();

    public Ytelse() {
        // hibernate
    }

    public Ytelse(Ytelse ytelse) {
        this.relatertYtelseType = ytelse.getRelatertYtelseType();
        this.status = ytelse.getStatus();
        this.periode = ytelse.getPeriode();
        this.saksnummer = ytelse.getSaksnummer();
        this.temaUnderkategori = ytelse.getBehandlingsTema();
        this.kilde = ytelse.getKilde();
        ytelse.getYtelseGrunnlag().ifPresent(yg -> {
            var ygn = new YtelseGrunnlag(yg);
            this.ytelseGrunnlag = ygn;
        });
        this.ytelseAnvist = ytelse.getYtelseAnvist().stream().map(ya -> {
            var ytelseAnvist = new YtelseAnvist(ya);
            return ytelseAnvist;
        }).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public String getIndexKey() {
        return IndexKey.createKey(periode, relatertYtelseType, saksnummer);
    }

    public RelatertYtelseType getRelatertYtelseType() {
        return relatertYtelseType;
    }

    void setRelatertYtelseType(RelatertYtelseType relatertYtelseType) {
        this.relatertYtelseType = relatertYtelseType;
    }

    public TemaUnderkategori getBehandlingsTema() {
        return temaUnderkategori;
    }

    void setBehandlingsTema(TemaUnderkategori behandlingsTema) {
        this.temaUnderkategori = behandlingsTema;
    }

    public RelatertYtelseTilstand getStatus() {
        return status;
    }

    void setStatus(RelatertYtelseTilstand status) {
        this.status = status;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    void setPeriode(DatoIntervallEntitet periode) {
        this.periode = periode;
    }

    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    void medSakId(Saksnummer saksnummer) {
        this.saksnummer = saksnummer;
    }

    public Fagsystem getKilde() {
        return kilde;
    }

    void setKilde(Fagsystem kilde) {
        this.kilde = kilde;
    }

    public Optional<YtelseGrunnlag> getYtelseGrunnlag() {
        return Optional.ofNullable(ytelseGrunnlag);
    }

    void setYtelseGrunnlag(YtelseGrunnlag ytelseGrunnlag) {
        if (ytelseGrunnlag != null) {
            this.ytelseGrunnlag = ytelseGrunnlag;
        }
    }

    public Collection<YtelseAnvist> getYtelseAnvist() {
        return Collections.unmodifiableCollection(ytelseAnvist);
    }

    void leggTilYtelseAnvist(YtelseAnvist ytelseAnvist) {
        this.ytelseAnvist.add(ytelseAnvist);

    }

    void tilbakestillAnvisteYtelser() {
        ytelseAnvist.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || !(o instanceof Ytelse)) {
            return false;
        }
        var that = (Ytelse) o;
        return Objects.equals(relatertYtelseType, that.relatertYtelseType) &&
                Objects.equals(temaUnderkategori, that.temaUnderkategori) &&
                Objects.equals(periode, that.periode) &&
                Objects.equals(saksnummer, that.saksnummer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(relatertYtelseType, periode, saksnummer);
    }

    @Override
    public String toString() {
        return "YtelseEntitet{" + //$NON-NLS-1$
                "relatertYtelseType=" + relatertYtelseType + //$NON-NLS-1$
                ", typeUnderkategori=" + temaUnderkategori + //$NON-NLS-1$
                ", periode=" + periode + //$NON-NLS-1$
                ", relatertYtelseStatus=" + status + //$NON-NLS-1$
                ", saksNummer='" + saksnummer + '\'' + //$NON-NLS-1$
                '}';
    }

}
