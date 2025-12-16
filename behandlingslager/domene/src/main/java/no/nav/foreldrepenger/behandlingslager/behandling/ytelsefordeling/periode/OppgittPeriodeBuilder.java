package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.Årsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

public class OppgittPeriodeBuilder {
    private final OppgittPeriodeEntitet kladd;

    private OppgittPeriodeBuilder() {
        kladd = new OppgittPeriodeEntitet();
    }

    public static OppgittPeriodeBuilder ny() {
        return new OppgittPeriodeBuilder();
    }

    public static OppgittPeriodeBuilder fraEksisterende(OppgittPeriodeEntitet oppgittPeriode) {
        var oppgittPeriodeBuilder = new OppgittPeriodeBuilder()
                .medÅrsak(oppgittPeriode.getÅrsak())
                .medPeriode(oppgittPeriode.getFom(), oppgittPeriode.getTom())
                .medPeriodeType(oppgittPeriode.getPeriodeType())
                .medMorsAktivitet(oppgittPeriode.getMorsAktivitet())
                .medSamtidigUttak(oppgittPeriode.isSamtidigUttak())
                .medSamtidigUttaksprosent(oppgittPeriode.getSamtidigUttaksprosent())
                .medFlerbarnsdager(oppgittPeriode.isFlerbarnsdager())
                .medArbeidsgiver(oppgittPeriode.getArbeidsgiver())
                .medGraderingAktivitetType(oppgittPeriode.getGraderingAktivitetType())
                .medMottattDato(oppgittPeriode.getMottattDato())
                .medTidligstMottattDato(oppgittPeriode.getTidligstMottattDato().orElse(null))
                .medDokumentasjonVurdering(oppgittPeriode.getDokumentasjonVurdering())
                .medPeriodeKilde(oppgittPeriode.getPeriodeKilde());

        if (oppgittPeriode.getArbeidsprosent() != null) {
            oppgittPeriodeBuilder.medArbeidsprosent(oppgittPeriode.getArbeidsprosent());
        }

        oppgittPeriode.getBegrunnelse().ifPresent(oppgittPeriodeBuilder::medBegrunnelse);
        return oppgittPeriodeBuilder;
    }

    public OppgittPeriodeBuilder medÅrsak(Årsak årsak) {
        kladd.setÅrsakType(årsak.getDiskriminator());
        kladd.setÅrsak(årsak);
        return this;
    }

    public OppgittPeriodeBuilder medPeriode(LocalDate fom, LocalDate tom) {
        kladd.setPeriode(fom, tom);
        return this;
    }

    public OppgittPeriodeBuilder medPeriode(DatoIntervallEntitet tidsperiode) {
        kladd.setPeriode(tidsperiode);
        return this;
    }

    public OppgittPeriodeBuilder medPeriodeType(UttakPeriodeType type) {
        kladd.setPeriodeType(type);
        return this;
    }

    public OppgittPeriodeBuilder medArbeidsprosent(BigDecimal arbeidsprosent) {
        Objects.requireNonNull(arbeidsprosent, "arbeidsprosent");
        kladd.setArbeidsprosent(arbeidsprosent);
        return this;
    }

    public OppgittPeriodeBuilder medMorsAktivitet(MorsAktivitet morsAktivitet) {
        kladd.setMorsAktivitet(morsAktivitet);
        return this;
    }

    public OppgittPeriodeBuilder medBegrunnelse(String begrunnelse) {
        kladd.setBegrunnelse(begrunnelse);
        return this;
    }

    public OppgittPeriodeBuilder medArbeidsgiver(Arbeidsgiver arbeidsgiver) {
        kladd.setArbeidsgiver(arbeidsgiver);
        return this;
    }

    public OppgittPeriodeBuilder medGraderingAktivitetType(GraderingAktivitetType type) {
        kladd.setGraderingAktivitetType(type);
        return this;
    }

    public OppgittPeriodeBuilder medSamtidigUttak(boolean samtidigUttak) {
        kladd.setSamtidigUttak(samtidigUttak);
        return this;
    }

    public OppgittPeriodeBuilder medSamtidigUttaksprosent(SamtidigUttaksprosent samtidigUttaksprosent) {
        kladd.setSamtidigUttaksprosent(samtidigUttaksprosent);
        return this;
    }

    public OppgittPeriodeBuilder medFlerbarnsdager(boolean flerbarnsdager) {
        kladd.setFlerbarnsdager(flerbarnsdager);
        return this;
    }

    public OppgittPeriodeBuilder medPeriodeKilde(FordelingPeriodeKilde periodeKilde) {
        kladd.setPeriodeKilde(periodeKilde);
        return this;
    }

    public OppgittPeriodeBuilder medMottattDato(LocalDate mottattDato) {
        kladd.setMottattDato(mottattDato);
        return this;
    }

    public OppgittPeriodeBuilder medTidligstMottattDato(LocalDate tidligstMottattDato) {
        kladd.setTidligstMottattDato(tidligstMottattDato);
        return this;
    }

    public OppgittPeriodeBuilder medDokumentasjonVurdering(DokumentasjonVurdering dokumentasjonVurdering) {
        kladd.setDokumentasjonVurdering(dokumentasjonVurdering);
        return this;
    }

    public OppgittPeriodeEntitet build() {
        Objects.requireNonNull(kladd.getFom(), "fom");
        Objects.requireNonNull(kladd.getTom(), "tom");
        Objects.requireNonNull(kladd.getPeriodeType(), "periodeType");
        Objects.requireNonNull(kladd.getPeriodeKilde(), "periodeKilde");
        if (kladd.getÅrsak() == Årsak.UKJENT && kladd.getPeriodeType() == UttakPeriodeType.UDEFINERT) {
            throw new IllegalStateException("Periode må enten være utsettele, overføring eller uttak");
        }
        if (morsStillingsprosentErSattForAktivetSomIkkeErArbeid()) {
            throw new IllegalStateException("Morsstillingsprosent kan bare være satt når mors aktivetet er ARBEID");
        }
        return kladd;
    }

    private boolean morsStillingsprosentErSattForAktivetSomIkkeErArbeid() {
        if (!MorsAktivitet.ARBEID.equals(kladd.getMorsAktivitet())) {
            return kladd.getDokumentasjonVurdering() != null && kladd.getDokumentasjonVurdering().morsStillingsprosent() != null;
        }
        return false;
    }
}
