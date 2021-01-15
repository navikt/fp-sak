package no.nav.foreldrepenger.behandlingslager.ytelse;

import java.util.Objects;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Beløp;


@Entity(name = "LonnskompVedtakEntitet")
@Table(name = "LONNSKOMP_VEDTAK")
public class LønnskompensasjonVedtak extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_LONNSKOMP_VEDTAK")
    private Long id;

    @ChangeTracked
    @Column(name = "sakid", nullable = false, updatable = false)
    private String sakId;  // Eg en ULID

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "aktoer_id")))
    private AktørId aktørId;

    @ChangeTracked
    @Column(name = "fnr")
    private String fnr;

    @ChangeTracked
    @Embedded
    private OrgNummer orgNummer;

    @Embedded
    @ChangeTracked
    private DatoIntervallEntitet periode;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "verdi", column = @Column(name = "beloep", nullable = false)))
    @ChangeTracked
    private Beløp beløp;

    @Column(name = "aktiv", nullable = false)
    private Boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public LønnskompensasjonVedtak() {
        // hibernate
    }

    public LønnskompensasjonVedtak(LønnskompensasjonVedtak ytelse) {
        this.sakId = ytelse.getSakId();
        this.aktørId = ytelse.getAktørId();
        this.fnr = ytelse.getFnr();
        this.orgNummer = ytelse.getOrgNummer();
        this.periode = ytelse.getPeriode();
        this.beløp = ytelse.getBeløp();
    }

    public Long getId() {
        return id;
    }

    public String getSakId() {
        return sakId;
    }

    public void setSakId(String sakId) {
        this.sakId = sakId;
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    public void setAktørId(AktørId aktørId) {
        this.aktørId = aktørId;
    }

    public String getFnr() {
        return fnr;
    }

    public void setFnr(String fnr) {
        this.fnr = fnr;
    }

    public OrgNummer getOrgNummer() {
        return orgNummer;
    }

    public void setOrgNummer(OrgNummer orgNummer) {
        this.orgNummer = orgNummer;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public void setPeriode(DatoIntervallEntitet periode) {
        this.periode = periode;
    }

    public Beløp getBeløp() {
        return beløp;
    }

    public void setBeløp(Beløp beløp) {
        this.beløp = beløp;
    }

    public boolean getAktiv() {
        return aktiv;
    }

    public void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LønnskompensasjonVedtak that = (LønnskompensasjonVedtak) o;
        return Objects.equals(sakId, that.sakId) &&
            //Objects.equals(fnr, that.fnr) &&
            Objects.equals(aktørId, that.aktørId) &&
            Objects.equals(orgNummer, that.orgNummer) &&
            Objects.equals(periode, that.periode) &&
            Objects.equals(beløp, that.beløp);
    }

    public static boolean erLikForBrukerOrg(LønnskompensasjonVedtak v1, LønnskompensasjonVedtak v2) {
        if (v1 == null && v2 == null) return true;
        if (v1 == null || v2 == null) return false;
        return Objects.equals(v1.aktørId, v2.aktørId) &&
            Objects.equals(v1.orgNummer, v2.orgNummer) &&
            Objects.equals(v1.periode, v2.periode) &&
            Objects.equals(v1.beløp, v2.beløp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sakId, aktørId, orgNummer, periode, beløp);
    }

    @Override
    public String toString() {
        return "LønnskompensasjonVedtak{" +
            "sakId='" + sakId + '\'' +
            ", orgNummer=" + orgNummer +
            ", periode=" + periode +
            ", beløp=" + beløp +
            '}';
    }
}
