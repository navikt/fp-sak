package no.nav.foreldrepenger.domene.iay.modell;

import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.VirksomhetType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

public class OppgittOpptjeningBuilder {

    private final OppgittOpptjening kladd;

    private OppgittOpptjeningBuilder(OppgittOpptjening kladd) {
        this.kladd = kladd;
    }

    public static OppgittOpptjeningBuilder ny() {
        return new OppgittOpptjeningBuilder(new OppgittOpptjening(UUID.randomUUID()));
    }

    public static OppgittOpptjeningBuilder ny(UUID eksternReferanse, LocalDateTime opprettetTidspunktOriginalt) {
        return new OppgittOpptjeningBuilder(new OppgittOpptjening(eksternReferanse, opprettetTidspunktOriginalt));
    }

    public static OppgittOpptjeningBuilder ny(UUID eksternReferanse, OffsetDateTime opprettetTidspunktOriginalt) {
        return new OppgittOpptjeningBuilder(
                new OppgittOpptjening(eksternReferanse, opprettetTidspunktOriginalt.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()));
    }

    public OppgittOpptjeningBuilder leggTilAnnenAktivitet(OppgittAnnenAktivitet annenAktivitet) {
        this.kladd.leggTilAnnenAktivitet(annenAktivitet);
        return this;
    }

    public OppgittOpptjeningBuilder leggTilFrilansOpplysninger(OppgittFrilans frilans) {
        this.kladd.leggTilFrilans(frilans);
        return this;
    }

    public OppgittOpptjeningBuilder leggTilEgneNæringer(List<EgenNæringBuilder> builders) {
        builders.forEach(builder -> this.kladd.leggTilEgenNæring(builder.build()));
        return this;
    }

    public OppgittOpptjeningBuilder leggTilOppgittArbeidsforhold(OppgittArbeidsforholdBuilder builder) {
        this.kladd.leggTilOppgittArbeidsforhold(builder.build());
        return this;
    }

    public OppgittOpptjening build() {
        return kladd;
    }

    public static class EgenNæringBuilder {
        private final OppgittEgenNæring entitet;

        private EgenNæringBuilder(OppgittEgenNæring entitet) {
            this.entitet = entitet;
        }

        public static EgenNæringBuilder ny() {
            return new EgenNæringBuilder(new OppgittEgenNæring());
        }

        public EgenNæringBuilder medPeriode(DatoIntervallEntitet periode) {
            this.entitet.setPeriode(periode);
            return this;
        }

        public EgenNæringBuilder medVirksomhet(String orgNr) {
            return medVirksomhet(new OrgNummer(orgNr));
        }

        public EgenNæringBuilder medVirksomhetType(VirksomhetType type) {
            this.entitet.setVirksomhetType(type);
            return this;
        }

        public EgenNæringBuilder medRegnskapsførerNavn(String navn) {
            this.entitet.setRegnskapsførerNavn(navn);
            return this;
        }

        public EgenNæringBuilder medRegnskapsførerTlf(String tlf) {
            this.entitet.setRegnskapsførerTlf(tlf);
            return this;
        }

        public EgenNæringBuilder medEndringDato(LocalDate dato) {
            this.entitet.setEndringDato(dato);
            return this;
        }

        public EgenNæringBuilder medBegrunnelse(String begrunnelse) {
            this.entitet.setBegrunnelse(begrunnelse);
            return this;
        }

        public EgenNæringBuilder medNyoppstartet(boolean nyoppstartet) {
            this.entitet.setNyoppstartet(nyoppstartet);
            return this;
        }

        public EgenNæringBuilder medVarigEndring(boolean varigEndring) {
            this.entitet.setVarigEndring(varigEndring);
            return this;
        }

        public EgenNæringBuilder medNærRelasjon(boolean nærRelasjon) {
            this.entitet.setNærRelasjon(nærRelasjon);
            return this;
        }

        public EgenNæringBuilder medBruttoInntekt(BigDecimal bruttoInntekt) {
            this.entitet.setBruttoInntekt(bruttoInntekt);
            return this;
        }

        public EgenNæringBuilder medUtenlandskVirksomhet(OppgittUtenlandskVirksomhet utenlandskVirksomhet) {
            this.entitet.setUtenlandskVirksomhet(utenlandskVirksomhet);
            return this;
        }

        public OppgittEgenNæring build() {
            return entitet;
        }

        public EgenNæringBuilder medNyIArbeidslivet(boolean nyIArbeidslivet) {
            this.entitet.setNyIArbeidslivet(nyIArbeidslivet);
            return this;

        }

        public EgenNæringBuilder medVirksomhet(OrgNummer orgNr) {
            this.entitet.setVirksomhetOrgnr(orgNr);
            return this;
        }
    }

    public static class OppgittArbeidsforholdBuilder {
        private OppgittArbeidsforhold entitet;

        private OppgittArbeidsforholdBuilder(OppgittArbeidsforhold entitet) {
            this.entitet = entitet;
        }

        public static OppgittArbeidsforholdBuilder ny() {
            return new OppgittArbeidsforholdBuilder(new OppgittArbeidsforhold());
        }

        public OppgittArbeidsforholdBuilder medPeriode(DatoIntervallEntitet periode) {
            this.entitet.setPeriode(periode);
            return this;
        }

        public OppgittArbeidsforholdBuilder medErUtenlandskInntekt(Boolean erUtenlandskInntekt) {
            this.entitet.setErUtenlandskInntekt(erUtenlandskInntekt);
            return this;
        }

        public OppgittArbeidsforholdBuilder medArbeidType(ArbeidType arbeidType) {
            this.entitet.setArbeidType(arbeidType);
            return this;
        }

        public OppgittArbeidsforholdBuilder medUtenlandskVirksomhet(OppgittUtenlandskVirksomhet utenlandskVirksomhet) {
            this.entitet.setUtenlandskVirksomhet(utenlandskVirksomhet);
            return this;
        }

        public OppgittArbeidsforhold build() {
            return entitet;
        }
    }
}
